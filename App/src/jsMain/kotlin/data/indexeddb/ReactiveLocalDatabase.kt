package data.indexeddb

import data.indexeddb.LocalDatabase.*
import data.indexeddb.ReactiveLocalDatabase.*
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import utils.isolatedScope
import utils.launchUndispatched


interface ReactiveLocalDatabase : LocalDatabase {

    fun readWriteTransaction(
        vararg storeDefinitions: StoreDefinition<*, *>,
        context: Any?,
        durability: TransactionDurability?
    ): ReadWriteTransaction

    fun <PrimaryKey : Any, Value : Any> watch(
        storeDefinition: StoreDefinition<PrimaryKey, Value>,
    ): StoreWatcher<PrimaryKey, Value>

    class Change<T>(
        val context: Any?,
        val result: T
    )

    interface Watcher<Key : Any, PrimaryKey : Any, Value : Any> {
        fun all(range: KeyRange<Key>? = null): Flow<Change<List<Value>>>
        fun allKeys(range: KeyRange<Key>? = null): Flow<Change<List<Key>>>
        fun allEntries(range: KeyRange<Key>? = null): Flow<Change<Flow<ReadEntry<Key, PrimaryKey, Value>>>>
        fun allKeyEntries(range: KeyRange<Key>? = null): Flow<Change<Flow<ReadKeyEntry<Key, PrimaryKey>>>>
        fun count(range: KeyRange<Key>? = null): Flow<Change<Int>>
    }

    interface StoreWatcher<PrimaryKey : Any, Value : Any> : Watcher<PrimaryKey, PrimaryKey, Value> {
        fun value(key: PrimaryKey): Flow<Change<Value?>>
        fun <IndexKey : Any> index(index: IndexDefinition<Value, IndexKey>): Watcher<IndexKey, PrimaryKey, Value>
        fun <IndexElement : Any> index(index: MultiEntryIndexDefinition<Value, IndexElement>): Watcher<IndexElement, PrimaryKey, Value>
    }
}

fun LocalDatabase.reactive(): ReactiveLocalDatabase = ReactiveLocalDatabaseImpl(this)

private class ReactiveLocalDatabaseImpl(
    val db: LocalDatabase
) : ReactiveLocalDatabase, LocalDatabase by db {

    data class Mutation(
        val storeName: String,
        val primaryKey: Any,
        val value: Any?
    )

    data class Mutations(
        val mutations: List<Mutation>,
        val context: Any?
    )

    operator fun Mutations.contains(storeName: String): Boolean =
        mutations.any { it.storeName == storeName }

    val mutationsFlow = MutableSharedFlow<Mutations>()

    override fun readWriteTransaction(
        vararg storeDefinitions: StoreDefinition<*, *>,
        context: Any?,
        durability: TransactionDurability?
    ): ReadWriteTransaction =
        ReactiveReadWriteTransaction(
            transaction = db.readWriteTransaction(
                storeDefinitions = storeDefinitions,
                durability = durability
            ),
            context = context
        )

    override fun readWriteTransaction(
        vararg storeDefinitions: StoreDefinition<*, *>,
        durability: TransactionDurability?
    ): ReadWriteTransaction =
        readWriteTransaction(
            storeDefinitions = storeDefinitions,
            context = null,
            durability = durability
        )

    override fun <PrimaryKey : Any, Value : Any> watch(storeDefinition: StoreDefinition<PrimaryKey, Value>): StoreWatcher<PrimaryKey, Value> =
        ReactiveStoreWatcher(storeDefinition)

    open inner class ReactiveWatcher<Key : Any, PrimaryKey : Any, Value : Any>(
        val storeName: String,
        val getData: () -> ReadQueryable<Key, PrimaryKey, Value>,
        val isInRange: Mutation.(KeyRange<Key>) -> Boolean
    ) : Watcher<Key, PrimaryKey, Value> {

        private fun Mutations.affects(range: KeyRange<Key>?): Boolean =
            mutations.any {
                if (range == null) it.storeName == storeName
                else it.storeName == storeName && it.isInRange(range)
            }

        private fun <T> watch(
            range: KeyRange<Key>?,
            get: suspend ReadQueryable<Key, PrimaryKey, Value>.() -> T,
        ) : Flow<Change<T>> = flow {
            emit(Change(null, getData().get()))
            mutationsFlow.collect { mutations ->
                if (mutations.affects(range)) {
                    emit(Change(mutations.context, getData().get()))
                }
            }
        }

        override fun all(range: KeyRange<Key>?): Flow<Change<List<Value>>> =
            watch(range) { getAll(range) }

        override fun allKeys(range: KeyRange<Key>?): Flow<Change<List<Key>>> =
            watch(range) { getAllKeys(range) }

        override fun allEntries(range: KeyRange<Key>?): Flow<Change<Flow<ReadEntry<Key, PrimaryKey, Value>>>> =
            watch(range) { query(range).executeAsFlow() }

        override fun allKeyEntries(range: KeyRange<Key>?): Flow<Change<Flow<ReadKeyEntry<Key, PrimaryKey>>>> =
            watch(range) { queryKeys(range).executeAsKeyFlow() }

        override fun count(range: KeyRange<Key>?): Flow<Change<Int>> =
            watch(range) { count(range) }
    }

    @Suppress("UNCHECKED_CAST")
    inner class ReactiveStoreWatcher<PrimaryKey : Any, Value : Any>(
        val storeDefinition: StoreDefinition<PrimaryKey, Value>,
    ) : ReactiveWatcher<PrimaryKey, PrimaryKey, Value>(
        storeName = storeDefinition.name,
        getData = { db.readTransaction(storeDefinition).objectStore(storeDefinition) },
        isInRange = { range -> db.isKeyInRange(storeDefinition.primaryKeyType, primaryKey as PrimaryKey, range) }
    ), StoreWatcher<PrimaryKey, Value> {
        override fun value(key: PrimaryKey): Flow<Change<Value?>> = flow {
            emit(Change(null, db.readTransaction(storeDefinition).objectStore(storeDefinition).get(key)))
            mutationsFlow.collect { mutations ->
                val mutation = mutations.mutations.lastOrNull { it.storeName == storeName && it.primaryKey == key }
                if (mutation != null) {
                    emit(Change(mutations.context, mutation.value as Value?))
                }
            }
        }
        override fun <IndexKey : Any> index(index: IndexDefinition<Value, IndexKey>): Watcher<IndexKey, PrimaryKey, Value> =
            ReactiveWatcher(
                storeName = storeDefinition.name,
                getData = { db.readTransaction(storeDefinition).objectStore(storeDefinition).index(index) },
                isInRange = { range ->
                    value as Value?
                    if (value == null) true
                    else db.isKeyInRange(index.keyType, index.keyPath.get(value), range)
                }
            )

        override fun <IndexElement : Any> index(index: MultiEntryIndexDefinition<Value, IndexElement>): Watcher<IndexElement, PrimaryKey, Value> =
            ReactiveWatcher(
                storeName = storeDefinition.name,
                getData = { db.readTransaction(storeDefinition).objectStore(storeDefinition).index(index) },
                isInRange = { range ->
                    value as Value?
                    if (value == null) true
                    else index.keyPath.get(value).any { db.isKeyInRange(index.elementType, it, range) }
                }
            )
    }

    inner class ReactiveReadWriteTransaction(
        val transaction: ReadWriteTransaction,
        val context: Any?
    ) : ReadWriteTransaction by transaction {

        val transactionMutations = ArrayList<Mutation>()

        init {
            isolatedScope().launchUndispatched(SupervisorJob()) {
                transaction.awaitCompletion()
                mutationsFlow.emit(
                    Mutations(
                        mutations = transactionMutations,
                        context = context
                    )
                )
            }
        }

        fun <T> MutationRequest<T>.register(mutation: (T) -> Mutation): MutationRequest<T> {
            isolatedScope().launchUndispatched(SupervisorJob()) {
                val result = await()
                transactionMutations += mutation(result)
            }
            return this
        }

        override fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition.KeyPathStore<PrimaryKey, Value>): ReadWriteAutoObjectStore<PrimaryKey, Value> =
            ReactiveReadWriteAutoObjectStore(transaction.objectStore(storeDefinition))
        override fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition.ExtKeyStore<PrimaryKey, Value>): ReadWriteExtKeyObjectStore<PrimaryKey, Value> =
            ReactiveReadWriteExtKeyObjectStore(transaction.objectStore(storeDefinition))
        override fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition.AutoExtKeyStore<PrimaryKey, Value>): ReadWriteAutoExtKeyObjectStore<PrimaryKey, Value> =
            ReactiveReadWriteAutoExtKeyObjectStore(transaction.objectStore(storeDefinition))

        open inner class ReactiveReadWriteObjectStore<PrimaryKey : Any, Value : Any>(
            open val store: ReadWriteObjectStore<PrimaryKey, Value>
        ) : ReadWriteObjectStore<PrimaryKey, Value> by store {
            override fun delete(key: PrimaryKey): MutationRequest<Unit> =
                store.delete(key).register { Mutation(store.name, key, null) }
            override fun <IndexKey : Any> index(indexDefinition: IndexDefinition<Value, IndexKey>): ReadWriteIndex<Value, IndexKey, PrimaryKey> =
                ReactiveReadWriteIndex(store.index(indexDefinition))
            override fun <IndexElement : Any> index(indexDefinition: MultiEntryIndexDefinition<Value, IndexElement>): ReadWriteIndex<Value, IndexElement, PrimaryKey> =
                ReactiveReadWriteIndex(store.index(indexDefinition))
            override fun query(range: KeyRange<PrimaryKey>?): Cursor<ReadWriteEntry<PrimaryKey, PrimaryKey, Value>> =
                store.query(range).map { ReactiveReadWriteEntry(it, store.name) }
        }

        open inner class ReactiveReadWriteAutoObjectStore<PrimaryKey : Any, Value : Any>(
            override val store: ReadWriteAutoObjectStore<PrimaryKey, Value>
        ) : ReactiveReadWriteObjectStore<PrimaryKey, Value>(store), ReadWriteAutoObjectStore<PrimaryKey, Value> {
            override fun add(value: Value): MutationRequest<PrimaryKey> =
                store.add(value).register { Mutation(store.name, it, value) }
            override fun put(value: Value): MutationRequest<PrimaryKey> =
                store.put(value).register { Mutation(store.name, it, value) }
        }

        open inner class ReactiveReadWriteExtKeyObjectStore<PrimaryKey : Any, Value : Any>(
            override val store: ReadWriteExtKeyObjectStore<PrimaryKey, Value>
        ) : ReactiveReadWriteObjectStore<PrimaryKey, Value>(store), ReadWriteExtKeyObjectStore<PrimaryKey, Value> {
            override fun add(key: PrimaryKey, value: Value): MutationRequest<PrimaryKey> =
                store.add(key, value).register { Mutation(store.name, it, value) }
            override fun put(key: PrimaryKey, value: Value) =
                store.put(key, value).register { Mutation(store.name, it, value) }
        }

        inner class ReactiveReadWriteAutoExtKeyObjectStore<PrimaryKey : Any, Value : Any>(
            override val store: ReadWriteAutoExtKeyObjectStore<PrimaryKey, Value>
        ) : ReactiveReadWriteExtKeyObjectStore<PrimaryKey, Value>(store), ReadWriteAutoExtKeyObjectStore<PrimaryKey, Value> {
            override fun add(value: Value): MutationRequest<PrimaryKey> =
                store.add(value).register { Mutation(store.name, it, value) }
            override fun put(value: Value): MutationRequest<PrimaryKey> =
                store.put(value).register { Mutation(store.name, it, value) }
        }

        inner class ReactiveReadWriteIndex<Value : Any, IndexKey : Any, PrimaryKey : Any>(
            val index: ReadWriteIndex<Value, IndexKey, PrimaryKey>
        ) : ReadWriteIndex<Value, IndexKey, PrimaryKey> by index {
            override fun query(range: KeyRange<IndexKey>?): Cursor<ReadWriteEntry<IndexKey, PrimaryKey, Value>> =
                index.query(range).map { ReactiveReadWriteEntry(it, index.store.name) }
        }

        inner class ReactiveReadWriteEntry<Key : Any, PrimaryKey : Any, Value : Any>(
            val entry: ReadWriteEntry<Key, PrimaryKey, Value>,
            val storeName: String
        ) : ReadWriteEntry<Key, PrimaryKey, Value> by entry {
            override val value: Value get() = entry.value
            override fun delete(): MutationRequest<Unit> =
                entry.delete().register { Mutation(storeName, entry.key, null) }
            override fun update(value: Value): MutationRequest<PrimaryKey> =
                entry.update(value).register { Mutation(storeName, it, value) }
        }
    }
}

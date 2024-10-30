package data.indexeddb

import data.indexeddb.LocalDatabase.*
import data.indexeddb.LocalDatabaseImpl.MigrationTransactionImpl
import data.indexeddb.LocalDatabaseImpl.MutationRequestImpl
import js.array.ReadonlyArray
import js.objects.jso
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromDynamic
import kotlinx.serialization.json.encodeToDynamic
import web.events.Event
import web.events.addEventHandler
import web.idb.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


internal object LocalDatabaseFactory : Factory {
    override suspend fun open(
        name: String,
        version: Int,
        migrate: MigrationTransaction.(MigrationTransaction.Versions) -> Unit
    ): LocalDatabase {
        val openRequest = indexedDB.open(
            name = name,
            version = version.toDouble(),
        )
        return suspendCancellableCoroutine { continuation ->
            openRequest.addEventHandler(Event.success()) { event ->
                continuation.resume(LocalDatabaseImpl(event.currentTarget.result))
            }
            openRequest.addEventHandler(Event.error()) { event ->
                continuation.resumeWithException(OpenError(event.currentTarget.error?.message))
            }
            openRequest.addEventHandler(IDBVersionChangeEvent.upgradeneeded()) { event ->
                MigrationTransactionImpl(
                    db = event.currentTarget.result,
                    idbTransaction = event.currentTarget.transaction!!
                ).migrate(MigrationTransaction.Versions(
                    oldVersion = event.oldVersion.toInt(),
                    newVersion = event.newVersion!!.toInt()
                ))
            }
        }
    }

    override suspend fun delete(name: String) {
        indexedDB.deleteDatabase(name).await()
    }
}

private suspend fun <T> IDBRequest<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addEventHandler(Event.success()) { event ->
        continuation.resume(event.currentTarget.result)
    }
    addEventHandler(Event.error()) { event ->
        continuation.resumeWithException(RequestError(event.currentTarget.error?.message ?: "Unknown error"))
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> KeyType<T>.toIdbKey(key: T): IDBValidKey =
    when (this) {
        is KeyType.OfInt -> key.unsafeCast<IDBValidKey>()
        is KeyType.OfLong -> (key as Long).toDouble().unsafeCast<IDBValidKey>()
        is KeyType.OfString -> key.unsafeCast<IDBValidKey>()
        is KeyType.OfBufferSource -> key.unsafeCast<IDBValidKey>()
        is KeyType.OfPair<*, *> -> {
            key as Pair<Any, Any>
            arrayOf(types.first.toIdbKey(key.first), types.second.toIdbKey(key.second)).unsafeCast<IDBValidKey>()
        }
        is KeyType.OfTriple<*, *, *> -> {
            key as Triple<Any, Any, Any>
            arrayOf(types.first.toIdbKey(key.first), types.second.toIdbKey(key.second), types.third.toIdbKey(key.third)).unsafeCast<IDBValidKey>()
        }
        is KeyType.OfList -> {
            key as List<Any>
            require(key.size == types.size)
            key.mapIndexed { index, subKey -> types[index].toIdbKey(subKey) }.toTypedArray().unsafeCast<IDBValidKey>()
        }
        is KeyType.OfWrap<*, *> -> {
            this as KeyType.OfWrap<T, Any>
            type.toIdbKey(unwrap(key))
        }
    }

@Suppress("UNCHECKED_CAST")
private fun <T : Any> KeyType<T>.fromIdbKey(key: IDBValidKey): T =
    when (this) {
        is KeyType.OfInt -> key.unsafeCast<T>()
        is KeyType.OfLong -> key.unsafeCast<Number>().toLong().unsafeCast<T>()
        is KeyType.OfString -> key.unsafeCast<T>()
        is KeyType.OfBufferSource -> key.unsafeCast<T>()
        is KeyType.OfPair<*, *> -> {
            val keys = key.unsafeCast<ReadonlyArray<IDBValidKey>>()
            Pair(types.first.fromIdbKey(keys[0]), types.second.fromIdbKey(keys[1])) as T
        }
        is KeyType.OfTriple<*, *, *> -> {
            val keys = key.unsafeCast<ReadonlyArray<IDBValidKey>>()
            Triple(types.first.fromIdbKey(keys[0]), types.second.fromIdbKey(keys[1]), types.third.fromIdbKey(keys[2])) as T
        }
        is KeyType.OfList -> {
            val keys = key.unsafeCast<ReadonlyArray<IDBValidKey>>()
            keys.mapIndexed { index, subKey -> types[index].fromIdbKey(subKey) } as T
        }
        is KeyType.OfWrap<*, *> -> {
            this as KeyType.OfWrap<T, Any>
            wrap(type.fromIdbKey(key))
        }
    }

private fun <T : Any> KeyType<T>.toIdbKeyRange(range: KeyRange<T>): IDBKeyRange =
    when (range) {
        is KeyRange.Only -> IDBKeyRange.only(toIdbKey(range.only))
        is KeyRange.LowerBound -> IDBKeyRange.lowerBound(toIdbKey(range.lower))
        is KeyRange.UpperBound -> IDBKeyRange.upperBound(toIdbKey(range.upper))
        is KeyRange.Bound -> IDBKeyRange.bound(toIdbKey(range.lower), toIdbKey(range.upper), range.excludesLower, range.excludesUpper)
    }

private fun <I, T> IDBRequest<I>.asMutationRequest(transform: (I) -> T): MutationRequest<T> = MutationRequestImpl(this, transform)

private val json = Json {
    ignoreUnknownKeys = true
}

private fun IDBObjectStore.createIndex(name: String, keyPath: KeyPath<*, *>, options: IDBIndexParameters): IDBIndex =
    when (keyPath) {
        is SingleKeyPath -> createIndex(name, keyPath.path, options)
        is MultiKeyPath -> createIndex(name, keyPath.path, options)
    }

@OptIn(ExperimentalSerializationApi::class)
private open class LocalDatabaseImpl(val db: IDBDatabase) : LocalDatabase {

    open class ReadTransactionImpl(val transaction: IDBTransaction): ReadTransaction {
        override fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition<PrimaryKey, Value>): ReadObjectStore<PrimaryKey, Value> =
            ReadObjectStoreImpl(storeDefinition, transaction.objectStore(storeDefinition.name))
    }

    class MutationRequestImpl<I, T>(
        idbRequest: IDBRequest<I>,
        transform: (I) -> T
    ) : MutationRequest<T> {
        val deferred = CompletableDeferred<T>()

        init {
            idbRequest.addEventHandler(Event.success()) { event ->
                deferred.complete(transform(event.currentTarget.result))
            }
            idbRequest.addEventHandler(Event.error()) { event ->
                deferred.completeExceptionally(RequestError(event.currentTarget.error?.message ?: "Unknown error"))
            }
        }

        override suspend fun await(): T = deferred.await()
    }

    open class ReadWriteTransactionImpl(transaction: IDBTransaction): ReadTransactionImpl(transaction), ReadWriteTransaction {
        private val job = Job()

        init {
            transaction.addEventHandler(Event.complete()) {
                job.complete()
            }
            transaction.addEventHandler(Event.error()) { event ->
                val error = event.currentTarget.error
                if (error != null) {
                    job.completeExceptionally(TransactionError(error.message))
                }
            }
            transaction.addEventHandler(Event.abort()) {
                if (job.isActive) {
                    job.completeExceptionally(TransactionError("Transaction aborted"))
                }
            }
        }

        override fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition.KeyPathStore<PrimaryKey, Value>): ReadWriteAutoObjectStore<PrimaryKey, Value> =
            ReadWriteAutoObjectStoreImpl(storeDefinition, transaction.objectStore(storeDefinition.name))

        override fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition.ExtKeyStore<PrimaryKey, Value>): ReadWriteExtKeyObjectStore<PrimaryKey, Value> =
            ReadWriteExtKeyObjectStoreImpl(storeDefinition, transaction.objectStore(storeDefinition.name))

        override fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition.AutoExtKeyStore<PrimaryKey, Value>): ReadWriteAutoExtKeyObjectStore<PrimaryKey, Value> =
            ReadWriteAutoExtKeyObjectStoreImpl(storeDefinition, transaction.objectStore(storeDefinition.name))

        override suspend fun awaitCompletion() {
            job.join()
        }
    }

    open class ReadObjectStoreImpl<PrimaryKey : Any, Value : Any>(
        open val storeDefinition: StoreDefinition<PrimaryKey, Value>,
        val idbStore: IDBObjectStore
    ): ReadObjectStore<PrimaryKey, Value> {

        override val name get() = idbStore.name

        private suspend fun idbGet(idbKey: IDBValidKey): Value? {
            val dao = idbStore[idbKey].await() ?: return null
            return json.decodeFromDynamic(storeDefinition.serializer, dao.asDynamic())
        }

        override suspend fun get(key: PrimaryKey): Value? = idbGet(storeDefinition.primaryKeyType.toIdbKey(key))

        override suspend fun getAll(range: KeyRange<PrimaryKey>?, count: Int?): List<Value> =
            idbStore.getAll(
                query = range?.let { storeDefinition.primaryKeyType.toIdbKeyRange(it) },
                count = count.unsafeCast<Int>()
            ).await().map { dao -> json.decodeFromDynamic(storeDefinition.serializer, dao.asDynamic()) }

        override suspend fun getAllKeys(range: KeyRange<PrimaryKey>?, count: Int?): List<PrimaryKey> =
            idbStore.getAllKeys(
                query = range?.let { storeDefinition.primaryKeyType.toIdbKeyRange(it) },
                count = count.unsafeCast<Int>()
            ).await().map { key -> storeDefinition.primaryKeyType.fromIdbKey(key) }

        override suspend fun count(range: KeyRange<PrimaryKey>?): Int =
            idbStore.count(
                query = range?.let { storeDefinition.primaryKeyType.toIdbKeyRange(it) }.unsafeCast<IDBKeyRange>()
            ).await()

        override fun query(range: KeyRange<PrimaryKey>?): Cursor<ReadEntry<PrimaryKey, PrimaryKey, Value>> =
            CursorImpl(
                openCursor = {
                    idbStore.openCursor(
                        query = range?.let { storeDefinition.primaryKeyType.toIdbKeyRange(it) }
                    )
                },
                wrap = { ReadEntryImpl(it, storeDefinition.primaryKeyType, storeDefinition.primaryKeyType, storeDefinition.serializer) }
            )

        override fun queryKeys(range: KeyRange<PrimaryKey>?): Cursor<ReadKeyEntry<PrimaryKey, PrimaryKey>> =
            CursorImpl(
                openCursor = {
                    idbStore.openKeyCursor(
                        query = range?.let { storeDefinition.primaryKeyType.toIdbKeyRange(it) }
                    )
                },
                wrap = { ReadKeyEntryImpl(it, storeDefinition.primaryKeyType, storeDefinition.primaryKeyType) }
            )

        override fun <IndexKey : Any> index(indexDefinition: IndexDefinition<Value, IndexKey>): ReadIndex<Value, IndexKey, PrimaryKey> =
            ReadIndexImpl(this, indexDefinition.keyType, idbStore.index(indexDefinition.name))

        override fun <IndexElement : Any> index(indexDefinition: MultiEntryIndexDefinition<Value, IndexElement>): ReadIndex<Value, IndexElement, PrimaryKey> =
            ReadIndexImpl(this, indexDefinition.elementType, idbStore.index(indexDefinition.name))
    }

    open class ReadWriteObjectStoreImpl<PrimaryKey : Any, Value : Any>(
        storeDefinition: StoreDefinition<PrimaryKey, Value>,
        idbStore: IDBObjectStore,
    ) : ReadObjectStoreImpl<PrimaryKey, Value>(storeDefinition, idbStore), ReadWriteObjectStore<PrimaryKey, Value> {

        override fun delete(key: PrimaryKey): MutationRequest<Unit> {
            val idbKey = storeDefinition.primaryKeyType.toIdbKey(key)
            return idbStore.delete(idbKey).asMutationRequest {}
        }

        override fun query(range: KeyRange<PrimaryKey>?): Cursor<ReadWriteEntry<PrimaryKey, PrimaryKey, Value>> =
            CursorImpl(
                openCursor = {
                    idbStore.openCursor(
                        query = range?.let { storeDefinition.primaryKeyType.toIdbKeyRange(it) }
                    )
                },
                wrap = { ReadWriteEntryImpl(it, storeDefinition.primaryKeyType, storeDefinition.primaryKeyType, storeDefinition.serializer) }
            )

        override fun <IndexKey : Any> index(indexDefinition: IndexDefinition<Value, IndexKey>): ReadWriteIndex<Value, IndexKey, PrimaryKey> =
            ReadWriteIndexImpl(this, indexDefinition.keyType, idbStore.index(indexDefinition.name))

        override fun <IndexKey : Any> index(indexDefinition: MultiEntryIndexDefinition<Value, IndexKey>): ReadWriteIndex<Value, IndexKey, PrimaryKey> =
            ReadWriteIndexImpl(this, indexDefinition.elementType, idbStore.index(indexDefinition.name))
    }

    open class ReadWriteAutoObjectStoreImpl<PrimaryKey : Any, Value : Any>(
        override val storeDefinition: StoreDefinition.KeyPathStore<PrimaryKey, Value>,
        idbStore: IDBObjectStore,
    ) : ReadWriteObjectStoreImpl<PrimaryKey, Value>(storeDefinition, idbStore), ReadWriteAutoObjectStore<PrimaryKey, Value> {

        override fun add(value: Value): MutationRequest<PrimaryKey> {
            val dao = json.encodeToDynamic(storeDefinition.serializer, value)
            return idbStore.add(dao).asMutationRequest { storeDefinition.primaryKeyType.fromIdbKey(it) }
        }

        override fun put(value: Value): MutationRequest<PrimaryKey> {
            val dao = json.encodeToDynamic(storeDefinition.serializer, value)
            return idbStore.put(dao).asMutationRequest { storeDefinition.primaryKeyType.fromIdbKey(it) }
        }
    }

    open class ReadWriteExtKeyObjectStoreImpl<PrimaryKey : Any, Value : Any>(
        override val storeDefinition: StoreDefinition.ExtKeyStore<PrimaryKey, Value>,
        idbStore: IDBObjectStore,
    ) : ReadWriteObjectStoreImpl<PrimaryKey, Value>(storeDefinition, idbStore), ReadWriteExtKeyObjectStore<PrimaryKey, Value> {

        override fun add(key: PrimaryKey, value: Value): MutationRequest<PrimaryKey> {
            val dao = json.encodeToDynamic(storeDefinition.serializer, value)
            return idbStore.add(value = dao, key = storeDefinition.primaryKeyType.toIdbKey(key)).asMutationRequest { storeDefinition.primaryKeyType.fromIdbKey(it) }
        }

        override fun put(key: PrimaryKey, value: Value): MutationRequest<PrimaryKey> {
            val dao = json.encodeToDynamic(storeDefinition.serializer, value)
            return idbStore.put(value = dao, key = storeDefinition.primaryKeyType.toIdbKey(key)).asMutationRequest { storeDefinition.primaryKeyType.fromIdbKey(it) }
        }
    }

    open class ReadWriteAutoExtKeyObjectStoreImpl<PrimaryKey : Any, Value : Any>(
        storeDefinition: StoreDefinition.AutoExtKeyStore<PrimaryKey, Value>,
        idbStore: IDBObjectStore,
    ) : ReadWriteExtKeyObjectStoreImpl<PrimaryKey, Value>(storeDefinition, idbStore), ReadWriteAutoExtKeyObjectStore<PrimaryKey, Value> {

        override fun add(value: Value): MutationRequest<PrimaryKey> {
            val dao = json.encodeToDynamic(storeDefinition.serializer, value)
            return idbStore.add(dao).asMutationRequest { storeDefinition.primaryKeyType.fromIdbKey(it) }
        }

        override fun put(value: Value): MutationRequest<PrimaryKey> {
            val dao = json.encodeToDynamic(storeDefinition.serializer, value)
            return idbStore.put(dao).asMutationRequest { storeDefinition.primaryKeyType.fromIdbKey(it) }
        }
    }

    open class ReadIndexImpl<Value : Any, IndexKey : Any, PrimaryKey : Any>(
        override val store: ReadObjectStoreImpl<PrimaryKey, Value>,
        val indexKeyType: KeyType<IndexKey>,
        val idbIndex: IDBIndex
    ) : ReadIndex<Value, IndexKey, PrimaryKey> {

        override suspend fun getAll(range: KeyRange<IndexKey>?, count: Int?): List<Value> =
            idbIndex.getAll(
                query = range?.let { indexKeyType.toIdbKeyRange(it) },
                count = count.unsafeCast<Int>()
            ).await().map { dao -> json.decodeFromDynamic(store.storeDefinition.serializer, dao.asDynamic()) }

        override suspend fun getAllKeys(range: KeyRange<IndexKey>?, count: Int?): List<IndexKey> =
            idbIndex.getAllKeys(
                query = range?.let { indexKeyType.toIdbKeyRange(it) },
                count = count.unsafeCast<Int>()
            ).await().map { key -> indexKeyType.fromIdbKey(key) }

        override suspend fun count(range: KeyRange<IndexKey>?): Int =
            idbIndex.count(
                query = range?.let { indexKeyType.toIdbKeyRange(it) }.unsafeCast<IDBKeyRange>()
            ).await()

        override fun query(range: KeyRange<IndexKey>?): Cursor<ReadEntry<IndexKey, PrimaryKey, Value>> =
            CursorImpl(
                openCursor = {
                    idbIndex.openCursor(
                        query = range?.let { indexKeyType.toIdbKeyRange(it) }
                    )
                },
                wrap = { ReadEntryImpl(it, indexKeyType, store.storeDefinition.primaryKeyType, store.storeDefinition.serializer) }
            )

        override fun queryKeys(range: KeyRange<IndexKey>?): Cursor<ReadKeyEntry<IndexKey, PrimaryKey>> =
            CursorImpl(
                openCursor = {
                    idbIndex.openKeyCursor(
                        query = range?.let { indexKeyType.toIdbKeyRange(it) }
                    )
                },
                wrap = { ReadKeyEntryImpl(it, indexKeyType, store.storeDefinition.primaryKeyType) }
            )

    }

    class ReadWriteIndexImpl<Value : Any, IndexKey : Any, PrimaryKey : Any>(
        override val store: ReadWriteObjectStoreImpl<PrimaryKey, Value>,
        indexKeyType: KeyType<IndexKey>,
        idbIndex: IDBIndex,
    ) : ReadIndexImpl<Value, IndexKey, PrimaryKey>(store, indexKeyType, idbIndex), ReadWriteIndex<Value, IndexKey, PrimaryKey> {

        override fun query(range: KeyRange<IndexKey>?): Cursor<ReadWriteEntry<IndexKey, PrimaryKey, Value>> =
            CursorImpl(
                openCursor = {
                    idbIndex.openCursor(
                        query = range?.let { indexKeyType.toIdbKeyRange(it) }
                    )
                },
                wrap = { ReadWriteEntryImpl(it, indexKeyType, store.storeDefinition.primaryKeyType, store.storeDefinition.serializer) }
            )
    }

    class CursorImpl<C : IDBCursor, out R : ReadKeyEntry<*, *>>(
        val openCursor: () -> IDBRequest<C?>,
        val wrap: (C) -> R
    ) : Cursor<R> {
        override suspend fun executeOnEach(action: suspend (R) -> Boolean) {
            coroutineScope {
                val idbRequest = openCursor()
                val job = Job()
                idbRequest.addEventHandler(Event.success()) { event ->
                    val cursor = event.currentTarget.result
                    if (cursor != null) {
                        launch {
                            val advance = action(wrap(cursor))
                            if (advance) {
                                cursor.`continue`()
                            } else {
                                job.complete()
                            }
                        }
                    } else {
                        job.complete()
                    }
                }
                idbRequest.addEventHandler(Event.error()) { event ->
                    job.completeExceptionally(CursorError(event.currentTarget.error?.message ?: "Unknown error"))
                }
                job.join()
            }
        }
    }

    open class ReadKeyEntryImpl<Key : Any, PrimaryKey : Any>(
        open val cursor : IDBCursor,
        val keyType: KeyType<Key>,
        val primaryKeyType: KeyType<PrimaryKey>
    ) : ReadKeyEntry<Key, PrimaryKey> {
        override val key: Key get() = keyType.fromIdbKey(cursor.key)
        override val primaryKey: PrimaryKey get() = primaryKeyType.fromIdbKey(cursor.primaryKey)
    }

    open class ReadEntryImpl<Key : Any, PrimaryKey : Any, Value : Any>(
        override val cursor : IDBCursorWithValue,
        keyType: KeyType<Key>,
        primaryKeyType: KeyType<PrimaryKey>,
        val serializer: KSerializer<Value>
    ) : ReadKeyEntryImpl<Key, PrimaryKey>(cursor, keyType, primaryKeyType), ReadEntry<Key, PrimaryKey, Value> {
        override val value: Value get() = Json.decodeFromDynamic(serializer, cursor.value.asDynamic())
    }

    class ReadWriteEntryImpl<Key : Any, PrimaryKey : Any, Value : Any>(
        override val cursor : IDBCursorWithValue,
        keyType: KeyType<Key>,
        primaryKeyType: KeyType<PrimaryKey>,
        serializer: KSerializer<Value>
    ) : ReadEntryImpl<Key, PrimaryKey, Value>(cursor, keyType, primaryKeyType, serializer), ReadWriteEntry<Key, PrimaryKey, Value> {
        override fun delete(): MutationRequest<Unit> = cursor.delete().asMutationRequest {}
        override fun update(value: Value): MutationRequest<PrimaryKey> =
            cursor.update(json.encodeToDynamic(serializer, value)).asMutationRequest { primaryKeyType.fromIdbKey(it) }
    }

    private fun IDBTransactionOptions(durability: TransactionDurability?): IDBTransactionOptions =
        jso {
            if (durability != null) {
                this.durability = when (durability) {
                    TransactionDurability.Default -> IDBTransactionDurability.default
                    TransactionDurability.Relaxed -> IDBTransactionDurability.relaxed
                    TransactionDurability.Strict -> IDBTransactionDurability.strict
                }
            }
        }

    override fun readTransaction(vararg storeDefinitions: StoreDefinition<*, *>): ReadTransaction =
        ReadTransactionImpl(
            db.transaction(
                storeNames = storeDefinitions.map { it.name }.toTypedArray(),
                mode = IDBTransactionMode.readonly,
            )
        )

    override fun readWriteTransaction(vararg storeDefinitions: StoreDefinition<*, *>, durability: TransactionDurability?): ReadWriteTransaction =
        ReadWriteTransactionImpl(
            db.transaction(
                storeNames = storeDefinitions.map { it.name }.toTypedArray(),
                mode = IDBTransactionMode.readwrite,
                options = IDBTransactionOptions(durability)
            )
        )

    override fun <Key : Any> isKeyInRange(keyType: KeyType<Key>, key: Key, range: KeyRange<Key>): Boolean =
        keyType.toIdbKeyRange(range).includes(keyType.toIdbKey(key))

    class MigrationTransactionImpl(
        val db: IDBDatabase,
        val idbTransaction: IDBTransaction
    ) : MigrationTransaction {
        override fun <PrimaryKey : Any, Value : Any> createObjectStore(storeDefinition: StoreDefinition.KeyPathStore<PrimaryKey, Value>): MigrationReadWriteAutoObjectStore<PrimaryKey, Value> =
            MigrationReadWriteAutoObjectStoreImpl(storeDefinition, db.createObjectStore(storeDefinition.name, jso {
                keyPath = when (val keyPath = storeDefinition.primaryKeyPath) {
                    is SingleKeyPath -> keyPath.path
                    is MultiKeyPath -> keyPath.path
                }
                autoIncrement = storeDefinition is StoreDefinition.AutoKeyPathStore
            }))
        override fun <PrimaryKey : Any, Value : Any> createObjectStore(storeDefinition: StoreDefinition.ExtKeyStore<PrimaryKey, Value>): MigrationReadWriteExtKeyObjectStore<PrimaryKey, Value> =
            MigrationReadWriteExtKeyObjectStoreImpl(storeDefinition, db.createObjectStore(storeDefinition.name, jso {
                keyPath = null
                autoIncrement = false
            }))
        override fun <PrimaryKey : Any, Value : Any> createObjectStore(storeDefinition: StoreDefinition.AutoExtKeyStore<PrimaryKey, Value>): MigrationReadWriteAutoExtKeyObjectStore<PrimaryKey, Value> =
            MigrationReadWriteAutoExtKeyObjectStoreImpl(storeDefinition, db.createObjectStore(storeDefinition.name, jso {
                keyPath = null
                autoIncrement = true
            }))
        override fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition<PrimaryKey, Value>): MigrationReadObjectStore<PrimaryKey, Value> =
            MigrationReadObjectStoreImpl(storeDefinition, idbTransaction.objectStore(storeDefinition.name))
        override fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition.KeyPathStore<PrimaryKey, Value>): MigrationReadWriteAutoObjectStore<PrimaryKey, Value> =
            MigrationReadWriteAutoObjectStoreImpl(storeDefinition, idbTransaction.objectStore(storeDefinition.name))
        override fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition.ExtKeyStore<PrimaryKey, Value>): MigrationReadWriteExtKeyObjectStore<PrimaryKey, Value> =
            MigrationReadWriteExtKeyObjectStoreImpl(storeDefinition, idbTransaction.objectStore(storeDefinition.name))
        override fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition.AutoExtKeyStore<PrimaryKey, Value>): MigrationReadWriteAutoExtKeyObjectStore<PrimaryKey, Value> =
            MigrationReadWriteAutoExtKeyObjectStoreImpl(storeDefinition, idbTransaction.objectStore(storeDefinition.name))
        override fun deleteObjectStore(storeName: String) = db.deleteObjectStore(storeName)
    }

    class MigrationReadObjectStoreImpl<PrimaryKey : Any, Value : Any>(
        storeDefinition: StoreDefinition<PrimaryKey, Value>,
        idbStore: IDBObjectStore
    ) : ReadObjectStoreImpl<PrimaryKey, Value>(storeDefinition, idbStore), MigrationReadObjectStore<PrimaryKey, Value> {
        override fun <IndexKey : Any> createIndex(indexDefinition: IndexDefinition<Value, IndexKey>): ReadIndex<Value, IndexKey, PrimaryKey> {
            val idbIndex = idbStore.createIndex(indexDefinition.name, indexDefinition.keyPath, jso {
                unique = indexDefinition.isUnique
            })
            return ReadIndexImpl(this, indexDefinition.keyType, idbIndex)
        }
        override fun <IndexElement : Any> createIndex(indexDefinition: MultiEntryIndexDefinition<Value, IndexElement>): ReadIndex<Value, IndexElement, PrimaryKey> {
            val idbIndex = idbStore.createIndex(indexDefinition.name, indexDefinition.keyPath, jso {
                unique = indexDefinition.isUnique
            })
            return ReadIndexImpl(this, indexDefinition.elementType, idbIndex)
        }
        override fun deleteIndex(name: String) = idbStore.deleteIndex(name)
    }

    private object MigrationObjectStoreImpl {
        fun <PrimaryKey : Any, Value : Any, IndexKey : Any> createIndex(
            store: ReadWriteObjectStoreImpl<PrimaryKey, Value>,
            indexDefinition: IndexDefinition<Value, IndexKey>
        ): ReadWriteIndex<Value, IndexKey, PrimaryKey> {
            val idbIndex = store.idbStore.createIndex(indexDefinition.name, indexDefinition.keyPath, jso {
                unique = indexDefinition.isUnique
            })
            return ReadWriteIndexImpl(store, indexDefinition.keyType, idbIndex)
        }

        fun <PrimaryKey : Any, Value : Any, IndexKey : Any> createIndex(
            store: ReadWriteObjectStoreImpl<PrimaryKey, Value>,
            indexDefinition: MultiEntryIndexDefinition<Value, IndexKey>
        ): ReadWriteIndex<Value, IndexKey, PrimaryKey> {
            val idbIndex = store.idbStore.createIndex(indexDefinition.name, indexDefinition.keyPath, jso {
                unique = indexDefinition.isUnique
                multiEntry = true
            })
            return ReadWriteIndexImpl(store, indexDefinition.elementType, idbIndex)
        }
    }

    class MigrationReadWriteAutoObjectStoreImpl<PrimaryKey : Any, Value : Any>(
        storeDefinition: StoreDefinition.KeyPathStore<PrimaryKey, Value>,
        idbStore: IDBObjectStore
    ) : ReadWriteAutoObjectStoreImpl<PrimaryKey, Value>(storeDefinition, idbStore), MigrationReadWriteAutoObjectStore<PrimaryKey, Value> {
        override fun <IndexKey : Any> createIndex(indexDefinition: IndexDefinition<Value, IndexKey>): ReadWriteIndex<Value, IndexKey, PrimaryKey> =
            MigrationObjectStoreImpl.createIndex(this, indexDefinition)
        override fun <IndexElement : Any> createIndex(indexDefinition: MultiEntryIndexDefinition<Value, IndexElement>): ReadWriteIndex<Value, IndexElement, PrimaryKey> =
            MigrationObjectStoreImpl.createIndex(this, indexDefinition)
        override fun deleteIndex(name: String) = idbStore.deleteIndex(name)
    }

    class MigrationReadWriteExtKeyObjectStoreImpl<PrimaryKey : Any, Value : Any>(
        storeDefinition: StoreDefinition.ExtKeyStore<PrimaryKey, Value>,
        idbStore: IDBObjectStore
    ) : ReadWriteExtKeyObjectStoreImpl<PrimaryKey, Value>(storeDefinition, idbStore), MigrationReadWriteExtKeyObjectStore<PrimaryKey, Value> {
        override fun <IndexKey : Any> createIndex(indexDefinition: IndexDefinition<Value, IndexKey>): ReadWriteIndex<Value, IndexKey, PrimaryKey> =
            MigrationObjectStoreImpl.createIndex(this, indexDefinition)
        override fun <IndexElement : Any> createIndex(indexDefinition: MultiEntryIndexDefinition<Value, IndexElement>): ReadWriteIndex<Value, IndexElement, PrimaryKey> =
            MigrationObjectStoreImpl.createIndex(this, indexDefinition)
        override fun deleteIndex(name: String) = idbStore.deleteIndex(name)
    }

    class MigrationReadWriteAutoExtKeyObjectStoreImpl<PrimaryKey : Any, Value : Any>(
        storeDefinition: StoreDefinition.AutoExtKeyStore<PrimaryKey, Value>,
        idbStore: IDBObjectStore
    ) : ReadWriteAutoExtKeyObjectStoreImpl<PrimaryKey, Value>(storeDefinition, idbStore), MigrationReadWriteAutoExtKeyObjectStore<PrimaryKey, Value> {
        override fun <IndexKey : Any> createIndex(indexDefinition: IndexDefinition<Value, IndexKey>): ReadWriteIndex<Value, IndexKey, PrimaryKey> =
            MigrationObjectStoreImpl.createIndex(this, indexDefinition)
        override fun <IndexElement : Any> createIndex(indexDefinition: MultiEntryIndexDefinition<Value, IndexElement>): ReadWriteIndex<Value, IndexElement, PrimaryKey> =
            MigrationObjectStoreImpl.createIndex(this, indexDefinition)
        override fun deleteIndex(name: String) = idbStore.deleteIndex(name)
    }
}

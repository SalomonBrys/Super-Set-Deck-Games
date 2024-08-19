package data.indexeddb

import js.buffer.BufferSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.KSerializer
import kotlin.reflect.KProperty1
import kotlin.collections.Collection


sealed interface LocalDatabase {
    sealed class Error(msg: String?) : Exception(msg)
    class OpenError(msg: String?) : Error(msg)
    class TransactionError(msg: String?) : Error(msg)
    class RequestError(msg: String?) : Error(msg)
    class CursorError(msg: String?) : Error(msg)

    interface Factory {
        suspend fun open(
            name: String,
            version: Int,
            migrate: MigrationTransaction.(MigrationTransaction.Versions) -> Unit
        ): LocalDatabase
        suspend fun delete(name: String)
    }

    companion object : Factory by LocalDatabaseFactory

    sealed interface StoreDefinition<PrimaryKey : Any, Value : Any> {
        val name: String
        val serializer: KSerializer<Value>
        val primaryKeyType: KeyType<PrimaryKey>

        interface KeyPathStore<PrimaryKey : Any, Value : Any> : StoreDefinition<PrimaryKey, Value> {
            val primaryKeyPath: KeyPath<Value, PrimaryKey>
        }
        interface AutoKeyPathStore<PrimaryKey : Any, Value : Any> : KeyPathStore<PrimaryKey, Value>, StoreDefinition<PrimaryKey, Value>
        interface ExtKeyStore<PrimaryKey : Any, Value : Any> : StoreDefinition<PrimaryKey, Value>
        interface AutoExtKeyStore<PrimaryKey : Any, Value : Any> : ExtKeyStore<PrimaryKey, Value>
    }

    interface IndexDefinitionBase<Value : Any, IndexKey : Any> {
        val name: String
        val keyPath: KeyPath<Value, IndexKey>
        val isUnique: Boolean get() = false
    }

    interface IndexDefinition<Value : Any, IndexKey : Any> : IndexDefinitionBase<Value, IndexKey> {
        val keyType: KeyType<IndexKey>
    }

    interface MultiEntryIndexDefinition<Value : Any, IndexElement: Any> : IndexDefinitionBase<Value, Collection<IndexElement>> {
        val elementType: KeyType<IndexElement>
    }

    sealed interface ReadTransaction {
        fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition<PrimaryKey, Value>): ReadObjectStore<PrimaryKey, Value>
    }
    sealed interface ReadWriteTransactionBase : ReadTransaction {
        fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition.KeyPathStore<PrimaryKey, Value>): ReadWriteAutoObjectStore<PrimaryKey, Value>
        fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition.ExtKeyStore<PrimaryKey, Value>): ReadWriteExtKeyObjectStore<PrimaryKey, Value>
        fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition.AutoExtKeyStore<PrimaryKey, Value>): ReadWriteAutoExtKeyObjectStore<PrimaryKey, Value>
    }
    sealed interface ReadWriteTransaction : ReadWriteTransactionBase {
        suspend fun awaitCompletion()
    }

    interface MutationRequest<T> {
        suspend fun await(): T
    }

    sealed interface ReadQueryable<Key : Any, PrimaryKey : Any, Value : Any> {
        suspend fun getAll(range: KeyRange<Key>? = null, count: Int? = null): List<Value>
        suspend fun getAllKeys(range: KeyRange<Key>? = null, count: Int? = null): List<Key>
        suspend fun count(range: KeyRange<Key>? = null): Int
        fun query(range: KeyRange<Key>? = null): Cursor<ReadEntry<Key, PrimaryKey, Value>>
        fun queryKeys(range: KeyRange<Key>? = null): Cursor<ReadKeyEntry<Key, PrimaryKey>>
    }
    sealed interface ReadWriteQueryable<Key : Any, PrimaryKey : Any, Value : Any> : ReadQueryable<Key, PrimaryKey, Value> {
        override fun query(range: KeyRange<Key>?): Cursor<ReadWriteEntry<Key, PrimaryKey, Value>>
    }
    fun interface Cursor<out T> {
        suspend fun executeOnEach(action: suspend (T) -> Boolean)
    }
    interface ReadKeyEntry<Key : Any, PrimaryKey : Any> {
        val key: Key
        val primaryKey: PrimaryKey
    }
    interface ReadEntry<Key : Any, PrimaryKey : Any, Value : Any> : ReadKeyEntry<Key, PrimaryKey> {
        val value: Value
    }
    interface ReadWriteEntry<Key : Any, PrimaryKey : Any, Value : Any> : ReadEntry<Key, PrimaryKey, Value> {
        fun delete(): MutationRequest<Unit>
        fun update(value: Value): MutationRequest<PrimaryKey>
    }

    sealed interface ReadObjectStore<PrimaryKey : Any, Value : Any> : ReadQueryable<PrimaryKey, PrimaryKey, Value> {
        val name: String
        suspend fun get(key: PrimaryKey): Value?
        fun <IndexKey : Any> index(indexDefinition: IndexDefinition<Value, IndexKey>): ReadIndex<Value, IndexKey, PrimaryKey>
        fun <IndexElement : Any> index(indexDefinition: MultiEntryIndexDefinition<Value, IndexElement>): ReadIndex<Value, IndexElement, PrimaryKey>
    }
    sealed interface ReadWriteObjectStore<PrimaryKey : Any, Value : Any> : ReadObjectStore<PrimaryKey, Value>, ReadWriteQueryable<PrimaryKey, PrimaryKey, Value> {
        fun delete(key: PrimaryKey): MutationRequest<Unit>
        override fun <IndexKey : Any> index(indexDefinition: IndexDefinition<Value, IndexKey>): ReadWriteIndex<Value, IndexKey, PrimaryKey>
        override fun <IndexElement : Any> index(indexDefinition: MultiEntryIndexDefinition<Value, IndexElement>): ReadWriteIndex<Value, IndexElement, PrimaryKey>
    }
    interface ReadWriteAutoObjectStore<PrimaryKey : Any, Value : Any> : ReadWriteObjectStore<PrimaryKey, Value> {
        fun add(value: Value): MutationRequest<PrimaryKey>
        fun put(value: Value): MutationRequest<PrimaryKey>
    }
    interface ReadWriteExtKeyObjectStore<PrimaryKey : Any, Value : Any> : ReadWriteObjectStore<PrimaryKey, Value> {
        fun add(key: PrimaryKey, value: Value): MutationRequest<PrimaryKey>
        fun put(key: PrimaryKey, value: Value): MutationRequest<PrimaryKey>
    }
    sealed interface ReadWriteAutoExtKeyObjectStore<PrimaryKey : Any, Value : Any> : ReadWriteExtKeyObjectStore<PrimaryKey, Value>, ReadWriteAutoObjectStore<PrimaryKey, Value>
    sealed interface ReadIndex<Value : Any, IndexKey : Any, PrimaryKey : Any> : ReadQueryable<IndexKey, PrimaryKey, Value> {
        val store: ReadObjectStore<PrimaryKey, Value>
    }
    sealed interface ReadWriteIndex<Value : Any, IndexKey : Any, PrimaryKey : Any> : ReadIndex<Value, IndexKey, PrimaryKey>, ReadWriteQueryable<IndexKey, PrimaryKey, Value> {
        override val store: ReadWriteObjectStore<PrimaryKey, Value>
    }

    enum class TransactionDurability { Default, Relaxed, Strict }

    fun readTransaction(vararg storeDefinitions: StoreDefinition<*, *>): ReadTransaction
    fun readWriteTransaction(vararg storeDefinitions: StoreDefinition<*, *>, durability: TransactionDurability? = null): ReadWriteTransaction

    fun <Key : Any> isKeyInRange(keyType: KeyType<Key>, key: Key, range: KeyRange<Key>): Boolean

    interface MigrationTransaction : ReadWriteTransactionBase {
        data class Versions(
            val oldVersion: Int,
            val newVersion: Int
        )

        fun <PrimaryKey : Any, Value : Any> createObjectStore(storeDefinition: StoreDefinition.KeyPathStore<PrimaryKey, Value>): MigrationReadWriteAutoObjectStore<PrimaryKey, Value>
        fun <PrimaryKey : Any, Value : Any> createObjectStore(storeDefinition: StoreDefinition.ExtKeyStore<PrimaryKey, Value>): MigrationReadWriteExtKeyObjectStore<PrimaryKey, Value>
        fun <PrimaryKey : Any, Value : Any> createObjectStore(storeDefinition: StoreDefinition.AutoExtKeyStore<PrimaryKey, Value>): MigrationReadWriteAutoExtKeyObjectStore<PrimaryKey, Value>
        fun deleteObjectStore(storeName: String)

        override fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition<PrimaryKey, Value>): MigrationReadObjectStore<PrimaryKey, Value>
        override fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition.KeyPathStore<PrimaryKey, Value>): MigrationReadWriteAutoObjectStore<PrimaryKey, Value>
        override fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition.ExtKeyStore<PrimaryKey, Value>): MigrationReadWriteExtKeyObjectStore<PrimaryKey, Value>
        override fun <PrimaryKey : Any, Value : Any> objectStore(storeDefinition: StoreDefinition.AutoExtKeyStore<PrimaryKey, Value>): MigrationReadWriteAutoExtKeyObjectStore<PrimaryKey, Value>
    }

    interface MigrationObjectStore<PrimaryKey : Any, Value : Any> {
        fun <IndexKey: Any> createIndex(indexDefinition: IndexDefinition<Value, IndexKey>) : ReadIndex<Value, IndexKey, PrimaryKey>
        fun <IndexElement : Any> createIndex(indexDefinition: MultiEntryIndexDefinition<Value, IndexElement>) : ReadIndex<Value, IndexElement, PrimaryKey>
        fun deleteIndex(name: String)
    }

    interface MigrationReadObjectStore<PrimaryKey : Any, Value : Any> : ReadObjectStore<PrimaryKey, Value>, MigrationObjectStore<PrimaryKey, Value>
    interface MigrationReadWriteAutoObjectStore<PrimaryKey : Any, Value : Any> : ReadWriteAutoObjectStore<PrimaryKey, Value>, MigrationObjectStore<PrimaryKey, Value>
    interface MigrationReadWriteExtKeyObjectStore<PrimaryKey : Any, Value : Any> : ReadWriteExtKeyObjectStore<PrimaryKey, Value>, MigrationObjectStore<PrimaryKey, Value>
    interface MigrationReadWriteAutoExtKeyObjectStore<PrimaryKey : Any, Value : Any> : ReadWriteAutoExtKeyObjectStore<PrimaryKey, Value>, MigrationObjectStore<PrimaryKey, Value>
}

sealed interface KeyPath<in Value, out Key> {
    fun get(value: Value): Key
}
sealed interface SingleKeyPath<in Value, out Key> : KeyPath<Value, Key> {
    val path: String
}
sealed interface MultiKeyPath<in Value, out Key> : KeyPath<Value, Key> {
    val path: Array<String>
}
private class PropertyKeyPath<in Value, out Key>(
    val property: KProperty1<in Value, Key>
) : SingleKeyPath<Value, Key> {
    override fun get(value: Value): Key = property.get(value)
    override val path: String get() = property.name
}
private class InnerKeyPath<in Value, Inter, out Key>(
    val parent: SingleKeyPath<Value, Inter>,
    val property: KProperty1<in Inter, Key>
) : SingleKeyPath<Value, Key> {
    override fun get(value: Value): Key = property.get(parent.get(value))
    override val path: String get() = "${parent.path}.${property.name}"
}
@Suppress("FunctionName")
fun <Value, Key> KeyPath(
    property: KProperty1<Value, Key>,
): SingleKeyPath<Value, Key> = PropertyKeyPath(property)
operator fun <Value, Inter, Key> SingleKeyPath<Value, Inter>.plus(property: KProperty1<Inter, Key>): SingleKeyPath<Value, Key> =
    InnerKeyPath(this, property)
@Suppress("FunctionName")
fun <Value, Inter, Key> KeyPath(
    property1: KProperty1<Value, Inter>,
    property2: KProperty1<Inter, Key>,
): SingleKeyPath<Value, Key> =
    PropertyKeyPath(property1) + property2
@Suppress("FunctionName")
fun <Value, Inter1, Inter2, Key> KeyPath(
    property1: KProperty1<Value, Inter1>,
    property2: KProperty1<Inter1, Inter2>,
    property3: KProperty1<Inter2, Key>,
): SingleKeyPath<Value, Key> =
    PropertyKeyPath(property1) + property2 + property3
private class PairKeyPath<in Value, out Key1, out Key2>(
    val first: SingleKeyPath<Value, Key1>,
    val second: SingleKeyPath<Value, Key2>
) : MultiKeyPath<Value, Pair<Key1, Key2>> {
    override fun get(value: Value): Pair<Key1, Key2> = Pair(first.get(value), second.get(value))
    override val path: Array<String> get() = arrayOf(first.path, second.path)
}
private class TripleKeyPath<in Value, out Key1, out Key2, out Key3>(
    val first: SingleKeyPath<Value, Key1>,
    val second: SingleKeyPath<Value, Key2>,
    val third: SingleKeyPath<Value, Key3>
) : MultiKeyPath<Value, Triple<Key1, Key2, Key3>> {
    override fun get(value: Value): Triple<Key1, Key2, Key3> = Triple(first.get(value), second.get(value), third.get(value))
    override val path: Array<String> get() = arrayOf(first.path, second.path, third.path)
}
@Suppress("FunctionName")
fun <Value, Key1, Key2> KeyPath(
    keyPath1: SingleKeyPath<Value, Key1>,
    keyPath2: SingleKeyPath<Value, Key2>
): MultiKeyPath<Value, Pair<Key1, Key2>> =
    PairKeyPath(keyPath1, keyPath2)
@Suppress("FunctionName")
fun <Value, Key1, Key2, Key3> KeyPath(
    keyPath1: SingleKeyPath<Value, Key1>,
    keyPath2: SingleKeyPath<Value, Key2>,
    keyPath3: SingleKeyPath<Value, Key3>
): MultiKeyPath<Value, Triple<Key1, Key2, Key3>> =
    TripleKeyPath(keyPath1, keyPath2, keyPath3)

sealed class KeyRange<out Key : Any> {
    data class Only<out Key : Any>(val only: Key) : KeyRange<Key>()
    data class LowerBound<out Key : Any>(val lower: Key, val excludes: Boolean = false) : KeyRange<Key>()
    data class UpperBound<out Key : Any>(val upper: Key, val excludes: Boolean = false) : KeyRange<Key>()
    data class Bound<out Key : Any>(val lower: Key, val upper: Key, val excludesLower: Boolean = false, val excludesUpper: Boolean = false) : KeyRange<Key>()
}

sealed interface KeyType<out T : Any> {
    data object OfInt : KeyType<Int>
    data object OfString : KeyType<String>
    data object OfLong : KeyType<Long>
    data object OfBufferSource : KeyType<BufferSource>
    data class OfPair<out A : Any, out B : Any>(val types: Pair<KeyType<A>, KeyType<B>>): KeyType<Pair<A, B>>
    data class OfTriple<out A : Any, out B : Any, out C : Any>(val types: Triple<KeyType<A>, KeyType<B>, KeyType<C>>): KeyType<Triple<A, B, C>>
    data class OfList(val types: List<KeyType<*>>): KeyType<List<*>>
    data class OfWrap<W : Any, T : Any>(val type: KeyType<T>, val wrap: (T) -> W, val unwrap: (W) -> T): KeyType<W>
}

suspend fun <Key : Any> LocalDatabase.ReadQueryable<Key, *, *>.contains(key: Key): Boolean =
    count(KeyRange.Only(key)) > 0

fun <Key : Any, PrimaryKey : Any> LocalDatabase.Cursor<LocalDatabase.ReadKeyEntry<Key, PrimaryKey>>.executeAsKeyFlow(): Flow<LocalDatabase.ReadKeyEntry<Key, PrimaryKey>> =
    flow {
        executeOnEach {
            // Copy
            emit(object : LocalDatabase.ReadKeyEntry<Key, PrimaryKey> {
                override val key: Key = it.key
                override val primaryKey: PrimaryKey = it.primaryKey
            })
            currentCoroutineContext().isActive
        }
    }

fun <Key : Any, PrimaryKey : Any, Value : Any> LocalDatabase.Cursor<LocalDatabase.ReadEntry<Key, PrimaryKey, Value>>.executeAsFlow(): Flow<LocalDatabase.ReadEntry<Key, PrimaryKey, Value>> =
    channelFlow {
        executeOnEach {
            // Copy
            send(object : LocalDatabase.ReadEntry<Key, PrimaryKey, Value> {
                override val key: Key = it.key
                override val primaryKey: PrimaryKey = it.primaryKey
                override val value: Value = it.value
            })
            currentCoroutineContext().isActive
        }
    }

internal fun < T, R> LocalDatabase.Cursor<T>.map(transform: suspend (T) -> R): LocalDatabase.Cursor<R> =
    LocalDatabase.Cursor { action ->
        executeOnEach {
            action(transform(it))
        }
    }

suspend fun <T : LocalDatabase.ReadWriteEntry<*, *, *>> LocalDatabase.Cursor<T>.deleteAll(): Unit =
    executeOnEach {
        it.delete()
        true
    }

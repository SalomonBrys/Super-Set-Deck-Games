package data

import data.indexeddb.*
import js.date.Date
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.builtins.serializer
import utils.asyncUndispatched
import utils.isolatedScope
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


class AppDatabase private constructor(
    private val db: ReactiveLocalDatabase
) {
    companion object {
        private var deferred: Deferred<Result<AppDatabase>>? = null

        suspend fun getResult(): Result<AppDatabase> {
            deferred?.let { return it.await() }

            deferred = isolatedScope().asyncUndispatched {
                runCatching {
                    AppDatabase(
                        LocalDatabase.open(
                            name = "Super-Set-Deck",
                            version = 1,
                            migrate = {
                                createObjectStore(UserListStore)
                                createObjectStore(UserListEntryStore).apply {
                                    createIndex(UserListEntryStore.GameIDIndex)
                                }
                                createObjectStore(UserListUpdateStore)
                            }
                        ).reactive()
                    )
                }
            }

            return deferred!!.await()
        }

        suspend fun get(): AppDatabase? = getResult().getOrNull()

        val favourites = UserList(UserList.ID("favourites"), "")
    }

    private object UserListStore : LocalDatabase.StoreDefinition.KeyPathStore<UserList.ID, UserList> {
        override val name = "List"
        override val primaryKeyPath = KeyPath(UserList::id)
        override val primaryKeyType = KeyType.OfWrap(
            type = KeyType.OfString,
            wrap = { UserList.ID(it) },
            unwrap = { it.id }
        )
        override val serializer = UserList.serializer()
    }

    private object UserListEntryStore : LocalDatabase.StoreDefinition.KeyPathStore<Pair<UserList.ID, String>, UserList.Entry> {
        override val name: String = "ListEntry"
        override val serializer = UserList.Entry.serializer()
        override val primaryKeyPath = KeyPath(KeyPath(UserList.Entry::listId), KeyPath(UserList.Entry::gameId))
        override val primaryKeyType = KeyType.OfPair(Pair(UserListStore.primaryKeyType, KeyType.OfString))

        object GameIDIndex : LocalDatabase.IndexDefinition<UserList.Entry, String> {
            override val name: String = "gameId"
            override val keyPath = KeyPath(UserList.Entry::gameId)
            override val keyType = KeyType.OfString
        }
    }

    private object UserListUpdateStore : LocalDatabase.StoreDefinition.ExtKeyStore<UserList.ID, Double> {
        override val name: String = "ListUpdate"
        override val serializer = Double.serializer()
        override val primaryKeyType = UserListStore.primaryKeyType
    }

    private fun allEntriesOf(id: UserList.ID) =
        KeyRange.Bound(
            lower = Pair(id, ""),
            upper = Pair(id, "\uffff")
        )

    @OptIn(ExperimentalUuidApi::class)
    suspend fun createUserList(list: UserList): UserList {
        require(list.id == UserList.ID.empty)
        val transaction = db.readWriteTransaction(UserListStore, UserListUpdateStore)
        val newList = list.copy(id = UserList.ID(Uuid.random().toString()))
        transaction.objectStore(UserListStore).add(newList)
        transaction.objectStore(UserListUpdateStore).add(newList.id, Date().getTime())
        transaction.awaitCompletion()
        return newList
    }

    suspend fun putUserList(date: Date, list: UserList, games: List<String>) {
        require(list.id != UserList.ID.empty)
        val transaction = db.readWriteTransaction(UserListStore, UserListEntryStore, UserListUpdateStore)
        if (list.id != favourites.id) {
            transaction.objectStore(UserListStore).put(list)
        }
        transaction.objectStore(UserListUpdateStore).put(list.id, date.getTime())
        val entryStore = transaction.objectStore(UserListEntryStore)
        entryStore.query(allEntriesOf(list.id)).deleteAll()
        games.forEach { gameId ->
            entryStore.add(UserList.Entry(list.id, gameId))
        }
    }

    suspend fun updateUserList(id: UserList.ID, update: (UserList) -> UserList): UserList? {
        val transaction = db.readWriteTransaction(UserListStore, UserListUpdateStore)
        val userListStore = transaction.objectStore(UserListStore)
        val list = userListStore.get(id) ?: return null
        val newList = update(list)
        require(newList.id == list.id) { "List ID changed" }
        userListStore.put(newList)
        transaction.objectStore(UserListUpdateStore).put(newList.id, Date().getTime())
        transaction.awaitCompletion()
        return newList
    }

    suspend fun deleteUserList(id: UserList.ID) {
        val transaction = db.readWriteTransaction(UserListStore, UserListEntryStore, UserListUpdateStore)
        transaction.objectStore(UserListStore).delete(id)
        transaction.objectStore(UserListEntryStore).query(allEntriesOf(id)).deleteAll()
        transaction.objectStore(UserListUpdateStore).put(id, Date().getTime())
        transaction.awaitCompletion()
    }

    fun watchUserList(id: UserList.ID): Flow<UserList?> {
        if (id == favourites.id) return flowOf(favourites)
        return db.watch(UserListStore).value(favourites.id).map { it.result }
    }

    fun watchAllUserLists(): Flow<List<UserList>> {
        val favList = listOf(favourites)
        return db.watch(UserListStore).all().map { favList + it.result }
    }

    fun watchGameCountInUserList(listId: UserList.ID): Flow<Int> =
        db.watch(UserListEntryStore).count(allEntriesOf(listId)).map { it.result }

    fun watchAllGamesInUserList(listId: UserList.ID): Flow<List<String>> =
        db.watch(UserListEntryStore).all(allEntriesOf(listId)).map { change -> change.result.map { it.gameId } }

    suspend fun addGameToUserList(listId: UserList.ID, gameId: String) {
        val transaction = db.readWriteTransaction(UserListStore, UserListEntryStore, UserListUpdateStore)
        val entry = UserList.Entry(
            listId = listId,
            gameId = gameId,
        )
        if (listId != favourites.id) {
            if (!transaction.objectStore(UserListStore).contains(listId)) {
                console.error("Trying to add into a list that does not exist ($listId)")
                return
            }
        }
        transaction.objectStore(UserListEntryStore).add(entry)
        transaction.objectStore(UserListUpdateStore).put(entry.listId, Date().getTime())
        transaction.awaitCompletion()
    }

    suspend fun removeGameFromUserList(listId: UserList.ID, gameId: String) {
        val transaction = db.readWriteTransaction(UserListEntryStore)
        transaction.objectStore(UserListEntryStore).delete(Pair(listId, gameId))
        transaction.objectStore(UserListUpdateStore).put(listId, Date().getTime())
        transaction.awaitCompletion()
    }

    fun watchAllUserListsOfGame(gameId: String): Flow<List<UserList>> =
        db.watch(UserListEntryStore).index(UserListEntryStore.GameIDIndex).all(KeyRange.Only(gameId)).map { change ->
            val store = db.readTransaction(UserListStore).objectStore(UserListStore)
            change.result.mapNotNull {
                if (it.listId == favourites.id) favourites
                else store.get(it.listId)
            }
        }

    suspend fun getAllUpdatedUserLists(): List<Triple<Date, UserList.ID, Pair<UserList, List<String>>?>> {
        val transaction = db.readTransaction(UserListStore, UserListUpdateStore, UserListEntryStore)
        return transaction.objectStore(UserListUpdateStore).query().executeAsFlow().map { entry ->
            val list = if (entry.key == favourites.id) favourites else transaction.objectStore(UserListStore).get(entry.key)
            val pair = list?.let {
                it to transaction.objectStore(UserListEntryStore).getAll(allEntriesOf(it.id)).map { it.gameId }
            }
            Triple(Date(entry.value), entry.key, pair)
        }.toList()
    }
}

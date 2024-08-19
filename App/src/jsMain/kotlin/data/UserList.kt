package data

import kotlinx.serialization.Serializable


@Serializable
data class UserList(
    val id: ID,
    val name: String,
) {
    @Serializable
    value class ID(val id: String) {
        companion object {
            val empty = ID("")
        }
    }

    @Serializable
    data class Entry(
        val listId: UserList.ID,
        val gameId: String,
    ) {
//        val id get() = Pair(listId, gameId)
    }
}

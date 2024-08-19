package data

import kotlinx.serialization.Serializable

@Serializable
data class SsdBak(
    val version: Int,
    val lists: Map<data.UserList.ID, UserList>
) {
    @Serializable
    data class UserList(
        val update: Long,
        val content: Content?
    ) {
        @Serializable
        data class Content(
            val name: String,
            val games: List<String>
        )
    }
}

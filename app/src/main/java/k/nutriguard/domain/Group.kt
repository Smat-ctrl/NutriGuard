package k.nutriguard.domain

import java.util.UUID

data class Group(
    val name: String,
    val id: UUID = UUID.randomUUID(),
    val owner: String,
    val members: MutableSet<String> = mutableSetOf(),
    val sharedCart: MutableSet<FoodItem> = mutableSetOf(),
    val sharedCartId: UUID
) {
    fun isMember(username: String): Boolean {
        return username == owner || username in members
    }

    fun addUser(username: String) {
        if (username != owner && username !in members) members += username
    }

    fun removeUser(username: String) {
        if (username != owner && username in members) members -= username
    }

    fun addItem(item: FoodItem) { sharedCart += item }

    fun removeItem(predicate: (FoodItem) -> Boolean) { sharedCart.removeIf(predicate) }
}



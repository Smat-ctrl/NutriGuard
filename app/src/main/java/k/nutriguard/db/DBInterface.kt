package k.nutriguard.db


import k.nutriguard.domain.UserProfile
import k.nutriguard.domain.Group
import k.nutriguard.domain.FoodItem
import k.nutriguard.domain.CartModel
import java.util.UUID

interface DBInterface {
    // UserProfile
    suspend fun createUser(user: UserProfile): UserProfile?
    suspend fun getUser(username: String): UserProfile?

    suspend fun getUserByAuthId(authId: java.util.UUID): UserProfile?
    suspend fun getUserBasic(username: String): UserProfile?
    suspend fun updateUser(user: UserProfile): Boolean
    suspend fun deleteUser(username: String): Boolean

    // Group
    suspend fun createGroup(group: Group): Group?
    suspend fun getGroup(groupId: String): Group?
    suspend fun updateGroup(group: Group): Boolean
    suspend fun deleteGroup(groupId: String): Boolean

    // CartModel
    suspend fun createCart(cart: CartModel): CartModel?
    suspend fun getCart(cartId: String): CartModel?
    suspend fun deleteCart(cartId: UUID): Boolean

    // FoodItem
    suspend fun createFoodItem(item: FoodItem): FoodItem?
    suspend fun getFoodItem(itemId: String): FoodItem?
    suspend fun updateFoodItem(item: FoodItem): Boolean
    suspend fun deleteFoodItem(itemId: String): Boolean

    // Friends (user_friends junction table)
    suspend fun addFriend(username: String, friendUsername: String): Boolean
    suspend fun getFriends(username: String): List<UserProfile>
    suspend fun removeFriend(username: String, friendUsername: String): Boolean

    // Group Members (group_members junction table)
    suspend fun addUserToGroup(groupId: String, username: String): Boolean
    suspend fun getGroupMembers(groupId: String): List<UserProfile>
    suspend fun getUserGroups(username: String): List<Group>
    suspend fun removeUserFromGroup(groupId: String, username: String): Boolean

    // Cart Items (cart_items junction table)
    suspend fun addItemToCart(cartId: String, itemId: String): Boolean
    suspend fun getCartItems(cartId: String): List<FoodItem>
    suspend fun removeItemFromCart(cartId: String, itemId: String): Boolean
}
package db

import k.nutriguard.domain.UserProfile
import k.nutriguard.domain.Group
import k.nutriguard.domain.FoodItem
import k.nutriguard.domain.CartModel
import java.util.UUID
import k.nutriguard.db.DBInterface

class MockDBRepository : DBInterface {

    // Main Tables
    private val usersByUsername = mutableMapOf<String, UserProfile>()
    private val groupsById = mutableMapOf<String, Group>()          // key = group.id.toString()
    private val cartsById = mutableMapOf<String, CartModel>()       // key = cart.id.toString()
    private val foodItemsById = mutableMapOf<String, FoodItem>()    // key = food.id.toString()

    // Junction tables
    private val userFriends = mutableSetOf<Pair<String, String>>()  // (user_name, friend_name)
    private val groupMembers = mutableSetOf<Pair<String, String>>() // (group_id, username)
    private val cartItems = mutableSetOf<Pair<String, String>>()    // (cart_id, fooditem_id)

    // UserProfile CRUD
    override suspend fun createUser(user: UserProfile): UserProfile? {
        // if username exists, overwrite for simplicity, won't do this for realDB
        usersByUsername[user.username] = user
        return user
    }

    override suspend fun getUser(username: String): UserProfile? {
        val base = usersByUsername[username] ?: return null

        // Friends from junction table
        val friendIds = userFriends
            .filter { (u, _) -> u == username }
            .map { (_, friend) -> friend }
            .toMutableSet()

        // Groups from junction table
        val groupIds = groupMembers
            .filter { (_, u) -> u == username }
            .map { (groupId, _) -> groupId }
            .toMutableSet()

        // Personal cart items from junction able
        val cartIdStr = base.personalCartId.toString()
        val itemIds = cartItems
            .filter { (cartId, _) -> cartId == cartIdStr }
            .map { (_, foodId) -> foodId }
            .distinct()

        val personalItems = itemIds
            .mapNotNull { foodItemsById[it] }
            .toMutableSet()

        return base.copy(
            friendIds = friendIds,
            groupIds = groupIds,
            personalCart = personalItems
        )
    }

    // basic version that does NOT populate friends/groups/cart
    override suspend fun getUserBasic(username: String): UserProfile? {
        return usersByUsername[username]
    }

    // lookup by authId, then reuse getUser(...) to get full user
    override suspend fun getUserByAuthId(authId: UUID): UserProfile? {
        // Find the base user with authId
        val base = usersByUsername.values.firstOrNull { it.authId == authId } ?: return null

        // Reuse getUser so we can attach friends, groups, and personalCart to user
        return getUser(base.username)
    }

    override suspend fun updateUser(user: UserProfile): Boolean {
        if (!usersByUsername.containsKey(user.username)) return false
        usersByUsername[user.username] = user
        return true
    }

    override suspend fun deleteUser(username: String): Boolean {
        val removed = usersByUsername.remove(username) != null

        // Clean up junction tables
        userFriends.removeIf { (u, f) -> u == username || f == username }
        groupMembers.removeIf { (_, u) -> u == username }

        return removed
    }

    // Group CRUD


    override suspend fun createGroup(group: Group): Group? {
        val key = group.id.toString()
        groupsById[key] = group
        return group
    }

    override suspend fun getGroup(groupId: String): Group? {
        val base = groupsById[groupId] ?: return null

        // Members from group_members junction table
        val members = groupMembers
            .filter { (gId, _) -> gId == groupId }
            .map { (_, username) -> username }
            .toMutableSet()

        // Shared cart items from cart_items + foodItems junction table
        val sharedCartIdStr = base.sharedCartId.toString()
        val itemIds = cartItems
            .filter { (cartId, _) -> cartId == sharedCartIdStr }
            .map { (_, foodId) -> foodId }
            .distinct()

        val sharedItems = itemIds
            .mapNotNull { foodItemsById[it] }
            .toMutableSet()

        return base.copy(
            members = members,
            sharedCart = sharedItems
        )
    }

    override suspend fun updateGroup(group: Group): Boolean {
        val key = group.id.toString()
        if (!groupsById.containsKey(key)) return false
        groupsById[key] = group
        return true
    }

    override suspend fun deleteGroup(groupId: String): Boolean {
        val removed = groupsById.remove(groupId) != null
        groupMembers.removeIf { (gId, _) -> gId == groupId }
        return removed
    }

    // CartModel CRD

    override suspend fun createCart(cart: CartModel): CartModel? {
        // respect client-side UUID like the real repo does
        val key = cart.id.toString()
        cartsById[key] = cart.copy(items = cart.items.toMutableList())
        return cartsById[key]
    }

    override suspend fun getCart(cartId: String): CartModel? {
        val base = cartsById[cartId] ?: return null

        val itemIds = cartItems
            .filter { (cId, _) -> cId == cartId }
            .map { (_, fId) -> fId }
            .distinct()

        val items = itemIds
            .mapNotNull { foodItemsById[it] }
            .toMutableList()

        return base.copy(items = items)
    }

    override suspend fun deleteCart(cartId: UUID): Boolean {
        val key = cartId.toString()
        val removed = cartsById.remove(key) != null
        cartItems.removeIf { (cId, _) -> cId == key }
        return removed
    }

    // FoodItem CRUD

    override suspend fun createFoodItem(item: FoodItem): FoodItem? {
        val key = item.id.toString()
        foodItemsById[key] = item.copy(owners = item.owners.toMutableList())
        return foodItemsById[key]
    }

    override suspend fun getFoodItem(itemId: String): FoodItem? {
        val base = foodItemsById[itemId] ?: return null

        val ownerCartIds = cartItems
            .filter { (_, fId) -> fId == itemId }
            .map { (cartId, _) -> cartId }
            .distinct()
            .toMutableList()

        return base.copy(owners = ownerCartIds)
    }

    override suspend fun updateFoodItem(item: FoodItem): Boolean {
        val key = item.id.toString()
        if (!foodItemsById.containsKey(key)) return false
        foodItemsById[key] = item.copy(owners = item.owners.toMutableList())
        return true
    }

    override suspend fun deleteFoodItem(itemId: String): Boolean {
        val removed = foodItemsById.remove(itemId) != null
        cartItems.removeIf { (_, fId) -> fId == itemId }
        return removed
    }

    // user_friends junction

    override suspend fun addFriend(username: String, friendUsername: String): Boolean {
        userFriends.add(username to friendUsername)
        return true
    }

    override suspend fun getFriends(username: String): List<UserProfile> {
        val friendUsernames = userFriends
            .filter { (u, _) -> u == username }
            .map { (_, friend) -> friend }
            .distinct()

        return friendUsernames.mapNotNull { usersByUsername[it] }
    }

    override suspend fun removeFriend(username: String, friendUsername: String): Boolean {
        return userFriends.remove(username to friendUsername)
    }

    // group_members junction

    override suspend fun addUserToGroup(groupId: String, username: String): Boolean {
        groupMembers.add(groupId to username)
        return true
    }

    override suspend fun getGroupMembers(groupId: String): List<UserProfile> {
        val usernames = groupMembers
            .filter { (gId, _) -> gId == groupId }
            .map { (_, u) -> u }
            .distinct()

        return usernames.mapNotNull { usersByUsername[it] }
    }

    override suspend fun getUserGroups(username: String): List<Group> {
        // groups where user is an explicit member
        val memberGroupIds = groupMembers
            .filter { (_, u) -> u == username }
            .map { (gId, _) -> gId }
            .toMutableSet()

        // groups where user is the owner
        val ownerGroupIds = groupsById
            .values
            .filter { it.owner == username }
            .map { it.id.toString() }

        val allIds = (memberGroupIds + ownerGroupIds).distinct()

        // Use getGroup(...) to populate members + sharedCart from junction tables
        return allIds.mapNotNull { getGroup(it) }
    }

    override suspend fun removeUserFromGroup(groupId: String, username: String): Boolean {
        return groupMembers.remove(groupId to username)
    }

    // cart_items junction

    override suspend fun addItemToCart(cartId: String, itemId: String): Boolean {
        cartItems.add(cartId to itemId)
        return true
    }

    override suspend fun getCartItems(cartId: String): List<FoodItem> {
        val itemIds = cartItems
            .filter { (cId, _) -> cId == cartId }
            .map { (_, fId) -> fId }
            .distinct()

        return itemIds.mapNotNull { foodItemsById[it] }
    }

    override suspend fun removeItemFromCart(cartId: String, itemId: String): Boolean {
        return cartItems.remove(cartId to itemId)
    }
}

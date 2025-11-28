package db

import k.nutriguard.domain.UserProfile
import k.nutriguard.domain.Group
import k.nutriguard.domain.FoodItem
import k.nutriguard.domain.CartModel
import k.nutriguard.db.DBInterface
import k.nutriguard.db.DBRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DBIntegrationTest {

    // Use the test client instead of the Android one
    private val db: DBInterface = DBRepository(TestSupabase.client)

    @Test
    fun userCrud_smoke() = runBlocking {
        // 1) create a cart
        val cart = CartModel()
        val retCart = db.createCart(cart)

        val sharedCart = CartModel()
        val sharRetCart = db.createCart(sharedCart)

        assertNotNull("createCart returned null", retCart)
        assertNotNull("create shared cart returned null", sharRetCart)

        val insertedCart = retCart!!
        val sharedInsertedCart = sharRetCart!!

        // if you generate ids client-side, this will match; otherwise you can just trust insertedCart.id
        assertEquals(cart.id, insertedCart.id)

        // 2) create a user linked to that cart
        val user = UserProfile(
            username = "test_prabh123",
            personalCartId = insertedCart.id
        )
        val created = db.createUser(user)
        assertNotNull("createUser failed", created)
        assertEquals("test_prabh123", created!!.username)

        // 2b) create a second user to test friends / group membership
        val friendCart = CartModel()
        val friendCartRet = db.createCart(friendCart)
        assertNotNull("createCart for friend returned null", friendCartRet)
        val insertedFriendCart = friendCartRet!!

        val friend = UserProfile(
            username = "test_friend456",
            personalCartId = insertedFriendCart.id
        )
        val createdFriend = db.createUser(friend)
        assertNotNull("createUser for friend failed", createdFriend)
        assertEquals("test_friend456", createdFriend!!.username)

        // 3) create a group with shared cart
        val group = Group(
            name = "testGroup",
            owner = "test_prabh123",
            sharedCartId = sharedInsertedCart.id
        )
        val groupcreated = db.createGroup(group)
        assertNotNull("createGroup failed", groupcreated)
        assertEquals("testGroup", groupcreated!!.name)

        // 4) create a food item
        val food = FoodItem(
            name = "Test Apple",
            price = 1.99,
            barcode = "1234567890",
            purchasedDate = "2025/01/01",
            expirationDate = "2099/01/01",
            // At least one owner: the user's personal cart
            owners = mutableListOf(insertedCart.id.toString()),
            quantity = 2.0
        )

        val createdFood = db.createFoodItem(food)
        assertNotNull("createFoodItem failed", createdFood)
        val insertedFood = createdFood!!
        assertEquals(food.name, insertedFood.name)

        // ---------- Junction table setup (no removals yet) ----------

        // A) user_friends
        val addFriendOk = db.addFriend(user.username, friend.username)
        assertTrue("addFriend failed", addFriendOk)

        // B) group_members (friend membership; owner may also be inserted in createGroup depending on impl)
        val addToGroupOk = db.addUserToGroup(group.id.toString(), friend.username)
        assertTrue("addUserToGroup failed", addToGroupOk)

        // C) cart_items:
        // createFoodItem already linked (insertedCart, insertedFood) via owners.
        // Here we explicitly link the food to the GROUP'S SHARED CART so that:
        // - getGroup.sharedCart sees it
        // - addItemToCart is exercised without hitting the PK again.
        val addItemOk = db.addItemToCart(sharedInsertedCart.id.toString(), insertedFood.id.toString())
        assertTrue("addItemToCart failed", addItemOk)

        // ---------- Hydrated read-backs (get* methods) ----------

        // getUser(owner) should now have friendIds + personalCart populated
        val fetchedUser = db.getUser(user.username)
        assertNotNull("getUser returned null", fetchedUser)
        fetchedUser!!
        assertEquals(insertedCart.id, fetchedUser.personalCartId)
        assertTrue(
            "getUser.friendIds missing friend",
            fetchedUser.friendIds.contains(friend.username)
        )
        assertTrue(
            "getUser.personalCart missing food",
            fetchedUser.personalCart.any { it.id == insertedFood.id }
        )

        // getUser(friend) should have groupIds populated via group_members
        val fetchedFriendUser = db.getUser(friend.username)
        assertNotNull("getUser(friend) returned null", fetchedFriendUser)
        fetchedFriendUser!!
        assertTrue(
            "getUser(friend).groupIds missing group",
            fetchedFriendUser.groupIds.contains(group.id.toString())
        )

        // getGroup should have members + sharedCart populated
        val fetchedGroup = db.getGroup(group.id.toString())
        assertNotNull("getGroup returned null", fetchedGroup)
        fetchedGroup!!
        assertEquals(sharedInsertedCart.id, fetchedGroup.sharedCartId)
        assertTrue(
            "getGroup.members missing friend",
            fetchedGroup.members.contains(friend.username)
        )
        assertTrue(
            "getGroup.sharedCart missing food",
            fetchedGroup.sharedCart.any { it.id == insertedFood.id }
        )

        // getFoodItem should have owners populated with the user's personal cart ID
        val fetchedFood = db.getFoodItem(insertedFood.id.toString())
        assertNotNull("getFoodItem returned null", fetchedFood)
        fetchedFood!!
        assertEquals(insertedFood.id, fetchedFood.id)
        assertTrue(
            "getFoodItem.owners missing cart id",
            fetchedFood.owners.contains(insertedCart.id.toString())
        )

        // getCart (user's personal cart) should have items populated via owners
        val fetchedCart = db.getCart(insertedCart.id.toString())
        assertNotNull("getCart returned null", fetchedCart)
        fetchedCart!!
        assertEquals(insertedCart.id, fetchedCart.id)
        assertTrue(
            "getCart.items missing food",
            fetchedCart.items.any { it.id == insertedFood.id }
        )

        // ---------- Junction table query + remove tests ----------

        // A) user_friends: verify and remove
        val friendsOfUser = db.getFriends(user.username)
        assertTrue(
            "getFriends did not return friend",
            friendsOfUser.any { it.username == friend.username }
        )

        val removeFriendOk = db.removeFriend(user.username, friend.username)
        assertTrue("removeFriend failed", removeFriendOk)

        val friendsAfterRemove = db.getFriends(user.username)
        assertTrue(
            "friend still present after removeFriend",
            friendsAfterRemove.none { it.username == friend.username }
        )

        // B) group_members: verify and remove
        val groupMembers = db.getGroupMembers(group.id.toString())
        assertTrue(
            "getGroupMembers did not include friend",
            groupMembers.any { it.username == friend.username }
        )

        val userGroups = db.getUserGroups(friend.username)
        assertTrue(
            "getUserGroups did not include group",
            userGroups.any { it.id == group.id }
        )

        val removeFromGroupOk = db.removeUserFromGroup(group.id.toString(), friend.username)
        assertTrue("removeUserFromGroup failed", removeFromGroupOk)

        val membersAfterRemove = db.getGroupMembers(group.id.toString())
        assertTrue(
            "friend still present after removeUserFromGroup",
            membersAfterRemove.none { it.username == friend.username }
        )

        // C) cart_items: verify and remove for the SHARED CART
        val sharedCartItems = db.getCartItems(sharedInsertedCart.id.toString())
        assertTrue(
            "getCartItems (shared cart) did not return inserted food",
            sharedCartItems.any { it.id == insertedFood.id }
        )

        val removeItemOk = db.removeItemFromCart(sharedInsertedCart.id.toString(), insertedFood.id.toString())
        assertTrue("removeItemFromCart failed", removeItemOk)

        val itemsAfterRemove = db.getCartItems(sharedInsertedCart.id.toString())
        assertTrue(
            "food still present after removeItemFromCart on shared cart",
            itemsAfterRemove.none { it.id == insertedFood.id }
        )

        // ---------- cleanup ----------

        assertTrue("deleteFoodItem failed", db.deleteFoodItem(insertedFood.id.toString()))
        assertTrue("deleteUser (friend) failed", db.deleteUser(friend.username))
        assertTrue("deleteUser failed", db.deleteUser(user.username))
        assertTrue("deleteGroup failed", db.deleteGroup(group.id.toString()))
        assertTrue("deleteCart failed", db.deleteCart(insertedCart.id))
        assertTrue("deleteCart shared failed", db.deleteCart(sharedInsertedCart.id))
        assertTrue("deleteCart friend failed", db.deleteCart(insertedFriendCart.id))
    }
}

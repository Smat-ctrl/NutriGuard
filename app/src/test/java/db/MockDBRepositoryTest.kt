package db

import k.nutriguard.domain.UserProfile
import k.nutriguard.domain.Group
import k.nutriguard.domain.FoodItem
import k.nutriguard.domain.CartModel
import k.nutriguard.db.DBInterface
import db.MockDBRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class MockDBRepositoryTest {

    private fun newDb(): DBInterface = MockDBRepository()

    // Basic User CRUD
    @Test
    fun userCrud_basic() = runBlocking {
        val db = newDb()

        // create a cart and a user
        val cart = CartModel()
        val createdCart = db.createCart(cart)
        assertNotNull("createCart returned null", createdCart)
        val insertedCart = createdCart!!

        val user = UserProfile(
            username = "user_basic",
            personalCartId = insertedCart.id
        )

        val createdUser = db.createUser(user)
        assertNotNull("createUser failed", createdUser)
        assertEquals("user_basic", createdUser!!.username)

        // read back
        val fetched = db.getUser("user_basic")
        assertNotNull("getUser returned null", fetched)
        fetched!!
        assertEquals(insertedCart.id, fetched.personalCartId)
        assertTrue("groupIds should start empty", fetched.groupIds.isEmpty())
        assertTrue("friendIds should start empty", fetched.friendIds.isEmpty())
        assertTrue("personalCart should start empty", fetched.personalCart.isEmpty())

        // update user info
        val updatedUser = fetched.copy(
            dietaryRestrictions = mutableSetOf(),
            allergies = mutableSetOf()
        )
        val updateOk = db.updateUser(updatedUser)
        assertTrue("updateUser failed", updateOk)

        val fetchedAfterUpdate = db.getUser("user_basic")
        assertNotNull("getUser after update returned null", fetchedAfterUpdate)

        // delete user
        val deleteOk = db.deleteUser("user_basic")
        assertTrue("deleteUser failed", deleteOk)

        val fetchedAfterDelete = db.getUser("user_basic")
        assertNull("user should be null after delete", fetchedAfterDelete)
    }

    // Friends + populated getUser
    @Test
    fun userFriends_andHydratedUser() = runBlocking {
        val db = newDb()

        // two carts & users
        val cartA = db.createCart(CartModel())!!
        val cartB = db.createCart(CartModel())!!

        val userA = UserProfile(
            username = "alice",
            personalCartId = cartA.id
        )
        val userB = UserProfile(
            username = "bob",
            personalCartId = cartB.id
        )
        db.createUser(userA)
        db.createUser(userB)

        // create a food item and link it to alice's cart in cart_items junction
        val food = FoodItem(
            name = "Apple",
            price = 1.5,
            barcode = "111",
            purchasedDate = "2025/01/01",
            expirationDate = "2099/01/01",
            owners = mutableListOf(),      // owners derived from cart_items in mock
            quantity = 3.0
        )
        val createdFood = db.createFoodItem(food)!!
        val addItemOk = db.addItemToCart(cartA.id.toString(), createdFood.id.toString())
        assertTrue("addItemToCart failed", addItemOk)

        // add bob as alice's friend
        val addFriendOk = db.addFriend("alice", "bob")
        assertTrue("addFriend failed", addFriendOk)

        // populated fields of getUser(alice)
        val hydratedAlice = db.getUser("alice")
        assertNotNull("hydrated getUser(alice) returned null", hydratedAlice)
        hydratedAlice!!

        // alice should see bob in friendIds
        assertTrue(
            "alice.friendIds should contain bob",
            hydratedAlice.friendIds.contains("bob")
        )

        // alice personalCart should contain the food we added with cart_items
        assertTrue(
            "alice.personalCart should contain the food item",
            hydratedAlice.personalCart.any { it.id == createdFood.id }
        )

        // getFriends should also return bob
        val aliceFriends = db.getFriends("alice")
        assertTrue(
            "getFriends(alice) did not return bob",
            aliceFriends.any { it.username == "bob" }
        )

        // remove friend
        val removeFriendOk = db.removeFriend("alice", "bob")
        assertTrue("removeFriend failed", removeFriendOk)

        val friendsAfterRemove = db.getFriends("alice")
        assertTrue(
            "bob still present after removeFriend",
            friendsAfterRemove.none { it.username == "bob" }
        )
    }

    // Group CRUD + members + shared cart
    @Test
    fun groupCrud_membersAndSharedCart() = runBlocking {
        val db = newDb()

        // shared cart for group
        val sharedCart = db.createCart(CartModel())!!
        // user who will join the group
        val memberCart = db.createCart(CartModel())!!
        val memberUser = UserProfile(
            username = "group_member",
            personalCartId = memberCart.id
        )
        db.createUser(memberUser)

        // create group
        val group = Group(
            name = "StudyGroup",
            owner = "owner_user",
            sharedCartId = sharedCart.id
        )
        db.createGroup(group)

        // add user to group_members junction
        val addToGroupOk = db.addUserToGroup(group.id.toString(), memberUser.username)
        assertTrue("addUserToGroup failed", addToGroupOk)

        // add a food item to groups sharedCart
        val food = FoodItem(
            name = "Chips",
            price = 3.0,
            barcode = "222",
            purchasedDate = "2025/02/01",
            expirationDate = "2099/02/01",
            owners = mutableListOf(),
            quantity = 1.0
        )
        val createdFood = db.createFoodItem(food)!!
        val addItemOk = db.addItemToCart(sharedCart.id.toString(), createdFood.id.toString())
        assertTrue("addItemToCart (shared cart) failed", addItemOk)

        // populated getGroup
        val fetchedGroup = db.getGroup(group.id.toString())
        assertNotNull("getGroup returned null", fetchedGroup)
        fetchedGroup!!

        assertEquals("group sharedCartId mismatch", sharedCart.id, fetchedGroup.sharedCartId)
        assertTrue(
            "group.members missing user",
            fetchedGroup.members.contains(memberUser.username)
        )
        assertTrue(
            "group.sharedCart missing food item",
            fetchedGroup.sharedCart.any { it.id == createdFood.id }
        )

        // getGroupMembers
        val members = db.getGroupMembers(group.id.toString())
        assertTrue(
            "getGroupMembers did not include user",
            members.any { it.username == memberUser.username }
        )

        // getUserGroups for a member
        val userGroups = db.getUserGroups(memberUser.username)
        assertTrue(
            "getUserGroups did not include group",
            userGroups.any { it.id == group.id }
        )

        // remove from group
        val removeFromGroupOk = db.removeUserFromGroup(group.id.toString(), memberUser.username)
        assertTrue("removeUserFromGroup failed", removeFromGroupOk)

        val membersAfterRemove = db.getGroupMembers(group.id.toString())
        assertTrue(
            "user still present after removeUserFromGroup",
            membersAfterRemove.none { it.username == memberUser.username }
        )

        // delete group
        val deleteGroupOk = db.deleteGroup(group.id.toString())
        assertTrue("deleteGroup failed", deleteGroupOk)
        assertNull("group should be null after delete", db.getGroup(group.id.toString()))
    }

    // Cart + FoodItem CRUD + cart_items + getCart/getFoodItem functionality
    @Test
    fun cartAndFoodItem_crudAndJunction() = runBlocking {
        val db = newDb()

        // create a cart
        val cart = db.createCart(CartModel())!!
        val cartIdStr = cart.id.toString()

        // create two food items
        val food1 = FoodItem(
            name = "Milk",
            price = 4.0,
            barcode = "333",
            purchasedDate = "2025/03/01",
            expirationDate = "2099/03/01",
            owners = mutableListOf(),
            quantity = 1.0
        )
        val food2 = FoodItem(
            name = "Bread",
            price = 2.5,
            barcode = "444",
            purchasedDate = "2025/03/02",
            expirationDate = "2099/03/02",
            owners = mutableListOf(),
            quantity = 2.0
        )

        val createdFood1 = db.createFoodItem(food1)!!
        val createdFood2 = db.createFoodItem(food2)!!

        // link both to the same cart
        assertTrue(db.addItemToCart(cartIdStr, createdFood1.id.toString()))
        assertTrue(db.addItemToCart(cartIdStr, createdFood2.id.toString()))

        // getCartItems
        val cartItems = db.getCartItems(cartIdStr)
        assertEquals("expected 2 items in cart", 2, cartItems.size)
        assertTrue(cartItems.any { it.id == createdFood1.id })
        assertTrue(cartItems.any { it.id == createdFood2.id })

        // populated getCart
        val fetchedCart = db.getCart(cartIdStr)
        assertNotNull("getCart returned null", fetchedCart)
        fetchedCart!!
        assertEquals(cart.id, fetchedCart.id)
        assertTrue(
            "getCart.items missing milk",
            fetchedCart.items.any { it.id == createdFood1.id }
        )
        assertTrue(
            "getCart.items missing bread",
            fetchedCart.items.any { it.id == createdFood2.id }
        )

        // populated getFoodItem: owners should list this cart
        val fetchedFood1 = db.getFoodItem(createdFood1.id.toString())
        assertNotNull("getFoodItem for milk returned null", fetchedFood1)
        fetchedFood1!!
        assertTrue(
            "milk.owners missing cart id",
            fetchedFood1.owners.contains(cartIdStr)
        )

        // remove one item from cart
        val removeItemOk = db.removeItemFromCart(cartIdStr, createdFood1.id.toString())
        assertTrue("removeItemFromCart failed", removeItemOk)

        val cartItemsAfterRemove = db.getCartItems(cartIdStr)
        assertTrue(
            "milk still present after removeItemFromCart",
            cartItemsAfterRemove.none { it.id == createdFood1.id }
        )
        assertTrue(
            "bread missing after removeItemFromCart (should remain)",
            cartItemsAfterRemove.any { it.id == createdFood2.id }
        )

        // delete food and cart
        assertTrue("deleteFoodItem (milk) failed", db.deleteFoodItem(createdFood1.id.toString()))
        assertTrue("deleteFoodItem (bread) failed", db.deleteFoodItem(createdFood2.id.toString()))
        assertTrue("deleteCart failed", db.deleteCart(cart.id))
    }
}

package model

import k.nutriguard.domain.Allergen
import k.nutriguard.domain.CartModel
import k.nutriguard.domain.FoodItem
import k.nutriguard.domain.Group
import k.nutriguard.domain.UserProfile
import org.junit.Test

import org.junit.Assert.*
import java.util.UUID

class GroupTest {
    private val ownerCart = CartModel()
    private val u2Cart = CartModel()
    private val u3Cart = CartModel()
    private val owner = UserProfile(username = "Owner", personalCartId = ownerCart.id)
    private val u2 = UserProfile(username = "Masood", personalCartId = u2Cart.id)
    private val u3 = UserProfile(username = "Sam", personalCartId = u3Cart.id)

    // small helper below to create items with required owners field
    private fun item(name: String, owners: MutableList<String> = mutableListOf(ownerCart.id.toString())) =
        FoodItem(name = name, barcode = name, owners = owners)

    // isMember Function

    @Test
    fun `owner is always a member`() {
        val g = Group(name = "Roommates", owner = owner.username, sharedCartId = UUID.randomUUID(),)
        assertTrue(g.isMember(owner.username))
        // owner doesn't need to appear in memberIds
        assertFalse(owner.username in g.members)
    }

    @Test
    fun `regular member is recognized and non-member is not`() {
        val g = Group(
            name = "Roommates", owner = owner.username,
            members = mutableSetOf(u2.username),
            sharedCartId = UUID.randomUUID(),
        )
        assertTrue(g.isMember(u2.username))
        assertFalse(g.isMember(u3.username))
    }

    // addUser & removerUser Functions

    @Test
    fun `addUser adds non-owner and is idempotent`() {
        val g = Group(name = "Roommates", owner = owner.username, sharedCartId = UUID.randomUUID(),)
        g.addUser(u2.username)
        g.addUser(u2.username) // no duplicates
        assertTrue(g.isMember(u2.username))
        assertEquals(1, g.members.size)
    }

    @Test
    fun `removeUser cannot remove owner but removes member`() {
        val g = Group(
            name = "Roommates", owner = owner.username,
            members = mutableSetOf(u2.username, u3.username),
            sharedCartId = UUID.randomUUID(),
        )
        g.removeUser(owner.username) // this should do nothing
        assertTrue(g.isMember(owner.username))

        g.removeUser(u2.username)
        assertFalse(g.isMember(u2.username))
        assertTrue(g.isMember(u3.username))
    }

    // addItem & removeItem Functions

    @Test
    fun `addItem inserts items into shared cart`() {
        val g = Group(name = "Roommates", owner = owner.username, sharedCartId = UUID.randomUUID(),)
        val milk = item("milk")
        val bread = item("bread")
        g.addItem(milk)
        g.addItem(bread)

        assertTrue(milk in g.sharedCart)
        assertTrue(bread in g.sharedCart)
        assertEquals(2, g.sharedCart.size)
    }

    @Test
    fun `removeItem removes by predicate`() {
        val g = Group(name = "Roommates", owner = owner.username, sharedCartId = UUID.randomUUID(),)
        val a = item("a")
        val b = item("b")
        g.addItem(a)
        g.addItem(b)
        g.removeItem { it === a } // removing exactly 'a'

        assertFalse(a in g.sharedCart)
        assertTrue(b in g.sharedCart)
        assertEquals(1, g.sharedCart.size)
    }

    // Constructor Defaults & Assignments

    @Test
    fun `defaults are empty for members and cart`() {
        val g = Group(name = "Roommates", owner = "U1", sharedCartId = UUID.randomUUID(),)
        assertTrue(g.members.isEmpty())
        assertTrue(g.sharedCart.isEmpty())
    }

    @Test
    fun `constructor assigns custom memberIds`() {
        val g = Group(
            name = "Roommates", owner = "U1",
            members = mutableSetOf("U2", "U3"),
            sharedCartId = UUID.randomUUID(),
        )
        assertTrue(g.isMember("U2"))
        assertTrue(g.isMember("U3"))
    }
}
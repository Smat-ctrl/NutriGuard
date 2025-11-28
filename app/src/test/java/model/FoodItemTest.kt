package model

import k.nutriguard.domain.Allergen
import k.nutriguard.domain.CartModel
import k.nutriguard.domain.FoodItem
import k.nutriguard.domain.UserProfile
import org.junit.Test

import org.junit.Assert.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FoodItemTest {
    private val userPeanutCart = CartModel()
    private val userMilkCart = CartModel()
    private val noAllergiesCart = CartModel()
    private val userPeanut = UserProfile(username = "Sam", personalCartId = userPeanutCart.id, allergies = mutableSetOf(Allergen.PEANUT))
    private val userMilk = UserProfile(username = "Alex", personalCartId = userMilkCart.id, allergies = mutableSetOf(Allergen.MILK))
    private val noAllergies = UserProfile(username = "Taylor", personalCartId = noAllergiesCart.id, allergies = mutableSetOf())



    // Contains Allergen Function

    @Test
    fun `containsAllergen returns true when user has matching allergen`() {
        val f = FoodItem(allergens = mutableSetOf(Allergen.PEANUT), owners = mutableListOf(userPeanutCart.id.toString()))
        assertTrue(f.containsAllergen(userPeanut))
    }

    @Test
    fun `containsAllergen returns false when user has no matching allergen`() {
        val f = FoodItem(allergens = mutableSetOf(), owners = mutableListOf(noAllergiesCart.id.toString()))
        assertFalse(f.containsAllergen(noAllergies))
    }

    @Test
    fun `no matching allergies`() {
        val f = FoodItem(allergens = mutableSetOf(Allergen.SOY), owners = mutableListOf(userMilkCart.id.toString()))
        assertFalse(f.containsAllergen(userMilk))

    }

    // Is Expired Function

    @Test
    fun `returns true when expiration date is before today`() {
        val pastDate = LocalDate.now().minusDays(5)
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val f = FoodItem(expirationDate = pastDate.format(formatter), owners = mutableListOf(userPeanutCart.id.toString()))
        assertTrue(f.isExpired())
    }

    @Test
   fun `returns false when expiration date equals today`() {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val f = FoodItem(expirationDate = today.format(formatter), owners = mutableListOf(userPeanutCart.id.toString()))
        assertFalse(f.isExpired())
    }

    @Test
      fun `returns false when blank`() {
        val f = FoodItem(purchasedDate = "", expirationDate = "", owners = mutableListOf(userPeanutCart.id.toString()))
        assertFalse(f.isExpired())
    }

    // Price Functions

    fun `returns default`() {
        val f = FoodItem(owners = mutableListOf(userPeanutCart.id.toString()))
        assertEquals(0.0, f.price)
    }

    fun `get correct non default price`() {
        val f = FoodItem(price = 12.50, owners = mutableListOf(userPeanutCart.id.toString()))
        assertEquals(12.50, f.price)
    }
}

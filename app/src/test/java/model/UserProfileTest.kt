package model

import k.nutriguard.domain.Allergen
import k.nutriguard.domain.CartModel
import k.nutriguard.domain.DietaryRestriction
import k.nutriguard.domain.UserProfile
import org.junit.Test

import org.junit.Assert.*
import java.util.UUID

class UserProfileTest {

   @Test
    fun createUser(){
        val cart = CartModel()
        val userProfile =
            UserProfile("Willy", UUID.randomUUID(), authId = UUID.randomUUID(), dietaryRestrictions = mutableSetOf(DietaryRestriction.NONE), personalCartId = cart.id, allergies = mutableSetOf(Allergen.GLUTEN))
        //assert(userProfile.tasks.isEmpty())
       assertTrue(DietaryRestriction.NONE in userProfile.dietaryRestrictions)
       assertTrue(Allergen.GLUTEN in userProfile.allergies)
    }
    @Test
    fun removeDietaryRestriction(){
        val cart = CartModel()
        val up = UserProfile(
            "Tri",
            UUID.randomUUID(),
            personalCartId = cart.id,
            dietaryRestrictions = mutableSetOf(DietaryRestriction.HALAL, DietaryRestriction.VEGAN),
            allergies = mutableSetOf(Allergen.GLUTEN, Allergen.TREE_NUT)
        )

        up.removeRestriction(DietaryRestriction.HALAL)
        assertTrue(DietaryRestriction.VEGAN in up.dietaryRestrictions)

    }

    @Test
    fun addAllergy(){
        val cart = CartModel()
        val up = UserProfile(
            "Titou", UUID.randomUUID(), authId = UUID.randomUUID(),
            mutableSetOf(DietaryRestriction.KOSHER, DietaryRestriction.HALAL),
            mutableSetOf(
                Allergen.SHELLFISH,
                Allergen.EGG
            ),
            personalCartId = cart.id
        )

        up.addAllergy(Allergen.SESAME)
        assertTrue(Allergen.SESAME in up.allergies)
        assertFalse(DietaryRestriction.PESCETARIAN in up.dietaryRestrictions)
    }


}
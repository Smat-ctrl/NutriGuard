package k.nutriguard.domain

import java.util.UUID

enum class DietaryRestriction { NONE, VEGETARIAN, VEGAN, HALAL, KOSHER, PESCETARIAN }
enum class Allergen { PEANUT, TREE_NUT, MILK, EGG, FISH, SHELLFISH, WHEAT, SOY, SESAME, GLUTEN }

data class UserProfile(
    val username: String,
    val id: UUID = UUID.randomUUID(),
    val authId: UUID? = null,

    val dietaryRestrictions: MutableSet<DietaryRestriction> = mutableSetOf(),
    val allergies: MutableSet<Allergen> = mutableSetOf(),

    val groupIds: MutableSet<String> = mutableSetOf(),
    val friendIds: MutableSet<String> = mutableSetOf(), //friend names

    val personalCart: MutableSet<FoodItem> = mutableSetOf(),
    val personalCartId: UUID,
    ) {

    fun addRestriction(r: DietaryRestriction) { dietaryRestrictions += r }
    fun removeRestriction(r: DietaryRestriction) { dietaryRestrictions -= r }

    fun addAllergy(a: Allergen) { allergies += a }
    fun removeAllergy(a: Allergen) { allergies -= a }
}







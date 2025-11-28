package viewmodel

import db.MockDBRepository
import k.nutriguard.domain.Allergen
import k.nutriguard.domain.CartModel
import k.nutriguard.domain.DietaryRestriction
import k.nutriguard.domain.UserProfile
import k.nutriguard.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setupDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDownDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onAllergyToggled adds and removes labels and keeps them sorted`() {
        val db = MockDBRepository()
        val vm = ProfileViewModel(db)

        // start empty
        assertTrue(vm.uiState.value.allergies.isEmpty())

        vm.onAllergyToggled("Peanut")
        vm.onAllergyToggled("Shellfish")
        vm.onAllergyToggled("Dairy")

        var s = vm.uiState.value
        assertEquals(listOf("Dairy", "Peanut", "Shellfish"), s.allergies)

        // toggle Peanut again
        vm.onAllergyToggled("Peanut")
        s = vm.uiState.value
        assertEquals(listOf("Dairy", "Shellfish"), s.allergies)
    }

    @Test
    fun `onDietaryRestrictionToggled adds and removes labels and keeps them sorted`() {
        val db = MockDBRepository()
        val vm = ProfileViewModel(db)

        vm.onDietaryRestrictionToggled("Vegetarian")
        vm.onDietaryRestrictionToggled("Vegan")
        vm.onDietaryRestrictionToggled("Gluten free")

        var s = vm.uiState.value
        assertEquals(listOf("Gluten free", "Vegan", "Vegetarian"), s.dietaryRestrictions)

        vm.onDietaryRestrictionToggled("Vegan")
        s = vm.uiState.value
        assertEquals(listOf("Gluten free", "Vegetarian"), s.dietaryRestrictions)
    }

    @Test
    fun `onSaveProfileClicked updates user allergies and restrictions in db`() = runBlocking {
        val db = MockDBRepository()

        // Pre-create a user
        val cart = CartModel(UUID.randomUUID(), mutableListOf())
        db.createCart(cart)
        db.createUser(
            UserProfile(
                username = "john_doe",
                personalCartId = cart.id
            )
        )

        val vm = ProfileViewModel(db)
        vm.onUsernameChanged("john_doe")

        // toggle some allergies
        vm.onAllergyToggled("Peanut")
        vm.onAllergyToggled("Shellfish")
        vm.onDietaryRestrictionToggled("Vegetarian")

        vm.onSaveProfileClicked()
        delay(1)

        val s = vm.uiState.value
        assertFalse(s.isLoading)
        assertNull(s.errorMessage)

        // ui state should still have readable strings
        assertEquals(listOf("Peanut", "Shellfish"), s.allergies)
        assertEquals(listOf("Vegetarian"), s.dietaryRestrictions)

        // verify DB user got updated enums
        val dbUser = db.getUser("john_doe")!!
        assertTrue(Allergen.PEANUT in dbUser.allergies)
        assertTrue(Allergen.SHELLFISH in dbUser.allergies)
        assertTrue(DietaryRestriction.VEGETARIAN in dbUser.dietaryRestrictions)
    }

    @Test
    fun `addFriendByUsername adds friend when user exists`() = runBlocking {
        val db = MockDBRepository()

        // base user in DB so updateUser works
        val cart = CartModel(UUID.randomUUID(), mutableListOf())
        db.createCart(cart)
        db.createUser(
            UserProfile(
                username = "me",
                personalCartId = cart.id
            )
        )

        // friend to add
        val friendCart = CartModel(UUID.randomUUID(), mutableListOf())
        db.createCart(friendCart)
        db.createUser(
            UserProfile(
                username = "friend",
                personalCartId = friendCart.id
            )
        )

        val vm = ProfileViewModel(db)
        vm.onUsernameChanged("me")

        vm.onSaveProfileClicked()
        delay(1)

        vm.addFriendByUsername("friend")
        delay(1)

        val s = vm.uiState.value
        assertFalse(s.isLoading)
        assertNull(s.errorMessage)
        assertEquals(listOf("friend"), s.friends)
    }

    @Test
    fun `addFriendByUsername sets error when user does not exist`() = runBlocking {
        val db = MockDBRepository()

        // base user in DB so updateUser works
        val cart = CartModel(UUID.randomUUID(), mutableListOf())
        db.createCart(cart)
        db.createUser(
            UserProfile(
                username = "me",
                personalCartId = cart.id
            )
        )

        val vm = ProfileViewModel(db)
        vm.onUsernameChanged("me")

        vm.onSaveProfileClicked()
        delay(1)

        vm.addFriendByUsername("ghost_user")
        delay(1)

        val s = vm.uiState.value
        assertFalse(s.isLoading)
        assertEquals("User does not exist", s.errorMessage)
        assertTrue(s.friends.isEmpty())
    }

    @Test
    fun `removeFriend removes friend from uiState and loadedUser`() = runBlocking {
        val db = MockDBRepository()

        // base user in DB so updateUser works
        val cart = CartModel(UUID.randomUUID(), mutableListOf())
        db.createCart(cart)
        db.createUser(
            UserProfile(
                username = "me",
                personalCartId = cart.id
            )
        )

        // friend user exists
        val friendCart = CartModel(UUID.randomUUID(), mutableListOf())
        db.createCart(friendCart)
        db.createUser(
            UserProfile(
                username = "friend",
                personalCartId = friendCart.id
            )
        )

        val vm = ProfileViewModel(db)
        vm.onUsernameChanged("me")

        vm.onSaveProfileClicked()
        delay(1)

        // first add friend
        vm.addFriendByUsername("friend")
        delay(1)
        assertEquals(listOf("friend"), vm.uiState.value.friends)

        // now remove friend
        vm.removeFriend("friend")
        delay(1)

        val s = vm.uiState.value
        assertFalse(s.isLoading)
        assertNull(s.errorMessage)
        assertTrue(s.friends.isEmpty())
    }
}

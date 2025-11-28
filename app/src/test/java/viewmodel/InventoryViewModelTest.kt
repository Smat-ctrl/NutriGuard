package model

import db.MockDBRepository
import k.nutriguard.db.DBInterface
import k.nutriguard.domain.Allergen
import k.nutriguard.domain.CartModel
import k.nutriguard.domain.FoodItem
import k.nutriguard.viewmodel.InventoryItem
import k.nutriguard.viewmodel.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.junit.Assert.*

private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

     // Helper to create a FoodItem
    private fun foodItem(
        name: String,
        expiresOn: LocalDate,
        cartId: UUID? = null
    ): FoodItem {
        return FoodItem(
            id = UUID.randomUUID(),
            name = name,
            expirationDate = expiresOn.format(formatter),
            owners = cartId?.let { mutableListOf(it.toString()) } ?: mutableListOf(),
            ingredients = mutableListOf(),
            allergens = mutableSetOf<Allergen>()
        )
    }

    // Helper for InventoryItem used in isExpired tests.
    private fun mockInventoryItem(
        id: String,
        name: String,
        expiresOn: LocalDate
    ) = InventoryItem(
        id = id,
        food = FoodItem(
            id = UUID.randomUUID(),
            name = name,
            expirationDate = expiresOn.format(formatter),
            owners = mutableListOf(),
            ingredients = mutableListOf(),
            allergens = mutableSetOf()
        )
    )

     // Seed a cart with 3 items: Chicken Breast, Milk, Bread.
    private suspend fun seedCartWithThreeItems(db: DBInterface, cartId: UUID) {
        db.createCart(
            CartModel(
                id = cartId,
                items = mutableListOf()
            )
        )

        val chicken = foodItem("Chicken Breast", LocalDate.now().plusDays(3), cartId)
        val milk = foodItem("Milk", LocalDate.now().plusDays(5), cartId)
        val bread = foodItem("Bread", LocalDate.now().plusDays(2), cartId)

        listOf(chicken, milk, bread).forEach { item ->
            val created = db.createFoodItem(item)!!
            db.addItemToCart(cartId.toString(), created.id.toString())
        }
    }

    @Test
    fun `loadUserCart loads items from db into uiState`() = runTest {
        val db = MockDBRepository()
        val cartId = UUID.randomUUID()
        seedCartWithThreeItems(db, cartId)

        val vm = InventoryViewModel(db = db)
        vm.loadUserCart(cartId)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(3, state.items.size)
        assertTrue(state.items.any { it.name == "Chicken Breast" })
        assertTrue(state.items.any { it.name == "Milk" })
        assertTrue(state.items.any { it.name == "Bread" })
        assertNull(state.errorMessage)
    }

    @Test
    fun `addMock adds a new item to the list and associates it with cart`() = runTest {
        val db = MockDBRepository()
        val cartId = UUID.randomUUID()
        db.createCart(CartModel(id = cartId, items = mutableListOf()))

        val vm = InventoryViewModel(db = db)

        val initialSize = vm.uiState.value.items.size

        val customFood = foodItem(
            name = "Test Item",
            expiresOn = LocalDate.now().plusDays(7)
        )

        vm.addMock(customFood, cartId)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(initialSize + 1, state.items.size)
        assertTrue(state.items.any { it.name == "Test Item" })
        assertNull(state.errorMessage)

        // verify DB cart items also contain it
        val cartItems = db.getCartItems(cartId.toString())
        assertTrue(cartItems.any { it.name == "Test Item" })
    }

    @Test
    fun `addCustomItem uses New Item as default name when blank`() = runTest {
        val db = MockDBRepository()
        val cartId = UUID.randomUUID()
        db.createCart(CartModel(id = cartId, items = mutableListOf()))

        val vm = InventoryViewModel(db = db)

        vm.addCustomItem(
            name = "",
            allergens = emptySet(),
            ingredients = mutableListOf(),
            expirationDate = LocalDate.now().plusDays(4).format(formatter),
            cartId = cartId
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.items.any { it.name == "New Item" })
        assertNull(state.errorMessage)
    }

    @Test
    fun `deleteItem removes the correct item from uiState and db`() = runTest {
        val db = MockDBRepository()
        val cartId = UUID.randomUUID()
        seedCartWithThreeItems(db, cartId)

        val vm = InventoryViewModel(db = db)
        vm.loadUserCart(cartId)
        advanceUntilIdle()

        val initialItems = vm.uiState.value.items
        assertTrue(initialItems.any { it.name == "Milk" })

        val milkItem = initialItems.first { it.name == "Milk" }
        vm.deleteItem(milkItem.id, cartId)
        advanceUntilIdle()

        val after = vm.uiState.value.items
        assertFalse(after.any { it.id == milkItem.id })
        assertEquals(initialItems.size - 1, after.size)

        // Verify DB also no longer contains that food in the cart
        val cartItemsAfter = db.getCartItems(cartId.toString())
        assertFalse(cartItemsAfter.any { it.name == "Milk" })
    }

    @Test
    fun `deleteItem does not affect other items`() = runTest {
        val db = MockDBRepository()
        val cartId = UUID.randomUUID()
        seedCartWithThreeItems(db, cartId)

        val vm = InventoryViewModel(db = db)
        vm.loadUserCart(cartId)
        advanceUntilIdle()

        val milkItem = vm.uiState.value.items.first { it.name == "Milk" }
        vm.deleteItem(milkItem.id, cartId)
        advanceUntilIdle()

        val after = vm.uiState.value.items
        assertTrue(after.any { it.name == "Chicken Breast" })
        assertTrue(after.any { it.name == "Bread" })
    }

    @Test
    fun `isExpired correctly identifies expired items`() {
        val expired = mockInventoryItem("x", "Old Cheese", LocalDate.now().minusDays(10))
        val fresh = mockInventoryItem("y", "Fresh Eggs", LocalDate.now().plusDays(3))

        assertTrue(expired.isExpired)
        assertFalse(fresh.isExpired)
    }
}

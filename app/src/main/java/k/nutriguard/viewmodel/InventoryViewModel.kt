package k.nutriguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import k.nutriguard.domain.Allergen
import k.nutriguard.domain.FoodItem
import k.nutriguard.repository.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import k.nutriguard.repository.DBModule
import k.nutriguard.db.DBInterface

// Below is the class for the UI wrapper around a FoodItem with some helper fields
data class InventoryItem(
    val id: String,
    val food: FoodItem
) {
    val name: String get() = food.name
    val allergies: List<Allergen> get() = food.allergens.toList()
    val ingredients: List<String> get() = food.ingredients.toList()
    val isExpired: Boolean get() = food.isExpired()
}

class InventoryViewModel(
    // All we need is the repo to talk to the API and access to the DB interface here
    private val repo: FoodRepository = FoodRepository(),
    private val db: DBInterface = DBModule.db
) : ViewModel() {

    data class UiState(
        // All the relevant fields for the UI state for the inventory screen
        val items: List<InventoryItem> = emptyList(),
        val query: String = "",
        val isSearching: Boolean = false,
        val results: List<FoodItem> = emptyList(),
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState()) // the backing state that the ViewModel mutates
    val uiState: StateFlow<UiState> = _uiState // read-only state exposted to the UI

    // This function is called when user types in the search bar
    fun onQueryChange(text: String) {
        _uiState.value = _uiState.value.copy(query = text)
    }

    // This triggers a name-based search using the current query
    fun searchNow() {
        val q = _uiState.value.query.trim()
        if (q.isEmpty()) return
        // We used viewModelScope.launch so the database works in a coroutine tied to the ViewModel's lifecycle
        // It keeps the long-running work off the main the thread so we can call db methods without ever freezing the UI
        /* When the scree is closed/ViewModel is cleared, viewModelScope automatically cancels any running coroutines
        so they don't leak or keep doing work unnecessarily. Basically allows us to have async work in the ViewModel.*/
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, results = emptyList())
            try {
                val hits: List<FoodItem> = repo.searchByName(q, pageSize = 12)
                _uiState.value = _uiState.value.copy(results = hits) // update the state with new results
            } finally {
                _uiState.value = _uiState.value.copy(isSearching = false) // always stop loading
            }
        }
    }

    // This function specifically triggers lookup for a single product by barcode
    fun searchByBarcode(barcode: String) {
        val q = barcode.trim()
        if (q.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, results = emptyList())
            try {
                val item: FoodItem? = repo.searchByBarcode(q)
                _uiState.value = _uiState.value.copy(
                    results = item?.let { listOf(it) } ?: emptyList() // This line tries to wrap it in a list or returns a empty list
                )
            } finally {
                _uiState.value = _uiState.value.copy(isSearching = false)
            }
        }
    }

    // This is called by the UI when the barcode scanner returns a code
    fun onBarcodeScanned(code: String) {
        onQueryChange(code) // we use this to update the search field to show the scanned code
        searchByBarcode(code) // we then immediately search with that barcode using the helper
    }

    // Simple function to clear search results from UI
    fun clearResults() {
        _uiState.value = _uiState.value.copy(results = emptyList())
    }

    // This removes an item from both the cart and database
    fun deleteItem(id: String, cartId: UUID) {
        viewModelScope.launch {
            try {
                // This is for first removing it from the cart in DB
                val okok = db.removeItemFromCart(cartId.toString(), id)
                if (!okok) {
                    _uiState.value = _uiState.value.copy(errorMessage = "Failed to delete it from cart")
                    return@launch
                }
                // This deletes the original food item itself from the DB
                val ok = db.deleteFoodItem(id)
                if (!ok) {
                    _uiState.value = _uiState.value.copy(errorMessage = "Failed to delete item")
                    return@launch
                }
                _uiState.value = _uiState.value.copy(
                    items = _uiState.value.items.filterNot { it.id == id },
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to delete item"
                )
            }
        }
    }

    // This function adds a FoodItem to the DB and then to the user's cart
    // It also then adds it to the UI list for the user to see
    fun addMock(food: FoodItem, cartId: UUID) {
        viewModelScope.launch {
            try {
                val created = db.createFoodItem(food) // trying to create a new food item in the DB
                    ?: throw IllegalStateException("Failed to create item")
                db.addItemToCart(cartId.toString(), food.id.toString())
                // Then we just wrap the DB object into an InventoryItem to use in our UI
                val newInvItem = InventoryItem(
                    id = created.id.toString(),
                    food = created
                )

                // This is just for appending the new item to current inventory list
                // It also clears the error on success
                _uiState.value = _uiState.value.copy(
                    items = _uiState.value.items + newInvItem,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to add item"
                )
            }
        }
    }

    // This is similar to earlier but builds a FoodItem from user input and reuses addMock to save it
    fun addCustomItem(name: String, allergens: Set<Allergen>, ingredients: MutableList<String>, expirationDate: String, cartId: UUID) {
        val customFood = FoodItem(
            // These are the specifications for a custom food item with some relevant default values
            // We left 'owners' empty for now but it's tied through
            name = name.ifBlank { "New Item" },
            allergens = allergens.toMutableSet(),
            ingredients = ingredients,
            expirationDate = expirationDate,
            owners = mutableListOf()
        )
        addMock(customFood, cartId) // Save to the DB and attach to the user's cart
    }

    // This loads all the items belonging to this user's cart from the DB
    fun loadUserCart(cartId: UUID) {
        viewModelScope.launch {
            try {
                val foodItems = db.getCartItems(cartId.toString()) // querying DB for all items in this cart
                val invItems = foodItems.map { food ->
                    InventoryItem(
                        id = food.id.toString(),
                        food = food
                    )
                }
                // All this above converts each DB item into InventoryItem for UI
                // This code below replaces the current inventory with loaded items and clears error if load worked
                _uiState.value = _uiState.value.copy(
                    items = invItems,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to load inventory"
                )
            }
        }
    }
}

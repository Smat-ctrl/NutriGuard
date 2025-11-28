package k.nutriguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import k.nutriguard.repository.DBModule
import k.nutriguard.db.DBInterface
import k.nutriguard.domain.CartModel
import k.nutriguard.domain.UserProfile
import k.nutriguard.domain.Allergen
import k.nutriguard.domain.DietaryRestriction
import kotlinx.coroutines.launch
import java.util.UUID

class ProfileViewModel(
    private val db: DBInterface = DBModule.db
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,

        val username: String = "",
        val initials: String = "",
        val allergies: List<String> = emptyList(),
        val dietaryRestrictions: List<String> = emptyList(),

        val friends: List<String> = emptyList(),

        // edit flags
        val isEditingUsername: Boolean = false,
        val isEditingAllergies: Boolean = false,
        val isEditingDietaryRestrictions: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState(isLoading = true))
    val uiState: StateFlow<UiState> = _uiState

    // Keep the full user loaded from DB (id, cartId, groups, friends, etc.)
    private var loadedUser: UserProfile? = null

    // new or exisiting user
    fun loadOrCreateUser(username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                var user = db.getUser(username)

                if (user == null) {
                    val newCart = CartModel(
                        id = UUID.randomUUID(),
                        items = mutableListOf()
                    )

                    val insertedCart = db.createCart(newCart)
                        ?: throw IllegalStateException("Failed to create cart")

                    val newUser = UserProfile(
                        username = username,
                        personalCartId = insertedCart.id
                    )

                    user = db.createUser(newUser)
                        ?: throw IllegalStateException("Failed to create user")
                }

                // store the full user so we keep id, personalCartId, groupIds, etc.
                loadedUser = user

                _uiState.update {
                    UiState(
                        isLoading = false,
                        errorMessage = null,
                        username = user.username,
                        initials = computeInitials(user.username),
                        allergies = user.allergies.map { a -> enumToReadable(a) }.sorted(),
                        dietaryRestrictions = user.dietaryRestrictions.map { d -> enumToReadable(d) }.sorted(),
                        friends = user.friendIds.toList().sorted(),
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load or create user"
                    )
                }
            }
        }
    }

    // edit and save
    fun onEditClicked() {
        val state = _uiState.value
        if (state.isEditingUsername ||
            state.isEditingAllergies ||
            state.isEditingDietaryRestrictions
        ) {
            // If anything is editing -> save all
            onSaveProfileClicked()
        } else {
            _uiState.update { it.copy(isEditingUsername = true) }
        }
    }

    fun onAllergyEditClicked() {
        val state = _uiState.value
        if (state.isEditingAllergies ||
            state.isEditingUsername ||
            state.isEditingDietaryRestrictions
        ) {
            onSaveProfileClicked()
        } else {
            _uiState.update { it.copy(isEditingAllergies = true) }
        }
    }

    fun onDietaryEditClicked() {
        val state = _uiState.value
        if (state.isEditingDietaryRestrictions ||
            state.isEditingUsername ||
            state.isEditingAllergies
        ) {
            onSaveProfileClicked()
        } else {
            _uiState.update { it.copy(isEditingDietaryRestrictions = true) }
        }
    }

    // Text change handlers
    fun onUsernameChanged(newUsername: String) {
        _uiState.update {
            it.copy(
                username = newUsername,
                initials = computeInitials(newUsername)
            )
        }
    }

    fun onAllergyToggled(label: String) {
        _uiState.update { state ->
            val newList = state.allergies.toMutableList()
            if (newList.contains(label)) newList.remove(label) else newList.add(label)
            state.copy(allergies = newList.sorted())
        }
    }

    fun onDietaryRestrictionToggled(label: String) {
        _uiState.update { state ->
            val newList = state.dietaryRestrictions.toMutableList()
            if (newList.contains(label)) newList.remove(label) else newList.add(label)
            state.copy(dietaryRestrictions = newList.sorted())
        }
    }

    // Save to database
    fun onSaveProfileClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val state = _uiState.value

                // Use the loadedUser as the base – keeps id, personalCartId, groups, etc.
                var baseUser = loadedUser

                // If somehow we don't have one (shouldn't normally happen), create fresh
                if (baseUser == null) {
                    val cart = CartModel(UUID.randomUUID(), mutableListOf())
                    val newCart = db.createCart(cart)
                        ?: throw IllegalStateException("Failed to create cart")

                    baseUser = UserProfile(
                        username = state.username,
                        personalCartId = newCart.id
                    )
                }

                val allergyEnums = state.allergies
                    .mapNotNull { readableToEnum<Allergen>(it) }
                    .toMutableSet()

                val restrictionEnums = state.dietaryRestrictions
                    .mapNotNull { readableToEnum<DietaryRestriction>(it) }
                    .toMutableSet()

                // Copy all other fields from baseUser, only change what user edited
                val updated = baseUser.copy(
                    username = state.username,
                    allergies = allergyEnums,
                    dietaryRestrictions = restrictionEnums
                )

                val ok = db.updateUser(updated)
                if (!ok) throw IllegalStateException("Failed to update user")


                loadedUser = updated

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEditingUsername = false,
                        isEditingAllergies = false,
                        isEditingDietaryRestrictions = false,
                        initials = computeInitials(updated.username)
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to save profile"
                    )
                }
            }
        }
    }

    // Helpers
    private fun computeInitials(username: String): String {
        val clean = username.replace("_", " ").trim()
        val parts = clean.split(" ")
        return when {
            parts.isEmpty() -> ""
            parts.size == 1 -> parts.first().take(2).uppercase()
            else -> (parts.first().first().toString() +
                    parts.last().first().toString()).uppercase()
        }
    }

    private fun enumToReadable(value: Enum<*>): String =
        value.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }

    private inline fun <reified E : Enum<E>> readableToEnum(readable: String): E? {
        val enumName = readable.uppercase().replace(" ", "_")
        return enumValues<E>().firstOrNull { it.name == enumName }
    }

    fun addFriendByUsername(friendUsername: String) {
        val baseUser = loadedUser ?: return
        val trimmed = friendUsername.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // 1) Ensure the user exists
                val user = db.getUser(trimmed)
                if (user == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "User does not exist"   // Error Message
                        )
                    }
                    return@launch
                }

                // 2) Call DB to add friend
                val ok = db.addFriend(baseUser.username, trimmed)
                if (!ok) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to add friend"
                        )
                    }
                    return@launch
                }

                // 3) Update local model + UI state
                val updatedFriendIds = (baseUser.friendIds + trimmed).toMutableSet()
                val updatedUser = baseUser.copy(friendIds = updatedFriendIds)
                loadedUser = updatedUser

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        friends = updatedFriendIds.toList().sorted()
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to add friend"
                    )
                }
            }
        }
    }

    fun removeFriend(friendUsername: String) {
        val baseUser = loadedUser ?: return
        val trimmed = friendUsername.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val ok = db.removeFriend(baseUser.username, trimmed)
                if (!ok) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to remove friend"
                        )
                    }
                    return@launch
                }

                val updatedFriendIds = baseUser.friendIds
                    .filterNot { it == trimmed }
                    .toMutableSet()

                val updatedUser = baseUser.copy(friendIds = updatedFriendIds)
                loadedUser = updatedUser

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        friends = updatedFriendIds.toList().sorted()
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to remove friend"
                    )
                }
            }
        }
    }

}


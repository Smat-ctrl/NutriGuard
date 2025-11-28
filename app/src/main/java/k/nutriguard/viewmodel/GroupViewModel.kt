package k.nutriguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import k.nutriguard.db.DBInterface
import k.nutriguard.domain.CartModel
import k.nutriguard.domain.FoodItem
import k.nutriguard.domain.Group
import k.nutriguard.repository.DBModule
import k.nutriguard.domain.Allergen
import java.util.UUID

class GroupViewModel(
    private val db: DBInterface = DBModule.db
) : ViewModel() {

    data class UiState(
        val groups: List<Group> = emptyList(),
        val currentUsername: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null,

        val selectedMemberUsername: String? = null,
        val selectedMemberProfile: k.nutriguard.domain.UserProfile? = null,
        val selectedMemberLoading: Boolean = false,
        val selectedMemberError: String? = null
    ) {
        /** Only show groups the current user belongs to (owner or member). */
        val myGroups: List<Group> = groups.filter { it.isMember(currentUsername) }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    /**
     * Called from GroupView when the logged-in user is known.
     * Only reloads if user changed or groups are empty.
     */
    fun initForUser(username: String) {
        if (_uiState.value.currentUsername == username && _uiState.value.groups.isNotEmpty()) return

        _uiState.update { it.copy(currentUsername = username) }
        refreshGroups()
    }

    /** Reload groups for the current user from Supabase. */
    fun refreshGroups() {
        val username = _uiState.value.currentUsername
        if (username.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // IMPORTANT: DBRepository.getUserGroups(username) should populate members + sharedCart
                val groups = db.getUserGroups(username)
                _uiState.update {
                    it.copy(
                        groups = groups,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load groups"
                    )
                }
            }
        }
    }

    /** User Clicked a Member, wants to see their allergies/restrictions */
    fun loadMemberProfile(username: String) {
        viewModelScope.launch {
            // Show dialog immediately with loading state
            _uiState.update {
                it.copy(
                    selectedMemberUsername = username,
                    selectedMemberLoading = true,
                    selectedMemberError = null,
                    selectedMemberProfile = null
                )
            }

            try {
                val user = db.getUser(username)
                    ?: throw IllegalStateException("User not found")

                _uiState.update {
                    it.copy(
                        selectedMemberProfile = user,
                        selectedMemberLoading = false,
                        selectedMemberError = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        selectedMemberLoading = false,
                        selectedMemberError = e.message ?: "Failed to load user profile"
                    )
                }
            }
        }
    }

    /** Close the member details dialog. */
    fun dismissMemberProfile() {
        _uiState.update {
            it.copy(
                selectedMemberUsername = null,
                selectedMemberProfile = null,
                selectedMemberLoading = false,
                selectedMemberError = null
            )
        }
    }

    /** Create a new group owned by the current user and persist it. */
    fun createGroup(name: String) {
        val username = _uiState.value.currentUsername
        if (username.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // 1) Create an empty shared cart for this group
                val newCart = CartModel(
                    id = UUID.randomUUID(),
                    items = mutableListOf()
                )

                val insertedCart = db.createCart(newCart)
                    ?: throw IllegalStateException("Failed to create shared cart")

                // 2) Create the group pointing to that cart
                val group = Group(
                    name = name.ifBlank { "Untitled Group" },
                    id = UUID.randomUUID(),
                    owner = username,
                    members = mutableSetOf(), // owner membership handled via group_members
                    sharedCartId = insertedCart.id,
                    sharedCart = mutableSetOf()
                )

                val created = db.createGroup(group)
                    ?: throw IllegalStateException("Failed to create group")

                // 3) Refresh from DB to keep everything consistent
                refreshGroups()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to create group"
                    )
                }
            }
        }
    }

    /** Delete a group from Supabase (only if current user is owner). */
    fun deleteGroup(groupId: UUID) {
        val username = _uiState.value.currentUsername
        val group = _uiState.value.groups.firstOrNull { it.id == groupId }

        if (group == null) {
            _uiState.update { it.copy(errorMessage = "Group not found") }
            return
        }

        if (group.owner != username) {
            _uiState.update { it.copy(errorMessage = "Only the owner can delete this group.") }
            return
        }

        viewModelScope.launch {
            try {
                val ok = db.deleteGroup(groupId.toString())
                if (!ok) throw IllegalStateException("Failed to delete group")

                _uiState.update { state ->
                    state.copy(
                        groups = state.groups.filterNot { it.id == groupId }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        errorMessage = e.message ?: "Failed to delete group"
                    )
                }
            }
        }
    }

    /** Leave a group (for non-owners who are members). */
    fun leaveGroup(groupId: UUID) {
        val username = _uiState.value.currentUsername
        val group = _uiState.value.groups.firstOrNull { it.id == groupId }

        if (group == null) {
            _uiState.update { it.copy(errorMessage = "Group not found") }
            return
        }

        if (group.owner == username) {
            _uiState.update { it.copy(errorMessage = "Owners cannot leave their own group. You can delete it instead.") }
            return
        }

        if (!group.isMember(username)) {
            _uiState.update { it.copy(errorMessage = "You are not a member of this group.") }
            return
        }

        viewModelScope.launch {
            try {
                val ok = db.removeUserFromGroup(groupId.toString(), username)
                if (!ok) {
                    _uiState.update { it.copy(errorMessage = "Failed to leave group.") }
                    return@launch
                }

                // Refresh groups after leaving
                reloadGroups()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to leave group.")
                }
            }
        }
    }

    /** Add a member (by username) to a group (only owner). */
    fun addMember(groupId: UUID, usernameToAdd: String) {
        val username = _uiState.value.currentUsername
        val group = _uiState.value.groups.firstOrNull { it.id == groupId }

        if (group == null) {
            _uiState.update { it.copy(errorMessage = "Group not found") }
            return
        }

        if (group.owner != username) {
            _uiState.update { it.copy(errorMessage = "Only the owner can add members.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }

            try {
                // 1) Validate user exists
                val user = db.getUser(usernameToAdd)
                if (user == null) {
                    _uiState.update {
                        it.copy(errorMessage = "User \"$usernameToAdd\" doesn't exist.")
                    }
                    return@launch
                }

                // 2) Add to group_members in DB
                val ok = db.addUserToGroup(groupId.toString(), usernameToAdd)
                if (!ok) {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to add member. Please try again.")
                    }
                    return@launch
                }

                // 3) Reload groups so UI updates
                reloadGroups()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Failed to add member."
                    )
                }
            }
        }
    }

    /** Remove a member (only owner; cannot remove owner). */
    fun removeMember(groupId: UUID, usernameToRemove: String) {
        val username = _uiState.value.currentUsername
        val group = _uiState.value.groups.firstOrNull { it.id == groupId }

        if (group == null) {
            _uiState.update { it.copy(errorMessage = "Group not found") }
            return
        }

        if (group.owner != username) {
            _uiState.update { it.copy(errorMessage = "Only the owner can remove members.") }
            return
        }

        if (usernameToRemove == group.owner) {
            _uiState.update { it.copy(errorMessage = "Owner cannot be removed from the group.") }
            return
        }

        viewModelScope.launch {
            try {
                val ok = db.removeUserFromGroup(groupId.toString(), usernameToRemove)
                if (!ok) {
                    _uiState.update { it.copy(errorMessage = "Failed to remove member.") }
                    return@launch
                }

                reloadGroups()
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        errorMessage = e.message ?: "Failed to remove member"
                    )
                }
            }
        }
    }

    /**
     * Remove all non-owner members from the group, both in DB and locally.
     * Only owner can do this.
     */
    fun clearMembers(groupId: UUID) {
        val username = _uiState.value.currentUsername
        val group = _uiState.value.groups.firstOrNull { it.id == groupId }

        if (group == null) {
            _uiState.update { it.copy(errorMessage = "Group not found") }
            return
        }

        if (group.owner != username) {
            _uiState.update { it.copy(errorMessage = "Only the owner can clear members.") }
            return
        }

        viewModelScope.launch {
            try {
                val members = db.getGroupMembers(groupId.toString())

                members.forEach { user ->
                    if (user.username != group.owner) {
                        db.removeUserFromGroup(groupId.toString(), user.username)
                    }
                }

                reloadGroups()
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        errorMessage = e.message ?: "Failed to clear members"
                    )
                }
            }
        }
    }

    /**
     * Add a fully populated item to the shared cart.
     * Any group member can do this.
     */
    fun addItemToSharedCart(
        groupId: UUID,
        itemName: String,
        quantity: Double,
        allergens: Set<Allergen>,
        ingredients: List<String>,
        expirationDate: String
    ) {
        val username = _uiState.value.currentUsername
        val group = _uiState.value.groups.firstOrNull { it.id == groupId }

        if (group == null) {
            _uiState.update { it.copy(errorMessage = "Group not found") }
            return
        }

        if (!group.isMember(username)) {
            _uiState.update { it.copy(errorMessage = "Only group members can modify the shared cart.") }
            return
        }

        viewModelScope.launch {
            try {
                val newItem = FoodItem(
                    id = UUID.randomUUID(),
                    price = 0.0,
                    barcode = "",
                    name = itemName,
                    ingredients = ingredients.toMutableList(),
                    allergens = allergens.toMutableSet(),
                    purchasedDate = "",
                    expirationDate = expirationDate,
                    owners = mutableListOf(group.sharedCartId.toString()),
                    quantity = quantity,
                    imageUrl = ""
                )

                val created = db.createFoodItem(newItem)
                    ?: throw IllegalStateException("Failed to create shared cart item")

                // Reload groups so sharedCart is refreshed with the fully populated item
                reloadGroups()
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        errorMessage = e.message ?: "Failed to add item to shared cart"
                    )
                }
            }
        }
    }

    /**
     * Remove a single item from the shared cart.
     * Any group member can do this.
     */
    fun removeItemFromSharedCart(groupId: UUID, itemId: UUID) {
        val username = _uiState.value.currentUsername
        val group = _uiState.value.groups.firstOrNull { it.id == groupId }

        if (group == null) {
            _uiState.update { it.copy(errorMessage = "Group not found") }
            return
        }

        if (!group.isMember(username)) {
            _uiState.update { it.copy(errorMessage = "Only group members can modify the shared cart.") }
            return
        }

        viewModelScope.launch {
            try {
                val ok = db.removeItemFromCart(group.sharedCartId.toString(), itemId.toString())
                if (!ok) {
                    _uiState.update { it.copy(errorMessage = "Failed to remove item from shared cart") }
                    return@launch
                }

                val ok2 = db.deleteFoodItem(itemId.toString())
                if (!ok2) {
                    _uiState.update { it.copy(errorMessage = "Failed to remove item from database") }
                    return@launch
                }

                reloadGroups()
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        errorMessage = e.message ?: "Failed to remove item from shared cart"
                    )
                }
            }
        }
    }

    /** Helper to reload groups for the current user. */
    private fun reloadGroups() {
        val username = _uiState.value.currentUsername
        if (username.isBlank()) return

        viewModelScope.launch {
            try {
                val groups = db.getUserGroups(username)
                _uiState.update { it.copy(groups = groups, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to refresh groups")
                }
            }
        }
    }
}

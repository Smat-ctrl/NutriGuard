@file:OptIn(ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class)

package k.nutriguard.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import k.nutriguard.domain.FoodItem
import k.nutriguard.domain.UserProfile
import k.nutriguard.viewmodel.GroupViewModel
import java.util.UUID
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExposedDropdownMenuBox
import k.nutriguard.domain.Allergen

// ---- Theme colors (match ProfileView.kt) ------------------------------------
private val BgSurface   = Color(0xFF0E0E0E)
private val CardSurface = Color(0xFF1C1C1C)
private val Divider     = Color(0xFF2A2A2A)
private val NutriYellow = Color(0xFFFFD200)
private val AccentBorder = Color(0xFF7A6A00)
private val MutedText    = Color(0xFFBBBBBB)

@Composable
fun GroupView(viewModel: GroupViewModel, user: UserProfile) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(user.username) {
        viewModel.initForUser(user.username)
    }

    var showCreateDialog by remember { mutableStateOf(false) }

    val selectedMemberUsername = state.selectedMemberUsername

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Groups",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                icon = Icons.Filled.Add,
                label = "Create Group",
                onClick = { showCreateDialog = true }
            )
        }

        // Global error message for group operations
        state.errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }

        // member details dialog
        if (selectedMemberUsername != null) {
            MemberDetailsDialog(
                username = selectedMemberUsername,
                profile = state.selectedMemberProfile,
                isLoading = state.selectedMemberLoading,
                errorMessage = state.selectedMemberError,
                onDismiss = { viewModel.dismissMemberProfile() }
            )
        }

        if (state.isLoading && state.myGroups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NutriYellow)
            }
        } else if (state.myGroups.isEmpty()) {
            Text(
                "You’re not in any groups yet.",
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(state.myGroups, key = { it.id }) { group ->
                    val isOwner = group.owner == state.currentUsername
                    val isMember = group.isMember(state.currentUsername)

                    GroupCard(
                        groupId = group.id,
                        name = group.name,
                        owner = group.owner,
                        members = group.members.toList(),
                        sharedCart = group.sharedCart.toList(),
                        isOwner = isOwner,
                        isMember = isMember,
                        onDelete = { viewModel.deleteGroup(group.id) },
                        onAddMember = { username -> viewModel.addMember(group.id, username) },
                        onRemoveMember = { username -> viewModel.removeMember(group.id, username) },
                        onClearMembers = { viewModel.clearMembers(group.id) },
                        onAddCartItem = { itemName, quantity, allergens, ingredients, expiration ->
                            viewModel.addItemToSharedCart(
                                group.id,
                                itemName,
                                quantity,
                                allergens,
                                ingredients,
                                expiration
                            )
                        },
                        onRemoveCartItem = { itemId ->
                            viewModel.removeItemFromSharedCart(group.id, itemId)
                        },
                        onMemberClick = { username ->
                            viewModel.loadMemberProfile(username)
                        },
                        onLeaveGroup = { viewModel.leaveGroup(group.id) },
                    )
                }
            }
        }
    }

    // Create group dialog
    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createGroup(name)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun GroupCard(
    groupId: UUID,
    name: String,
    owner: String,
    members: List<String>,
    sharedCart: List<FoodItem>,
    isOwner: Boolean,
    isMember: Boolean,
    onDelete: () -> Unit,
    onAddMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onClearMembers: () -> Unit,
    onAddCartItem: (
        String,                // name
        Double,                // quantity
        Set<Allergen>,         // allergens
        List<String>,          // ingredients
        String                 // expirationDate "yyyy/MM/dd"
    ) -> Unit,
    onRemoveCartItem: (UUID) -> Unit,
    onMemberClick: (String) -> Unit,
    onLeaveGroup: () -> Unit,
) {
    // Per-group UI state
    var expanded by rememberSaveable(groupId) { mutableStateOf(false) }
    var showAddMemberDialog by rememberSaveable(groupId) { mutableStateOf(false) }
    var showClearConfirm by rememberSaveable(groupId) { mutableStateOf(false) }
    var showAddCartItemDialog by rememberSaveable(groupId) { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Divider, RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(12.dp)) {
            GroupPillRow(
                name = name,
                isExpanded = expanded,
                onToggleExpand = { expanded = !expanded },
                canDelete = isOwner,
                onDelete = onDelete,
                canLeave = !isOwner && isMember,
                onLeave = onLeaveGroup
            )

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Divider(color = Divider)
                Spacer(Modifier.height(12.dp))

                OwnerRow(ownerId = owner)
                Spacer(Modifier.height(8.dp))

                MembersSection(
                    members = members,
                    isOwner = isOwner,
                    onRemoveMember = onRemoveMember,
                    onMemberClick = onMemberClick
                )

                Spacer(Modifier.height(12.dp))

                // Owner-only member management actions
                if (isOwner) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            icon = Icons.Filled.Add,
                            label = "Add Member",
                            onClick = { showAddMemberDialog = true }
                        )
                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedText),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Remove All Members")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Divider(color = Divider)
                Spacer(Modifier.height(12.dp))

                CartSection(
                    sharedCart = sharedCart,
                    isMember = isMember,
                    onAddCartItem = { name, qty, allergens, ingredients, expiration ->
                        onAddCartItem(name, qty, allergens, ingredients, expiration)
                    },
                    onRemoveCartItem = { itemId -> onRemoveCartItem(itemId) },
                    onShowAddDialog = { showAddCartItemDialog = true }
                )
            }
        }
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { username ->
                onAddMember(username)
                showAddMemberDialog = false
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    onClearMembers()
                    showClearConfirm = false
                }) { Text("Remove", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            },
            title = { Text("Remove all members?") },
            text = { Text("This will remove all non-owner members from the group.") },
            containerColor = CardSurface,
            titleContentColor = Color.White,
            textContentColor = MutedText
        )
    }

    if (showAddCartItemDialog) {
        AddCartItemDialog(
            onDismiss = { showAddCartItemDialog = false },
            onConfirm = { itemName, qty, allergens, ingredients, expiration ->
                onAddCartItem(itemName, qty, allergens, ingredients, expiration)
                showAddCartItemDialog = false
            }
        )
    }
}

@Composable
private fun GroupPillRow(
    name: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    canDelete: Boolean,
    onDelete: () -> Unit,
    canLeave: Boolean,
    onLeave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF262626))
            .border(1.dp, Divider, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(NutriYellow)
                .border(1.dp, AccentBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Group, contentDescription = null, tint = Color.Black)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onToggleExpand) {
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MutedText
            )
        }
        when {
            canDelete -> {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete group",
                        tint = Color(0xFFFF6B6B)
                    )
                }
            }
            canLeave -> {
                TextButton(onClick = onLeave) {
                    Text("Leave", color = Color(0xFFFF6B6B))
                }
            }
        }
    }
}

@Composable
private fun OwnerRow(ownerId: String) {
    Column {
        Text("Owner", color = MutedText, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        MemberChip(userId = ownerId, removable = false, onRemove = {}, onClick = {})
    }
}

@Composable
private fun MembersSection(
    members: List<String>,
    isOwner: Boolean,
    onRemoveMember: (String) -> Unit,
    onMemberClick: (String) -> Unit,
) {
    Column {
        Text("Members (${members.size})", color = MutedText, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        FlowRowWrap(spacing = 8.dp, runSpacing = 8.dp) {
            members.forEach { username ->
                MemberChip(
                    userId = username,
                    removable = isOwner,  // only owner can remove
                    onRemove = { onRemoveMember(username) },
                    onClick = { onMemberClick(username) }
                )
            }
        }
    }
}

@Composable
private fun MemberChip(
    userId: String,
    removable: Boolean,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF2E2E2E))
            .border(1.dp, Divider, RoundedCornerShape(50))
            .clickable { onClick() }              // NEW: click anywhere on the chip to inspect
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(NutriYellow)
                .border(1.dp, AccentBorder, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(userId, color = Color.White, style = MaterialTheme.typography.labelMedium)
        if (removable) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove",
                tint = MutedText,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onRemove() }
            )
        }
    }
}

@Composable
private fun CartSection(
    sharedCart: List<FoodItem>,
    isMember: Boolean,
    onAddCartItem: (
        String,
        Double,
        Set<Allergen>,
        List<String>,
        String
    ) -> Unit,
    onRemoveCartItem: (UUID) -> Unit,
    onShowAddDialog: () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Shared Cart (${sharedCart.size})", color = MutedText, style = MaterialTheme.typography.labelLarge)

            if (isMember) {
                TextButton(onClick = onShowAddDialog) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = NutriYellow)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Item", color = NutriYellow)
                }
            }
        }

        if (sharedCart.isEmpty()) {
            Text(
                "No items yet. Group members can add shared items here.",
                color = MutedText,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sharedCart.forEach { item ->
                    CartItemRow(
                        item = item,
                        canRemove = isMember,
                        onRemove = { onRemoveCartItem(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: FoodItem,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF252525))
            .border(1.dp, Divider, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, color = Color.White, fontWeight = FontWeight.SemiBold)

            val meta = buildString {
                if (item.quantity != null) append("Qty: ${item.quantity}  ")
                if (item.expirationDate.isNotBlank()) append("Exp: ${item.expirationDate}")
            }
            if (meta.isNotBlank()) {
                Text(meta, color = MutedText, style = MaterialTheme.typography.bodySmall)
            }

            if (item.allergens.isNotEmpty()) {
                Text(
                    text = "Allergens: " + item.allergens.joinToString(", ") {
                        niceEnumLabel(it.name)
                    },
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (item.ingredients.isNotEmpty()) {
                Text(
                    text = "Ingredients: " + item.ingredients.joinToString(", "),
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (canRemove) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Remove item",
                tint = Color(0xFFFF6B6B),
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onRemove() }
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NutriYellow,
            contentColor = Color.Black
        )
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun FlowRowWrap(
    spacing: Dp,
    runSpacing: Dp,
    content: @Composable FlowRowScope.() -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(runSpacing),
        content = content
    )
}

@Composable
private fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = input.isNotBlank(),
                onClick = { onConfirm(input.trim()) }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Add Member") },
        text = {
            Column {
                Text("Enter the user's username to add them to the group.", color = MutedText)
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    placeholder = { Text("username") }
                )
            }
        },
        containerColor = CardSurface,
        titleContentColor = Color.White,
        textContentColor = MutedText
    )
}

@Composable
private fun AddCartItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Set<Allergen>, List<String>, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }

    var manualIngredients by remember { mutableStateOf("") }
    var manualExpiration by remember { mutableStateOf("") }

    var allergensExpanded by remember { mutableStateOf(false) }
    var selectedAllergens by remember { mutableStateOf(setOf<Allergen>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val qty = quantityText.toDoubleOrNull() ?: 1.0
                    val ingredientsList = manualIngredients.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    onConfirm(
                        name.trim(),
                        qty,
                        selectedAllergens,
                        ingredientsList,
                        manualExpiration.trim()
                    )
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Add Item to Shared Cart") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food name") },
                    singleLine = true
                )

                TextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity") },
                    singleLine = true
                )

                // --- Allergens dropdown (Compose-compatible) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2A2A2A))
                        .clickable { allergensExpanded = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allergens", color = Color.White)

                        val label = if (selectedAllergens.isEmpty()) {
                            "None"
                        } else {
                            selectedAllergens.joinToString(", ") { niceEnumLabel(it.name) }
                        }

                        Text(
                            label,
                            color = MutedText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.White
                    )

                    DropdownMenu(
                        expanded = allergensExpanded,
                        onDismissRequest = { allergensExpanded = false }
                    ) {

                        // Select All
                        DropdownMenuItem(
                            text = { Text("Select All") },
                            onClick = {
                                selectedAllergens =
                                    if (selectedAllergens.size == Allergen.values().size)
                                        emptySet()
                                    else
                                        Allergen.values().toSet()
                            }
                        )

                        Divider()

                        // Individual allergens
                        Allergen.values().forEach { allergen ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = selectedAllergens.contains(allergen),
                                            onCheckedChange = null
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(niceEnumLabel(allergen.name))
                                    }
                                },
                                onClick = {
                                    selectedAllergens =
                                        if (selectedAllergens.contains(allergen))
                                            selectedAllergens - allergen
                                        else
                                            selectedAllergens + allergen
                                }
                            )
                        }
                    }
                }

                TextField(
                    value = manualIngredients,
                    onValueChange = { manualIngredients = it },
                    label = { Text("Ingredients (comma-separated)") }
                )

                TextField(
                    value = manualExpiration,
                    onValueChange = { manualExpiration = it },
                    label = { Text("Expiration (yyyy/MM/dd)") }
                )
            }
        },
        containerColor = CardSurface,
        titleContentColor = Color.White,
        textContentColor = MutedText
    )
}


@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim()) }
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Create Group") },
        text = {
            Column {
                Text("Give your group a name.", color = MutedText)
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text("My Awesome Group") }
                )
            }
        },
        containerColor = CardSurface,
        titleContentColor = Color.White,
        textContentColor = MutedText
    )
}

@Composable
private fun MemberDetailsDialog(
    username: String,
    profile: k.nutriguard.domain.UserProfile?,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = { Text("Member details") },
        text = {
            when {
                isLoading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = NutriYellow,
                            strokeWidth = 2.dp
                        )
                        Text("Loading profile for @$username...", color = MutedText)
                    }
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                profile != null -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "@${profile.username}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Dietary restrictions
                        val dietLabel = if (profile.dietaryRestrictions.isEmpty()) {
                            "None"
                        } else {
                            profile.dietaryRestrictions
                                .joinToString(", ") { niceEnumLabel(it.name) }
                        }

                        val allergyLabel = if (profile.allergies.isEmpty()) {
                            "None"
                        } else {
                            profile.allergies
                                .joinToString(", ") { niceEnumLabel(it.name) }
                        }

                        Column {
                            Text("Dietary restrictions", color = MutedText, style = MaterialTheme.typography.labelLarge)
                            Text(dietLabel, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }

                        Column {
                            Text("Allergies", color = MutedText, style = MaterialTheme.typography.labelLarge)
                            Text(allergyLabel, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                else -> {
                    Text("No data available.", color = MutedText)
                }
            }
        },
        containerColor = CardSurface,
        titleContentColor = Color.White,
        textContentColor = MutedText
    )
}

// Helper to print enum names like "TREE_NUT" -> "Tree nut"
private fun niceEnumLabel(raw: String): String {
    return raw
        .lowercase()
        .split('_')
        .joinToString(" ") { part ->
            part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}


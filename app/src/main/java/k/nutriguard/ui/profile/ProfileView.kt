package k.nutriguard.ui.profile

import androidx.compose.foundation.background
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import k.nutriguard.domain.Allergen
import k.nutriguard.domain.DietaryRestriction
import k.nutriguard.domain.UserProfile        // <-- NEW IMPORT
import k.nutriguard.viewmodel.ProfileViewModel

// Colors to match your UI
private val BgSurface   = Color(0xFF0E0E0E)
private val CardSurface = Color(0xFF1C1C1C)
private val Divider     = Color(0xFF2A2A2A)
private val NutriYellow = Color(0xFFFFD200)
private val ChipRed     = Color(0xFFE74C3C)

@Composable
fun ProfileView(
    viewModel: ProfileViewModel,
    user: UserProfile,
    onLogout: () -> Unit,
) {
    // Run once per logged-in user
    LaunchedEffect(user.id) {
        viewModel.loadOrCreateUser(user.username)
    }

    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSurface),
        containerColor = BgSurface,
        bottomBar = {
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB00020),  // a redish color
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Log out", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgSurface)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error message if any
            state.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // User profile header
            HeaderCard(
                initials = state.initials,
                username = state.username,
            )

            // Allergies
            EditableSectionCard(
                title = "Allergies",
                values = state.allergies,
                allEnumValues = Allergen.values().map { readableFromEnum(it) },
                isEditing = state.isEditingAllergies,
                onEditClick = { viewModel.onAllergyEditClicked() },
                onToggle = { viewModel.onAllergyToggled(it) },
                chipBg = ChipRed,
                chipFg = Color.White
            )

            // Dietary restrictions
            EditableSectionCard(
                title = "Dietary Restrictions",
                values = state.dietaryRestrictions,
                allEnumValues = DietaryRestriction.values().map { readableFromEnum(it) },
                isEditing = state.isEditingDietaryRestrictions,
                onEditClick = { viewModel.onDietaryEditClicked() },
                onToggle = { viewModel.onDietaryRestrictionToggled(it) },
                chipBg = NutriYellow,
                chipFg = Color.Black
            )

            FriendsSectionCard(
                friends = state.friends,
                onAddFriend = { username: String -> viewModel.addFriendByUsername(username) },
                onRemoveFriend = { username: String -> viewModel.removeFriend(username) }
            )

            // Save profile button stays scrollable with the content
            if (state.isEditingUsername || state.isEditingAllergies || state.isEditingDietaryRestrictions) {
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.onSaveProfileClicked() },
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NutriYellow,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@Composable
private fun HeaderCard(
    initials: String,
    username: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Divider, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(NutriYellow)
                    .border(2.dp, Color(0xFF7A6A00), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "Username",
                    color = Color(0xFFBBBBBB),
                    fontSize = 13.sp
                )
                Text(
                    text = username,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EditableSectionCard(
    title: String,
    values: List<String>,
    allEnumValues: List<String>,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onToggle: (String) -> Unit,
    chipBg: Color,
    chipFg: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Divider, RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                FilledTonalButton(
                    onClick = onEditClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = NutriYellow,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(if (isEditing) "Save" else "Edit")
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!isEditing) {
                // Show chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (values.isEmpty()) {
                        Text("None", color = Color(0xFF888888))
                    } else {
                        values.forEach { value ->
                            PillChip(text = value, bg = chipBg, fg = chipFg)
                        }
                    }
                }
            } else {
                // Show checkboxes for all possible values
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    allEnumValues.forEach { label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = values.contains(label),
                                onCheckedChange = { onToggle(label) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = NutriYellow,
                                    uncheckedColor = Color.White
                                )
                            )
                            Text(label, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PillChip(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun FriendsSectionCard(
    friends: List<String>,
    onAddFriend: (String) -> Unit,
    onRemoveFriend: (String) -> Unit
) {
    var friendInput by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Divider, RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(16.dp)) {

            Text(
                text = "Friends",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(12.dp))

            if (friends.isEmpty()) {
                Text(
                    "You have no friends added yet.",
                    color = Color(0xFF888888),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    friends.forEach { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF2A2A2A))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = friend,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { onRemoveFriend(friend) }
                            ) {
                                Text("Remove", color = ChipRed, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Add friend by username",
                color = Color(0xFFBBBBBB),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = friendInput,
                    onValueChange = { friendInput = it },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        disabledContainerColor = CardSurface,

                        focusedIndicatorColor = NutriYellow,
                        unfocusedIndicatorColor = Divider,
                        disabledIndicatorColor = Divider,

                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color.Gray,

                        cursorColor = NutriYellow
                    ),
                    placeholder = { Text("username") }
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = friendInput.isNotBlank(),
                    onClick = {
                        onAddFriend(friendInput)
                        friendInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NutriYellow,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add")
                }
            }
        }
    }
}

// Same logic as enumToReadable in ViewModel
private fun readableFromEnum(value: Enum<*>): String =
    value.name
        .lowercase()
        .replace("_", " ")
        .replaceFirstChar { it.uppercase() }

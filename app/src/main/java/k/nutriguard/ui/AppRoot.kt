@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package k.nutriguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import k.nutriguard.ui.group.GroupView
import k.nutriguard.viewmodel.GroupViewModel
import k.nutriguard.ui.profile.ProfileView
import k.nutriguard.viewmodel.ProfileViewModel
import k.nutriguard.ui.inventory.InventoryView
import k.nutriguard.viewmodel.InventoryViewModel
import k.nutriguard.domain.UserProfile   // <-- NEW IMPORT

// Simple palette (matches your dark/yellow look)
private val NutriYellow = Color(0xFFFFD200)
private val SurfaceBar  = Color(0xFF0F0F0F)

// Tabs
enum class NutriTab { Inventory, Groups, Profile }

@Composable
fun NutriAppRoot(
    profileVm: ProfileViewModel,
    groupVm: GroupViewModel,
    inventoryVm: InventoryViewModel,
    user: UserProfile,                     // <-- CHANGED: was `username: String`, now an actual userprofile
    onLogout: () -> Unit,
) {
    // remember across config changes
    var tab by rememberSaveable { mutableStateOf(NutriTab.Inventory) }

    Scaffold(
        topBar = { NutriTopBar() },
        bottomBar = { NutriBottomBar(selected = tab, onSelected = { tab = it }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)

        ) {
            when (tab) {
                NutriTab.Inventory -> InventoryView(
                    vm = inventoryVm,
                    user = user
                )
                NutriTab.Groups    -> GroupView(viewModel = groupVm, user = user)

                // Pass full user to ProfileView
                NutriTab.Profile   -> ProfileView(
                    viewModel = profileVm,
                    user = user,               // <-- CHANGED: was `username`
                    onLogout = onLogout

                )
            }
        }
    }
}

// ---- Shared Chrome ----
@Composable
private fun NutriTopBar() {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SurfaceBar,
            titleContentColor = Color.White
        ),
        title = {
            Column {
                Text(text = "NutriGuard", color = NutriYellow)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Track your food, reduce waste",
                    color = Color(0xFFBDBDBD),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    )
}

@Composable
private fun NutriBottomBar(
    selected: NutriTab,
    onSelected: (NutriTab) -> Unit
) {
    NavigationBar(containerColor = SurfaceBar) {
        NavigationBarItem(
            selected = selected == NutriTab.Inventory,
            onClick = { onSelected(NutriTab.Inventory) },
            icon = { Icon(Icons.Outlined.ListAlt, contentDescription = "Inventory") },
            label = { Text("Inventory") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NutriYellow,
                selectedTextColor = NutriYellow,
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color(0xFFBDBDBD),
                unselectedTextColor = Color(0xFFBDBDBD)
            )
        )
        NavigationBarItem(
            selected = selected == NutriTab.Groups,
            onClick = { onSelected(NutriTab.Groups) },
            icon = { Icon(Icons.Outlined.People, contentDescription = "Groups") },
            label = { Text("Groups") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NutriYellow,
                selectedTextColor = NutriYellow,
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color(0xFFBDBDBD),
                unselectedTextColor = Color(0xFFBDBDBD)
            )
        )
        NavigationBarItem(
            selected = selected == NutriTab.Profile,
            onClick = { onSelected(NutriTab.Profile) },
            icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NutriYellow,
                selectedTextColor = NutriYellow,
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color(0xFFBDBDBD),
                unselectedTextColor = Color(0xFFBDBDBD)
            )
        )
    }
}

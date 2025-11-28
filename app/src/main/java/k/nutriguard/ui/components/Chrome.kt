package k.nutriguard.ui.components


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Column

enum class NutriTab { Inventory, Groups, Profile }

private val NutriYellow = Color(0xFFFFD200)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutriTopBar() {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF0F0F0F),
            titleContentColor = Color.White
        ),
        title = {
            Column {
                Text(text = "NutriGuard", color = NutriYellow, fontWeight = FontWeight.SemiBold)
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
fun NutriBottomBar(
    selected: NutriTab,
    onSelected: (NutriTab) -> Unit
) {
    NavigationBar(containerColor = Color(0xFF0F0F0F)) {
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

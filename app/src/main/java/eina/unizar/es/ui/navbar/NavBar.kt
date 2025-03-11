package eina.unizar.es.ui.navbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Componente reutilizable para la barra de navegación inferior.
 */
@Composable
fun BottomNavigationBar(navController: NavController) {
    val bottomNavItems = listOf(
        "menu" to Icons.Default.Home,
        "search" to Icons.Default.Search,
        "library" to Icons.Rounded.Menu,
    )

    // Obtener la ruta actual desde el NavController
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar(containerColor = Color.Transparent) {
        bottomNavItems.forEach { (route, icon) ->
            NavigationBarItem(
                selected = (currentRoute == route), // Selecciona el ítem según la ruta actual
                onClick = {
                    if (currentRoute != route) { // Evita repetir navegación en la misma pantalla
                        navController.navigate(route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(icon, contentDescription = route) },
                label = { Text(route.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                    unselectedIconColor = MaterialTheme.colorScheme.inverseSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                    unselectedTextColor = MaterialTheme.colorScheme.inverseSurface,
                )
            )
        }
    }
}

package eina.unizar.es.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import eina.unizar.es.R
import eina.unizar.es.ui.components.UserProfileMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(navController: NavController) {
    var searchText by remember { mutableStateOf(TextFieldValue("")) } // Estado del texto de búsqueda
    var isSearching by remember { mutableStateOf(false) } // Estado para mostrar la barra de búsqueda

    val allLibraryItems = getLibraryItems("Playlists") // Lista completa
    val filteredLibraryItems = allLibraryItems.filter {
        it.name.contains(searchText.text, ignoreCase = true)
    }

    // Estado de la barra de navegación inferior
    var selectedItem by remember { mutableStateOf(2) } // 2 es "Biblioteca" por defecto

    val bottomNavItems = listOf(
        Pair("Inicio", Icons.Default.Home),
        Pair("Buscar", Icons.Default.Search),
        Pair("Biblioteca", Icons.Rounded.Menu)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF121212)) {
                bottomNavItems.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = (selectedItem == index),
                        onClick = {
                            selectedItem = index
                            when (index) {
                                0 -> navController.navigate("menu") // Inicio
                                1 -> navController.navigate("search") // Buscador
                                2 -> navController.navigate("library") // Biblioteca
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF0D47A1) // Azul cuando está seleccionado
                        )
                    )
                }
            }
        },
        containerColor = Color.Black // Color de fondo
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
                .padding(8.dp)
        ) {
            // Barra superior con perfil y lupa
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserProfileMenu(navController) // Icono de usuario
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        // Icono de lupa para activar la búsqueda
                        IconButton(onClick = { isSearching = !isSearching }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black
                )
            )

            // Si la búsqueda está activa, mostramos el cuadro de búsqueda debajo del perfil
            if (isSearching) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Buscar en tu biblioteca", color = Color.Gray) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color.White
                    ),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            searchText = TextFieldValue("")
                            isSearching = false // Cerrar la búsqueda al tocar la "X"
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar búsqueda",
                                tint = Color.White
                            )
                        }
                    }
                )
            }

            // Título "Tu Biblioteca"
            Text(
                text = "Tu Biblioteca",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Lista de elementos filtrados según la búsqueda
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                items(filteredLibraryItems) { item ->
                    LibraryItem(item)
                }
            }
        }
    }
}



@Composable
fun LibraryItem(item: LibraryItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp).clickable {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.kanyeperfil),
            contentDescription = "Imagen",
            modifier = Modifier.size(50.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = item.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = item.type, color = Color.Gray, fontSize = 14.sp)
        }
    }
}

// Datos simulados
data class LibraryItem(val name: String, val type: String)

fun getLibraryItems(filter: String): List<LibraryItem> {
    return when (filter) {
        "Playlists" -> listOf(
            LibraryItem("Canciones que te gustan", "Playlist"),
            LibraryItem("Playlist prueba", "Playlist")
        )
        else -> emptyList()
    }
}
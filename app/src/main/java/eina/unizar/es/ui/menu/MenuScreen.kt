package eina.unizar.es.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import eina.unizar.es.ui.components.UserProfileMenu // Asegúrate de tener este import y el Composable definido

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    // Colores básicos
    val backgroundColor = Color(0xFF000000)      // Negro
    val textColor = Color(0xFFFFFFFF)            // Blanco
    val cardBackgroundColor = Color(0xFF121212)  // Negro oscuro
    val buttonColor = Color(0xFF0D47A1)          // Azul oscuro

    // Estado para la búsqueda
    var searchText by remember { mutableStateOf(TextFieldValue("")) }

    // Dos listas de playlists para mostrarlas en filas diferentes
    val playlistRecomendadas = listOf("Rock", "Pop", "Indie", "Jazz")
    val playlistRecomendadas2 = listOf("Electrónica", "Reggaeton", "Trap", "Funk")

    // Canciones recomendadas
    val cancionesRecomendadas = listOf("Canción A", "Canción B", "Canción C", "Canción D")

    // ---- Barra de navegación inferior ----
    val bottomNavItems = listOf(
        Pair("Inicio", Icons.Default.Home),
        Pair("Biblioteca", Icons.Default.Star),
        //Pair("Premium", Icons.Default.Star),
        Pair("Chat", Icons.Default.Star)
    )
    var selectedItem by remember { mutableStateOf(0) }

    // Scaffold con bottomBar
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF121212)
            ) {
                bottomNavItems.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = (selectedItem == index),
                        onClick = {
                            selectedItem = index
                            // Ej: navController.navigate("ruta_${label.lowercase()}") { ... }
                        },
                        icon = {
                            Icon(icon, contentDescription = label)
                        },
                        label = {
                            Text(label, fontSize = 12.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = buttonColor // Animación azul de selección
                        )
                    )
                }
            }
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        // Contenido principal
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(backgroundColor)
                .padding(8.dp)
        ) {
            Column {
                // Barra de búsqueda + icono de usuario
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = {
                            Text("Buscar música o artistas", color = textColor)
                        },
                        textStyle = TextStyle(color = textColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.8f),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = buttonColor,
                            unfocusedBorderColor = textColor,
                            cursorColor = textColor
                        ),
                        keyboardOptions = KeyboardOptions.Default
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Icono de usuario con menú desplegable
                    UserProfileMenu(navController)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primera fila de playlists
                Text("Listas Recomendadas", color = textColor)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(playlistRecomendadas.size) { idx ->
                        val playlist = playlistRecomendadas[idx]
                        Card(
                            modifier = Modifier
                                .size(width = 120.dp, height = 80.dp)
                                .clickable {
                                    //navController.navigate("playlist/$playlist")
                                    navController.navigate("playlist")
                                },
                            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // sombra
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(playlist, color = textColor)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Segunda fila de playlists
                Text("Otras Listas Recomendadas", color = textColor)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(playlistRecomendadas2.size) { idx ->
                        val playlist = playlistRecomendadas2[idx]
                        Card(
                            modifier = Modifier
                                .size(width = 120.dp, height = 80.dp)
                                .clickable {
                                    // navController.navigate("playlist/$playlist")
                                },
                            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // sombra
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(playlist, color = textColor)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Canciones recomendadas
                Text("Canciones Recomendadas", color = textColor)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cancionesRecomendadas.size) { idx ->
                        val cancion = cancionesRecomendadas[idx]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // navController.navigate("player/$cancion")
                                },
                            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // sombra
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(cancion, color = textColor)
                            }
                        }
                    }
                }
            }
        }
    }
}
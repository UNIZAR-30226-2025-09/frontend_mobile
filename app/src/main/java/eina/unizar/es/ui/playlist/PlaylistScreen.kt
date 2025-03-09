package eina.unizar.es.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(navController: NavController) {
    // Colores básicos
    val backgroundColor = Color(0xFF000000)       // Negro
    val textColor = Color(0xFFFFFFFF)             // Blanco
    val cardBackgroundColor = Color(0xFF121212)   // Negro un poco más claro
    val buttonColor = Color(0xFF0D47A1)           // Azul oscuro

    // Datos simulados de la playlist
    val playlistTitle = "Playlist: Rock"
    val playlistAuthor = "Autor: John Doe"

    // Simulación de 20 canciones
    val allSongs = (1..20).map { "Canción $it" }

    // Estados para búsqueda y orden
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    var sortOption by remember { mutableStateOf("Título") }
    val filteredSongs = allSongs.filter { it.contains(searchText.text, ignoreCase = true) }
    val sortedSongs = remember(filteredSongs, sortOption) {
        when (sortOption) {
            "Título" -> filteredSongs.sortedBy { it }
            "Añadido recientemente" -> filteredSongs.reversed() // Orden inverso simulado
            "Artista" -> filteredSongs.sortedBy { it }
            else -> filteredSongs
        }
    }

    // Estado del LazyColumn para detectar scroll y aplicar efecto en la portada
    val lazyListState = rememberLazyListState()
    // Definimos el tamaño inicial de la imagen (cuadrado)
    val imageSize = 180.dp
    // Usamos un offset máximo para el efecto (por ejemplo, 150 px)
    val maxOffset = 150f
    val scrollOffset = lazyListState.firstVisibleItemScrollOffset.toFloat()
    // Calculamos un factor de colapso (entre 0 y 1)
    val collapseFraction = (scrollOffset / maxOffset).coerceIn(0f, 1f)
    // Usamos el factor para ajustar solo la escala y la opacidad
    val imageAlpha = 1f - collapseFraction
    val imageScale = 1f - (collapseFraction * 0.5f)

    Scaffold(
        topBar = {
            // TopAppBar siempre visible con botón de volver en blanco
            TopAppBar(
                title = { /* Puedes dejarlo vacío o poner un título corto */ },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(backgroundColor)
        ) {
            // Header: Portada, título y autor
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Portada con efecto de colapso (solo se aplica una vez)
                    Box(
                        modifier = Modifier
                            .size(imageSize)
                            .graphicsLayer {
                                alpha = imageAlpha
                                scaleX = imageScale
                                scaleY = imageScale
                            }
                            .background(Color.Gray) // Placeholder para la imagen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Título
                    Text(
                        text = playlistTitle,
                        color = textColor,
                        style = TextStyle(fontSize = 20.sp)
                    )
                    // Autor (más pequeño)
                    Text(
                        text = playlistAuthor,
                        color = textColor,
                        style = TextStyle(fontSize = 14.sp)
                    )
                }
            }
            // Fila con barra de búsqueda y botón de reproducir (Play) – ahora a la izquierda
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text("Buscar en playlist", color = textColor) },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = textColor),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = buttonColor,
                            unfocusedBorderColor = textColor,
                            cursorColor = textColor
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Botón circular para reproducir la playlist (Play) en esta fila
                    IconButton(
                        onClick = {
                            // Acción para reproducir la playlist (simulada)
                            // Ejemplo: navController.navigate("playPlaylist")
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(buttonColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproducir Playlist",
                            tint = textColor
                        )
                    }
                }
            }
            // Fila con dropdown para ordenar y botón de añadir (Add) – ahora el +
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Ordenar por:", color = textColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    var expandirMenu by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { expandirMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                        ) {
                            Text(sortOption, color = textColor)
                        }
                        DropdownMenu(
                            expanded = expandirMenu,
                            onDismissRequest = { expandirMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Título") },
                                onClick = {
                                    sortOption = "Título"
                                    expandirMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Añadido recientemente") },
                                onClick = {
                                    sortOption = "Fecha"
                                    expandirMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Artista") },
                                onClick = {
                                    sortOption = "Artista"
                                    expandirMenu = false
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Botón para añadir canción (Add) en esta fila
                    IconButton(
                        onClick = {
                            // Acción para añadir una canción (simulada)
                            // Ejemplo: navController.navigate("addSong")
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Añadir canción",
                            tint = textColor
                        )
                    }
                }
            }
            // Espacio de separación
            item { Spacer(modifier = Modifier.height(26.dp)) }
            // Lista de canciones (20 canciones simuladas)
            items(sortedSongs) { cancion ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clickable {
                                // Acción al pulsar en la canción (simulada)
                                // Ejemplo: navController.navigate("player/$cancion")
                            },
                        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
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
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

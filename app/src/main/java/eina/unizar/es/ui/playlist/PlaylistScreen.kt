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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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

    // Simulación de 20 canciones y sus artistas
    val allSongs = (1..20).map { "Canción $it" }
    val songArtistMap = allSongs.associateWith { song ->
        val number = song.filter { it.isDigit() }
        "Artista $number"
    }

    // Estados para búsqueda y orden
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    var sortOption by remember { mutableStateOf("Título") }
    val filteredSongs = allSongs.filter { it.contains(searchText.text, ignoreCase = true) }
    val sortedSongs = remember(filteredSongs, sortOption) {
        when (sortOption) {
            "Título" -> filteredSongs.sortedBy { it }
            "Añadido recientemente" -> filteredSongs.reversed()
            "Artista" -> filteredSongs.sortedBy { songArtistMap[it] ?: "" }
            else -> filteredSongs
        }
    }

    // Estado para mostrar/ocultar la barra de búsqueda
    var showSearch by remember { mutableStateOf(false) }

    // Estado del LazyColumn para detectar scroll y aplicar efecto en el header
    val lazyListState = rememberLazyListState()
    val imageSize = 150.dp
    val maxOffset = with(LocalDensity.current) { imageSize.toPx() }
    val scrollOffset = lazyListState.firstVisibleItemScrollOffset.toFloat()
    val collapseFraction = (scrollOffset / maxOffset).coerceIn(0f, 1f)
    // Ajustamos solo la opacidad (sin escala) con Modifier.alpha
    val imageAlpha = 1f - collapseFraction

    // Alpha para el título en el TopAppBar: aparece gradualmente conforme se hace scroll
    val topTitleAlpha = if (lazyListState.firstVisibleItemIndex > 0) {
        1f
    } else {
        ((scrollOffset) / (maxOffset / 2)).coerceIn(0f, 1f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = playlistTitle,
                        color = textColor,
                        modifier = Modifier.alpha(topTitleAlpha)
                    )
                },
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
                    Box(
                        modifier = Modifier
                            .size(imageSize)
                            .alpha(imageAlpha)
                            .background(Color.Gray) // Placeholder para la portada
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playlistTitle,
                        color = textColor,
                        style = TextStyle(fontSize = 20.sp)
                    )
                    Text(
                        text = playlistAuthor,
                        color = textColor,
                        style = TextStyle(fontSize = 14.sp)
                    )
                }
            }
            // Fila para la búsqueda: si showSearch es true, se muestra la barra de búsqueda que deja espacio para el icono de reproducir
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showSearch) {
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
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showSearch = !showSearch },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Mostrar/Ocultar búsqueda",
                            tint = textColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            // Acción para reproducir la playlist (simulada)
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
            // Fila con dropdown para ordenar y botón de añadir (Add)
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
                    IconButton(
                        onClick = {
                            // Acción para añadir una canción (simulada)
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
            // Separador
            item { Spacer(modifier = Modifier.height(26.dp)) }
            // Lista de canciones: Cada banner con imagen a la izquierda y título/artista a la derecha
            items(sortedSongs) { song ->
                val artist = songArtistMap[song] ?: "Artista Desconocido"
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clickable {
                                // Acción al pulsar en la canción (simulada)
                            },
                        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Imagen de la canción (cuadrado)
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(Color.DarkGray) // Placeholder de la imagen
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = song,
                                    color = textColor,
                                    style = TextStyle(fontSize = 18.sp)
                                )
                                Text(
                                    text = artist,
                                    color = textColor,
                                    style = TextStyle(fontSize = 14.sp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
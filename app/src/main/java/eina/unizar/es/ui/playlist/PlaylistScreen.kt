package eina.unizar.es.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistId: String,
    onSongClick: (String) -> Unit,
    onBack: () -> Unit
) {
    // Colores
    val backgroundColor = Color(0xFF000000)       // Negro
    val textColor = Color(0xFFFFFFFF)             // Blanco
    val cardBackgroundColor = Color(0xFF121212)   // Negro un poco más claro
    val buttonColor = Color(0xFF0D47A1)           // Azul oscuro

    // Datos ficticios
    val allSongs = listOf("Canción 1", "Canción 2", "Canción 3", "Canción 4", "Canción 5")

    // Estado de la búsqueda interna
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    // Opción de orden actual
    var sortOption by remember { mutableStateOf("Título") }

    // Filtrar canciones por texto
    val filteredSongs = allSongs.filter { it.contains(searchText.text, ignoreCase = true) }

    // Ordenar según la opción elegida
    val sortedSongs = remember(filteredSongs, sortOption) {
        when (sortOption) {
            "Título" -> filteredSongs.sortedBy { it }
            "Fecha" -> filteredSongs.reversed() // Simula orden por fecha inversa
            else -> filteredSongs
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(8.dp)
    ) {
        // Encabezado con el nombre de la Playlist y botón para volver
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Playlist: $playlistId", color = textColor)
            Button(
                onClick = { onBack() },
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
            ) {
                Text("Volver", color = textColor)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Barra de búsqueda interna
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Buscar en playlist", color = textColor) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = buttonColor,
                unfocusedBorderColor = textColor,
                cursorColor = textColor
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Dropdown para el orden
        Row {
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
                        text = { Text("Fecha") },
                        onClick = {
                            sortOption = "Fecha"
                            expandirMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de canciones
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sortedSongs) { cancion ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSongClick(cancion) },
                    colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(cancion, color = textColor)
                    }
                }
            }
        }
    }
}

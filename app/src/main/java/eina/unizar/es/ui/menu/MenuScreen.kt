package eina.unizar.es.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    //navController: NavController,
   /* onPlaylistClick: (String) -> Unit,  // Para navegar a la pantalla de Playlist
    onSongClick: (String) -> Unit       // Para navegar a la pantalla de Player
*/
    ) {
    // Colores básicos
    val backgroundColor = Color(0xFF000000)   // Negro
    val textColor = Color(0xFFFFFFFF)         // Blanco
    val cardBackgroundColor = Color(0xFF121212) // Negro oscuro
    val buttonColor = Color(0xFF0D47A1)       // Azul oscuro

    // Estado para la búsqueda
    var searchText by remember { mutableStateOf(TextFieldValue("")) }

    // Datos ficticios
    val playlistRecomendadas = listOf("Rock", "Pop", "Indie", "Jazz")
    val cancionesRecomendadas = listOf("Canción A", "Canción B", "Canción C", "Canción D")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(8.dp)
    ) {
        // Barra de búsqueda
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = {
                Text("Buscar música o artistas", color = textColor)
            },
            textStyle = TextStyle(color = textColor),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = buttonColor,
                unfocusedBorderColor = textColor,
                cursorColor = textColor
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Título de sección playlists
        Text(
            text = "Listas Recomendadas",
            color = textColor
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Sección de playlists
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(playlistRecomendadas) { playlist ->
                Card(
                    modifier = Modifier
                        .size(width = 120.dp, height = 80.dp)
                        .clickable { /*onPlaylistClick(playlist)*/ },
                    colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
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

        // Título de sección canciones
        Text(
            text = "Canciones Recomendadas",
            color = textColor
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Sección de canciones
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cancionesRecomendadas) { cancion ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /*onSongClick(cancion)*/ },
                    colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
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

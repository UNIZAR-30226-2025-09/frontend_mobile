package eina.unizar.es.ui.search

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.musicapp.ui.theme.VibraBlue
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.data.model.network.ApiClient.getUserPlaylists
import eina.unizar.es.ui.player.MusicPlayerViewModel
import eina.unizar.es.ui.playlist.Playlist

@Composable
fun ADSongs(
    viewModel: MusicPlayerViewModel,
    onDismiss: () -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }
    val darkBackground = Color(0xFF121212)
    val darkSurface = Color(0xFF212121)
    val textColor = Color.White
    val subtitleColor = Color.Gray

    // Para obtener el contexto de la actividad
    val context = LocalContext.current // Contexto de la actividad

    // Estado para mostrar un indicador de carga
    var isLoading by remember { mutableStateOf(true) }

    // User ID from the ViewModel
    var userId = viewModel.getUserId()

    // Estado para almacenar las playlists del usuario
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }

    // Estado para manejar la selección de playlists
    var selectedPlaylistIds by remember { mutableStateOf(setOf<String>()) }

    val filteredPlaylists = remember(searchText, playlists) {
        if (searchText.isEmpty()) {
            playlists
        } else {
            playlists.filter { it.title.contains(searchText, ignoreCase = true) }
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.getUserId().isEmpty()) {
            viewModel.setUserId(context)
        }
        if (userId.isNotEmpty()) {
            try {
                Log.d("User ID", "User ID: $userId")
                val userPlaylists = getUserPlaylists(userId)

                if (userPlaylists != null) {
                    // Convertir las playlists y ordenarlas para que Vibra_likedSong aparezca primero
                    playlists = userPlaylists.sortedWith(compareBy {
                        // Si el tipo es Vibra_likedSong, va primero (valor -1)
                        // El resto mantiene su orden original
                        if (it.esAlbum == "Vibra_likedSong") -1 else 0
                    }).map { playlist ->
                        Playlist(
                            id = playlist.id,
                            title = playlist.title,
                            imageUrl = playlist.imageUrl,
                            idAutor = playlist.idAutor,
                            idArtista = playlist.idArtista,
                            description = playlist.description,
                            esPublica = playlist.esPublica,
                            esAlbum = playlist.esAlbum
                        )
                    }
                } else {
                    Log.e("Error", "No se pudieron cargar las playlists")
                }
            } catch (e: Exception) {
                Log.d("Error", "Error: ${e.message}")
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 32.dp),
            shape = RoundedCornerShape(8.dp),
            color = darkBackground
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Title
                Text(
                    text = "Añadir a lista",
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Search field
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Busca una lista", color = subtitleColor) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = subtitleColor) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = darkSurface,
                        unfocusedContainerColor = darkSurface,
                        disabledContainerColor = darkSurface,
                        cursorColor = textColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Divider(
                    color = Color(0xFF444444),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Playlists
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(225.dp) // Altura fija para el contenedor de listas
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = VibraBlue
                        )
                    } else if (filteredPlaylists.isEmpty()) {
                        Text(
                            text = "No se encontraron playlists",
                            color = subtitleColor,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Tus playlists",
                                color = subtitleColor,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            )

                            // LazyColumn con scroll dentro del contenedor de altura fija
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(filteredPlaylists) { playlist ->
                                    PlaylistRowWithCheckbox(
                                        playlist = playlist,
                                        isSelected = selectedPlaylistIds.contains(playlist.id),
                                        onClick = {
                                            selectedPlaylistIds = if (selectedPlaylistIds.contains(playlist.id)) {
                                                selectedPlaylistIds - playlist.id
                                            } else {
                                                selectedPlaylistIds + playlist.id
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Cancel button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(
                            text = "Volver",
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistRowWithCheckbox(
    playlist: Playlist,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Playlist cover image
        val playlistImage = getImageUrl(playlist.imageUrl, "/default-playlist.jpg")
        AsyncImage(
            model = playlistImage,
            contentDescription = "Playlist cover",
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )

        // Playlist info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = playlist.title,
                color = Color.White,
                fontSize = 16.sp
            )
        }

        // Custom radio button (checkbox circular)
        CustomRadioButton(
            selected = isSelected,
            onClick = onClick
        )
    }
}

@Composable
fun CustomRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color = VibraBlue,
    size: Dp = 20.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size/2))
            .border(
                width = 2.dp,
                color = if (selected) selectedColor else Color.Gray,
                shape = RoundedCornerShape(size/2)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(size - 8.dp)
                    .clip(RoundedCornerShape((size - 8.dp)/2))
                    .background(selectedColor)
            )
        }
    }
}
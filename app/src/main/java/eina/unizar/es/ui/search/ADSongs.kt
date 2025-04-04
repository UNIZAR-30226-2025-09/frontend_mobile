package eina.unizar.es.ui.search

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.musicapp.ui.theme.VibraBlue
import eina.unizar.es.R
import eina.unizar.es.ui.player.MusicPlayerViewModel

data class PlaylistItem(
    val id: String,
    val name: String,
    val songCount: Int,
    val imageResId: Int,
    val isSelected: Boolean = false
)

@Composable
fun ADSongs(
    navController: NavController? = null,
    viewModel: MusicPlayerViewModel? = null,
    onDismiss: () -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }
    val darkBackground = Color(0xFF121212)
    val darkSurface = Color(0xFF212121)
    val textColor = Color.White
    val subtitleColor = Color.Gray

    // Sample playlists data
    val playlists = remember {
        listOf(
            PlaylistItem("1", "Canciones que te gustan", 212, R.drawable.kanyeperfil, true),
            PlaylistItem("2", "Ejemplo1", 21, R.drawable.kanyeperfil),
            PlaylistItem("3", "Ejemplo2", 79, R.drawable.kanyeperfil),
            PlaylistItem("4", "Ejemplo3", 0, R.drawable.kanyeperfil)
        )
    }

    // Estado para manejar la selección de playlists
    var selectedPlaylistIds by remember { mutableStateOf(playlists.filter { it.isSelected }.map { it.id }.toSet()) }

    val filteredPlaylists = remember(searchText, playlists) {
        if (searchText.isEmpty()) {
            playlists
        } else {
            playlists.filter { it.name.contains(searchText, ignoreCase = true) }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
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
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                ) {
                    item {
                        Text(
                            text = "Tus playlists",
                            color = subtitleColor,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }

                    items(filteredPlaylists) { playlist ->
                        PlaylistRowWithCheckbox(
                            playlist = playlist,
                            isSelected = selectedPlaylistIds.contains(playlist.id),
                            onClick = {
                                // Toggle selection
                                selectedPlaylistIds = if (selectedPlaylistIds.contains(playlist.id)) {
                                    selectedPlaylistIds - playlist.id
                                } else {
                                    selectedPlaylistIds + playlist.id
                                }
                            }
                        )
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
                            text = "Cancelar",
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
    playlist: PlaylistItem,
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
        Image(
            painter = painterResource(id = playlist.imageResId),
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
                text = playlist.name,
                color = Color.White,
                fontSize = 16.sp
            )
            Text(
                text = "${playlist.songCount} canciones",
                color = Color.Gray,
                fontSize = 14.sp
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

@Preview(showBackground = true)
@Composable
fun ADSongsPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        ADSongs()
    }
}
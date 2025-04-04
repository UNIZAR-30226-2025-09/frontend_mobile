package eina.unizar.es.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import eina.unizar.es.data.model.network.ApiClient.checkIfSongIsLiked
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.ui.playlist.getArtistName
import kotlinx.coroutines.launch

@Composable
fun FloatingMusicPlayer(navController: NavController, viewModel: MusicPlayerViewModel, onLikeToggle: (() -> Unit) = {}) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isLiked by viewModel.isCurrentSongLiked.collectAsState()
    val context = LocalContext.current

    var artista by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentSong?.id) {
        currentSong?.id?.let {
            viewModel.loadLikedStatus(it)
        }
        artista = currentSong?.id?.let { getArtistName(it.toInt()) }.toString()
    }
    currentSong?.let { song ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    navController.navigate("song/${song.id}")
                },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model= getImageUrl(song.photo, "default-playlist.jpg"),
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(64.dp)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(text = song.title, fontSize = 16.sp)
                    artista?.let { Text(text = it, fontSize = 14.sp) }
                }
                Icon(
                    imageVector = Icons.Filled.Computer,
                    contentDescription = "Computer",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { /* acción futuro */ }
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {viewModel.toggleLike(context)},
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Me gusta",
                        tint = if (isLiked) Color.Red else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (!song.isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (!song.isPlaying) "Play" else "Pause",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { viewModel.togglePlayPause() }
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}
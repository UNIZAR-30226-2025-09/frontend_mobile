package eina.unizar.es.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import eina.unizar.es.R

@Composable
fun FloatingMusicPlayer(
    viewModel: MusicPlayerViewModel,
    navController: NavController
) {
    val currentSong by viewModel.currentSong.collectAsState()

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
                Image(
                    painter = painterResource(id = song.albumArt),
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
                    Text(text = song.artist, fontSize = 14.sp)
                }
                Icon(
                    imageVector = Icons.Filled.Computer,
                    contentDescription = "Computer",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { /* acción futuro */ }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { /* acción futuro */ }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (song.isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (song.isPlaying) "Play" else "Pause",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { viewModel.togglePlayPause() }
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}
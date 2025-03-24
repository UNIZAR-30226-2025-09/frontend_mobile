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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import eina.unizar.es.R // Reemplaza con tu paquete y recursos

@Composable
fun FloatingMusicPlayer(
    title: String,
    artist: String,
    albumArt: Int,
    isPlaying: Boolean,
   // onPlayPauseClick: () -> Unit,
   // onFavoriteClick: () -> Unit,
   // onComputerClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp)
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
            .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = albumArt),
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
                Text(text = title, fontSize = 16.sp)
                Text(text = artist, fontSize = 14.sp)
            }
            // Icono de ordenador
            Icon(
                imageVector = Icons.Filled.Computer,
                contentDescription = "Computer",
                modifier = Modifier
                    .size(32.dp)
                    .clickable { /*onComputerClick()*/ }
            )
            Spacer(modifier = Modifier.width(4.dp)) // Espacio entre iconos
            // Icono de corazón vacío
            Icon(
                imageVector = Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite",
                modifier = Modifier
                    .size(32.dp)
                    .clickable { /*onComputerClick()*/ }
            )
            Spacer(modifier = Modifier.width(4.dp)) // Espacio entre iconos
            // Icono de play/pause
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier
                    .size(32.dp)
                    .clickable { /*onComputerClick()*/ }
            )
           Spacer(modifier = Modifier.width(12.dp)) // Espacio al final
        }
    }
}
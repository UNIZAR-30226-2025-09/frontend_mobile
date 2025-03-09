package eina.unizar.es.ui.song

/*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun PlayerScreen(
    songId: String,                // El ID o nombre de la canción
    onShowLyrics: (String) -> Unit // Acción para ver la letra
) {
    // Colores básicos
    val backgroundColor = Color(0xFF000000)   // Negro
    val textColor = Color(0xFFFFFFFF)         // Blanco
    val buttonColor = Color(0xFF0D47A1)       // Azul oscuro

    // Simulación de reproducción
    var isPlaying by remember { mutableStateOf(false) }

    // Carátula y título ficticios
    val songTitle = "Reproduciendo: $songId"
    val imageUrl = "https://via.placeholder.com/300.png"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Imagen/caratula
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = "Carátula de la canción",
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Título
            Text(songTitle, color = textColor)

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para ver la letra
            Button(
                onClick = { onShowLyrics(songId) },
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
            ) {
                Text("Ver Letra", color = textColor)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controles de reproducción
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Anterior
                Button(
                    onClick = { /* Lógica para ir a canción anterior */ },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                ) {
                    Text("Anterior", color = textColor)
                }

                // Reproducir/Pausar
                Button(
                    onClick = { isPlaying = !isPlaying },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                ) {
                    val icon: ImageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                    val actionText = if (isPlaying) "Pausar" else "Reproducir"
                    Icon(icon, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(actionText, color = textColor)
                }

                // Siguiente
                Button(
                    onClick = { /* Lógica para ir a canción siguiente */ },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                ) {
                    Text("Siguiente", color = textColor)
                }
            }
        }
    }
}
*/
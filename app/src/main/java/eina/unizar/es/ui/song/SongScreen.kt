package com.example.musicapp.ui.song

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import eina.unizar.es.R
import eina.unizar.es.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongScreen(navController: NavController) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0.1f) }
    var lyricsExpanded by remember { mutableStateOf(false) } // Estado para expandir la letra

    // Estado de desplazamiento
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // Bajamos más la imagen y la barra

            // Botón para minimizar la pantalla de canción
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Minimizar",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(74.dp))

            // Imagen del álbum (más abajo y centrada)
            Image(
                painter = painterResource(id = R.drawable.kanyeperfil),
                contentDescription = "Portada del álbum",
                modifier = Modifier
                    .size(320.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color.Gray, RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(72.dp))

            // Información de la canción
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LA RANGER (feat. Myke Towers)",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "The Academy: Segunda Misión, Sech, Justin Quiles",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(15.dp)) // Bajamos más la barra de progreso

            // 🔵 Barra de progreso
            Slider(
                value = progress,
                onValueChange = { progress = it },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            // Tiempo transcurrido / Duración total
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0:03", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("-3:46", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controles de reproducción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Acción: Anterior */ }) {
                    Icon(Icons.Filled.FastRewind, contentDescription = "Anterior", tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(16.dp))

                FloatingActionButton(
                    onClick = { isPlaying = !isPlaying },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { /* Acción: Siguiente */ }) {
                    Icon(Icons.Filled.FastForward, contentDescription = "Siguiente", tint = MaterialTheme.colorScheme.onBackground)
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Letra de la canción en un rectángulo deslizante
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
                    .scrollable(rememberScrollableState { delta ->
                        if (delta > 0) {
                            lyricsExpanded = true
                        }
                        delta
                    }, orientation = Orientation.Vertical),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),

                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 🔽 Indicador de que se puede deslizar para ver la letra
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(6.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = """
                            LETRA
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Letra de la canción (desplazable)
                    Text(
                        text = """
                            Primera línea de la canción
                            Segunda línea de la canción
                            Tercera línea de la canción
                            Cuarta línea de la canción
                            Quinta línea de la canción
                            Sexta línea de la canción
                            Séptima línea de la canción
                            Octava línea de la canción
                            Novena línea de la canción
                            Décima línea de la canción
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

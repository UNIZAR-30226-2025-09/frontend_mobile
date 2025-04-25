package eina.unizar.es.ui.player

import android.annotation.SuppressLint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.fontscaling.MathUtils.lerp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.musicapp.ui.theme.VibraBlue
import com.example.musicapp.ui.theme.VibraDarkGrey
import com.example.musicapp.ui.theme.VibraMediumGrey
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient.checkIfSongIsLiked
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.ui.playlist.getArtistName
import kotlinx.coroutines.launch
import kotlin.math.abs

@SuppressLint("UseOfNonLambdaOffsetOverload")
@Composable
fun FloatingMusicPlayer(navController: NavController, viewModel: MusicPlayerViewModel, onLikeToggle: (() -> Unit) = {}) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isLiked by viewModel.isCurrentSongLiked.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Estados para gestión de la interfaz
    var artista by remember { mutableStateOf<String?>(null) }
    var offsetX by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isChangingSong by remember { mutableStateOf(false) }

    // Animación constante de rotación del disco (mantenemos esta parte)
    val infiniteTransition = rememberInfiniteTransition(label = "diskSpin")
    val constantDiskRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "diskRotation"
    )

    // Parámetros de animación - Simplificados para una experiencia más minimalista
    val maxOffset = 120f
    val actionThreshold = 70f

    // Colores
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    LaunchedEffect(currentSong?.id) {
        currentSong?.id?.let {
            viewModel.loadLikedStatus(it)
        }
        artista = currentSong?.id?.let { getArtistName(it.toInt()) }.toString()
    }

    currentSong?.let { song ->

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(horizontal = 8.dp)
        ) {
            // Reproductor principal con estilo minimalista
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp, // Reducido para un estilo más minimalista
                        color = surfaceVariantColor,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = !isDragging && !isChangingSong) {
                        navController.navigate("song/${song.id}")
                    }
                    .offset(x = offsetX.dp)
                    .graphicsLayer {
                        // Eliminamos rotación y efectos excesivos, mantenemos sólo sombra básica
                        shadowElevation = 4f
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (abs(offsetX) > actionThreshold) {
                                        // Cambio de canción con animación minimalista
                                        isChangingSong = true

                                        // Determinar dirección
                                        val isNext = offsetX < 0

                                        // Animación de salida suave
                                        animate(
                                            initialValue = offsetX,
                                            targetValue = if (isNext) -maxOffset*1.5f else maxOffset*1.5f,
                                            animationSpec = tween(150, easing = FastOutSlowInEasing)
                                        ) { value, _ ->
                                            offsetX = value
                                        }

                                        // Cambiar canción
                                        if (isNext) {
                                            if (currentSong?.title != "Anuncio Vibra") {viewModel.nextSong(context)}
                                        } else {
                                            if (currentSong?.title != "Anuncio Vibra") {viewModel.previousSong(context)}
                                        }

                                        // Entrar desde el lado opuesto con una animación suave
                                        offsetX = if (isNext) maxOffset*1.5f else -maxOffset*1.5f

                                        // Animación de entrada minimalista
                                        animate(
                                            initialValue = offsetX,
                                            targetValue = 0f,
                                            animationSpec = tween(200, easing = FastOutSlowInEasing)
                                        ) { value, _ ->
                                            offsetX = value
                                        }

                                        isChangingSong = false
                                    } else {
                                        // Animación de retorno si no cambia canción
                                        animate(
                                            initialValue = offsetX,
                                            targetValue = 0f,
                                            animationSpec = tween(150, easing = FastOutSlowInEasing)
                                        ) { value, _ ->
                                            offsetX = value
                                        }
                                    }

                                    isDragging = false
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    animate(
                                        initialValue = offsetX,
                                        targetValue = 0f,
                                        animationSpec = tween(150, easing = FastOutSlowInEasing)
                                    ) { value, _ ->
                                        offsetX = value
                                    }

                                    isDragging = false
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                // Mantenemos la física con resistencia pero más suave
                                val resistanceFactor = 1f - (abs(offsetX) / 300f).coerceIn(0f, 0.7f)
                                offsetX = (offsetX + dragAmount * resistanceFactor).coerceIn(-maxOffset, maxOffset)
                            }
                        )
                    },
                color = VibraMediumGrey.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Imagen con rotación simple del disco
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .graphicsLayer {
                                // Mantenemos solo la rotación constante
                                rotationZ = constantDiskRotation
                                shadowElevation = 2f
                            }
                    ) {
                        // Imagen de portada
                        AsyncImage(
                            model = getImageUrl(song.photo, "default-playlist.jpg"),
                            contentDescription = "Album Art",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.defaultx),
                            error = painterResource(R.drawable.defaultx)
                        )

                        // Pequeño punto central (minimalista)
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.Center)
                                .background(VibraDarkGrey, CircleShape)
                                .border(0.5.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                        )
                    }

                    // Información con movimiento sutil
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .graphicsLayer {
                                // Movimiento paralaje sutil
                                translationX = -offsetX * 0.1f
                            }
                    ) {
                        Text(
                            text = song.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        artista?.let {
                            if (it.isNotEmpty()) {
                                Text(
                                    text = it,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Light,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = onSurfaceColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Controles con movimiento paralaje sutil
                    Row(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = offsetX * 0.1f
                            }
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icono del ordenador
                        IconButton(
                            onClick = { /* acción futuro */ },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Computer,
                                contentDescription = "Computer",
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        if (currentSong?.title != "Anuncio Vibra") {
                            // Botón de like
                            IconButton(
                                onClick = { viewModel.toggleLike(context) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Me gusta",
                                    tint = if (isLiked) Color(0xFFFF6B6B) else Color.Gray,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Botón de reproducción/pausa
                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (!song.isPlaying)
                                    Icons.Filled.PlayArrow
                                else
                                    Icons.Filled.Pause,
                                contentDescription = if (!song.isPlaying) "Play" else "Pause",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
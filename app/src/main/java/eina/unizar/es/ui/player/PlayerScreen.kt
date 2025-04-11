package eina.unizar.es.ui.player

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.fontscaling.MathUtils.lerp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import eina.unizar.es.data.model.network.ApiClient.checkIfSongIsLiked
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.ui.playlist.getArtistName
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun FloatingMusicPlayer(navController: NavController, viewModel: MusicPlayerViewModel, onLikeToggle: (() -> Unit) = {}) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isLiked by viewModel.isCurrentSongLiked.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Estados para animaciones
    var artista by remember { mutableStateOf<String?>(null) }
    var offsetX by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var diskRotation by remember { mutableStateOf(0f) }
    var isChangingSong by remember { mutableStateOf(false) }

    // Animación constante de rotación del disco (vinilo)
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

    // Parámetros de animación
    val maxOffset = 150f
    val actionThreshold = 90f
    val maxScale = 1.05f
    val minScale = 0.95f

    // Colores para usar dentro de Canvas
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    LaunchedEffect(currentSong?.id) {
        currentSong?.id?.let {
            viewModel.loadLikedStatus(it)
        }
        artista = currentSong?.id?.let { getArtistName(it.toInt()) }.toString()
    }

    currentSong?.let { song ->
        // Cálculos para efectos visuales
        val swipeFactor = (offsetX / maxOffset).coerceIn(-1f, 1f)
        val rotation = swipeFactor * 10f
        val scale = if (isDragging) {
            androidx.compose.ui.util.lerp(maxScale, minScale, abs(swipeFactor))
        } else 1f

        // Rotación adicional para el disco cuando se arrastra
        val dynamicDiskRotation = constantDiskRotation + (swipeFactor * 180f)

        // Opacidad para indicadores
        val nextOpacity = (-offsetX / actionThreshold).coerceIn(0f, 1f)
        val prevOpacity = (offsetX / actionThreshold).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(horizontal = 8.dp)
        ) {
            // Efecto de brillo/resplandor según dirección de arrastre
            if (isDragging) {
                val glowColorAlpha = (abs(swipeFactor) * 0.5f).coerceIn(0f, 0.5f)
                val glowColor = primaryColor.copy(alpha = glowColorAlpha)
                val startFraction = if (swipeFactor > 0) 0f else 1f
                val endFraction = if (swipeFactor > 0) 1f else 0f

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, glowColor, Color.Transparent),
                            startX = size.width * startFraction,
                            endX = size.width * endFraction
                        )
                    )
                }

                // Indicadores de acción estilo vinilo
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .alpha(nextOpacity)
                        .padding(end = 16.dp)
                        .size(28.dp)
                        .graphicsLayer {
                            rotationZ = -dynamicDiskRotation * 0.5f
                            shadowElevation = 4f
                        }
                ) {
                    // Disco pequeño indicador
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Disco exterior
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.7f),
                            radius = size.minDimension / 2
                        )
                        // Centro del disco
                        drawCircle(
                            color = onPrimaryColor,
                            radius = size.minDimension / 6
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .alpha(prevOpacity)
                        .padding(start = 16.dp)
                        .size(28.dp)
                        .graphicsLayer {
                            rotationZ = dynamicDiskRotation * 0.5f
                            shadowElevation = 4f
                        }
                ) {
                    // Disco pequeño indicador
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Disco exterior
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.7f),
                            radius = size.minDimension / 2
                        )
                        // Centro del disco
                        drawCircle(
                            color = onPrimaryColor,
                            radius = size.minDimension / 6
                        )
                    }
                }
            }

            // Reproductor principal con efecto de vinilo
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = !isDragging && !isChangingSong) {
                        navController.navigate("song/${song.id}")
                    }
                    .offset(x = offsetX.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                        scaleX = scale
                        scaleY = scale
                        shadowElevation = 8f + abs(swipeFactor) * 8f
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (abs(offsetX) > actionThreshold) {
                                        // Cambio de canción con animación vinilo
                                        isChangingSong = true

                                        // Determinar dirección
                                        val isNext = offsetX < 0

                                        // Primera fase: girar y desaparecer
                                        val initialRotation = rotation
                                        val targetRotation = if (isNext) -180f else 180f

                                        // Animación de giro y escala
                                        animate(
                                            initialValue = 0f,
                                            targetValue = 1f,
                                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                                        ) { progress, _ ->
                                            // Rotación progresiva
                                            val currentRotation = androidx.compose.ui.util.lerp(initialRotation, targetRotation, progress)
                                            diskRotation = currentRotation * 3f

                                            // Escala que disminuye
                                            // val currentScale = lerp(scale, 0.7f, progress) - No usado, eliminado

                                            // Aplicar transformaciones
                                            offsetX = androidx.compose.ui.util.lerp(offsetX, if (isNext) -maxOffset*1.5f else maxOffset*1.5f, progress)
                                        }

                                        // Cambiar canción
                                        if (isNext) {
                                            viewModel.nextSong(context)
                                        } else {
                                            viewModel.previousSong(context)
                                        }

                                        // Segunda fase: aparecer desde el otro lado
                                        offsetX = if (isNext) maxOffset*1.5f else -maxOffset*1.5f

                                        // Animación de entrada
                                        animate(
                                            initialValue = 0f,
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        ) { progress, _ ->
                                            offsetX = androidx.compose.ui.util.lerp(if (isNext) maxOffset*1.5f else -maxOffset*1.5f, 0f, progress)
                                        }

                                        isChangingSong = false
                                    } else {
                                        // Animación de retorno si no cambia canción
                                        animate(
                                            initialValue = offsetX,
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
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
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    ) { value, _ ->
                                        offsetX = value
                                    }

                                    isDragging = false
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                // Física con resistencia adaptativa
                                val resistanceFactor = 1f - (abs(offsetX) / 350f).coerceIn(0f, 0.8f)
                                offsetX = (offsetX + dragAmount * resistanceFactor).coerceIn(-maxOffset, maxOffset)
                            }
                        )
                    },
                color = surfaceVariantColor.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Imagen con efecto de disco giratorio
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .graphicsLayer {
                                // Combinar rotación constante con dinámica
                                rotationZ = if (isChangingSong) diskRotation else constantDiskRotation

                                // Efecto de "disco levitando" cuando se arrastra
                                translationY = if (isDragging) (-swipeFactor * 2f).dp.toPx() else 0f
                                shadowElevation = if (isDragging) 12f else 4f
                            }
                    ) {
                        // Vinilo base (negro)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        )

                        // Imagen de portada
                        AsyncImage(
                            model = getImageUrl(song.photo, "default-playlist.jpg"),
                            contentDescription = "Album Art",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            Color.Black,
                                            primaryColor.copy(alpha = 0.7f),
                                            Color.Black
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentScale = ContentScale.Crop
                        )

                        // Agujero central del vinilo
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.Center)
                                .background(Color.DarkGray, CircleShape)
                                .border(1.dp, Color.Black, CircleShape)
                        )
                    }

                    // Información con efecto de movimiento parallax
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .graphicsLayer {
                                translationX = -offsetX * 0.2f
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

                    // Controles con efecto de movimiento parallax inverso
                    Row(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = offsetX * 0.15f
                            }
                            .padding(end = 8.dp),  // Añado padding para separar del borde
                        verticalAlignment = Alignment.CenterVertically  // Alineación vertical centrada
                    ) {
                        // Icono del ordenador
                        IconButton(
                            onClick = { /* acción futuro */ },
                            modifier = Modifier.size(40.dp)  // Aumento tamaño del área táctil
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Computer,
                                contentDescription = "Computer",
                                modifier = Modifier.size(30.dp),  // Aumento tamaño del icono
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }

                        // Botón de like
                        IconButton(
                            onClick = { viewModel.toggleLike(context) },
                            modifier = Modifier.size(40.dp)  // Aumento tamaño del área táctil
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Me gusta",
                                tint = if (isLiked) Color.Red else Color.Gray,
                                modifier = Modifier.size(30.dp)  // Aumento tamaño del icono
                            )
                        }

                        // Botón de reproducción/pausa
                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier.size(35.dp)  // Ligeramente más grande que los otros
                        ) {
                            Icon(
                                imageVector = if (!song.isPlaying)
                                    Icons.Filled.PlayArrow
                                else
                                    Icons.Filled.Pause,
                                contentDescription = if (!song.isPlaying) "Play" else "Pause",
                                modifier = Modifier.size(34.dp),  // Más grande que los otros iconos
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
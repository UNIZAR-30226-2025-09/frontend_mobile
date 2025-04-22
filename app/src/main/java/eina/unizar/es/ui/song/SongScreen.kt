package com.example.musicapp.ui.song

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import eina.unizar.es.R
import eina.unizar.es.ui.player.MusicPlayerViewModel
import androidx.compose.animation.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musicapp.ui.theme.VibraBlue
import com.example.musicapp.ui.theme.VibraDarkGrey
import com.example.musicapp.ui.theme.VibraLightGrey
import com.example.musicapp.ui.theme.VibraMediumGrey
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.ui.artist.SongOptionsBottomSheetContent
import eina.unizar.es.ui.navbar.BottomNavigationBar
import eina.unizar.es.ui.playlist.getArtistName
import eina.unizar.es.ui.song.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Añade este enum class para manejar los estados del shuffle
enum class ShuffleMode {
    OFF,       // Reproducción normal
    RANDOM     // Reproducción aleatoria
}

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongScreen(navController: NavController, playerViewModel: MusicPlayerViewModel) {
    val context = LocalContext.current
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying = currentSong?.isPlaying ?: false
    val scaffoldState = rememberBottomSheetScaffoldState()

    val shuffleMode by playerViewModel.shuffleMode.collectAsState()
    val isLooping by playerViewModel.isLooping.collectAsState()

    val scrollState = rememberScrollState()
    var artista by remember { mutableStateOf<String?>(null) }
    var showSongOptionsBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(currentSong?.id){
        if (currentSong?.id != null) {
            artista = getArtistName(currentSong?.id!!.toInt())
            Log.d("Nombre", "Nombre : " + artista)
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 50.dp,
        sheetContainerColor = Color(0xFF1E1E1E),
        sheetContentColor = Color.White,
        sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        sheetShadowElevation = 8.dp,
        sheetContent = {
            EnhancedLyricsSheet(lyrics = currentSong?.lyrics)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            VibraMediumGrey,
                            VibraMediumGrey,
                            VibraDarkGrey
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Barra superior con información
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSongOptionsBottomSheet = true },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            tint = Color.White
                        )
                    }

                    // BottomSheet para opciones de la canción
                    if (showSongOptionsBottomSheet && currentSong != null) {
                        // Usamos el artista ya cargado previamente
                        val artistName = artista ?: "Artista desconocido"

                        if (currentSong?.title != "Anuncio Vibra") {
                            ModalBottomSheet(
                                onDismissRequest = { showSongOptionsBottomSheet = false },
                                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                            ) {
                                SongOptionsBottomSheetContent(
                                    songId = currentSong?.id.toString(),
                                    viewModel = playerViewModel,
                                    songTitle = currentSong?.title ?: "",
                                    artistName = artistName,
                                    onClick = {
                                        // Aquí puedes manejar la acción de añadir a la cola
                                        // Por ejemplo, puedes usar el ViewModel para añadir la canción a la cola
                                        playerViewModel.addToQueue(currentSong?.id.toString())
                                        Toast.makeText(
                                            context,
                                            "Añadido a la cola",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Portada del álbum
                AsyncImage(
                    model = getImageUrl(playerViewModel.currentSong.value?.photo, "default-song.jpg"),
                    contentDescription = "Portada",
                    modifier = Modifier
                        .size(320.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    placeholder = painterResource(R.drawable.defaultx),
                    error = painterResource(R.drawable.defaultx),
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Información de la canción
                currentSong?.let { song ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            artista?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Barra de progreso
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Slider(
                            value = currentSong?.progress ?: 0f,
                            onValueChange = { playerViewModel.seekTo(it) },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val currentTime = remember(currentSong?.progress) {
                                (currentSong?.progress ?: 0f) * (playerViewModel.getDuration() ?: 0L)
                            }
                            val duration = playerViewModel.getDuration() ?: 0L

                            Text(
                                text = formatDuration(currentTime.toLong()),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )

                            Text(
                                text = formatDuration(duration),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Controles de reproducción
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (currentSong?.title != "Anuncio Vibra") {playerViewModel.toggleShuffleMode()} }
                        ) {
                            Icon(
                                imageVector = when (shuffleMode) {
                                    ShuffleMode.OFF -> Icons.Filled.Shuffle
                                    ShuffleMode.RANDOM -> Icons.Filled.ShuffleOn
                                },
                                contentDescription = when (shuffleMode) {
                                    ShuffleMode.OFF -> "Activar reproducción"
                                    ShuffleMode.RANDOM -> "Desactivar aleatorio"
                                },
                                tint = when (shuffleMode) {
                                    ShuffleMode.OFF -> Color.White.copy(alpha = 0.7f)
                                    ShuffleMode.RANDOM -> VibraBlue
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { if (currentSong?.title != "Anuncio Vibra") {playerViewModel.previousSong(context)} },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Anterior",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White, CircleShape)
                                .clickable { playerViewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (!isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (!isPlaying) "Reproducir" else "Pausar",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = { playerViewModel.nextSong(context) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Siguiente",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        IconButton(onClick = { if (currentSong?.title != "Anuncio Vibra") {playerViewModel.loopSong() }}) {
                            Icon(
                                imageVector = if (isLooping) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                contentDescription = if (isLooping) "Desactivar repetición" else "Activar repetición",
                                tint = if (isLooping) VibraBlue else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}


fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}


@Composable
fun EnhancedLyricsSheet(lyrics: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LETRA",
            fontSize = 14.sp,
            color = Color.Gray,
            letterSpacing = 1.sp,
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        Divider(
            color = Color.Gray.copy(alpha = 0.2f),
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth(0.1f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (lyrics.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Lryics no disponibles",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            val paragraphs = lyrics.split("\n\n")

            Column(horizontalAlignment = Alignment.Start) {
                paragraphs.forEachIndexed { index, paragraph ->
                    val lines = paragraph.split("\n")

                    Column(modifier = Modifier.fillMaxWidth()) {
                        lines.forEach { line ->
                            Text(
                                text = line,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 28.sp,
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
                        }
                    }

                    if (index < paragraphs.size - 1) {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}


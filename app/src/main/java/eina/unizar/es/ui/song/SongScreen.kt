package com.example.musicapp.ui.song

import android.annotation.SuppressLint
import android.util.Log
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musicapp.ui.theme.VibraBlue
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.ui.navbar.BottomNavigationBar
import eina.unizar.es.ui.playlist.getArtistName
import eina.unizar.es.ui.song.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongScreen(navController: NavController, playerViewModel: MusicPlayerViewModel) {
    val context = LocalContext.current

    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying = currentSong?.isPlaying ?: false
    val scaffoldState = rememberBottomSheetScaffoldState()
    val isLooping by playerViewModel.isLooping.collectAsState()
    val scrollState = rememberScrollState()

    var artista by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentSong?.id){
        if (currentSong?.id != null) {
            artista = getArtistName(currentSong?.id!!.toInt())
            Log.d("Nombre", "Nombre : " + artista)
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 70.dp,
        sheetContainerColor = Color(0xFF1E1E1E),
        sheetContentColor = Color.White,
        sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        sheetShadowElevation = 8.dp,
        sheetContent = {
            EnhancedLyricsSheet(lyrics = currentSong?.lyrics)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Botón para cerrar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(74.dp))

            AsyncImage(
                model = getImageUrl(playerViewModel.currentSong.value?.photo, "default-song.jpg"),
                contentDescription = "Portada",
                modifier = Modifier
                    .size(320.dp)
                    .clip(RoundedCornerShape(16.dp))
            )


            Spacer(modifier = Modifier.height(72.dp))

            currentSong?.let { song ->
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                artista?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Log.d("Nombre", "Nombre despues: " + it)
                }


                Spacer(modifier = Modifier.height(15.dp))

                SongProgressBar(viewModel = playerViewModel)

                // Controles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { playerViewModel.previousSong(context) }) {
                        Icon(Icons.Filled.FastRewind, contentDescription = "Anterior", tint = MaterialTheme.colorScheme.onBackground)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    FloatingActionButton(
                        onClick = { playerViewModel.togglePlayPause() },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = if (!isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (!isPlaying) "Reproducir" else "Pausar",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(onClick = { playerViewModel.nextSong(context) }) {
                        Icon(Icons.Default.FastForward, contentDescription = "Siguiente")
                    }

                    IconButton(onClick = { playerViewModel.loopSong() }) {
                        Icon(
                            imageVector = if (isLooping) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = if (isLooping) "Desactivar repetición" else "Activar repetición",
                            tint = if (isLooping) VibraBlue else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun SongProgressBar(viewModel: MusicPlayerViewModel) {
    val currentSong by viewModel.currentSong.collectAsState()
    val progress = currentSong?.progress ?: 0f

    val currentTime = remember(progress) {
        (progress * (viewModel.getDuration() ?: 0L)).toLong()
    }
    val duration = viewModel.getDuration() ?: 0L

    Column(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(horizontal = 16.dp)
    ) {
        Slider(
            value = progress,
            onValueChange = {
                viewModel.seekTo(it)
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatDuration(currentTime), style = MaterialTheme.typography.labelMedium)
            Text(text = formatDuration(duration), style = MaterialTheme.typography.labelMedium)
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
            .padding(horizontal = 12.dp, vertical = 4.dp)
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
                        text = "Lyrics not available",
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


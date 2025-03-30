package com.example.musicapp.ui.song

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
import eina.unizar.es.ui.navbar.BottomNavigationBar
import eina.unizar.es.ui.song.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongScreen(navController: NavController, songId: String?, playerViewModel: MusicPlayerViewModel) {
    val context = LocalContext.current

    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying = currentSong?.isPlaying ?: false
    val scaffoldState = rememberBottomSheetScaffoldState()

    val scrollState = rememberScrollState()


    LaunchedEffect(songId) {
        playerViewModel.loadSongsFromApi(songId, context, R.drawable.kanyeperfil)
    }


    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 60.dp,
        sheetContainerColor = Color(0xFF2C2C2C),
        sheetContent = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Handle visual
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.LightGray)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("LETRA", fontSize = 18.sp, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = currentSong?.lyrics ?: "Cargando letra...",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )

                //BottomNavigationBar(navController)
            }
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

            Image(
                painter = painterResource(id = R.drawable.kanyeperfil),
                contentDescription = "Portada",
                modifier = Modifier
                    .size(320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background)
            )

            Spacer(modifier = Modifier.height(72.dp))

            currentSong?.let { song ->
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

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
                            imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (isPlaying) "Reproducir" else "Pausar",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(onClick = { playerViewModel.nextSong(context) }) {
                        Icon(Icons.Default.FastForward, contentDescription = "Siguiente")
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


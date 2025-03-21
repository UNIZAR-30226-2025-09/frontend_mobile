package com.example.musicapp.ui.song

import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient.get
import eina.unizar.es.ui.playlist.Playlist
import eina.unizar.es.ui.song.Song
import eina.unizar.es.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun SongScreen(navController: NavController, songId: String?) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0.1f) }
    var lyricsExpanded by remember { mutableStateOf(true) } // Estado para expandir la letra

    var songInfo by remember { mutableStateOf<Song?>(null) }
    var currentSongIndex by remember { mutableIntStateOf(0)}

    var songChanged by remember { mutableStateOf(false) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }

    // Estado de desplazamiento
    val scrollState = rememberScrollState()




    LaunchedEffect(songId) {
        songId?.let {
            val response = get("songs/$it") // Llamamos a la API
            response?.let {
                val jsonObject = JSONObject(response)
                songInfo = Song(
                    id = jsonObject.getInt("id"),
                    name = jsonObject.getString("name"),
                    duration = jsonObject.getInt("duration"),
                    photo_video = jsonObject.getString("photo_video"),
                    url_mp3 = jsonObject.getString("url_mp3"),
                    letra = jsonObject.getString("lyrics")
                )
            }
        }

        // Sacamos todas las canciones por si pasamos de pantalla
        val response = get("songs") // Llamada a la API para obtener canciones
        response?.let {
            val jsonArray = JSONArray(it)
            val fetchedSongs = mutableListOf<Song>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                fetchedSongs.add(
                    Song(
                        id = jsonObject.getInt("id"),
                        name = jsonObject.getString("name"),
                        duration = jsonObject.getInt("duration"),
                        letra = jsonObject.getString("lyrics"),
                        photo_video = jsonObject.getString("photo_video"),
                        url_mp3 = jsonObject.getString("url_mp3")
                    )
                )
            }
            songs = fetchedSongs
        }
    }

// Reproducir la musica
    val context = LocalContext.current
    var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }

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
                songInfo?.let {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text( //Hay que sacar el artista y ponerlo aqui
                    text = "The Academy: Segunda Misión, Sech, Justin Quiles",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(15.dp)) // Bajamos más la barra de progreso

            // Reemplaza el Slider y los Text con SongProgress
            SongProgress(exoPlayer = exoPlayer, isPlaying = isPlaying, songIndex = currentSongIndex)

            Spacer(modifier = Modifier.height(16.dp))

            // Controles de reproducción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (songs.isNotEmpty()) {
                    currentSongIndex = (currentSongIndex - 1 + songs.size) % songs.size
                    songInfo = songs[currentSongIndex]
                    exoPlayer?.release()
                    exoPlayer = null
                    isPlaying = true
                    progress = 0f
                        exoPlayer?.play()
                } }) {
                    Icon(Icons.Filled.FastRewind, contentDescription = "Anterior", tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(16.dp))

                FloatingActionButton(
                    onClick = {
                        isPlaying = !isPlaying
                       },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = {  if (songs.isNotEmpty()) {
                    currentSongIndex = (currentSongIndex + 1 + songs.size) % songs.size
                    songInfo = songs[currentSongIndex]
                    exoPlayer?.release()
                    exoPlayer = null
                    isPlaying = true
                    progress = 0f
                    exoPlayer?.play()
                } }) {
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
                    songInfo?.let {
                        Text(
                            text = it.letra,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }



    LaunchedEffect(isPlaying, songInfo) {
        if (songInfo != null && isPlaying) {
            val urlCompleta = "http://164.90.160.181:5001/${(songInfo?.url_mp3)?.removePrefix("/")}"

            if (exoPlayer == null) {
                exoPlayer = ExoPlayer.Builder(context).build().apply {
                    val mediaItem = MediaItem.fromUri(urlCompleta)
                    setMediaItem(mediaItem)
                    prepare()
                    play()
                }
            } else {
                exoPlayer?.play()
            }
        } else {
            exoPlayer?.pause()
        }
    }

    // Liberar el ExoPlayer cuando el Composable se destruye
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
        }
    }
}


@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SongProgress(exoPlayer: ExoPlayer?, songIndex: Int, isPlaying: Boolean) {
    var currTime by remember { mutableStateOf("0:00") }
    var totalTime by remember { mutableStateOf("0:00") }
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableLongStateOf(0L) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    //val isPlaying = exoPlayer?.isPlaying ?: false // Verifica si la canción está en reproducción

    // LaunchedEffect para escuchar cambios en la posición del reproductor y actualizar el progreso
    LaunchedEffect(exoPlayer) {
        if (exoPlayer != null) {
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        duration = exoPlayer.duration
                        totalTime = formatDuration(duration)
                    }
                }



                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        launch {
                            while (exoPlayer.isPlaying) {
                                currentPosition = exoPlayer.currentPosition
                                delay(10) // Espera 100ms antes de actualizar
                            }
                        }
                    }
                }

                override fun onPositionDiscontinuity(reason: Int) {
                    currentPosition = exoPlayer.currentPosition
                }
            })

            // Actualiza la posición inicial si la canción ya está reproduciéndose
            if (exoPlayer.isPlaying) {
                currentPosition = exoPlayer.currentPosition
            }
        }
    }

    // Reiniciar el progreso cuando cambia la canción
    LaunchedEffect(songIndex) {
            currentPosition = 0L
            currTime = formatDuration(0L)
            progress = 0f
    }

    // LaunchedEffect para simular el avance del slider y el tiempo
    LaunchedEffect(isPlaying) {
        launch {
                while (isPlaying) {
                    currentPosition += 100 // Avanzar un segundo (1000 ms)
                    currTime = formatDuration(currentPosition)
                    progress =
                        if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                    delay(100) // Actualizar cada segundo
                }
        }
    }

    // Simulación de avance en el Slider cuando no se reproduce
    LaunchedEffect(currentPosition) {
        if (exoPlayer != null) {
            currTime = formatDuration(currentPosition)
            progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
        }
    }

    // Slider que permite cambiar la posición de la canción
    Slider(
        value = progress,
        onValueChange = {
            exoPlayer?.seekTo((it * duration).toLong()) // Cambia la posición de la canción
            currentPosition = (it * duration).toLong() // Actualiza la posición
            currTime = formatDuration(currentPosition) // Actualiza el tiempo
        },
        modifier = Modifier.fillMaxWidth(0.85f)
    )

    // Mostrar el tiempo actual y el total de la canción
    Row(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(currTime, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(totalTime, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

fun formatDuration(durationMs: Long): String {
    val duration = durationMs.milliseconds
    val minutes = duration.inWholeMinutes
    val seconds = duration.inWholeSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

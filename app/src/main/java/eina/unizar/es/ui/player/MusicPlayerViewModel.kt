package eina.unizar.es.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import eina.unizar.es.data.model.network.ApiClient.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import org.json.JSONArray
import org.json.JSONObject


// Modelo de canción general usado en el ViewModel
data class CurrentSong(
    val id: String,
    val title: String,
    val artist: String,
    val albumArt: Int,
    val url: String,
    val lyrics: String,
    val isPlaying: Boolean,
    val progress: Float
)

class MusicPlayerViewModel : ViewModel() {

    private var exoPlayer: ExoPlayer? = null

    private val _currentSong = MutableStateFlow<CurrentSong?>(null)
    val currentSong: StateFlow<CurrentSong?> = _currentSong

    private var songList: List<CurrentSong> = emptyList()
    private var currentIndex: Int = 0

    fun loadSongsFromApi(songId: String?, context: Context, albumArtResId: Int) {
        viewModelScope.launch {
            try {
                // Evitar reiniciar si ya se está reproduciendo la canción solicitada
                if (_currentSong.value?.id == songId) return@launch

                // Obtener canción específica
                songId?.let {
                    val response = get("songs/$it")
                    response?.let { res ->
                        val json = JSONObject(res)
                        val selectedSong = CurrentSong(
                            id = json.getInt("id").toString(),
                            title = json.getString("name"),
                            artist = "(artista)",
                            albumArt = albumArtResId,
                            url = "http://164.90.160.181:5001/${json.getString("url_mp3").removePrefix("/")}",
                            lyrics = json.getString("lyrics"),
                            isPlaying = true,
                            progress = 0f
                        )
                        _currentSong.value = selectedSong
                        initializePlayer(context, selectedSong.url)
                    }
                }

                // Obtener lista completa de canciones
                val listResponse = get("songs")
                listResponse?.let {
                    val jsonArray = JSONArray(it)
                    val fetched = mutableListOf<CurrentSong>()
                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        fetched.add(
                            CurrentSong(
                                id = json.getInt("id").toString(),
                                title = json.getString("name"),
                                artist = "Artista desconocido",
                                albumArt = albumArtResId,
                                url = "http://164.90.160.181:5001/${json.getString("url_mp3").removePrefix("/")}",
                                lyrics = json.getString("lyrics"),
                                isPlaying = false,
                                progress = 0f
                            )
                        )
                    }
                    songList = fetched
                    currentIndex = songList.indexOfFirst { it.id == songId }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun initializePlayer(context: Context, songUri: String) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
        }
        val mediaItem = MediaItem.fromUri(songUri)
        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _currentSong.value = _currentSong.value?.copy(isPlaying = isPlaying)
                    if (isPlaying) {
                        startProgressTracking() // <-- Reanudar seguimiento
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        startProgressTracking()
                    }
                }
            })
        }
    }

    private fun startProgressTracking() {
        viewModelScope.launch {
            while (true) {
                val player = exoPlayer ?: break
                if (!player.isPlaying) break

                val duration = player.duration.takeIf { it > 0 } ?: break
                val position = player.currentPosition
                val progress = position.toFloat() / duration.toFloat()

                _currentSong.value = _currentSong.value?.copy(progress = progress)

                delay(500L)
            }
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        val current = _currentSong.value ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        _currentSong.value = current.copy(isPlaying = !player.isPlaying)
    }

    fun getDuration(): Long? = exoPlayer?.duration

    fun seekTo(progress: Float) {
        val duration = exoPlayer?.duration ?: return
        val newPosition = (progress * duration).toLong()
        exoPlayer?.seekTo(newPosition)
        _currentSong.value = _currentSong.value?.copy(progress = progress)
    }

    fun nextSong(context: Context) {
        if (songList.isEmpty()) return
        currentIndex = (currentIndex + 1) % songList.size
        val song = songList[currentIndex]
        _currentSong.value = song.copy(isPlaying = true, progress = 0f)
        initializePlayer(context, song.url)
    }

    fun previousSong(context: Context) {
        if (songList.isEmpty()) return
        currentIndex = (currentIndex - 1 + songList.size) % songList.size
        val song = songList[currentIndex]
        _currentSong.value = song.copy(isPlaying = true, progress = 0f)
        initializePlayer(context, song.url)
    }

    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
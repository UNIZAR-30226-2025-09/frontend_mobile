package eina.unizar.es.ui.player

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import eina.unizar.es.data.model.network.ApiClient.checkIfSongIsLiked
import eina.unizar.es.data.model.network.ApiClient.get
import eina.unizar.es.data.model.network.ApiClient.getUserData
import eina.unizar.es.data.model.network.ApiClient.likeUnlikeSong
import eina.unizar.es.ui.playlist.PlaylistScreen
import eina.unizar.es.ui.playlist.getArtistName
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import org.json.JSONObject


// Model used to represent the current song
data class CurrentSong(
    val id: String,
    val title: String,
    val artist: String,
    val photo: String,
    val albumArt: Int,
    val url: String,
    val lyrics: String,
    val isPlaying: Boolean,
    val progress: Float
)

class MusicPlayerViewModel : ViewModel() {
    // ExoPlayer instance
    private var exoPlayer: ExoPlayer? = null

    // State for the current song
    private val _currentSong = MutableStateFlow<CurrentSong?>(null)
    val currentSong: StateFlow<CurrentSong?> = _currentSong

    // List of songs
    private var songList: List<CurrentSong> = emptyList()

    // State for the current song index
    private var currentIndex: Int = 0

    // State for the current playlist ID
    var idCurrentPlaylist by mutableStateOf("")

    // State for the liked songs
    private val _likedSongs = MutableStateFlow<Set<String>>(emptySet())
    val likedSongs: StateFlow<Set<String>> = _likedSongs

    // Check if current song is liked
        val isCurrentSongLiked = currentSong.combine(likedSongs) { song, liked ->
        song?.id?.let { id -> id in liked } ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    // User ID
    private var _userId: String = ""

    // State Loop
    private val _isLooping = MutableStateFlow(false)
    val isLooping: StateFlow<Boolean> = _isLooping

    // PARA ACTUALIZAR EL VIBRABANNER HABRIA QUE HACER ALGO ASI PARA ACTUALIZAR EL ESTADO DE ISPREMIUM
    /*
    // State Premium
    private val _isPremium = mutableStateOf(false)
    val isPremium: State<Boolean> = _isPremium

    // Función para actualizar el estado premium
    fun updatePremiumStatus(isPremium: Boolean) {
        _isPremium.value = isPremium
    }

    // Función para cargar el estado premium desde SharedPreferences
    fun loadPremiumStatus(context: Context) {
        viewModelScope.launch {
            val userData = getUserData(context)
            if (userData != null) {
                _isPremium.value = userData["is_premium"] as Boolean
                Log.d("Premium", "Estado premium cargado: ${_isPremium.value}")
            }
        }
    }
     */

    // Function to get the liked songs
    fun getLikedSongs(): Set<String> {
        return _likedSongs.value
    }

    // Function to get the userId
    fun getUserId(): String {
        return _userId
    }

    // Function to set the userId when context is available
    fun setUserId(context: Context) {
        viewModelScope.launch {
            try {
                val userData = getUserData(context)
                if (userData != null) {
                    _userId = (userData["id"] ?: "").toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    // Function to load initial liked status
    fun loadLikedStatus(songId: String?) {
        viewModelScope.launch {
            try {
                // If userId is empty, try to get it first
                if (_userId.isEmpty()) {
                    // This might be why it's not working - we need to ensure userId is set
                    return@launch
                }

                if (songId.isNullOrEmpty()) {
                    return@launch
                }

                // Call the API to check if the song is liked
                val isLiked = checkIfSongIsLiked(songId, _userId)

                // Log for debugging
                println("Song $songId is liked: $isLiked")

                // Update the liked songs set based on the API response
                _likedSongs.value = if (isLiked) {
                    _likedSongs.value + songId
                } else {
                    _likedSongs.value - songId
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Function to toggle like/unlike a song
    fun toggleLike(context: Context) {
        val currentSongId = _currentSong.value?.id ?: return

        // Make sure userId is set
        if (_userId.isEmpty()) {
            setUserId(context)
            if (_userId.isEmpty()) return
        }

        viewModelScope.launch {
            try {
                // Determine the current like state
                val currentlyLiked = currentSongId in _likedSongs.value
                val newLikeState = !currentlyLiked

                // Convert song ID to Int for the API call
                val songIdInt = currentSongId.toIntOrNull() ?: return@launch

                // Call API to update like status on the server
                val response = likeUnlikeSong(songIdInt.toString(), _userId, newLikeState)

                if (response != null) {
                    // Update local state only if API call is successful
                    _likedSongs.value = if (newLikeState) {
                        _likedSongs.value + currentSongId
                    } else {
                        _likedSongs.value - currentSongId
                    }

                    // Log for debugging
                    println("Song $currentSongId like status updated to: $newLikeState")
                } else {
                    // Handle error case
                    println("Error updating song like status")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Exception in toggleLike: ${e.message}")
            }
        }
    }

    // Function to load songs from API
    fun loadSongsFromApi(songId: String?, context: Context, albumArtResId: Int) {
        viewModelScope.launch {
            try {
                // Check if the songId is already playing
                if (_currentSong.value?.id == songId) return@launch

                // Set userId if needed
                if (_userId.isEmpty()) {
                    setUserId(context)
                }

                // Set the current song
                songId?.let {
                    val response = get("songs/$it")
                    response?.let { res ->
                        val json = JSONObject(res)
                        val selectedSong = CurrentSong(
                            id = json.getInt("id").toString(),
                            title = json.getString("name"),
                            artist = "(artista)",
                            photo = json.getString("photo_video"),
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

                // Load the list of songs
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
                                artist ="Desconocido" ,
                                photo = json.getString("photo_video"),
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
                    idCurrentPlaylist = ""
                }
                songId?.let { loadLikedStatus(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Function to load songs from a playlist
    fun loadSongsFromPlaylist(playlistSongs: List<CurrentSong>, songId: String?, context: Context, idPlaylist: String) {
        // Set userId if needed
        if (_userId.isEmpty()) {
            setUserId(context)
        }

        songList = playlistSongs
        currentIndex = songList.indexOfFirst { it.id == songId } // Find the index of the song in the playlist
        idCurrentPlaylist = idPlaylist

        // Initialize the player with the first song in the playlist
        val selectedSong = songList[currentIndex]
        _currentSong.value = selectedSong
        initializePlayer(context, selectedSong.url)
        songId?.let { loadLikedStatus(it) }
    }

    // Function to initialize the ExoPlayer
    private fun initializePlayer(context: Context, songUri: String) {
        // Context from the application to avoid memory leaks
        val appContext = context.applicationContext

        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(appContext).build()
        }

        // Clean up the previous media items
        exoPlayer?.removeListener(playerListener)

        // Create a new MediaItem
        val mediaItem = MediaItem.fromUri(songUri)

        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()

            // Configure the player
            repeatMode = if (_isLooping.value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

            // Add the listener to track playback state
            addListener(playerListener)
        }
    }

    // Crear un listener como propiedad de clase para poder eliminarlo después
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _currentSong.value = _currentSong.value?.copy(isPlaying = isPlaying)
            if (isPlaying) {
                startProgressTracking()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                startProgressTracking()
            }
            else if (playbackState == Player.STATE_ENDED) {
                // Only manage the next song if we are not in loop mode
                // and if we are in loop mode, the viewModel will handle it
                if (!_isLooping.value) {
                    // Thread Principal
                    viewModelScope.launch {
                        // Advance to the next song
                        val index = (currentIndex + 1) % songList.size
                        currentIndex = index
                        val nextSong = songList[index]

                        // Update the current song
                        _currentSong.value = nextSong.copy(isPlaying = true, progress = 0f)

                        // Reset the player
                        exoPlayer?.clearMediaItems()
                        exoPlayer?.setMediaItem(MediaItem.fromUri(nextSong.url))
                        exoPlayer?.prepare()
                        exoPlayer?.play()

                        // Load liked status for the next song
                        loadLikedStatus(nextSong.id)
                    }
                }
            }
        }
    }

    // Function to start tracking the song progress
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

    // Function to toggle play/pause
    fun togglePlayPause() {
        val player = exoPlayer ?: return
        val current = _currentSong.value ?: return

        // Guarda el nuevo estado que queremos
        val newPlayingState = !player.isPlaying

        if (newPlayingState) {
            player.play()
        } else {
            player.pause()
        }

        // Actualiza con el nuevo estado que sabemos es correcto
        _currentSong.value = current.copy(isPlaying = newPlayingState)
    }

    // Function to get the current song duration
    fun getDuration(): Long? = exoPlayer?.duration

    // Function to get the current song position
    fun seekTo(progress: Float) {
        val duration = exoPlayer?.duration ?: return
        val newPosition = (progress * duration).toLong()
        exoPlayer?.seekTo(newPosition)
        _currentSong.value = _currentSong.value?.copy(progress = progress)
    }

    // Function to pass to the next song
    fun nextSong(context: Context) {
        if (songList.isEmpty()) return
        currentIndex = (currentIndex + 1) % songList.size
        val song = songList[currentIndex]
        _currentSong.value = song.copy(isPlaying = true, progress = 0f)
        initializePlayer(context, song.url)

        // Load liked status for the new song
        loadLikedStatus(song.id)
    }

    // Function to pass to the previous song
    fun previousSong(context: Context) {
        if (songList.isEmpty()) return
        currentIndex = (currentIndex - 1 + songList.size) % songList.size
        val song = songList[currentIndex]
        _currentSong.value = song.copy(isPlaying = true, progress = 0f)
        initializePlayer(context, song.url)

        // Load liked status for the new song
        loadLikedStatus(song.id)
    }

    // Function to release the player
    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    // Function to clear the ViewModel
    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }

    // Function to clean up data on logout
    fun cleanupOnLogout() {
        // Release the player
        releasePlayer()

        // Reset all data
        _currentSong.value = null
        songList = emptyList()
        currentIndex = 0
        idCurrentPlaylist = ""
        _likedSongs.value = emptySet()
        _userId = ""
        _isLooping.value = false

        Log.d("MusicPlayerViewModel", "Todos los datos del reproductor han sido limpiados")
    }

    // Function to stopp the music
    fun stopMusic() {
        exoPlayer?.stop()
        _currentSong.value = _currentSong.value?.copy(isPlaying = false)
    }

    // Función para inicializar las canciones con like
    fun initializeLikedSongs(userId: String) {
        viewModelScope.launch {
            if (userId.isEmpty()) {
                Log.e("MusicPlayerViewModel", "No se puede cargar likes: userId está vacío")
                return@launch
            }

            try {
                // La ruta correcta según tus comentarios
                val response = get("/song_like/$userId/likedSongs")

                if (response == null) {
                    Log.e("MusicPlayerViewModel", "Respuesta nula del servidor al cargar likes")
                    return@launch
                }

                val jsonArray = JSONArray(response)
                val likedIds = mutableSetOf<String>()

                for (i in 0 until jsonArray.length()) {
                    val songObject = jsonArray.getJSONObject(i)
                    val songId = songObject.getInt("id").toString()
                    likedIds.add(songId)
                    Log.d("MusicPlayerViewModel", "Canción con like añadida: $songId")
                }

                _likedSongs.value = likedIds
                Log.d("MusicPlayerViewModel", "Total de canciones con like cargadas: ${likedIds.size}")

                // Actualiza también el estado de la canción actual si existe
                _currentSong.value?.id?.let { currentId ->
                    if (currentId in likedIds) {
                        Log.d("MusicPlayerViewModel", "La canción actual está en likes")
                    } else {
                        Log.d("MusicPlayerViewModel", "La canción actual NO está en likes")
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicPlayerViewModel", "Error cargando canciones con like: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Function to loop the current song
    fun loopSong() {
        // Change the loop state
        val newLoopState = !_isLooping.value
        _isLooping.value = newLoopState

        // Apply the loop state to the ExoPlayer
        exoPlayer?.repeatMode = if (newLoopState) {
            Player.REPEAT_MODE_ONE // Repeat the current song
        } else {
            Player.REPEAT_MODE_OFF // No repeat
        }

        Log.d("MusicPlayer", "Loop mode: ${if (newLoopState) "ON" else "OFF"}")
    }
}
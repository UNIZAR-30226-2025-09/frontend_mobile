package eina.unizar.es.ui.player

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicapp.ui.song.ShuffleMode
import eina.unizar.es.data.model.network.ApiClient.checkIfSongIsLiked
import eina.unizar.es.data.model.network.ApiClient.get
import eina.unizar.es.data.model.network.ApiClient.getSongDetails
import eina.unizar.es.data.model.network.ApiClient.getUserData
import eina.unizar.es.data.model.network.ApiClient.likeUnlikeSong
import eina.unizar.es.ui.playlist.PlaylistScreen
import eina.unizar.es.ui.playlist.getArtistName
import eina.unizar.es.ui.song.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
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

    // State shuffle
    private val _shuffleMode = MutableStateFlow(ShuffleMode.OFF)
    val shuffleMode = _shuffleMode.asStateFlow()

    // Original song list
    private var originalSongList: List<CurrentSong> = emptyList()

    // Cola de reproducción
    private val _songsQueue = MutableStateFlow<List<Song>>(emptyList())
    val songsQueue: StateFlow<List<Song>> = _songsQueue

    // Función para cambiar el modo shuffle
    fun toggleShuffleMode() {
        val previousMode = _shuffleMode.value
        _shuffleMode.value = when (previousMode) {
            ShuffleMode.OFF -> ShuffleMode.RANDOM
            ShuffleMode.RANDOM -> ShuffleMode.OFF
        }

        Log.d("MusicPlayer", "Modo shuffle cambiado a: ${_shuffleMode.value}")

        when (_shuffleMode.value) {
            ShuffleMode.OFF -> {
                // Restaurar el orden original de la lista de reproducción
                restoreOriginalPlaylist()
            }
            ShuffleMode.RANDOM -> {
                // Mezclar aleatoriamente la lista de reproducción
                shuffleOrderPlaylist()
            }
        }
    }

    // Función para restaurar la lista original
    private fun restoreOriginalPlaylist() {
        // Solo restauramos si tenemos una lista original guardada
        if (originalSongList.isNotEmpty()) {
            // Guardamos el ID de la canción actual para mantenerla después de restaurar
            val currentSongId = _currentSong.value?.id

            // Restauramos la lista original
            songList = originalSongList.toList()

            // Encontramos la posición de la canción actual en la lista restaurada
            currentIndex = songList.indexOfFirst { it.id == currentSongId }
            if (currentIndex == -1) currentIndex = 0 // Si no se encuentra, usamos la primera canción

            Log.d("MusicPlayer", "Playlist restaurada al orden original")
        }
    }


    // Función para mezclar aleatoriamente la lista
    private fun shuffleOrderPlaylist() {
        if (songList.isEmpty()) return

        // Guardamos la lista original si aún no lo hemos hecho
        if (originalSongList.isEmpty()) {
            originalSongList = songList.toList()
        }

        // Guardamos el ID de la canción actual
        val currentSongId = _currentSong.value?.id

        // Creamos una copia de la lista para mezclarla
        val shuffledList = songList.toMutableList()

        // Quitamos la canción actual de la lista antes de mezclar (si existe)
        val currentSong = shuffledList.find { it.id == currentSongId }
        if (currentSong != null) {
            shuffledList.remove(currentSong)
        }

        // Mezclamos el resto de canciones
        shuffledList.shuffle()

        // Volvemos a añadir la canción actual al principio (para mantenerla en reproducción)
        if (currentSong != null) {
            shuffledList.add(0, currentSong)
        }

        // Actualizamos la lista de canciones y el índice actual
        songList = shuffledList
        currentIndex = if (currentSong != null) 0 else currentIndex

        Log.d("MusicPlayer", "Playlist mezclada aleatoriamente")
    }

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
    fun toggleSongLike(songId: String, userId: String) {
        viewModelScope.launch {
            try {
                // Determine the new like state based on the _likedSongs set
                val currentLikeState = songId in _likedSongs.value
                val newLikeState = !currentLikeState

                // Make API call to like/unlike the song
                val response = likeUnlikeSong(songId, userId, newLikeState)

                if (response != null) {
                    // Update local state only if API call is successful
                    _likedSongs.value = if (newLikeState) {
                        _likedSongs.value + songId
                    } else {
                        _likedSongs.value - songId
                    }

                    // Log the update
                    Log.d("MusicPlayerViewModel", "Song $songId like status updated to: $newLikeState")
                } else {
                    // Handle error case
                    Log.e("MusicPlayerViewModel", "Error updating song like status")
                }

                // Reload liked status to ensure UI consistency
                loadLikedStatus(songId)

            } catch (e: Exception) {
                Log.e("MusicPlayerViewModel", "Exception in toggleSongLike: ${e.message}")
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
                        val rawUrl = "http://164.90.160.181:5001/${json.getString("url_mp3").removePrefix("/")}"
                        val formattedUrl = formatSongUrl(rawUrl)

                        val selectedSong = CurrentSong(
                            id = json.getInt("id").toString(),
                            title = json.getString("name"),
                            artist = "(artista)",
                            photo = json.getString("photo_video"),
                            albumArt = albumArtResId,
                            url = formattedUrl, // URL ya formateada
                            lyrics = json.getString("lyrics"),
                            isPlaying = true,
                            progress = 0f
                        )
                        _currentSong.value = selectedSong
                        initializePlayer(context, formattedUrl)
                    }
                }

                // Load the list of songs with formatted URLs
                val listResponse = get("songs")
                listResponse?.let {
                    val jsonArray = JSONArray(it)
                    val fetched = mutableListOf<CurrentSong>()
                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        val rawUrl = "http://164.90.160.181:5001/${json.getString("url_mp3").removePrefix("/")}"
                        val formattedUrl = formatSongUrl(rawUrl)

                        fetched.add(
                            CurrentSong(
                                id = json.getInt("id").toString(),
                                title = json.getString("name"),
                                artist = "Desconocido",
                                photo = json.getString("photo_video"),
                                albumArt = albumArtResId,
                                url = formattedUrl, // URL ya formateada
                                lyrics = json.getString("lyrics"),
                                isPlaying = false,
                                progress = 0f
                            )
                        )
                    }
                    songList = fetched
                    originalSongList = fetched
                    currentIndex = songList.indexOfFirst { it.id == songId }
                    idCurrentPlaylist = ""
                }
                songId?.let { loadLikedStatus(it) }
            } catch (e: Exception) {
                Log.e("MusicPlayerViewModel", "Error cargando canciones: ${e.message}")
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
        originalSongList = playlistSongs
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
        try {
            // Context from the application to avoid memory leaks
            val appContext = context.applicationContext

            // Formatear correctamente la URL
            val formattedUri = formatSongUrl(songUri)
            Log.d("MusicPlayerViewModel", "Intentando reproducir: $formattedUri")

            if (exoPlayer == null) {
                exoPlayer = ExoPlayer.Builder(appContext).build()
            }

            // Clean up the previous media items
            exoPlayer?.removeListener(playerListener)

            // Create a new MediaItem
            val mediaItem = MediaItem.fromUri(formattedUri)

            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
                play()

                // Configure the player
                repeatMode = if (_isLooping.value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

                // Add the listener to track playback state
                addListener(playerListener)

                // Añadir un listener específico para errores
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("MusicPlayerViewModel", "Error de reproducción: ${error.message}")

                        // Intenta cargar la siguiente canción en caso de error
                        viewModelScope.launch {
                            // Si hay canciones en cola, intentar con la siguiente
                            if (_songsQueue.value.isNotEmpty()) {
                                Log.d("MusicPlayerViewModel", "Intentando con la siguiente canción de la cola")
                                val nextQueueSong = _songsQueue.value.first()
                                _songsQueue.value = _songsQueue.value.drop(1)

                                val nextSongUrl = formatSongUrl(nextQueueSong.url_mp3)
                                val nextCurrentSong = CurrentSong(
                                    id = nextQueueSong.id.toString(),
                                    title = nextQueueSong.name,
                                    artist = "Desconocido",
                                    photo = nextQueueSong.photo_video,
                                    albumArt = 0,
                                    url = nextSongUrl,
                                    lyrics = nextQueueSong.letra,
                                    isPlaying = true,
                                    progress = 0f
                                )

                                _currentSong.value = nextCurrentSong

                                // Limpiar y cargar nuevo item
                                exoPlayer?.clearMediaItems()
                                exoPlayer?.setMediaItem(MediaItem.fromUri(nextSongUrl))
                                exoPlayer?.prepare()
                                exoPlayer?.play()
                            } else if (songList.isNotEmpty()) {
                                // Si no hay canciones en cola, pasar a la siguiente de la lista
                                Log.d("MusicPlayerViewModel", "Intentando con la siguiente canción de la lista")
                                val index = (currentIndex + 1) % songList.size
                                currentIndex = index
                                val nextSong = songList[index]

                                _currentSong.value = nextSong.copy(isPlaying = true, progress = 0f)

                                val nextSongUrl = formatSongUrl(nextSong.url)
                                exoPlayer?.clearMediaItems()
                                exoPlayer?.setMediaItem(MediaItem.fromUri(nextSongUrl))
                                exoPlayer?.prepare()
                                exoPlayer?.play()
                            }
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("MusicPlayerViewModel", "Error inicializando player: ${e.message}")
            e.printStackTrace()
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
                // Solo gestionamos la siguiente canción si no estamos en modo bucle
                if (!_isLooping.value) {
                    // Thread Principal
                    viewModelScope.launch {
                        try {
                            // Siempre dar prioridad a la cola
                            if (_songsQueue.value.isNotEmpty()) {
                                val nextQueueSong = _songsQueue.value.first()

                                // Actualizar la cola (eliminar la primera canción)
                                _songsQueue.value = _songsQueue.value.drop(1)

                                // Formatear la URL correctamente
                                val formattedUrl = formatSongUrl(nextQueueSong.url_mp3)

                                // Intentar obtener detalles completos para tener el artista
                                val songDetails = getSongDetails(nextQueueSong.id.toString())
                                val artistName = songDetails?.get("artist_name") as? String ?: "Desconocido"

                                // Crear una CurrentSong con todos los datos actualizados
                                val nextCurrentSong = CurrentSong(
                                    id = nextQueueSong.id.toString(),
                                    title = nextQueueSong.name,
                                    artist = artistName,
                                    photo = nextQueueSong.photo_video,
                                    albumArt = 0,
                                    url = formattedUrl,
                                    lyrics = nextQueueSong.letra,
                                    isPlaying = true,
                                    progress = 0f
                                )

                                // Actualizar la canción actual
                                _currentSong.value = nextCurrentSong

                                // Preparar el reproductor
                                exoPlayer?.clearMediaItems()
                                exoPlayer?.setMediaItem(MediaItem.fromUri(formattedUrl))
                                exoPlayer?.prepare()
                                exoPlayer?.play()

                                // Cargar estado de like
                                loadLikedStatus(nextCurrentSong.id)

                                Log.d("MusicPlayerViewModel", "Reproduciendo siguiente de la cola: ${nextQueueSong.name} (Artista: $artistName)")
                            } else {
                                // Si no hay canciones en la cola, pasar a la siguiente de la lista normal
                                val index = (currentIndex + 1) % songList.size
                                currentIndex = index
                                val nextSong = songList[index]

                                // Formatear la URL correctamente
                                val formattedUrl = formatSongUrl(nextSong.url)

                                // Update the current song
                                _currentSong.value = nextSong.copy(isPlaying = true, progress = 0f)

                                // Reset the player
                                exoPlayer?.clearMediaItems()
                                exoPlayer?.setMediaItem(MediaItem.fromUri(formattedUrl))
                                exoPlayer?.prepare()
                                exoPlayer?.play()

                                // Load liked status for the next song
                                loadLikedStatus(nextSong.id)

                                Log.d("MusicPlayerViewModel", "Reproduciendo siguiente de la lista: ${nextSong.title}")
                            }
                        } catch (e: Exception) {
                            // Error handling code...
                        }
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
        refreshQueueState()
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
        // Primero verificar si hay canciones en la cola
        if (_songsQueue.value.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    // Obtener y remover la primera canción de la cola
                    val nextQueueSong = _songsQueue.value.first()
                    _songsQueue.update { it.drop(1) }

                    // Formatear la URL correctamente
                    val formattedUrl = formatSongUrl(nextQueueSong.url_mp3)

                    // Obtener detalles completos de la canción
                    val songDetails = getSongDetails(nextQueueSong.id.toString())
                    val artistName = songDetails?.get("artist_name") as? String ?: "Desconocido"

                    // Crear el objeto CurrentSong con todos los datos
                    val nextCurrentSong = CurrentSong(
                        id = nextQueueSong.id.toString(),
                        title = nextQueueSong.name,
                        artist = artistName,
                        photo = nextQueueSong.photo_video,
                        albumArt = 0,
                        url = formattedUrl,
                        lyrics = nextQueueSong.letra,
                        isPlaying = true,
                        progress = 0f
                    )

                    // Actualizar el estado antes de inicializar el reproductor
                    _currentSong.value = nextCurrentSong

                    // Inicializar el reproductor con la nueva canción
                    initializePlayer(context, formattedUrl)

                    // Cargar estado de like
                    loadLikedStatus(nextCurrentSong.id)

                    Log.d("MusicPlayer", "Reproduciendo de cola: ${nextCurrentSong.title}")
                } catch (e: Exception) {
                    Log.e("MusicPlayer", "Error al pasar a siguiente canción de cola: ${e.message}")
                }
            }
        } else {
            // Comportamiento original si no hay cola
            if (songList.isEmpty()) return
            currentIndex = (currentIndex + 1) % songList.size
            val song = songList[currentIndex]

            // Actualizar el estado antes de inicializar el reproductor
            _currentSong.value = song.copy(isPlaying = true, progress = 0f)
            initializePlayer(context, song.url)
            loadLikedStatus(song.id)
        }
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
        _songsQueue.value = emptyList()

        Log.d("MusicPlayerViewModel", "Todos los datos del reproductor han sido limpiados")
    }

    // Add a queue change state to force UI updates
    private val _queueChangeCounter = MutableStateFlow(0)
    val queueChangeCounter = _queueChangeCounter.asStateFlow()

    // Function to add a song to the queue
    fun addToQueue(songId: String, playNext: Boolean = true) {
        viewModelScope.launch {
            try {
                val songDetails = getSongDetails(songId.toString())
                songDetails?.let { details ->
                    // Obtener la URL y formatearla correctamente
                    val rawSongUrl = details["url_mp3"] as? String ?: return@let
                    val formattedSongUrl = formatSongUrl(rawSongUrl)

                    val newSong = Song(
                        id = songId.toInt(),
                        name = details["name"] as? String ?: "Desconocido",
                        duration = (details["duration"] as? Int) ?: 0,
                        photo_video = details["photo_video"] as? String ?: "",
                        url_mp3 = formattedSongUrl, // URL ya formateada
                        letra = details["lyrics"] as? String ?: ""
                    )

                    // Actualizar la cola
                    _songsQueue.update { currentQueue ->
                        currentQueue + newSong
                    }

                    // Notificar cambio para forzar recomposición
                    refreshQueueState()
                }
            } catch (e: Exception) {
                Log.e("MusicPlayerViewModel", "Error al añadir a la cola: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Add this function to MusicPlayerViewModel
    fun refreshQueueState() {
        _queueChangeCounter.value += 1
    }

    // Función para formatear correctamente la URL de una canción
    private fun formatSongUrl(rawUrl: String): String {
        // Verificar si la URL ya está formateada correctamente (comienza con http)
        if (rawUrl.startsWith("http")) {
            return rawUrl
        }

        // Si la URL contiene código JavaScript o texto extraño, extraemos solo la parte de la ruta
        if (rawUrl.contains("function") || rawUrl.contains("{")) {
            // Buscar un patrón que podría ser una ruta de archivo
            val pathPattern = "/songs/.*\\.mp3".toRegex()
            val matchResult = pathPattern.find(rawUrl)

            if (matchResult != null) {
                // Extraer solo la parte de la ruta y formatear la URL
                val path = matchResult.value
                return "http://164.90.160.181:5001$path"
            }
        }

        // Si la URL es solo una ruta relativa, añadir el prefijo del servidor
        val cleanPath = rawUrl.trim().removePrefix("/")
        return "http://164.90.160.181:5001/$cleanPath"
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
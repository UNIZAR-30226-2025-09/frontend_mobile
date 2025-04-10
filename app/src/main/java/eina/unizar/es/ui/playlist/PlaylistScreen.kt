package eina.unizar.es.ui.playlist

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient.get
import eina.unizar.es.data.model.network.ApiClient.delete
import eina.unizar.es.ui.song.Song
import org.json.JSONArray
import org.json.JSONObject
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import eina.unizar.es.data.model.network.ApiClient.getLikedPlaylists
import eina.unizar.es.data.model.network.ApiClient.getUserData
import eina.unizar.es.data.model.network.ApiClient.likeUnlikePlaylist
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.stripe.android.core.strings.resolvableString
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.data.model.network.ApiClient.checkIfSongIsLiked
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.data.model.network.ApiClient.getLikedPlaylists
import eina.unizar.es.data.model.network.ApiClient.getLikedSongsPlaylist
import eina.unizar.es.data.model.network.ApiClient.getSongDetails
import eina.unizar.es.data.model.network.ApiClient.getUserData
import eina.unizar.es.data.model.network.ApiClient.isPlaylistOwner
import eina.unizar.es.data.model.network.ApiClient.likeUnlikePlaylist
import eina.unizar.es.data.model.network.ApiClient.likeUnlikeSong
import eina.unizar.es.ui.artist.SongOptionItem
import eina.unizar.es.ui.navbar.BottomNavigationBar
import eina.unizar.es.ui.player.FloatingMusicPlayer
import eina.unizar.es.ui.player.MusicPlayerViewModel
import eina.unizar.es.ui.search.SongItem
import eina.unizar.es.ui.search.convertSongsToCurrentSongs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files.delete
import eina.unizar.es.ui.artist.SongOptionsBottomSheetContent



// Criterios de ordenacion de canciones de una lista
enum class SortOption {
    TITULO, DURACION, ARTISTA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(navController: NavController, playlistId: String?, playerViewModel: MusicPlayerViewModel,
                   isSencillo: Boolean = false, singleId: String?) {

    // Colores básicos
    val backgroundColor = MaterialTheme.colorScheme.background // Negro
    val textColor = MaterialTheme.colorScheme.onBackground // Blanco
    val buttonColor = MaterialTheme.colorScheme.primary

    //Gestion del ViewModel
    val currentSong by playerViewModel.currentSong.collectAsState()
    var currentIdPlaylist = playerViewModel.idCurrentPlaylist
    val isPlaying = currentSong?.isPlaying ?: false


    // Estados para búsqueda y orden
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    var sortOption by remember { mutableStateOf(SortOption.TITULO) }

    // Variable para poder saber si se ha dado like desde el viewModel
    val likedSongsSet by playerViewModel.likedSongs.collectAsState()

    // Estado para mostrar/ocultar la barra de búsqueda
    var showSearch by remember { mutableStateOf(false) }

    // Estado del LazyColumn para detectar scroll y aplicar efecto en el header
    val lazyListState = rememberLazyListState()
    val imageSize = 150.dp
    val maxOffset = with(LocalDensity.current) { imageSize.toPx() }
    val scrollOffset = lazyListState.firstVisibleItemScrollOffset.toFloat()
    val collapseFraction = (scrollOffset / maxOffset).coerceIn(0f, 1f)

    // Ajustamos solo la opacidad (sin escala) con Modifier.alpha
    val imageAlpha = 1f - collapseFraction

    // Estado para controlar la visibilidad del BottomSheet
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Reproducir la musica
    val context = LocalContext.current


    // Alpha para el título en el TopAppBar: aparece gradualmente conforme se hace scroll
    val topTitleAlpha = if (lazyListState.firstVisibleItemIndex > 0) {
        1f
    } else {
        ((scrollOffset) / (maxOffset / 2)).coerceIn(0f, 1f)
    }

    // Estado para me gusta o no de la playlist
    var isLikedPlaylist by remember { mutableStateOf(false) }

    // Estado para almacenar la información de la playlist y sus canciones
    var playlistInfo by remember { mutableStateOf<Playlist?>(null) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }

    // Usamos un Map para manejar el estado de "me gusta" por canción usando el índice o algún identificador único
    var songLikes by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }

    // Para poder realizar el post del like/unlike
    val coroutineScope = rememberCoroutineScope()

    // Id del usuario a guardar al darle like
    var userId by remember { mutableStateOf("") }  // Estado inicial

    // Función para cambiar el estado de "me gusta" de una canción
    fun toggleSongLike(songId: Int, userId: String) {
        coroutineScope.launch {
            try {
                // Determine the new like state
                val currentLikeState = songLikes[songId] ?: false
                val newLikeState = !currentLikeState

                // Make API call to like/unlike the song
                val response = likeUnlikeSong(songId.toString(), userId, newLikeState)
                val responseCheck = checkIfSongIsLiked(songId.toString(), userId)

                if (response != null) {
                    // Update local state only if API call is successful
                    songLikes = songLikes.toMutableMap().apply {
                        this[songId] = newLikeState
                    }
                } else {
                    // Handle error case (e.g., show error message)
                    println("Error updating song like status")
                }

                if (responseCheck != null) {
                    playerViewModel.loadLikedStatus(songId.toString())
                } else {
                    // Handle error case (e.g., show error message)
                    println("Error updating song check like status")
                }
            } catch (e: Exception) {
                println("Exception in toggleSongLike: ${e.message}")
            }
        }
    }

    LaunchedEffect(songs, likedSongsSet) {
        songLikes = songs.associate { song ->
            song.id to likedSongsSet.contains(song.id.toString())
        }
    }


    //Logica si es un sencillo
    if(isSencillo){
        LaunchedEffect(Unit) {
            val response = withContext(Dispatchers.IO) { get("player/details/$singleId") }
            response?.let {
                val songObject = JSONObject(it)
                //val songObject = jsonObject.getJSONObject("song")

                // Crear un objeto Playlist que simula ser un sencillo
                playlistInfo = Playlist(
                    id = songObject.getString("id"),
                    title = songObject.getString("name"),
                    imageUrl = songObject.getString("photo_video"),
                    idAutor = "",//jsonObject.getJSONArray("artists").getJSONObject(0).getString("id"),
                    idArtista = "",//jsonObject.getJSONArray("artists").getJSONObject(0).getString("id"),
                    description = "Sencillo",
                    esPublica = "public",
                    esAlbum = "single"
                )

                // Crear lista con una sola canción
                songs = mutableListOf(
                    Song(
                        id = songObject.getInt("id"),
                        name = songObject.getString("name"),
                        duration = songObject.getInt("duration"),
                        letra = songObject.getString("lyrics"),
                        photo_video = songObject.getString("photo_video"),
                        url_mp3 = songObject.getString("url_mp3")
                    )
                )
            }
            Log.d("Sencillo", "Datos del backend: ${songs.first().name}")
        }
    } else { //Logica si es una playlist
        // Llamar a la API para obtener los datos de la playlist seleccionada
        LaunchedEffect(playlistId) {
            playlistId?.let { id ->
                val response = withContext(Dispatchers.IO) { get("playlists/$id") }
                response?.let {
                    val jsonObject = JSONObject(it)
                    playlistInfo = Playlist(
                        id = jsonObject.getString("id"),
                        title = jsonObject.getString("name"),
                        imageUrl = jsonObject.getString("front_page"),
                        idAutor = jsonObject.getString("user_id"),
                        idArtista = jsonObject.getString("artist_id"),
                        description = jsonObject.getString("description"),
                        esPublica = jsonObject.getString("type"),
                        esAlbum = jsonObject.getString("typeP"),
                    )

                    // Extraer las canciones del array "songs"
                    val songsArray = jsonObject.getJSONArray("songs")
                    val fetchedSongs = mutableListOf<Song>()
                    for (i in 0 until songsArray.length()) {
                        val songObject = songsArray.getJSONObject(i)
                        fetchedSongs.add(
                            Song(
                                id = songObject.getInt("id"),
                                name = songObject.getString("name"),
                                duration = songObject.getInt("duration"),
                                letra = songObject.getString("lyrics"),
                                photo_video = songObject.getString("photo_video"),
                                url_mp3 = songObject.getString("url_mp3")
                            )
                        )
                    }
                    songs = fetchedSongs
                }
            }
        }
    }

    LaunchedEffect(Unit){
        val userData = getUserData(context)
        if (userData != null) {
            userId =
                (userData["id"]
                    ?: "Id").toString()  // Si no hay nickname, usa "Usuario"
        }
        // Consultar si el usuario ya le ha dado like a esta playlist (para poder guardar el like)
        val likedPlaylistsResponse = getLikedPlaylists(userId)
        likedPlaylistsResponse?.let { playlists ->
            // Verificamos si la playlist actual está en la lista de "liked" del usuario
            isLikedPlaylist = playlists.any { it.id == playlistId }
        }


        // Consultar si el usuario ya le ha dado like a alguna cancion (para poder guardar el like)
        val likedSongsResponse = getLikedSongsPlaylist(userId)
        likedSongsResponse?.let { likedSongs ->
            // Create a map of song IDs to their liked status
            val likedSongIds = likedSongs.map { it.id }.toSet()

            // Update song likes based on the fetched liked songs
            songLikes = songs.associate { song ->
                song.id to likedSongIds.contains(song.id)
            }
        }
    }

    val playlistAuthor = playlistInfo?.let { getPlaylistAuthor(it) };

    // Mapa para almacenar el nombre del artista por canción
    var songArtistMap by remember { mutableStateOf<Map<Song, String>>(emptyMap()) }


    // Obtener los nombres de los artistas para cada canción
    LaunchedEffect(songs) {
        val artistNames = mutableMapOf<Song, String>()
        songs.forEach { song ->
            val artistName = getArtistName(song.id)
            artistNames[song] = artistName
        }
        songArtistMap = artistNames
    }


    val sortedSongs = remember(songs, sortOption) {
        when (sortOption) {
            SortOption.TITULO -> songs.sortedBy { it.name } // Ordenar por título
            SortOption.DURACION -> songs.sortedBy { it.duration } // Ordenar por duración
            SortOption.ARTISTA -> songs.sortedBy { song ->
                songArtistMap[song] ?: "Artista Desconocido" } // Ordenar por artista
        }
    }

    // Primero filtramos por búsqueda
    val filteredSongs = remember(songs, searchText.text) {
        if (searchText.text.isEmpty()) {
            sortedSongs
        } else {
            songs.filter { song ->
                song.name.contains(searchText.text, ignoreCase = true)
            }
        }
    }

    // Luego ordenamos las canciones filtradas
    val sortedAndFilteredSongs = remember(filteredSongs, sortOption) {
        when (sortOption) {
            SortOption.TITULO -> filteredSongs.sortedBy { it.name }
            SortOption.DURACION -> filteredSongs.sortedBy { it.duration }
            SortOption.ARTISTA -> songs.sortedBy { song ->
                songArtistMap[song] ?: "Artista Desconocido" } // Ordenar por artista
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    playlistInfo?.let {
                        Text(
                            text = it.title,
                            color = textColor,
                            modifier = Modifier.alpha(topTitleAlpha)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(backgroundColor)
        ) {
            // Header: Portada, título y autor
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    playlistInfo?.let { Log.d("URL antes", "URL antes: " + it.imageUrl) }
                    // Cargar la imagen desde la URL
                    val urlAntes = playlistInfo?.imageUrl
                    val playlistImage = getImageUrl(urlAntes, "/default-playlist.jpg")
                    AsyncImage(
                        model = getImageUrl(urlAntes, "/default-playlist.jpg"),
                        contentDescription = "Portada de la playlist",
                        placeholder = painterResource(R.drawable.defaultplaylist), // Fallback local
                        error = painterResource(R.drawable.defaultplaylist),
                        modifier = Modifier
                            .size(250.dp)
                            .alpha(imageAlpha)
                            .clip(RoundedCornerShape(8.dp)) // Opcional: añade esquinas redondeadas
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    playlistInfo?.let {
                        Text(
                            text = it.title,
                            color = textColor,
                            style = TextStyle(fontSize = 20.sp)
                        )
                    }
                    if (playlistAuthor != null && playlistInfo?.esAlbum != "album") {
                        Text(
                            text = "Autor: " + playlistAuthor,
                            color = textColor,
                            style = TextStyle(fontSize = 14.sp)
                        )
                    }
                }
            }
            // Fila para la búsqueda: si showSearch es true, se muestra la barra de búsqueda que deja espacio para el icono de reproducir
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            label = { Text("Buscar en playlist", color = textColor) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            textStyle = TextStyle(color = textColor),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = buttonColor,
                                unfocusedBorderColor = textColor,
                                cursorColor = textColor
                            )
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            showSearch = !showSearch
                            // Si estamos ocultando la búsqueda, limpiamos el texto
                            if (!showSearch) {
                                searchText = TextFieldValue("")
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Mostrar/Ocultar búsqueda",
                            tint = textColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (isPlaying && currentIdPlaylist == playlistId) {
                                playerViewModel.togglePlayPause()
                            } else {
                                if (isSencillo) {
                                    if (playlistId != null) {
                                        playerViewModel.loadSongsFromPlaylist(
                                            convertSongsToCurrentSongs(sortedAndFilteredSongs, 1),
                                            sortedAndFilteredSongs.first().id.toString(), context,
                                            playlistId)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(buttonColor)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = Color.Black
                        )
                    }
                }
            }
            // Fila con dropdown para ordenar y botón de like
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                        .padding(start = 12.dp)
                ) {
                    Text("Ordenar por:", color = textColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    var expandirMenu by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { expandirMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                        ) {
                            Text(sortOption.toString(), color = Color.Black)
                        }
                        DropdownMenu(
                            expanded = expandirMenu,
                            onDismissRequest = { expandirMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Título") },
                                onClick = {
                                    sortOption = SortOption.TITULO
                                    expandirMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Artista") },
                                onClick = {
                                    sortOption = SortOption.ARTISTA
                                    expandirMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duración") },
                                onClick = {
                                    sortOption = SortOption.DURACION
                                    expandirMenu = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    if (playlistInfo?.esPublica != "private" && playlistInfo?.idAutor != userId && !isSencillo) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    // Invertir el estado local de "me gusta"
                                    isLikedPlaylist = !isLikedPlaylist
                                    // Hacer el POST al backend para actualizar el "me gusta"
                                    val response = playlistInfo?.let {
                                        likeUnlikePlaylist(
                                            playlistInfo!!.id,
                                            userId,
                                            isLikedPlaylist
                                        )
                                    }
                                    if (response != null) {
                                        // Aquí puedes manejar la respuesta, como mostrar un mensaje o cambiar algo en UI
                                        println("Respuesta del backend: $response")
                                    } else {
                                        // En caso de que falle la solicitud, revertir el cambio en el estado
                                        isLikedPlaylist = !isLikedPlaylist
                                        println("Error al hacer el POST en el backend")
                                    }
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (!isSencillo){
                                Icon(
                                    imageVector = Icons.Default.Favorite, // Usamos el ícono de "me gusta"
                                    contentDescription = "Me gusta",
                                    tint = if (isLikedPlaylist) Color.Red else Color.Gray // Si está seleccionado, se colorea rojo, si no es gris
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(2.dp)) // Espacio entre iconos
                    }
                    IconButton(
                        onClick = {
                            showBottomSheet = true // Mostrar el BottomSheet al hacer clic
                        },
                        modifier = Modifier.size(25.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            tint = textColor
                        )
                    }

                }
            }

            item { Spacer(modifier = Modifier.height(26.dp)) }

            // Lista de canciones: Cada banner con imagen a la izquierda y título/artista a la derecha
            if (songs.isNullOrEmpty()) {
                // Mostrar mensaje cuando no hay canciones
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicOff, // Puedes usar otro icono si prefieres
                                contentDescription = "No hay canciones",
                                modifier = Modifier.size(48.dp),
                                tint = textColor.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No hay canciones en esta playlist",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(sortedAndFilteredSongs) { song ->
                    //val artist = songArtistMap[song] ?: "Artista Desconocido"
                    var showSongOptionsBottomSheet by remember { mutableStateOf(false) } // Estado para mostrar el BottomSheet de opciones de la canción
                    var songArtists by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }

                    LaunchedEffect(song.id) {
                        val songDetails = getSongDetails(song.id.toString())
                        songDetails?.let { details ->
                            @Suppress("UNCHECKED_CAST")
                            songArtists = details["artists"] as? List<Map<String, String>> ?: emptyList()
                        }
                    }

                    if (playlistId != null) {
                            SongItem(
                                song = song,
                                showHeartIcon = true,
                                showMoreVertIcon = true,
                                isLiked = songLikes[song.id] ?: false,
                                onLikeToggle = {
                                    // Lógica para manejar el like
                                    toggleSongLike(song.id, userId)
                                },
                                onMoreVertClick = {
                                    // Mostrar opciones de la canción
                                    showSongOptionsBottomSheet = true
                                },
                                viewModel = playerViewModel,
                                isPlaylist = true,
                                playlistSongs = sortedSongs,
                                idPlaylist = playlistId
                            )
                            Log.d("Sencillo", "Canción cargada: ${song.name}")
                    } else {
                        SongItem(
                            song = song,
                            showHeartIcon = true,
                            isSencillo = true,
                            showMoreVertIcon = true,
                            isLiked = songLikes[song.id] ?: false,
                            onLikeToggle = {
                                // Lógica para manejar el like
                                toggleSongLike(song.id, userId)
                            },
                            onMoreVertClick = {
                                // Mostrar opciones de la canción
                                showSongOptionsBottomSheet = true
                            },
                            viewModel = playerViewModel,
                            isPlaylist = true,
                            playlistSongs = sortedSongs,
                            idPlaylist = "1"
                        )
                        Log.d("Sencillo", "Canción cargada: ${song.name}")
                    }
                    // BottomSheet para opciones de la canción (dentro del items)
                    if (showSongOptionsBottomSheet) {
                        val artistName = if (songArtists.isNotEmpty()) {
                            songArtists.joinToString(", ") { it["name"] ?: "" }
                        } else {
                            "Artista desconocido"
                        }
                        ModalBottomSheet(
                            onDismissRequest = { showSongOptionsBottomSheet = false },
                            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        ) {
                            SongOptionsBottomSheetContent(
                                songId = song.id.toString(),
                                viewModel = playerViewModel,
                                songTitle = song.name, // Pasa el título de la canción
                                artistName = artistName // Pasa el nombre del artista
                            )
                        }
                    }
                }
            }
        }
        // Mostrar el BottomSheet de la playlist (fuera del items)
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                var urlAntes = playlistInfo?.imageUrl
                val playlistImage = getImageUrl(urlAntes, "default-playlist.jpg")
                playlistInfo?.let {
                    if (playlistAuthor != null) {
                        BottomSheetContent(
                            playlistImage = playlistImage,
                            playlistTitle = playlistInfo!!.title, // Reemplaza con el título
                            playlistAuthor = playlistAuthor,
                            onDismiss = { showBottomSheet = false },
                            navController = navController,
                            playlistId = playlistId,
                            playlistMeGusta = playlistInfo!!.esAlbum
                        )
                    }
                }
            }
        }

    }
}

/**
 * Contenido del Bottom Sheet con las opciones de la playlist.
 */
@Composable
fun BottomSheetContent(
    playlistImage: String,
    playlistTitle: String,
    playlistAuthor: String,
    navController: NavController,
    playlistId: String?,
    onDismiss: () -> Unit,  // Llamar a esta función para cerrar
    playlistMeGusta: String
) {
    val scope = rememberCoroutineScope()  // Para lanzar corrutinas en Compose
    val textColor = Color.White
    var showAlertDialog by remember { mutableStateOf(false) }
    var soyPropietario by remember { mutableStateOf(false) }// Estado para saber si es propietario
    val context = LocalContext.current
    var userId by remember { mutableStateOf("") }  // Estado inicial
    val coroutineScope = rememberCoroutineScope()

    // Función interna para manejar el dismissal
    val dismiss = {
        showAlertDialog = false
    }
    Log.d("BottomSheetContent", "Playlist ID: $playlistId")

    LaunchedEffect(Unit) {
        coroutineScope.launch  {
            val userData = getUserData(context)
            if (userData != null) {
                userId =
                    (userData["id"]
                        ?: "Id").toString()  // Si no hay nickname, usa "Usuario"
            }
            // Verificar si el usuario es propietario de la playlist
            soyPropietario = if (playlistId != null) {
                isPlaylistOwner(playlistId, userId) ?: true
            } else {
                false
            }
            Log.d("BottomSheetContent", "Soy propietario: $soyPropietario")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // Imagen, título y autor de la playlist en fila
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = playlistImage,
                contentDescription = "Portada de la playlist",
                placeholder = painterResource(R.drawable.defaultplaylist), // Fallback local
                error = painterResource(R.drawable.defaultplaylist),
                modifier = Modifier
                    .size(50.dp)
                    //.alpha(imageAlpha)
                    .clip(RoundedCornerShape(8.dp)) // Opcional: añade esquinas redondeadas

            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(playlistTitle, color = textColor, fontSize = 16.sp)
                Text("de $playlistAuthor", color = Color.Gray, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Opciones de la playlist centradas
        Column(
            modifier = Modifier
                .fillMaxWidth()
                //.padding(start = 120.dp)
                .wrapContentWidth(Alignment.CenterHorizontally), // Centra el Column en su contenedor
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(15.dp))
            SongOptionItem("Añadir a la biblioteca", onClick = dismiss)
            Spacer(modifier = Modifier.height(8.dp))
            SongOptionItem("Compartir", onClick = dismiss)
            Spacer(modifier = Modifier.height(8.dp))
            SongOptionItem("Valorar Playlist", onClick = dismiss)
            Spacer(modifier = Modifier.height(8.dp))

            if(playlistMeGusta != "Vibra_likedSong" && soyPropietario) {
                // Opción "Eliminar Playlist" con estilo personalizado
                SongOptionItem("Editar Playlist", onClick = dismiss)
                Spacer(modifier = Modifier.height(8.dp))
                SongOptionItem(
                    text = "Eliminar Playlist",
                    textColor = Color(0xFFFF6B6B),
                    onClick = {
                        // Llamada al backend en una corrutina
                        scope.launch {
                            if (!playlistId.isNullOrEmpty()) {
                                try {
                                    eliminarPlaylistEnBackend(playlistId)
                                    // Si se elimina con éxito, realizamos un popBackStack
                                    navController.popBackStack()
                                    // Cierra tu bottomSheet como veas (estado local, etc.)
                                } catch (e: Exception) {
                                    println("Error al eliminar la playlist: ${e.message}")
                                }
                            }
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(38.dp))
        }
    }
}


suspend fun eliminarPlaylistEnBackend(
    playlistId: String
): Boolean {
    // Llama a tu API
    val response = withContext(Dispatchers.IO) {
        delete("playlists/$playlistId")
    }
    println("Respuesta de la API: $response")
    // Devuelve true/false, o lanza excepción, según sea tu preferencia
    return true
}

// Función para obtener el nombre del artista de una canción
suspend fun getArtistName(songId: Int): String {
    return withContext(Dispatchers.IO) {
        val response = get("player/details/$songId")
        response?.let {
            val jsonObject = JSONObject(it)
            val artistsArray = jsonObject.getJSONArray("artists")
            if (artistsArray.length() > 0) {
                artistsArray.getJSONObject(0).getString("name")
            } else {
                "Artista Desconocido"
            }
        } ?: "Artista Desconocido"
    }
}

//Funcion para obtener el autor de la lista
private fun getPlaylistAuthor(playlist: Playlist): String {
    return when (playlist?.esAlbum) {
        "Vibra" -> "Vibra"
        "album" -> playlist.title?.let { "$it" } ?: "Álbum sin título"
        null -> "Origen desconocido"
        else -> "Colección personalizada"
    }
}
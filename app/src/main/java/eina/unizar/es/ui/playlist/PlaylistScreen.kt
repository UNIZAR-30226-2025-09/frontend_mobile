package eina.unizar.es.ui.playlist

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Space
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathSegment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import com.example.musicapp.ui.theme.VibraBlack
import com.example.musicapp.ui.theme.VibraBlue
import com.example.musicapp.ui.theme.VibraLightGrey
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
import eina.unizar.es.data.model.network.ApiClient.togglePlaylistType
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay


// Criterios de ordenacion de canciones de una lista
enum class SortOption {
    TITULO, DURACION, ARTISTA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(navController: NavController, playlistId: String?, playerViewModel: MusicPlayerViewModel,
                   isSencillo: Boolean = false, singleId: String?) {

    // Colores y estilos mejorados
    val backgroundColor = Color(0xFF121212) // Negro más suave
    val textColor = Color.White
    val secondaryTextColor = Color(0xFFB3B3B3) // Gris claro para textos secundarios
    val accentColor = Color(0xFF1DB954) // Verde Spotify
    val dividerColor = Color(0xFF2A2A2A) // Color sutil para divisores

    // Estado para diálogo de valoración
    var showRatingDialog by remember { mutableStateOf(false) }
    var currentRating by remember { mutableStateOf(0) }

    // Gestion del ViewModel
    val currentSong by playerViewModel.currentSong.collectAsState()
    var currentIdPlaylist = playerViewModel.idCurrentPlaylist
    val isPlaying = currentSong?.isPlaying ?: false

    // Estados para búsqueda y orden
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    var sortOption by remember { mutableStateOf(SortOption.TITULO) }
    val likedSongsSet by playerViewModel.likedSongs.collectAsState()
    var showSearch by remember { mutableStateOf(false) }

    // Animation states
    val searchFieldWidth by animateDpAsState(
        targetValue = if (showSearch) 200.dp else 0.dp,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "searchFieldWidth"
    )

    // Estado del LazyColumn para detectar scroll y aplicar efecto en el header
    val lazyListState = rememberLazyListState()
    val imageSize = 150.dp
    val maxOffset = with(LocalDensity.current) { imageSize.toPx() }
    val scrollOffset = lazyListState.firstVisibleItemScrollOffset.toFloat()
    val collapseFraction = (scrollOffset / maxOffset).coerceIn(0f, 1f)

    // Ajustamos solo la opacidad (sin escala) con Modifier.alpha
    val imageAlpha = 1f - collapseFraction * 0.6f

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

    // Estado para almacenar información de la playlist y sus canciones
    var playlistInfo by remember { mutableStateOf<Playlist?>(null) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var songLikes by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()
    var userId by remember { mutableStateOf("") }

    // Función para cambiar el estado de "me gusta" de una canción
    fun toggleSongLike(songId: Int, userId: String) {
        coroutineScope.launch {
            try {
                val currentLikeState = songLikes[songId] ?: false
                val newLikeState = !currentLikeState
                val response = likeUnlikeSong(songId.toString(), userId, newLikeState)
                val responseCheck = checkIfSongIsLiked(songId.toString(), userId)

                if (response != null) {
                    songLikes = songLikes.toMutableMap().apply {
                        this[songId] = newLikeState
                    }
                }

                if (responseCheck != null) {
                    playerViewModel.loadLikedStatus(songId.toString())
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

    // Cargar datos de la API
    if (isSencillo) {
        LaunchedEffect(Unit) {
            val response = withContext(Dispatchers.IO) { get("player/details/$singleId") }
            response?.let {
                val songObject = JSONObject(it)
                playlistInfo = Playlist(
                    id = songObject.getString("id"),
                    title = songObject.getString("name"),
                    imageUrl = songObject.getString("photo_video"),
                    idAutor = "",
                    idArtista = "",
                    description = "Sencillo",
                    esPublica = "public",
                    esAlbum = "single"
                )

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
        }
    } else {
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

    LaunchedEffect(Unit) {
        val userData = getUserData(context)
        if (userData != null) {
            userId = (userData["id"] ?: "Id").toString()
        }

        val likedPlaylistsResponse = getLikedPlaylists(userId)
        likedPlaylistsResponse?.let { playlists ->
            isLikedPlaylist = playlists.any { it.id == playlistId }
        }

        val likedSongsResponse = getLikedSongsPlaylist(userId)
        likedSongsResponse?.let { likedSongs ->
            val likedSongIds = likedSongs.map { it.id }.toSet()
            songLikes = songs.associate { song ->
                song.id to likedSongIds.contains(song.id)
            }
        }
    }

    val playlistAuthor = playlistInfo?.let { getPlaylistAuthor(it) }
    var songArtistMap by remember { mutableStateOf<Map<Song, String>>(emptyMap()) }

    LaunchedEffect(songs) {
        val artistNames = mutableMapOf<Song, String>()
        songs.forEach { song ->
            val artistName = getArtistName(song.id)
            artistNames[song] = artistName
        }
        songArtistMap = artistNames
    }

    // Ordenar y filtrar canciones
    val sortedSongs = remember(songs, sortOption) {
        when (sortOption) {
            SortOption.TITULO -> songs.sortedBy { it.name }
            SortOption.DURACION -> songs.sortedBy { it.duration }
            SortOption.ARTISTA -> songs.sortedBy { song ->
                songArtistMap[song] ?: "Artista Desconocido" }
        }
    }

    val filteredSongs = remember(sortedSongs, searchText.text) {
        if (searchText.text.isEmpty()) {
            sortedSongs
        } else {
            sortedSongs.filter { song ->
                song.name.contains(searchText.text, ignoreCase = true)
            }
        }
    }

    // Crea un gradiente de fondo basado en la portada
    val gradientColors = listOf(
        Color(0xFF121212).copy(alpha = 0.9f),
        Color(0xFF121212)
    )

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(gradientColors)
                )
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(275.dp)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .shadow(8.dp)
                        ) {
                            val urlAntes = playlistInfo?.imageUrl
                            AsyncImage(
                                model = getImageUrl(urlAntes, "/defaultplaylist.jpg"),
                                contentDescription = "Portada",
                                placeholder = painterResource(R.drawable.defaultplaylist),
                                error = painterResource(R.drawable.defaultplaylist),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(imageAlpha)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        playlistInfo?.let {
                            Text(
                                text = it.title,
                                color = textColor,
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center
                            )
                        }

                        if (playlistAuthor != null && playlistInfo?.esAlbum != "album") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = playlistAuthor,
                                color = secondaryTextColor,
                                style = TextStyle(fontSize = 16.sp)
                            )
                        }
                        // Añadir descripción de la playlist al final
                        playlistInfo?.description?.takeIf { it.isNotBlank() && it != "Sencillo" && it != "null"}?.let { description ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 30.dp, bottom = 2.dp)
                            ) {
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = secondaryTextColor,
                                        lineHeight = 20.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Barra de búsqueda y controles
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Sección izquierda: búsqueda y ordenamiento juntos
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                // Animación de búsqueda
                                AnimatedVisibility(
                                    visible = showSearch,
                                    enter = expandHorizontally() + fadeIn(),
                                    exit = shrinkHorizontally() + fadeOut()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = searchText,
                                            onValueChange = { searchText = it },
                                            label = { Text("Buscar", color = Color.White) },
                                            modifier = Modifier
                                                .width(180.dp)
                                                .height(60.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                                focusedBorderColor = VibraBlue,
                                                unfocusedBorderColor = Color(0xFF3E3E3E),
                                                cursorColor = VibraBlue,
                                                containerColor = Color(0xFF2A2A2A)
                                            ),
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        )

                                        IconButton(
                                            onClick = {
                                                showSearch = false
                                                searchText = TextFieldValue("")
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Cerrar búsqueda",
                                                tint = secondaryTextColor
                                            )
                                        }
                                    }
                                }

                                // Icono de búsqueda (cuando no está mostrando el campo)
                                if (!showSearch) {
                                    IconButton(
                                        onClick = { showSearch = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Buscar",
                                            tint = secondaryTextColor
                                        )
                                    }
                                }

                                // Dropdown para ordenar (sin espaciado)
                                Box(
                                    modifier = Modifier.padding(start = 0.dp) // Reduce o elimina el espaciado
                                ) {
                                    var expandirMenu by remember { mutableStateOf(false) }
                                    TextButton(
                                        onClick = { expandirMenu = true },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = secondaryTextColor
                                        ),
                                        contentPadding = PaddingValues(
                                            start = 4.dp, // Reduce el padding interno del botón
                                            end = 8.dp,
                                            top = 8.dp,
                                            bottom = 8.dp
                                        )
                                    ) {
                                        Text(
                                            text = when (sortOption) {
                                                SortOption.TITULO -> "Título"
                                                SortOption.DURACION -> "Duración"
                                                SortOption.ARTISTA -> "Artista"
                                            },
                                            style = TextStyle(fontSize = 14.sp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = expandirMenu,
                                        onDismissRequest = { expandirMenu = false },
                                        modifier = Modifier.background(Color(0xFF282828))
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Título", color = textColor) },
                                            onClick = {
                                                sortOption = SortOption.TITULO
                                                expandirMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Artista", color = textColor) },
                                            onClick = {
                                                sortOption = SortOption.ARTISTA
                                                expandirMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Duración", color = textColor) },
                                            onClick = {
                                                sortOption = SortOption.DURACION
                                                expandirMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Botones de la derecha
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Botón "Me gusta" para la playlist
                                if (playlistInfo?.esPublica != "private" && playlistInfo?.idAutor != userId && !isSencillo) {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isLikedPlaylist = !isLikedPlaylist
                                                val response = playlistInfo?.let {
                                                    likeUnlikePlaylist(
                                                        it.id,
                                                        userId,
                                                        isLikedPlaylist
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = "Me gusta",
                                            tint = if (isLikedPlaylist) Color(0xFFFF6B6B) else secondaryTextColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // Botón de opciones
                                IconButton(
                                    onClick = { showBottomSheet = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Más opciones",
                                        tint = secondaryTextColor
                                    )
                                }

                                // Botón de play principal
                                IconButton(
                                    onClick = {
                                        if (currentIdPlaylist == playlistId) {
                                            // Si ya está reproduciendo esta playlist, solo pausar/reanudar
                                            playerViewModel.togglePlayPause()
                                        } else {
                                            // Si es otra playlist o no está reproduciendo nada, cargar las canciones
                                            if (playlistId != null && filteredSongs.isNotEmpty()) {
                                                playerViewModel.loadSongsFromPlaylist(
                                                    convertSongsToCurrentSongs(filteredSongs, filteredSongs.size),
                                                    filteredSongs.firstOrNull()?.id.toString() ?: "",
                                                    context,
                                                    playlistId
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(VibraBlue)
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying && currentIdPlaylist == playlistId)
                                            Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying && currentIdPlaylist == playlistId)
                                            "Pausar" else "Reproducir",
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Divisor
                item {
                    Divider(
                        color = dividerColor,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

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
                    items(filteredSongs) { song ->
                        //val artist = songArtistMap[song] ?: "Artista Desconocido"
                        var showSongOptionsBottomSheet by remember { mutableStateOf(false) } // Estado para mostrar el BottomSheet de opciones de la canción
                        var songArtists by remember {
                            mutableStateOf<List<Map<String, String>>>(
                                emptyList()
                            )
                        }

                        LaunchedEffect(song.id) {
                            val songDetails = getSongDetails(song.id.toString())
                            songDetails?.let { details ->
                                @Suppress("UNCHECKED_CAST")
                                songArtists =
                                    details["artists"] as? List<Map<String, String>> ?: emptyList()
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
            StarRatingDialog(
                showDialog = showRatingDialog,
                onDismiss = { showRatingDialog = false },
                onConfirm = { stars ->
                    currentRating = stars
                    // Lógica para guardar la valoración
                    CoroutineScope(Dispatchers.IO).launch {
                        playlistId?.let {
                            //savePlaylistRating(it, stars, LocalContext.current)
                        }
                    }
                }
            )
            // Mostrar el BottomSheet de la playlist (fuera del items)
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState
                ) {
                    var urlAntes = playlistInfo?.imageUrl
                    val playlistImage = getImageUrl(urlAntes, "defaultplaylist.jpg")
                    playlistInfo?.let {
                        if (playlistAuthor != null) {
                            BottomSheetContent(
                                playlistInfo = it,
                                playlistImage = playlistImage,
                                playlistTitle = playlistInfo!!.title, // Reemplaza con el título
                                playlistAuthor = playlistAuthor,
                                isLikedPlaylist = isLikedPlaylist,
                                onDismiss = { showBottomSheet = false },
                                navController = navController,
                                playlistId = playlistId,
                                playlistMeGusta = playlistInfo!!.esAlbum,
                                onRateClick = { showRatingDialog = true },
                                onLikeUpdate = { newState ->
                                    // Actualiza el estado en el componente padre
                                    isLikedPlaylist = newState
                                },
                                onPlaylistUpdated = {
                                    // Actualiza la playlist en el componente padre
                                    playlistInfo = it
                                }
                            )
                        }
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
    playlistInfo: Playlist,
    playlistImage: String,
    playlistTitle: String,
    playlistAuthor: String,
    isLikedPlaylist: Boolean,
    navController: NavController,
    playlistId: String?,
    onRateClick: () -> Unit,
    onDismiss: () -> Unit,  // Llamar a esta función para cerrar
    playlistMeGusta: String,
    onLikeUpdate: (Boolean) -> Unit,
    onPlaylistUpdated: (Playlist) -> Unit
) {
    val scope = rememberCoroutineScope()  // Para lanzar corrutinas en Compose
    val textColor = Color.White
    var showAlertDialog by remember { mutableStateOf(false) }
    var soyPropietario by remember { mutableStateOf(false) }// Estado para saber si es propietario
    val context = LocalContext.current
    var userId by remember { mutableStateOf("") }  // Estado inicial
    val coroutineScope = rememberCoroutineScope()

    // Estado para controlar la carga
    var isLoading by remember { mutableStateOf(true) }

    // Función interna para manejar el dismissal
    val dismiss = {
        showAlertDialog = false
    }


    // Estado para controlar si mostrar las opciones de compartir
    var showShareOptions by remember { mutableStateOf(false) }

    // URL base para compartir
    val baseShareUrl = "https://vibra.eina.unizar.es/playlist/" // Usa HTTPS para compatibilidad universal
    val vibraDeepLink = "vibra://playlist/" // Para abrir directamente en la app si está instalada
    val fullShareUrl = playlistId?.let { "$baseShareUrl$it" } ?: ""
    val fullDeepLink = playlistId?.let { "$vibraDeepLink$it" } ?: ""

    // Función para copiar al portapapeles
    fun copyToClipboard() {
        val clipboardManager = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        val clipData = ClipData.newPlainText("Enlace a playlist", fullShareUrl)
        clipboardManager?.setPrimaryClip(clipData)
        Toast.makeText(context, "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show()
        onDismiss()
    }

    // Modifica tu función de compartir para incluir enlaces que funcionen en navegadores
    fun sharePlaylist(playlistId: String, playlistTitle: String, context: Context) {
        // URL con formato clickable universal
        val webUrl = "https://vibra.eina.unizar.es/playlist/$playlistId"
        val deepLink = "vibra://playlist/$playlistId"

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, """
            ¡Escucha "$playlistTitle" en Vibra App!
            
            $webUrl
            
            También puedes usar: $deepLink
        """.trimIndent())
            type = "text/plain"
        }

        context.startActivity(Intent.createChooser(shareIntent, "Compartir playlist"))
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

            // Simular tiempo de carga o esperar a que termine de cargar los datos
            delay(500)  // Un pequeño retraso para simular la carga de datos
            isLoading = false
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

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vibra://playlist/3"))
                    context.startActivity(intent)
                }
            ) {
                Text("Abrir Vibra App")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Box contenedor para el círculo de carga o las opciones
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Color(0xFF00a0d7),
                    trackColor = Color(0xFF303030)
                )
            } else {
                // Opciones de la playlist centradas - mostrar solo cuando isLoading es false
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(15.dp))
                    // Si showShareOptions es false, mostrar la opción "Compartir"
                    if (!showShareOptions) {
                        SongOptionItem("Compartir", onClick = {
                            showShareOptions = true
                        })
                    } else {
                        // Si showShareOptions es true, mostrar las opciones de compartir
                        SongOptionItem("Copiar enlace", onClick = { copyToClipboard() })
                        Spacer(modifier = Modifier.height(8.dp))
                        SongOptionItem(
                            text = "Compartir",
                            onClick = {
                                if (!playlistId.isNullOrEmpty() && !playlistTitle.isNullOrEmpty()) {
                                    sharePlaylist(playlistId, playlistTitle, context)
                                }
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SongOptionItem("Cancelar", onClick = { showShareOptions = false })
                    }
                    if (!showShareOptions) {
                        if(playlistMeGusta != "Vibra_likedSong" && !soyPropietario) {
                            Spacer(modifier = Modifier.height(8.dp))
                            SongOptionItem("Valorar Playlist", onClick = {
                                onRateClick()
                                onDismiss()
                            })
                        }
                    if (!soyPropietario){
                        Spacer(modifier = Modifier.height(8.dp))
                        SongOptionItem(
                            text = if (isLikedPlaylist) "Eliminar de la biblioteca" else "Añadir a la biblioteca",
                            textColor = if (isLikedPlaylist) Color(0xFFFF6B6B) else Color.White,
                            onClick = {
                                coroutineScope.launch {
                                    if (playlistId != null) {
                                        try {
                                            // Invertir el estado actual
                                            val newLikedState = !isLikedPlaylist

                                            val response = likeUnlikePlaylist(
                                                playlistId,
                                                userId,
                                                newLikedState
                                            )

                                            // Actualizar el estado en el componente padre
                                            onLikeUpdate(newLikedState)

                                            // Mostrar feedback al usuario
                                            Toast.makeText(
                                                context,
                                                if (newLikedState) "Añadido a tu biblioteca" else "Eliminado de tu biblioteca",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            // Cerrar el bottom sheet
                                            onDismiss()
                                        } catch (e: Exception) {
                                            Log.e("BottomSheetContent", "Error: ${e.message}")
                                            Toast.makeText(
                                                context,
                                                "Error al actualizar la biblioteca",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        )
                    }

                    if(playlistMeGusta != "Vibra_likedSong" && soyPropietario) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Log.d("PlaylistPrivacy", "Estado actual: ${playlistInfo?.esPublica}")
                        SongOptionItem(
                            // Cambiamos la condición aquí para mostrar el texto correcto
                            text = if (playlistInfo?.esPublica == "public") "Convertir a privada" else "Convertir a pública",
                            onClick = {
                                coroutineScope.launch {
                                    if (playlistId != null && playlistInfo != null) {
                                        try {
                                            // Invertir el estado actual
                                            // Llamar a la API para cambiar el estado
                                            val response = togglePlaylistType(playlistId, playlistInfo)

                                            if (response != null) {
                                                // Actualizar el estado de la playlist localmente
                                                playlistInfo.esPublica = response

                                                Log.d("PlaylistPrivacy", "Nuevo estado: ${playlistInfo.esPublica}")

                                                // Mostrar mensaje de confirmación
                                                Toast.makeText(
                                                    context,
                                                    if (response == "public") "La playlist ahora es pública" else "La playlist ahora es privada",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                onPlaylistUpdated(playlistInfo)
                                            }
                                            onDismiss()
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "Error al cambiar la privacidad de la playlist",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SongOptionItem("Editar colaboradores", onClick = dismiss)
                        Spacer(modifier = Modifier.height(8.dp))
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
                    }
                    Spacer(modifier = Modifier.height(38.dp))
                }
            }
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

@Composable
fun StarRatingDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedRating by remember { mutableStateOf(0) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Valorar playlist", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row {
                        (1..5).forEach { rating ->
                            IconButton(
                                onClick = { selectedRating = rating },
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Icon(
                                    imageVector = if (rating <= selectedRating) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = "$rating estrellas",
                                    tint = if (rating <= selectedRating) VibraBlue else VibraLightGrey,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm(selectedRating)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibraBlue),
                    modifier = Modifier
                        .padding(end = 12.dp)
                ) {
                    Text("Confirmar", color = VibraBlack)
                }
            },
            dismissButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = VibraLightGrey),
                    modifier = Modifier
                    .padding(end = 20.dp)
                ) {
                    Text("Cancelar", color = VibraBlack)
                }
            }
        )
    }
}
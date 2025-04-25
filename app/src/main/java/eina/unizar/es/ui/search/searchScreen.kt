package eina.unizar.es.ui.search

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import coil.compose.AsyncImage
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.ui.artist.SongOptionsBottomSheetContent
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.data.model.network.ApiClient.getSongDetails
import eina.unizar.es.data.model.network.ApiClient.likeUnlikeSong
import eina.unizar.es.ui.artist.Artist
import eina.unizar.es.ui.library.LibraryItem
import eina.unizar.es.ui.player.CurrentSong
import eina.unizar.es.ui.player.MusicPlayerViewModel
import eina.unizar.es.ui.playlist.Playlist
import eina.unizar.es.ui.song.Song
import eina.unizar.es.ui.user.UserProfileMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

// Clase para manejar el almacenamiento persistente del historial de búsqueda
class SearchHistoryManager(private val context: Context) {
    private val MAX_HISTORY_ITEMS = 10
    private val HISTORY_KEY_PREFIX = "search_history_"

    // Método para obtener las SharedPreferences específicas del usuario
    private fun getUserPreferences(userId: String): SharedPreferences {
        return context.getSharedPreferences("search_history_prefs_$userId", Context.MODE_PRIVATE)
    }

    // Método para obtener la clave específica para el usuario
    private fun getHistoryKey(userId: String): String {
        return HISTORY_KEY_PREFIX + userId
    }

    // Obtener el historial actual para un usuario específico
    fun getSearchHistory(userId: String): List<String> {
        val prefs = getUserPreferences(userId)
        val historyJson = prefs.getString(getHistoryKey(userId), null) ?: return emptyList()
        return historyJson.split(",").filter { it.isNotEmpty() }
    }

    // Añadir una nueva búsqueda al historial de un usuario específico
    fun addToHistory(query: String, userId: String) {
        if (query.isBlank()) return

        val prefs = getUserPreferences(userId)
        val currentHistory = getSearchHistory(userId).toMutableList()

        // Eliminar la consulta si ya existe (para moverla al principio)
        currentHistory.remove(query)

        // Añadir la nueva consulta al principio
        currentHistory.add(0, query)

        // Limitar el tamaño del historial
        val limitedHistory = currentHistory.take(MAX_HISTORY_ITEMS)

        // Guardar el historial actualizado
        prefs.edit().putString(getHistoryKey(userId), limitedHistory.joinToString(",")).apply()
    }

    // Eliminar una búsqueda del historial de un usuario específico
    fun removeFromHistory(query: String, userId: String) {
        val prefs = getUserPreferences(userId)
        val currentHistory = getSearchHistory(userId).toMutableList()
        currentHistory.remove(query)
        prefs.edit().putString(getHistoryKey(userId), currentHistory.joinToString(",")).apply()
    }

    // Limpiar todo el historial de un usuario específico
    fun clearHistory(userId: String) {
        val prefs = getUserPreferences(userId)
        prefs.edit().remove(getHistoryKey(userId)).apply()
    }
}

@Composable
fun SearchHistoryView(
    visible: Boolean,
    onHistoryItemClick: (String) -> Unit,
    userId: String
) {
    val context = LocalContext.current
    val searchHistoryManager = remember { SearchHistoryManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val searchHistory = remember { mutableStateOf(searchHistoryManager.getSearchHistory(userId)) }

    // Actualizar la lista de historial cuando cambie
    LaunchedEffect(visible, userId) {
        if (visible) {
            searchHistory.value = searchHistoryManager.getSearchHistory(userId)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        if (searchHistory.value.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Búsquedas recientes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )

                        TextButton(onClick = {
                            coroutineScope.launch {
                                searchHistoryManager.clearHistory(userId)
                                searchHistory.value = emptyList()
                            }
                        }) {
                            Text("Borrar todo")
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(searchHistory.value) { historyItem ->
                            HistoryItem(
                                query = historyItem,
                                onClick = { onHistoryItemClick(historyItem) },
                                onDelete = {
                                    coroutineScope.launch {
                                        searchHistoryManager.removeFromHistory(historyItem, userId)
                                        searchHistory.value = searchHistoryManager.getSearchHistory(userId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBarWithHistory(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    isFocused: Boolean,
    interactionSource: MutableInteractionSource,
    showHistory: Boolean,
    onHistoryItemClick: (String) -> Unit,
    userId: String
) {
    val searchBarUnfocusedColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    val searchBarFocusedColor = MaterialTheme.colorScheme.surface
    val currentContainerColor = if (isFocused) searchBarFocusedColor else searchBarUnfocusedColor
    val textColor = MaterialTheme.colorScheme.onSurface

    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .zIndex(1f),
            color = currentContainerColor,
            tonalElevation = if (isFocused) 4.dp else 0.dp,
            shadowElevation = if (isFocused) 4.dp else 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(12.dp))

                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 16.sp
                    ),
                    maxLines = 1,
                    singleLine = true,
                    interactionSource = interactionSource,
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Buscar ...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = onClearClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpiar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Historial de búsqueda emergente
        SearchHistoryView(
            visible = showHistory && isFocused && searchQuery.isEmpty(),
            onHistoryItemClick = onHistoryItemClick,
            userId = userId
        )
    }
}

@Composable
fun HistoryItem(
    query: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = query,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Eliminar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@SuppressLint("UnrememberedGetBackStackEntry", "CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController, playerViewModel: MusicPlayerViewModel) {
    val backgroundColor = MaterialTheme.colorScheme.background

    var searchQuery by remember { mutableStateOf("") }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current

    var allSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var allPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var allArtists by remember { mutableStateOf<List<Artist>>(emptyList()) }

    var filteredSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var filteredPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var filteredArtists by remember { mutableStateOf<List<Artist>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()
    val likedSongs by playerViewModel.likedSongs.collectAsState()
    val context = LocalContext.current

    val userId = playerViewModel.getUserId()
    val searchHistoryManager = remember { SearchHistoryManager(context) }
    var showHistory by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            allSongs = fetchAllSongs()
            allPlaylists = fetchAllPlaylists()
            allArtists = fetchAllArtists()

            if (playerViewModel.getUserId().isEmpty()) {
                playerViewModel.setUserId(context)
            }
            playerViewModel.initializeLikedSongs(playerViewModel.getUserId())
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            filteredSongs = allSongs.filter { song ->
                if (song.name == "Anuncio Vibra") {
                    return@filter false
                }
                song.name.contains(searchQuery, ignoreCase = true)
            }
            filteredPlaylists = allPlaylists.filter { playlist ->
                playlist.title.contains(searchQuery, ignoreCase = true)
            }
            filteredArtists = allArtists.filter { artist ->
                artist.name.contains(searchQuery, ignoreCase = true)
            }
        } else {
            filteredSongs = emptyList()
            filteredPlaylists = emptyList()
            filteredArtists = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row (verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(15.dp))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 4.dp)) {
                        UserProfileMenu(navController, playerViewModel)
                    }
                }
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(backgroundColor)
        ) {
            SearchBarWithHistory(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onClearClick = {
                    searchQuery = ""
                    focusManager.clearFocus()
                },
                isFocused = isFocused,
                interactionSource = interactionSource,
                showHistory = showHistory,
                onHistoryItemClick = { historyItem ->
                    searchQuery = historyItem
                    // Guardar en el historial
                    coroutineScope.launch {
                        searchHistoryManager.addToHistory(historyItem, userId)
                    }
                },
                userId = userId
            )

            // Cuando se hace una búsqueda real, guardarla en el historial
            LaunchedEffect(searchQuery) {
                if (searchQuery.isNotEmpty()) {
                    // Guardar la búsqueda en el historial solo cuando tiene al menos 3 caracteres
                    if (searchQuery.length >= 3) {
                        searchHistoryManager.addToHistory(searchQuery, userId)
                    }
                    showHistory = false
                } else {
                    showHistory = true
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (searchQuery.isEmpty()) {
                    // Mostrar contenido de inicio cuando no hay búsqueda
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Busca artistas, canciones o playlists",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Mostrar resultados de búsqueda
                    if (filteredSongs.isNotEmpty()) {
                        item {
                            SearchResultSection(title = "Canciones") {}
                        }
                        items(filteredSongs) { song ->
                            var songIsLiked = song.id.toString() in likedSongs
                            var showSongOptionsBottomSheet by remember { mutableStateOf(false) }
                            var songArtists by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }

                            LaunchedEffect(song.id) {
                                val songDetails = getSongDetails(song.id.toString())
                                songDetails?.let { details ->
                                    @Suppress("UNCHECKED_CAST")
                                    songArtists = details["artists"] as? List<Map<String, String>> ?: emptyList()
                                }
                            }

                            SongItem(
                                song = song,
                                showHeartIcon = true,
                                showMoreVertIcon = true,
                                isLiked = songIsLiked,
                                onLikeToggle = {
                                    coroutineScope.launch {
                                        try {
                                            likeUnlikeSong(song.id.toString(), playerViewModel.getUserId(), !songIsLiked)
                                            songIsLiked = !songIsLiked
                                            playerViewModel.loadLikedStatus(song.id.toString())
                                        } catch (e: Exception) {
                                            Log.e("SearchScreen", "Error al cambiar like: ${e.message}")
                                        }
                                    }
                                },
                                onMoreVertClick = {
                                    showSongOptionsBottomSheet = true
                                },
                                viewModel = playerViewModel,
                                isPlaylist = false
                            )

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
                                        songTitle = song.name,
                                        artistName = artistName,
                                        onClick = {
                                            // Aquí puedes manejar la acción de añadir a la cola
                                            // Por ejemplo, puedes usar el ViewModel para añadir la canción a la cola
                                            playerViewModel.addToQueue(song.id.toString())
                                            Toast.makeText(context, "Añadido a la cola", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (filteredPlaylists.isNotEmpty()) {
                        item {
                            SearchResultSection(title = "Playlists") {}
                        }
                        items(filteredPlaylists) { playlist ->
                            LibraryItem(playlist = playlist, navController = navController)
                        }
                    }

                    if (filteredArtists.isNotEmpty()) {
                        item {
                            SearchResultSection(title = "Artistas") {}
                        }
                        items(filteredArtists) { artist ->
                            ArtistItem(navController = navController, artist = artist)
                        }
                    }

                    // Si no hay resultados
                    if (filteredSongs.isEmpty() && filteredPlaylists.isEmpty() && filteredArtists.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No se encontraron resultados para \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

    suspend fun fetchAllSongs(): List<Song> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.get("songs")
            val jsonArray = JSONArray(response)
            val songs = mutableListOf<Song>()

            for (i in 0 until jsonArray.length()) {
                val songObject = jsonArray.getJSONObject(i)
                songs.add(
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
            songs
        } catch (e: Exception) {
            Log.e("SearchScreen", "Error fetching songs: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchAllPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.get("playlists")
            val jsonArray = JSONArray(response)
            val playlists = mutableListOf<Playlist>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val playlistType = jsonObject.getString("type")
                if (playlistType == "public") {
                    playlists.add(
                        Playlist(
                            id = jsonObject.getString("id"),
                            title = jsonObject.getString("name"),
                            imageUrl = jsonObject.getString("front_page"),
                            idAutor = jsonObject.getString("user_id"),
                            idArtista = jsonObject.getString("artist_id"),
                            description = jsonObject.getString("description"),
                            esPublica = jsonObject.getString("type"),
                            esAlbum = jsonObject.getString("typeP"),
                        )
                    )
                }
            }
            playlists
        } catch (e: Exception) {
            Log.e("SearchScreen", "Error fetching songs: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchAllArtists(): List<Artist> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.get("artist/artists")
            val jsonArray = JSONArray(response)
            val artists = mutableListOf<Artist>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                artists.add(
                        Artist(
                            id = jsonObject.getString("id"),
                            name = jsonObject.getString("name"),
                            biography = "Prueba",//jsonObject.getString("bio")//
                            photo = jsonObject.getString("photo"),
                        )
                )
            }
            artists
        } catch (e: Exception) {
            Log.e("SearchScreen", "Error fetching songs: ${e.message}", e)
            emptyList()
        }
    }

fun convertSongsToCurrentSongs(songs: List<Song>, albumArtResId: Int): List<CurrentSong> {
    return songs.map { song ->
        CurrentSong(
            id = song.id.toString(),  // Convertir el id de Int a String
            title = song.name,
            artist = "Desconocido",  // Puedes modificar esto según sea necesario
            photo = song.photo_video,
            albumArt = albumArtResId,  // Aquí pasas el valor de albumArt como parámetro
            url = "http://164.90.160.181:5001/${song.url_mp3.removePrefix("/")}",
            lyrics = song.letra,
            isPlaying = false,  // Puedes poner el valor por defecto según lo que necesites
            progress = 0f  // Valor inicial para el progreso
        )
    }
}

@Composable
fun SearchResultSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                .fillMaxWidth()
        )

        Divider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        content()
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun SongItem(
    song: Song,
    showHeartIcon: Boolean = false,
    showMoreVertIcon: Boolean = false,
    isLiked: Boolean = false,
    onLikeToggle: (() -> Unit) = {},
    onMoreVertClick: (() -> Unit) = {},
    viewModel: MusicPlayerViewModel,
    isPlaylist: Boolean = false,
    isSencillo: Boolean = false,
    playlistSongs: List<Song> = emptyList(),
    idPlaylist: String = ""
) {
    val context = LocalContext.current
    val currentSong by viewModel.currentSong.collectAsState()
    val isCurrentlyPlaying = song.id.toString() == currentSong?.id

    // Colores y estilos actualizados
    val cardBackgroundColor = if (isCurrentlyPlaying)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)

    val titleColor = if (isCurrentlyPlaying)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurface

    val artistColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                if (isSencillo) {
                    viewModel.loadSongsFromApi(songId = song.id.toString(), context = context, albumArtResId = R.drawable.defaultx)
                } else if (isPlaylist) {
                    viewModel.loadSongsFromPlaylist(
                        playlistSongs = convertSongsToCurrentSongs(playlistSongs, R.drawable.defaultplaylist),
                        songId = song.id.toString(),
                        context,
                        idPlaylist = idPlaylist
                    )
                } else {
                    viewModel.loadSongsFromApi(songId = song.id.toString(), context = context, albumArtResId = R.drawable.defaultx)
                }
            },
        color = cardBackgroundColor,
        tonalElevation = if (isCurrentlyPlaying) 4.dp else 1.dp,
        shadowElevation = if (isCurrentlyPlaying) 4.dp else 0.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .height(44.dp)
        ) {
            // Imagen de la canción con sombra suave
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = getImageUrl(song.photo_video, "defaultsong.jpg"),
                    placeholder = painterResource(R.drawable.defaultsong),
                    error = painterResource(R.drawable.defaultsong),
                    contentDescription = "Foto Canción",
                    modifier = Modifier.fillMaxSize()
                )

                // Indicador de reproducción actual
                if (isCurrentlyPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "Reproduciendo",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Texto de la canción y artista
            var idsArtistas by remember { mutableStateOf(listOf<Int>()) }
            var nombresArtistas by remember { mutableStateOf(listOf<String>()) }

            LaunchedEffect(song.id) {
                launch {
                    val response = ApiClient.get("player/details/${song.id}")
                    response?.let {
                        val jsonObject = JSONObject(it)
                        val artistsArray = jsonObject.getJSONArray("artists")

                        val ids = mutableListOf<Int>()
                        val nombres = mutableListOf<String>()

                        for (i in 0 until artistsArray.length()) {
                            val artistObject = artistsArray.getJSONObject(i)
                            ids.add(artistObject.getInt("id"))
                            nombres.add(artistObject.getString("name"))
                        }

                        idsArtistas = ids
                        nombresArtistas = nombres
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                Text(
                    text = song.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = nombresArtistas.joinToString(", "),
                    fontSize = 12.sp,
                    color = artistColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Iconos alineados a la derecha
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (showHeartIcon) {
                    IconButton(
                        onClick = onLikeToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Me gusta",
                            tint = if (isLiked) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (showMoreVertIcon) {
                    IconButton(
                        onClick = onMoreVertClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistItem(
    navController: NavController,
    artist: Artist,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                navController.navigate("artist/${artist.id}")
            },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen del artista
            val playlistImage = getImageUrl(artist.photo, "/default-artist.jpg")
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.2f), CircleShape)
            ) {
                AsyncImage(
                    model = playlistImage,
                    placeholder = painterResource(R.drawable.defaultartist),
                    error = painterResource(R.drawable.defaultartist),
                    contentDescription = "Foto del artista",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            // Nombre del artista
            Text(
                text = artist.name,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Flecha indicativa
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Ver artista",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
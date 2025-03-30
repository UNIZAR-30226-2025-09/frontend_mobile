package eina.unizar.es.ui.search

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.ui.artist.Artist
import eina.unizar.es.ui.library.LibraryItem
import eina.unizar.es.ui.navbar.BottomNavigationBar
import eina.unizar.es.ui.player.FloatingMusicPlayer
import eina.unizar.es.ui.player.MusicPlayerViewModel
import eina.unizar.es.ui.playlist.Playlist
import eina.unizar.es.ui.song.Song
import eina.unizar.es.ui.user.UserProfileMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

@SuppressLint("UnrememberedGetBackStackEntry")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController, playerViewModel: MusicPlayerViewModel) {

    val backgroundColor = MaterialTheme.colorScheme.background
    val searchBarUnfocusedColor = MaterialTheme.colorScheme.onBackground
    val searchTextUnfocusedColor = MaterialTheme.colorScheme.background
    val searchBarFocusedColor = MaterialTheme.colorScheme.background
    val searchTextFocusedColor = MaterialTheme.colorScheme.onBackground

    val textColor = MaterialTheme.colorScheme.onSurface
    val buttonColor = MaterialTheme.colorScheme.primary
    val cardBackgroundColor = MaterialTheme.colorScheme.surface

    var searchQuery by remember { mutableStateOf("") }


    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val currentContainerColor = if (isFocused) searchBarFocusedColor else searchBarUnfocusedColor
    val currentTextColor = if (isFocused) searchTextFocusedColor else searchTextUnfocusedColor

    var allSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var allPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var allArtists by remember { mutableStateOf<List<Artist>>(emptyList()) }


    var filteredSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var filteredPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var filteredArtists by remember { mutableStateOf<List<Artist>>(emptyList()) }

    var searchResults by remember { mutableStateOf<List<Any>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            allSongs = fetchAllSongs()
            allPlaylists = fetchAllPlaylists()
            allArtists = fetchAllArtists()
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            filteredSongs = allSongs.filter { song ->
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserProfileMenu(navController)
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
//        bottomBar = {
//            Column {
//                FloatingMusicPlayer(navController, playerViewModel)
//                BottomNavigationBar(navController)
//            }
//        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(backgroundColor)
                .padding(16.dp)
        ) {
            Text(
                text = "Buscar",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar...", color = currentTextColor) },
                textStyle = TextStyle(color = currentTextColor),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = currentContainerColor,
                    focusedBorderColor = buttonColor,
                    unfocusedBorderColor = currentTextColor,
                    cursorColor = currentTextColor,
                    focusedLabelColor = currentTextColor,
                    unfocusedLabelColor = currentTextColor
                ),
                interactionSource = interactionSource,



                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            focusManager.clearFocus() // Esto quita el foco
                        }) {
                            Icon(Icons.Default.Clear, "Limpiar")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (filteredSongs.isNotEmpty()) {
                    item {
                        Text(
                            "Canciones",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }
                    items(filteredSongs) { song ->
                        SongItem(song = song, viewModel = playerViewModel)
                    }
                }

                if (filteredPlaylists.isNotEmpty()) {
                    item {
                        Text(
                            "Playlists",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }
                    items(filteredPlaylists) { playlist ->
                        LibraryItem(playlist = playlist, navController = navController)
                    }
                }

                if (filteredArtists.isNotEmpty()) {
                    item {
                        Text(
                            "Artistas",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }
                    items(filteredArtists) { artist ->
                        ArtistItem(navController = navController, artist = artist)

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
                            id = jsonObject.getInt("id"),
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


@Composable
fun SongItem(
    song: Song,
    showHeartIcon: Boolean = false,
    showMoreVertIcon: Boolean = false,
    isLiked: Boolean = false,
    onLikeToggle: (() -> Unit) = {},
    onMoreVertClick: (() -> Unit) = {},
    viewModel: MusicPlayerViewModel,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .padding(start = 16.dp, end = 16.dp)
            .clickable {
                viewModel.loadSongsFromApi(songId = song.id.toString(), context = context, albumArtResId = R.drawable.kanyeperfil)
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Imagen de la canción
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.DarkGray)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Texto de la canción y artista
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp) // Espacio adicional a la derecha del texto
            ) {
                Text(
                    text = song.name,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = "Anuel AA",
                    fontSize = 12.sp,
                    color = Color.White
                )
            }

            // Iconos alineados a la izquierda del espacio restante
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.wrapContentWidth()
            ) {
                if (showHeartIcon) {
                    IconButton(
                        onClick = onLikeToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Me gusta",
                            tint = if (isLiked) Color.Red else Color.Gray
                        )
                    }
                }

                if (showMoreVertIcon) {
                    IconButton(
                        onClick = onMoreVertClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            tint = Color.White
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Navegar a la pantalla de detalle del artista
                //navController.navigate("artist/${artist.id}")
                navController.navigate("artist")
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Imagen del artista
        val playlistImage = getImageUrl(artist.photo, "/default-playlist.jpg")
        AsyncImage(
            model = playlistImage,
            contentDescription = "Foto del artista",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )

        // Nombre del artista
        Text(
            text = artist.name,
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold
            )
        )
    }
}
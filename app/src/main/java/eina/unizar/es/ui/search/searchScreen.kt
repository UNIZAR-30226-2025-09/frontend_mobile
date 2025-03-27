package eina.unizar.es.ui.search

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient
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
fun SearchScreen(navController: NavController) {
    val playerViewModel: MusicPlayerViewModel = viewModel()

    val backgroundColor = MaterialTheme.colorScheme.background
    val searchBarUnfocusedColor = MaterialTheme.colorScheme.onBackground
    val searchTextUnfocusedColor = MaterialTheme.colorScheme.background
    val searchBarFocusedColor = MaterialTheme.colorScheme.background
    val searchTextFocusedColor = MaterialTheme.colorScheme.onBackground

    val textColor = MaterialTheme.colorScheme.onSurface
    val buttonColor = MaterialTheme.colorScheme.primary
    val cardBackgroundColor = MaterialTheme.colorScheme.surface

    var searchQuery by remember { mutableStateOf("") }
    val searchHistory = remember {
        mutableStateListOf("Canción 1", "Canción 2", "Playlist 1", "Canción 3", "Playlist 2")
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val currentContainerColor = if (isFocused) searchBarFocusedColor else searchBarUnfocusedColor
    val currentTextColor = if (isFocused) searchTextFocusedColor else searchTextUnfocusedColor

    var allSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var allPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<Any>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            allSongs = fetchAllSongs()
            allPlaylists = fetchAllPlaylists()
        }
    }

    LaunchedEffect(searchQuery) {
        searchResults = if (searchQuery.isNotEmpty()) {
            val filteredSongs = allSongs.filter { song ->
                song.name.contains(searchQuery, ignoreCase = true)
            }
            val filteredPlaylists = allPlaylists.filter { playlist ->
                playlist.title.contains(searchQuery, ignoreCase = true)
            }
            filteredSongs + filteredPlaylists
        } else {
            emptyList()
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
        bottomBar = {
            Column {
                FloatingMusicPlayer(viewModel = playerViewModel, navController = navController)
                BottomNavigationBar(navController)
            }
        },
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
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
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
                items(searchResults) { result ->
                    when (result) {
                        is Song -> SongItem(song = result)
                        is Playlist -> LibraryItem(playlist = result, navController = navController)
                    }
                }
            }

            if (searchHistory.isNotEmpty()) {
                Button(
                    onClick = { searchHistory.clear() },
                    modifier = Modifier
                        .width(200.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                ) {
                    Text("Limpiar historial", color = Color.White)
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
            val songs = mutableListOf<Playlist>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                songs.add(
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
            songs
        } catch (e: Exception) {
            Log.e("SearchScreen", "Error fetching songs: ${e.message}", e)
            emptyList()
        }
    }


@Composable
fun SongItem(song: Song) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp) // Reducimos el padding vertical
        .padding(start = 16.dp, end = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Reducimos la elevación
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp) // Reducimos el padding interno

        ) {
            Box(
                modifier = Modifier
                    .size(40.dp) // Reducimos el tamaño del Box
                    .background(Color.DarkGray)
            )
            Spacer(modifier = Modifier.width(8.dp)) // Reducimos el espacio
            Column {
                Text(
                    text = song.name,
                    fontSize = 16.sp, // Reducimos el tamaño de la fuente
                    color = Color.White
                )
                Text(
                    text = "Anuel AA",
                    fontSize = 12.sp, // Reducimos el tamaño de la fuente
                    color = Color.White
                )
            }
        }
    }
}
package eina.unizar.es.ui.artist

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.data.model.network.ApiClient.getSongDetails
import eina.unizar.es.data.model.network.ApiClient.likeUnlikeSong
import eina.unizar.es.ui.main.Rubik
import eina.unizar.es.ui.navbar.BottomNavigationBar
import eina.unizar.es.ui.player.FloatingMusicPlayer
import eina.unizar.es.ui.player.MusicPlayerViewModel
import eina.unizar.es.ui.playlist.Playlist
import eina.unizar.es.ui.search.ADSongs
import eina.unizar.es.ui.search.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.nio.file.Files.delete

@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun ArtistScreen(navController: NavController, playerViewModel: MusicPlayerViewModel, artistId: Int?) {

    // Colores básicos
    val backgroundColor = Color(0xFF000000) // Negro
    val textColor = Color(0xFFFFFFFF) // Blanco

    // Estado del LazyColumn para detectar scroll y aplicar efecto en el header

    val lazyListState = rememberLazyListState()
    val imageSize = 150.dp
    val maxOffset = with(LocalDensity.current) { imageSize.toPx() }
    val scrollOffset = lazyListState.firstVisibleItemScrollOffset.toFloat()
    val collapseFraction = (scrollOffset / maxOffset).coerceIn(0f, 1f)

    // Ajustamos solo la opacidad (sin escala) con Modifier.alpha

    val imageAlpha = 1f - collapseFraction


    // Alpha para el título en el TopAppBar: aparece gradualmente conforme se hace scroll
    val topTitleAlpha = if (lazyListState.firstVisibleItemIndex > 0) {
        1f
    } else {
        ((scrollOffset) / (maxOffset / 2)).coerceIn(0f, 1f)
    }


    var songsList by remember { mutableStateOf<List<Song>>(emptyList()) }
    var artistInfo by remember { mutableStateOf<Artist?>(null) }
    var albums by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var sencillos by remember { mutableStateOf<List<Song>>(emptyList()) }

    // Observa la lista de canciones con like desde el ViewModel
    val likedSongs by playerViewModel.likedSongs.collectAsState()

    // Para poder realizar el post del like/unlike
    val coroutineScope = rememberCoroutineScope()

    // Para obtener el contexto de la actividad
    val context = LocalContext.current // Contexto de la actividad

    LaunchedEffect(Unit) {

        coroutineScope {
            // Inicializar el ViewModel con el ID de usuario
            if (playerViewModel.getUserId().isEmpty()) {
                playerViewModel.setUserId(context)
            }

            // Cargar explícitamente el estado de "me gusta"
            // ya que si el ViewModel no se inicializa, no se saben las likedSongs
            playerViewModel.initializeLikedSongs(playerViewModel.getUserId())
        }

        val responseS = get("artist/${artistId}")
        responseS?.let { jsonResponse ->
            try {
                val jsonObject = JSONObject(jsonResponse)

                // Parsear artista
                val artistJson = jsonObject.optJSONObject("artist")
                artistJson?.let {
                    artistInfo = Artist(
                        id = it.getString("id"),
                        name = it.optString("name", "Nombre no disponible"),
                        biography = it.optString("bio", ""),
                        photo = it.optString("photo", "")
                    )
                    Log.d("PARSING", "Artista parseado: ${artistInfo}")
                }

                // Parsear canciones
                val songsArray = jsonObject.optJSONArray("songs")
                val fetchedSongs = mutableListOf<Song>()

                songsArray?.let {
                    for (i in 0 until it.length()) {
                        val songJson = it.getJSONObject(i)
                        val song = Song(
                            id = songJson.getInt("id"),
                            name = songJson.optString("name", "Sin título"),
                            duration = songJson.optInt("duration", 0),
                            photo_video = songJson.optString("photo_video", ""),
                            //type = songJson.optString("type"),
                            url_mp3 = songJson.optString("url_mp3", ""),
                            letra = ""
                        )
                        fetchedSongs.add(song)
                        Log.d("PARSING", "Canción ${i + 1}: $song")
                    }
                    songsList = fetchedSongs
                }

                // Parsear álbumes
                val albumsArray = jsonObject.optJSONArray("albums")
                val fetchedAlbums = mutableListOf<Playlist>()

                albumsArray?.let {
                    for (i in 0 until it.length()) {
                        val songJson = it.getJSONObject(i)
                        val album = Playlist(
                            id = songJson.getString("id"),
                            title = songJson.optString("name", "Sin título"),
                            imageUrl = songJson.optString("front_page", ""),
                            idArtista = artistId.toString(),
                            idAutor = "",
                            description = "",
                            esPublica = "public",
                            esAlbum = "album"
                        )
                        fetchedAlbums.add(album)
                        Log.d("PARSING", "Canción ${i + 1}: $album")
                    }
                    albums = fetchedAlbums
                    Log.d("PARSING", "Número de álbumes: ${it.length()}")
                }

                // Parsear canciones
                val singlesArray = jsonObject.optJSONArray("songs")
                val fetchedSingles = mutableListOf<Song>()

                singlesArray?.let {
                    for (i in 0 until it.length()) {
                        val songJson = it.getJSONObject(i)
                        val song = Song(
                            id = songJson.getInt("id"),
                            name = songJson.optString("name", "Sin título"),
                            duration = songJson.optInt("duration", 0),
                            photo_video = songJson.optString("photo_video", ""),
                            //type = songJson.optString("type"),
                            url_mp3 = songJson.optString("url_mp3", ""),
                            letra = ""
                            //songId = songJson.optInt("artists.song_artist.song_id", 0),
                            //artistId = songJson.optInt("artists.song_artist.artist_id", 0),
                            //likes = songJson.optInt("likes", 0)
                        )
                        fetchedSingles.add(song)
                        Log.d("PARSING", "Canción ${i + 1}: $song")
                    }
                    sencillos = fetchedSingles
                }
            } catch (e: JSONException) {
                Log.e("PARSE_ERROR", "Error al parsear JSON: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    artistInfo?.let {
                        Text(
                            text = artistInfo!!.name,
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
//        bottomBar = {
//            Column {
//                val isPlaying = remember { mutableStateOf(false) }
//                FloatingMusicPlayer(navController, playerViewModel)
//                BottomNavigationBar(navController)
//            }
//        },
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
                    // Cargar la imagen desde la URL
                    val urlAntes = artistInfo?.photo
                    val playlistImage = getImageUrl(urlAntes, "/default-playlist.jpg")
                    AsyncImage(
                        model = playlistImage,
                        contentDescription = "Portada de la playlist",
                        modifier = Modifier
                            .size(250.dp)
                            .alpha(imageAlpha)
                            .clip(RoundedCornerShape(8.dp)) // Opcional: añade esquinas redondeadas
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    artistInfo?.let {
                        Text(
                            text = artistInfo!!.name,
                            color = textColor,
                            style = TextStyle(fontSize = 20.sp)
                        )

                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Biografía",
                                color = Color(0xFF79E2FF),
                                fontFamily = Rubik,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = artistInfo!!.biography.ifEmpty { "No hay biografía disponible" },
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(26.dp))
                Text(
                    text = "Populares",
                    fontSize = 24.sp, // Cambia el tamaño aquí
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                            .padding(start = 20.dp)
                )
            }


            // Lista de canciones populares
            items(songsList) { song ->
                var songIsLiked = song.id.toString() in likedSongs
                var showSongOptionsBottomSheet by remember { mutableStateOf(false) } // Estado para mostrar el BottomSheet de opciones de la canción
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
                        // Mostrar opciones de la canción
                        showSongOptionsBottomSheet = true
                    },
                    viewModel = playerViewModel,
                    isPlaylist = false,
                )


                // BottomSheet para opciones de la canción (dentro del items)
                if (showSongOptionsBottomSheet) {
                    val artistName = if (songArtists.isNotEmpty()) {
                        songArtists.joinToString(", ") { it["name"] ?: "" }
                    } else {
                        artistInfo?.name ?: "Artista desconocido"
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
            item { Spacer(modifier = Modifier.height(12.dp)) }

            // Canciones populares
            if (songsList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tiene canciones populares",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Albumes",
                    fontSize = 24.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .padding(start = 20.dp)
                )
                Column(
                    modifier = Modifier.padding(start = 20.dp) // Aplicamos el margen solo al principio del LazyRow
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(albums) { album ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Card(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clickable {
                                            navController.navigate("playlist/${album.id}")
                                        }, // PASAMOS EL ID
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Cargar la imagen desde la URL
                                        val urlAntes = album.imageUrl
                                        val playlistImage =
                                            getImageUrl(urlAntes, "/default-playlist.jpg")
                                        AsyncImage(
                                            model = playlistImage,
                                            contentDescription = "Portada de la playlist",
                                            modifier = Modifier
                                                //.size(imageSize)
                                                //.alpha(imageAlpha)
                                                .clip(RoundedCornerShape(8.dp)) // Opcional: añade esquinas redondeadas
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = album.title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // Álbumes
            if (albums.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tiene álbumes publicados",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Sencillos",
                    fontSize = 24.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .padding(start = 20.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(end = 8.dp)  // Padding al final para mejor scroll
                    ) {
                        items(songsList) { single ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(120.dp)  // Ancho fijo para toda la columna (imagen + texto)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clickable {
                                            Log.d("Error", "Id del sencillo: " + single.id)
                                            navController.navigate("single/${single.id}")
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = getImageUrl(single.photo_video, "/default-playlist.jpg"),
                                            contentDescription = "Portada de la playlist",
                                            contentScale = ContentScale.Crop,  // Añadir scale para recortar imagen
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }
                                }

                                //Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = single.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)  // Ocupa todo el ancho disponible
                                        .padding(horizontal = 4.dp),  // Pequeño padding lateral
                                    textAlign = TextAlign.Center,  // Texto centrado
                                    maxLines = 2,  // Máximo 2 líneas
                                    overflow = TextOverflow.Ellipsis  // Puntos suspensivos si sobrepasa
                                )
                            }
                        }
                    }
                }
            }

            // Sencillos
            if (sencillos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tiene sencillos disponibles",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}


// Desplegable para las canciones
@Composable
fun SongOptionsBottomSheetContent(
    viewModel: MusicPlayerViewModel,
    songId: String,
    songTitle: String,
    artistName: String
) {
    val textColor = Color.White
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    // Si el diálogo está visible, muéstralo
    if (showAddToPlaylistDialog) {
        ADSongs(
            viewModel = viewModel,
            songId = songId,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            songTitle,
            color = textColor,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "de $artistName",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SongOptionItem("Añadir a lista", onClick = { showAddToPlaylistDialog = true })
            Spacer(modifier = Modifier.height(8.dp))
            SongOptionItem("Añadir a la cola", onClick = { /* Acción de añadir a la cola */ })
            Spacer(modifier = Modifier.height(8.dp))
            SongOptionItem("Compartir", onClick = { /* Acción de compartir */ })
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SongOptionItem(
    text: String,
    onClick: () -> Unit,
    textColor: Color = Color.White,
    background: Color = Color.Transparent,
    roundedCornerShape: RoundedCornerShape = RoundedCornerShape(0.dp),
    textAlign: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Box(
        modifier = modifier
            .clip(roundedCornerShape)
            .background(background)
            .clickable { onClick() } // Clickable en todo el Box
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 16.sp,
            textAlign = textAlign,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}


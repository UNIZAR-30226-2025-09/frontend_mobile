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
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.ui.navbar.BottomNavigationBar
import eina.unizar.es.ui.player.FloatingMusicPlayer
import eina.unizar.es.ui.player.MusicPlayerViewModel
import eina.unizar.es.ui.playlist.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files.delete

@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun ArtistScreen(navController: NavController, playerViewModel: MusicPlayerViewModel) {

    // Colores básicos
    val backgroundColor = Color(0xFF000000) // Negro
    val textColor = Color(0xFFFFFFFF) // Blanco
    val cardBackgroundColor = Color(0xFF121212) // Negro un poco más claro
    val buttonColor = Color(0xFF0D47A1) // Azul oscuro


    val playlistAuthor = "Autor: John Doe"


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
    var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }
    //val audioUrl = "URL_DEL_ARCHIVO_DE_AUDIO" // Reemplaza con la URL de tu archivo de audio


    // Alpha para el título en el TopAppBar: aparece gradualmente conforme se hace scroll
    val topTitleAlpha = if (lazyListState.firstVisibleItemIndex > 0) {
        1f
    } else {
        ((scrollOffset) / (maxOffset / 2)).coerceIn(0f, 1f)
    }


    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var artistInfo: Artist? = null
    var albums by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var sencillos by remember { mutableStateOf<List<Playlist>>(emptyList()) }


    // Usamos un Map para manejar el estado de "me gusta" por canción usando el índice o algún identificador único
    val songLikes = remember { mutableStateOf(songs.associateWith { false }) }

    // Función para cambiar el estado de "me gusta" de una canción
    fun toggleLike(song: Song) {
        songLikes.value = songLikes.value.toMutableMap().apply {
            this[song] = !(this[song] ?: false) // Cambiar el estado de "me gusta" de esta canción
        }
    }

    // Para poder realizar el post del like/unlike
    val coroutineScope = rememberCoroutineScope()

    // Id del usuario a guardar al darle like
    var userId by remember { mutableStateOf("") }  // Estado inicial



    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }



    // Cargar playlists desde el backend
    LaunchedEffect(Unit) {
        val response = get("playlists") // Llamada a la API
        response?.let {
            val jsonArray = JSONArray(it)
            val fetchedPlaylists = mutableListOf<Playlist>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                fetchedPlaylists.add(
                    Playlist(
                        id = jsonObject.getString("id"),
                        title = jsonObject.getString("name"),
                        idAutor = jsonObject.getString("user_id"),
                        idArtista = jsonObject.getString("artist_id"),
                        description = jsonObject.getString("description"),
                        esPublica = jsonObject.getString("type"),
                        esAlbum = jsonObject.getString("typeP"),
                        imageUrl = jsonObject.getString("front_page")
                    )
                )
            }
            playlists = fetchedPlaylists
        }
    }
    LaunchedEffect(Unit) {
        val response = get("songs") // Llamada a la API
        response?.let {
            val songsArray = JSONArray(it)
            val fetchedSongs = mutableListOf<Song>()
            for (i in 0 until 5) {
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

            // Artistas
            val responseS = get("artist/artists") // Llamada a la API para obtener canciones
            responseS?.let {
                val jsonArray = JSONArray(it)

                if (jsonArray.length() > 0) { // Verifica si hay al menos un elemento en el array
                    val jsonObject =
                        jsonArray.getJSONObject(0) // Obtiene el primer objeto del array
                    artistInfo = Artist(
                        id = jsonObject.getInt("id"),
                        name = jsonObject.getString("name"),
                        biography = "Prueba", // o jsonObject.getString("bio") si prefieres la biografía real
                        photo = jsonObject.getString("photo"),
                    )
                }
            }
        }
    }

    /*************************************************************************
     * Añadir aqui un bucle que solo coja las canciones que estan relacionadas
     * con nuestra playlist
     *************************************************************************/

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
                Spacer(modifier = Modifier.height(12.dp))
            }


            // Lista de canciones: Cada banner con imagen a la izquierda y título/artista a la derecha
            items(songs) { song ->
                //val artist = songArtistMap[song] ?: "Artista Desconocido"
                var showSongOptionsBottomSheet by remember { mutableStateOf(false) } // Estado para mostrar el BottomSheet de opciones de la canción
                // Reproducir la musica
                // val context = LocalContext.current
                // var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clickable {
                                playerViewModel.loadSongsFromApi(songId = song.id.toString(), context = context, albumArtResId = R.drawable.kanyeperfil)
                            },
                        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Imagen de la canción (cuadrado)
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(Color.DarkGray) // Placeholder de la imagen
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.name,
                                    color = textColor,
                                    style = TextStyle(fontSize = 18.sp)
                                )
                                Text(
                                    text = /*song.artist*/"Artista de prueba",
                                    color = textColor,
                                    style = TextStyle(fontSize = 14.sp)
                                )
                            }
                            // Botón de "me gusta"
                            IconButton(
                                onClick = {
                                    toggleLike(song) // Cambia el estado de "me gusta" solo para esta canción
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite, // Ícono de "me gusta"
                                    contentDescription = "Me gusta",
                                    tint = if (songLikes.value[song] == true) Color.Red else Color.Gray // Color cambia dependiendo del like
                                )
                            }
                            IconButton(onClick = { showSongOptionsBottomSheet = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Opciones de la canción",
                                    tint = textColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BottomSheet para opciones de la canción (dentro del items)
                if (showSongOptionsBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showSongOptionsBottomSheet = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ) {
                        SongOptionsBottomSheetContent(
                            onDismiss = { showSongOptionsBottomSheet = false },
                            songTitle = song.name, // Pasa el título de la canción
                            artistName = /*artist*/ "Artista de prueba" // Pasa el nombre del artista
                        )
                    }
                }
            }
            item {
                Text(
                    text = "Albumes",
                    fontSize = 24.sp, // Cambia el tamaño aquí
                    textAlign = TextAlign.Start, // Alinea el texto a la izquierda
                    modifier = Modifier
                        .padding(start = 20.dp)
                )
                Column(
                    modifier = Modifier.padding(start = 20.dp) // Aplicamos el margen solo al principio del LazyRow
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(playlists) { album ->
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
                                        val urlAntes = album?.imageUrl
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
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Text(
                    text = "Sencillos",
                    fontSize = 24.sp, // Cambia el tamaño aquí
                    textAlign = TextAlign.Start, // Alinea el texto a la izquierda
                    modifier = Modifier
                        .padding(start = 20.dp)
                )
                Column(
                    modifier = Modifier.padding(start = 20.dp) // Aplicamos el margen solo al principio del LazyRow
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(playlists) { album ->
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
                                        val urlAntes = album?.imageUrl
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
        }
    }
}


// Desplegable para las canciones
@Composable
fun SongOptionsBottomSheetContent(
    onDismiss: () -> Unit,
    songTitle: String,
    artistName: String
) {
    val textColor = Color.White

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
            SongOptionItem("Añadir a lista", onDismiss)
            Spacer(modifier = Modifier.height(8.dp))
            SongOptionItem("Añadir a la biblioteca", onDismiss)
            Spacer(modifier = Modifier.height(8.dp))
            SongOptionItem("Añadir a la cola", onDismiss)
            Spacer(modifier = Modifier.height(8.dp))
            SongOptionItem("Eliminar de la lista", onDismiss)
            Spacer(modifier = Modifier.height(8.dp))
            SongOptionItem("Compartir", onDismiss)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SongOptionItem(
    text: String,
    onClick: () -> Unit,  // Hacer onClick como una función normal y no @Composable
    textColor: Color = Color.White,
    background: Color = Color.Transparent,
    roundedCornerShape: RoundedCornerShape = RoundedCornerShape(0.dp),
    textAlign: TextAlign = TextAlign.Start, // Alineación del texto
    modifier: Modifier = Modifier.fillMaxWidth() // Modificador por defecto
) {
    Box(modifier = modifier) {
        Text(
            text = text,
            color = textColor,
            fontSize = 16.sp,
            textAlign = textAlign,
            modifier = Modifier
                .clip(roundedCornerShape)
                .background(background)
                .padding(8.dp)
                .clickable { onClick() }  // Ejecutar la función onClick cuando se haga clic
        )
    }
}


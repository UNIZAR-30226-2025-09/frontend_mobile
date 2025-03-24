package eina.unizar.es.ui.playlist

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
import androidx.navigation.NavController
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient.get
import eina.unizar.es.data.model.network.ApiClient.delete
import eina.unizar.es.ui.song.Song
import org.json.JSONArray
import org.json.JSONObject
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import eina.unizar.es.data.model.network.getLikedPlaylists
import eina.unizar.es.data.model.network.getUserData
import eina.unizar.es.data.model.network.likeUnlikePlaylist
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.ui.navbar.BottomNavigationBar
import eina.unizar.es.ui.player.FloatingMusicPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files.delete

@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun PlaylistScreen(navController: NavController, playlistId: String?) {

// Colores básicos
    val backgroundColor = Color(0xFF000000) // Negro
    val textColor = Color(0xFFFFFFFF) // Blanco
    val cardBackgroundColor = Color(0xFF121212) // Negro un poco más claro
    val buttonColor = Color(0xFF0D47A1) // Azul oscuro


// Datos simulados de la playlist
    val playlistTitle = "Playlist: Rock"
    val playlistAuthor = "Autor: John Doe"


// Simulación de 20 canciones y sus artistas

    val allSongs = (1..20).map { "Canción $it" }
    val songArtistMap = allSongs.associateWith { song ->
        val number = song.filter { it.isDigit() }
        "Artista $number"
    }


// Estados para búsqueda y orden

    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    var sortOption by remember { mutableStateOf("Título") }
    val filteredSongs = allSongs.filter { it.contains(searchText.text, ignoreCase = true) }
    val sortedSongs = remember(filteredSongs, sortOption) {

        when (sortOption) {
            "Título" -> filteredSongs.sortedBy { it }
            "Añadido recientemente" -> filteredSongs.reversed()
            "Artista" -> filteredSongs.sortedBy { songArtistMap[it] ?: "" }
            else -> filteredSongs
        }
    }

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
    var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }
    //val audioUrl = "URL_DEL_ARCHIVO_DE_AUDIO" // Reemplaza con la URL de tu archivo de audio



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
        coroutineScope.launch {
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
        bottomBar = {
            Column {
                val isPlaying = remember { mutableStateOf(false) }
                FloatingMusicPlayer("Sensualidad", "god", R.drawable.kanyeperfil, isPlaying.value)
                BottomNavigationBar(navController)
            }
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
                        model = playlistImage,
                        contentDescription = "Portada de la playlist",
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
                    Text(
                        text = playlistAuthor,
                        color = textColor,
                        style = TextStyle(fontSize = 14.sp)
                    )
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
                        onClick = { showSearch = !showSearch },
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
// Acción para reproducir la playlist (simulada)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(buttonColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproducir Playlist",
                            tint = textColor
                        )
                    }
                }
            }
// Fila con dropdown para ordenar y botón de añadir (Add)
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Ordenar por:", color = textColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    var expandirMenu by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { expandirMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                        ) {
                            Text(sortOption, color = textColor)
                        }
                        DropdownMenu(
                            expanded = expandirMenu,
                            onDismissRequest = { expandirMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Título") },
                                onClick = {
                                    sortOption = "Título"
                                    expandirMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Añadido recientemente") },
                                onClick = {
                                    sortOption = "Fecha"
                                    expandirMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Artista") },
                                onClick = {
                                    sortOption = "Artista"
                                    expandirMenu = false
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                // Invertir el estado local de "me gusta"
                                isLikedPlaylist = !isLikedPlaylist

                                // Hacer el POST al backend para actualizar el "me gusta"
                                val response = playlistInfo?.let { likeUnlikePlaylist(playlistInfo!!.id, userId, isLikedPlaylist) }
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
                        Icon(
                            imageVector = Icons.Default.Favorite, // Usamos el ícono de "me gusta"
                            contentDescription = "Me gusta",
                            tint = if (isLikedPlaylist) Color.Red else Color.Gray // Si está seleccionado, se colorea rojo, si no es gris
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp)) // Espacio entre iconos
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
// Separador
            item { Spacer(modifier = Modifier.height(26.dp)) }
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
                                val urlCompleta = "http://164.90.160.181:5001/${(song.url_mp3).removePrefix("/")}"
                                exoPlayer?.release()
                                exoPlayer = ExoPlayer.Builder(context).build().apply {
                                    val mediaItem = MediaItem.fromUri(urlCompleta)
                                    setMediaItem(mediaItem)
                                    prepare()
                                    play()
                                }
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
        }
        // Mostrar el BottomSheet de la playlist (fuera del items)
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                val urlAntes = playlistInfo?.imageUrl
                val playlistImage = getImageUrl(urlAntes, "/default-playlist.jpg")
                playlistInfo?.let {
                    BottomSheetContent(
                        playlistImage = playlistImage,
                        playlistTitle = it.title, // Reemplaza con el título
                        playlistAuthor = "Kanye Playlist", // Reemplaza con el autor
                        onDismiss = { showBottomSheet = false },
                        navController = navController,
                        playlistId = playlistId
                    )
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
                onDismiss: () -> Unit  // Llamar a esta función para cerrar
            ) {
                val scope = rememberCoroutineScope()  // Para lanzar corrutinas en Compose
                val textColor = Color.White
                var showAlertDialog by remember { mutableStateOf(false) }

                // Función interna para manejar el dismissal
                val dismiss = {
                    showAlertDialog = false
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Opciones de la playlist centradas
                    Column(
                        modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 120.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally), // Centra el Column en su contenedor
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SongOptionItem("Añadir a lista", onClick = dismiss)
                        Spacer(modifier = Modifier.height(18.dp))
                        SongOptionItem("Añadir a la biblioteca", onClick = dismiss)
                        Spacer(modifier = Modifier.height(18.dp))
                        SongOptionItem("Añadir a la cola", onClick = dismiss)
                        Spacer(modifier = Modifier.height(18.dp))
                        SongOptionItem("Eliminar de la lista", onClick = dismiss)
                        Spacer(modifier = Modifier.height(18.dp))
                        SongOptionItem("Compartir", onClick = dismiss)

                        Spacer(modifier = Modifier.height(18.dp))

                        // Opción "Eliminar Playlist" con estilo personalizado
                        SongOptionItem(
                            text = "Eliminar Playlist",
                            textColor = Color(0xFFFF6B6B),
                            onClick = {
                                // Llamada al backend en una corrutina
                                scope.launch {
                                    if (!playlistId.isNullOrEmpty()) {
                                        try {
                                            eliminarPlaylistEnBackend(playlistId)
                                            // Si se elimina con éxito, navega y cierra bottomSheet
                                            navController.navigate("menu")
                                            // Cierra tu bottomSheet como veas (estado local, etc.)
                                        } catch (e: Exception) {
                                            println("Error al eliminar la playlist: ${e.message}")
                                        }
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(38.dp))
                    }
                }
            }

            /**
             * Item de la lista de opciones en el Bottom Sheet.
             */
            @Composable
            fun PlaylistOptionItem(text: String, onClick: () -> Unit) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable { onClick() }
                )
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
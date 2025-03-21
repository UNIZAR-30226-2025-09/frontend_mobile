package eina.unizar.es.ui.playlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import eina.unizar.es.ui.song.Song
import org.json.JSONArray
import org.json.JSONObject
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

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


    // Estado para almacenar la información de la playlist y sus canciones
    var playlistInfo by remember { mutableStateOf<Playlist?>(null) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    val songList = mutableListOf<Song>()
    // Llamar a la API para obtener los datos de la playlist seleccionada
    LaunchedEffect(playlistId) {
        playlistId?.let {
            val response = get("playlists/$it") // Llamamos a la API
            response?.let {
                val jsonObject = JSONObject(response)
                playlistInfo = Playlist(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getString("name"),
                    imageUrl = jsonObject.getString("front_page"),
                    idAutor = jsonObject.getString("user_id"),
                    idArtista = jsonObject.getString("artist_id"),
                    description = jsonObject.getString("description"),
                    esPublica = jsonObject.getString("type"),
                    esAlbum = jsonObject.getString("typeP"),
                    //author = jsonObject.getString("author") habra que hacer un get con el id
                )
            }
        }

        val response = get("songs") // Llamada a la API para obtener canciones
        response?.let {
            val jsonArray = JSONArray(it)
            val fetchedSongs = mutableListOf<Song>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                fetchedSongs.add(
                    Song(
                        id = jsonObject.getInt("id"),
                        name = jsonObject.getString("name"),
                        duration = jsonObject.getInt("duration"),
                        letra = jsonObject.getString("lyrics"),
                        photo_video = jsonObject.getString("photo_video"),
                        url_mp3 = jsonObject.getString("url_mp3")
                    )
                )
            }
            songs = fetchedSongs
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
                    Box(
                        modifier = Modifier
                            .size(imageSize)
                            .alpha(imageAlpha)
                            .background(Color.Gray) // Placeholder para la portada
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
// Acción para añadir una canción (simulada)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Añadir canción",
                            tint = textColor
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


// Mostrar el BottomSheet de la playlist (fuera del items)
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState
                ) {
                    BottomSheetContent(
                        playlistImage = R.drawable.kanyeperfil, // Reemplaza con tu imagen
                        playlistTitle = "Mi Playlist", // Reemplaza con el título
                        playlistAuthor = "KanyeWest", // Reemplaza con el autor
                        onDismiss = { showBottomSheet = false }
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
                playlistImage: Int, // Recibe la imagen de la playlist
                playlistTitle: String, // Recibe el título de la playlist
                playlistAuthor: String, // Recibe el autor de la playlist
                onDismiss: () -> Unit
            ) {
                val textColor = Color.White

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Imagen, título y autor de la playlist en fila
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = playlistImage),
                            contentDescription = null,
                            modifier = Modifier.size(50.dp) // Imagen más pequeña
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PlaylistOptionItem("Añadir Canción", onDismiss)
                        Spacer(modifier = Modifier.height(8.dp))
                        PlaylistOptionItem("Eliminar Canción", onDismiss)
                        Spacer(modifier = Modifier.height(8.dp))
                        PlaylistOptionItem("Compartir", onDismiss)
                        Spacer(modifier = Modifier.height(8.dp))
                        PlaylistOptionItem("Descargar", onDismiss)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
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
            fun SongOptionItem(text: String, onClick: () -> Unit) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onClick() }
                )
            }

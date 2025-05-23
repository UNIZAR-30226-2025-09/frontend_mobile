package eina.unizar.es.ui.playlist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicapp.ui.theme.VibraBlack
import com.example.musicapp.ui.theme.VibraBlue
import com.example.musicapp.ui.theme.VibraLightGrey
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.data.model.network.ApiClient.get
import eina.unizar.es.data.model.network.ApiClient.delete
import eina.unizar.es.data.model.network.ApiClient.getLikedPlaylists
import eina.unizar.es.data.model.network.ApiClient.getUserData
import eina.unizar.es.data.model.network.ApiClient.likeUnlikePlaylist
import kotlinx.coroutines.launch
import coil.compose.rememberAsyncImagePainter
import eina.unizar.es.data.model.network.ApiClient.checkIfSongIsLiked
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.data.model.network.ApiClient.getLikedSongsPlaylist
import eina.unizar.es.data.model.network.ApiClient.getPlaylistCollaborators
import eina.unizar.es.data.model.network.ApiClient.getSongDetails
import eina.unizar.es.data.model.network.ApiClient.isPlaylistOwner
import eina.unizar.es.data.model.network.ApiClient.likeUnlikeSong
import eina.unizar.es.data.model.network.ApiClient.post
import eina.unizar.es.data.model.network.ApiClient.processCollaborators
import eina.unizar.es.data.model.network.ApiClient.recordPlaylistVisit
import eina.unizar.es.data.model.network.ApiClient.updatePlaylist
import eina.unizar.es.data.model.network.ApiClient.togglePlaylistType
import eina.unizar.es.data.model.network.ApiClient.updatePlaylistImage
import eina.unizar.es.data.model.network.ApiClient.uriToBase64
import eina.unizar.es.ui.artist.SongOptionItem
import eina.unizar.es.ui.player.MusicPlayerViewModel
import eina.unizar.es.ui.search.SongItem
import eina.unizar.es.ui.search.convertSongsToCurrentSongs
import eina.unizar.es.ui.artist.SongOptionsBottomSheetContent
import eina.unizar.es.ui.friends.Friend
import eina.unizar.es.ui.song.Song
import eina.unizar.es.ui.user.UserColorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder

// Criterios de ordenacion de canciones de una lista
enum class SortOption {
    TITULO, DURACION, ARTISTA
}

data class PendingInvitationItem(
    val playlistId: String,
    val invitationId: String,
    val userId: String,
    val nickname: String,
    val message: String
)

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
    var averageRating by remember { mutableStateOf(0.0) }


    // Gestion del ViewModel
    val currentSong by playerViewModel.currentSong.collectAsState()
    var currentIdPlaylist = playerViewModel.idCurrentPlaylist
    val isPlaying = currentSong?.isPlaying ?: false

    // Estados para búsqueda y orden
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    var sortOption by remember { mutableStateOf(SortOption.TITULO) }
    val likedSongsSet by playerViewModel.likedSongs.collectAsState()
    var showSearch by remember { mutableStateOf(false) }


    // Variable para mantener la valoración del usuario
    var userRating by remember { mutableStateOf<Float?>(null) }

    // Animation states
    val searchFieldWidth by animateDpAsState(
        targetValue = if (showSearch) 200.dp else 0.dp,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "searchFieldWidth"
    )

    // Los estados para el diálogo:
    var collaborators by remember { mutableStateOf<List<CollaboratorItem>>(emptyList()) }
    var pendingInvitesCollaborate by remember { mutableStateOf<List<CollaboratorItem>>(emptyList()) }
    val pendingInvites = remember { mutableStateListOf<PendingInvitationItem>() }
    var newInviteUserId by remember { mutableStateOf("") }

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



    var needToRefreshCollaborators by remember { mutableStateOf(false) }

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
    var soyPropietario by remember { mutableStateOf(false) }

    // Estado para los colaboradores
    var showCollaboratorsDialog by remember { mutableStateOf(false) }
    val colorManager = remember { UserColorManager(context) }

    var needToRefresh by remember { mutableStateOf(false) }




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

    // Registrar la visita a la playlist cuando se carga la pantalla
    LaunchedEffect(playlistId) {
        val userData = getUserData(context)
        if (userData != null) {
            userId = (userData["id"] ?: "").toString()

            // Si tenemos un ID de usuario y un ID de playlist válidos, registramos la visita
            if (userId.isNotEmpty() && !playlistId.isNullOrEmpty()) {
                coroutineScope.launch {
                    try {
                        val response = recordPlaylistVisit(playlistId, userId)
                        response?.let {
                            // Opcional: Puedes hacer algo con la respuesta si lo necesitas
                            Log.d("PlaylistScreen", "Visita registrada correctamente")
                        }
                    } catch (e: Exception) {
                        Log.e("PlaylistScreen", "Error al registrar visita: ${e.message}")
                    }
                }
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
                //Obtencion de la valoracion de la lista
                val ratingResponse = withContext(Dispatchers.IO) {
                    get("ratingPlaylist/$playlistId/rating")
                }
                ratingResponse?.let {
                    val json = JSONObject(it)
                    val avgRating = json.optString("averageRating", "0.0")
                    averageRating = avgRating.toDoubleOrNull() ?: 0.0
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

    LaunchedEffect(needToRefresh) {
        if (needToRefresh) {
            Log.d("Collaborators", "Refrescando colaboradores…")
            val resp = getPlaylistCollaborators(context, playlistId!!)
            val processed = processCollaborators(resp)
            Log.d("Collaborators", "Colaboradores obtenidos: $processed")
        }
    }

    var playlistAuthor by remember { mutableStateOf("Cargando...") }

    // Usamos un scope de corrutina para manejar la operación asíncrona y cancelarla si es necesario
    val scope = rememberCoroutineScope()

    // Aseguramos que el efecto se ejecute cuando cambia la información de la playlist
    LaunchedEffect(playlistInfo) {
        // Limpiamos el autor cuando cambia la playlist
        playlistAuthor = "Cargando..."

        playlistInfo?.let { playlist ->
            // Lanzamos en el scope de la corrutina para manejar mejor el ciclo de vida
            scope.launch {
                try {
                    // Log para depuración
                    Log.d("PlaylistAuthor", "Obteniendo autor para playlist: ${playlist.title}, idArtista: ${playlist.idArtista}")

                    // Obtenemos el autor y actualizamos el estado
                    val author = getPlaylistAuthor(playlist)
                    playlistAuthor = author

                    Log.d("PlaylistAuthor", "Autor obtenido: $author")
                } catch (e: Exception) {
                    Log.e("PlaylistAuthor", "Error en LaunchedEffect: ${e.message}", e)
                    playlistAuthor = "Artista desconocido"
                }
            }
        } ?: run {
            playlistAuthor = "Artista desconocido"
        }
    }

    var songArtistMap by remember { mutableStateOf<Map<Song, String>>(emptyMap()) }

    LaunchedEffect(songs) {
        val artistNames = mutableMapOf<Song, String>()
        songs.forEach { song ->
            val artistName = getArtistName(song.id)
            artistNames[song] = artistName
        }
        songArtistMap = artistNames
    }

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
                        // Luego el título
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
                            PlaylistCoverWithEdit(
                                playlistId = playlistInfo?.id,
                                playlistInfo = playlistInfo,
                                onUpdateSuccess = { responseJson ->
                                    // Actualizar el estado con la nueva URL de imagen
                                    if (responseJson.has("image_url")) {
                                        val newImageUrl = responseJson.getString("image_url")
                                        // Actualizar el objeto playlistInfo con la nueva URL
                                        playlistInfo = playlistInfo?.let { currentPlaylist ->
                                            Playlist(
                                                id = currentPlaylist.id,
                                                title = currentPlaylist.title,
                                                imageUrl = newImageUrl,
                                                idAutor = currentPlaylist.idAutor,
                                                idArtista = currentPlaylist.idArtista,
                                                description = currentPlaylist.description,
                                                esPublica = currentPlaylist.esPublica,
                                                esAlbum = currentPlaylist.esAlbum
                                            )
                                        }
                                    }

                                    // Mostrar mensaje de éxito
                                    Toast.makeText(
                                        context,
                                        "¡Imagen de portada actualizada correctamente!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onUpdateError = { errorMessage ->
                                    // Mostrar mensaje de error
                                    Toast.makeText(
                                        context,
                                        errorMessage,
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                modifier = Modifier.fillMaxSize(),
                                // Permitir edición solo si el usuario es propietario de la playlist
                                editEnabled = soyPropietario
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

                        playlistAuthor?.let { author ->
                            if (playlistInfo?.esAlbum != "album") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = author,
                                    color = secondaryTextColor,
                                    style = TextStyle(fontSize = 16.sp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Espacio para la descripción (se muestra o queda vacío)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 24.dp, end = 12.dp)
                            ) {
                                // Mostrar la descripción solo si existe y no es "Sencillo" o "null"
                                playlistInfo?.description?.takeIf { it.isNotBlank() && it != "Sencillo" && it != "null" }
                                    ?.let { description ->
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = secondaryTextColor,
                                                lineHeight = 20.sp
                                            ),
                                            maxLines = 3, // Límite de líneas
                                            overflow = TextOverflow.Ellipsis // Puntos suspensivos si es muy largo
                                        )
                                    }
                            }

                            // Rating con fondo redondeado (siempre visible)
                            Box(
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = VibraBlue.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .background(
                                        color = VibraBlue.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Valoración promedio",
                                        tint = VibraBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1f", averageRating),
                                        color = VibraLightGrey,
                                        style = TextStyle(fontSize = 16.sp)
                                    )
                                }
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
                                                    convertSongsToCurrentSongs(
                                                        filteredSongs,
                                                        filteredSongs.size
                                                    ),
                                                    filteredSongs.firstOrNull()?.id.toString()
                                                        ?: "",
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
                                    artistName = artistName, // Pasa el nombre del artista
                                    onClick = {
                                        // Aquí puedes manejar la acción de añadir a la cola
                                        // Por ejemplo, puedes usar el ViewModel para añadir la canción a la cola
                                        playerViewModel.addToQueue(song.id.toString())
                                        Toast.makeText(
                                            context,
                                            "Añadido a la cola",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            //Valoracion actual de la lista
            LaunchedEffect(userId) {
                try {
                    Log.d("Rating", "Usuario id: $userId, Playlist id: $playlistId")
                    val rating = playlistId?.let { ApiClient.getUserRating(it, userId) }
                    userRating = rating
                    Log.d("Rating", "Valoracion de la lista: " + userRating)
                } catch (e: Exception) {
                    Log.e("PlaylistDetail", "Error al cargar la valoración: ${e.message}")
                }
            }

            userRating?.let {
                StarRatingDialog(
                    showDialog = showRatingDialog,
                    onDismiss = { showRatingDialog = false },
                    onConfirm = { stars ->
                        currentRating = stars
                        userRating = stars.toFloat()
                        // Lógica para guardar la valoración
                        coroutineScope.launch {
                            val ratingJson = JSONObject().apply {
                                put("user_id", userId) // Aquí añadimos el user_id
                                put("rating", currentRating)
                            }
                            val response = post("ratingPlaylist/${playlistId}/rate", ratingJson)
                            if (response != null) {
                                // Opcionalmente recargar el promedio después de valorar
                                val ratingResponse =
                                    withContext(Dispatchers.IO) { get("ratingPlaylist/$playlistId/rating") }
                                ratingResponse?.let {
                                    val json = JSONObject(it)
                                    val avgRating = json.optString("averageRating", "0.0")
                                    averageRating =
                                        avgRating.toDoubleOrNull() ?: 0.0 // Convertir a Double
                                }
                            }
                        }
                    },
                    lastRating = it.toInt(),
                )
            }
            // Mostrar el BottomSheet de la playlist (fuera del items)
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState
                ) {
                    var urlAntes = playlistInfo?.imageUrl
                    val playlistImage = getImageUrl(
                        urlAntes,
                        "defaultplaylist.jpg"
                    ) + "?t=${System.currentTimeMillis()}"

                    playlistInfo?.let { playlist ->
                        // Log para debuggear valores justo antes de mostrar el BottomSheet
                        Log.d("BottomSheet", "Mostrando BottomSheet con autor: $playlistAuthor")

                        BottomSheetContent(
                            playlistInfo = playlist,
                            playlistImage = playlistImage,
                            playlistTitle = playlist.title,
                            playlistAuthor = playlistAuthor,
                            playlistDescription = playlist.description,
                            isLikedPlaylist = isLikedPlaylist,
                            onDismiss = { showBottomSheet = false },
                            navController = navController,
                            playlistId = playlistId,
                            playlistMeGusta = playlist.esAlbum,
                            onRateClick = { showRatingDialog = true },
                            onLikeUpdate = { newState ->
                                isLikedPlaylist = newState
                            },
                            onPlaylistUpdated = { updatedPlaylist ->
                                playlistInfo = updatedPlaylist
                                needToRefresh = true
                            },
                            collaborators = collaborators,
                            onCollaboratorsUpdated = { newCollaborators ->
                                collaborators = newCollaborators
                            },
                            newInviteUserId = newInviteUserId,
                            onNewInviteUserIdChange = { newInviteUserId = it },
                            onShowCollaboratorsDialogChange = { showCollaboratorsDialog = true },
                            showCollaboratorsDialog = showCollaboratorsDialog,
                            pendingInvites = pendingInvites,
                            pendingInvitesCollaborate = pendingInvitesCollaborate,
                            onPendingInvitesUpdated = { pendingInvitesCollaborate = it }
                        )
                    }
                }
            }
                // Dialogo para los colaboradores
            if (showCollaboratorsDialog) {
                AlertDialog(
                    onDismissRequest = { showCollaboratorsDialog = false },
                    title = {
                        Text(
                            text = "Colaboradores de la Playlist",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    containerColor = Color(0xFF1E1E1E),
                    text = {
                        Column {
                            // Sección Colaboradores actuales con flecha desplegable
                            var showCollaborators by remember { mutableStateOf(false) }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        needToRefreshCollaborators = true
                                        showCollaborators = !showCollaborators
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Spacer(Modifier.height(16.dp))

                                Icon(
                                    if (showCollaborators) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Expandir colaboradores",
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    "Colaboradores actuales",
                                    style = MaterialTheme.typography.titleMedium
                                )

                            }

                            AnimatedVisibility(
                                visible = showCollaborators,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(Modifier.padding(start = 16.dp)) {
                                    if (collaborators.isEmpty()) {
                                        Text("No hay colaboradores actualmente",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 200.dp)
                                        ) {
                                            items(collaborators) { collaborator ->
                                                // Comprobar si el colaborador es el propietario
                                                var soyPropietarioCollab = collaborator.id == userId
                                                if (soyPropietario && soyPropietarioCollab){
                                                    soyPropietarioCollab = true
                                                }
                                                else{
                                                    soyPropietarioCollab = false
                                                }
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Avatar
                                                    if (collaborator.pictureUrl == "null" || collaborator.pictureUrl.isEmpty()) {
                                                        val userProfileColor = remember(collaborator.id) {
                                                            colorManager.getUserProfileColor(collaborator.id)
                                                        }


                                                        Box(
                                                            modifier = Modifier
                                                                .size(48.dp)
                                                                .background(userProfileColor, CircleShape)
                                                                .clip(CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = collaborator.nickname.take(1).uppercase(),
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                color = Color.White
                                                            )
                                                        }
                                                    } else {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(context)
                                                                .data(getImageUrl(collaborator.pictureUrl))
                                                                .crossfade(true)
                                                                .build(),
                                                            contentDescription = "Foto de perfil",
                                                            modifier = Modifier
                                                                .size(48.dp)
                                                                .clip(CircleShape),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(16.dp))

                                                    // Nombre
                                                    Text(
                                                        text = collaborator.nickname,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        modifier = Modifier.weight(1f),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    if (soyPropietarioCollab == false) {
                                                        // Botón de eliminar
                                                        IconButton(
                                                            onClick = {
                                                                coroutineScope.launch {
                                                                    if (playlistId != null) {
                                                                        ApiClient.removeCollaborator(
                                                                            playlistId,
                                                                            collaborator.id,
                                                                            context
                                                                        )
                                                                    }
                                                                    // Refrescar lista tras eliminar
                                                                    val nuevaListaColaboradores = collaborators.filterNot { it.id == collaborator.id }
                                                                    collaborators = nuevaListaColaboradores
                                                                }
                                                            },
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = "Eliminar",
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            /*****************************/
                            var showPendingInvites by remember { mutableStateOf(false) }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showPendingInvites = !showPendingInvites


                                        if (showPendingInvites) {
                                            pendingInvitesCollaborate = emptyList()

                                            coroutineScope.launch {
                                                playlistId?.let { pid ->
                                                    Log.d("PendingInvites", "Fetching pending invitations for $pid")
                                                    val resp = ApiClient.getPendingInvitations(pid, context)
                                                    Log.d("PendingInvites", "Raw response: $resp")

                                                    resp
                                                        ?.optJSONArray("pendingInvitations")
                                                        ?.let { arr ->
                                                            for (i in 0 until arr.length()) {
                                                                arr.optJSONObject(i)?.let { obj ->
                                                                    val invite = CollaboratorItem(
                                                                        id = obj.optString("id"),
                                                                        nickname     = obj.optString("nickname"),
                                                                        pictureUrl   = obj.optString("pictureUrl")
                                                                    )
                                                                    pendingInvitesCollaborate += invite
                                                                }
                                                            }
                                                            Log.d("PendingInvites", "Parsed invites: $pendingInvitesCollaborate")
                                                        }
                                                }
                                            }
                                        }
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    if (showPendingInvites) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Invitaciones pendientes", style = MaterialTheme.typography.titleMedium)
                            }

                            // ───────────────────────────────────────────────────────────
                            // Contenido expandible
                            // ───────────────────────────────────────────────────────────
                            AnimatedVisibility(
                                visible = showPendingInvites,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                if (pendingInvitesCollaborate.isEmpty()) {
                                    Text(
                                        "No hay invitaciones pendientes",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
                                            .padding(start = 16.dp, top = 8.dp)
                                    ) {
                                        items(pendingInvitesCollaborate) { invite ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Avatar
                                                if (invite.pictureUrl == "null" || invite.pictureUrl.isEmpty()) {
                                                    val userProfileColor = remember(invite.id) {
                                                        colorManager.getUserProfileColor(invite.id)
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .background(userProfileColor, CircleShape)
                                                            .clip(CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = invite.nickname.take(1).uppercase(),
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            color = Color.White
                                                        )
                                                    }
                                                } else {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(context)
                                                            .data(getImageUrl(invite.pictureUrl))
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = "Foto de perfil",
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }


                                                Spacer(Modifier.width(16.dp))

                                                // Nombre
                                                Text(
                                                    text = invite.nickname,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    modifier = Modifier.weight(1f),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                                /*********************************/
                            Spacer(Modifier.height(16.dp))

                            // Sección para invitar amigos como colaboradores
                            var showFriendSelection by remember { mutableStateOf(false) }
                            var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }
                            var isLoadingFriends by remember { mutableStateOf(false) }

                            // Botón para mostrar selección de amigos
                            Button(
                                onClick = {
                                    showFriendSelection = true
                                    // Cargar amigos cuando se pulsa el botón
                                    if (friends.isEmpty()) {
                                        isLoadingFriends = true
                                        coroutineScope.launch {
                                            try {
                                                val friendsArray = ApiClient.getFriendsList(context)
                                                if (friendsArray != null) {
                                                    val friendsList = mutableListOf<Friend>()

                                                    for (i in 0 until friendsArray.length()) {
                                                        val friend = friendsArray.getJSONObject(i)
                                                        friendsList.add(
                                                            Friend(
                                                                id = friend.getString("friendId"),
                                                                name = friend.getString("nickname"),
                                                                photo = friend.optString("user_picture", "")
                                                            )
                                                        )
                                                    }

                                                    friends = friendsList
                                                }
                                            } catch (e: Exception) {
                                                Log.e("CollaboratorsDialog", "Error cargando amigos: ${e.message}")
                                            } finally {
                                                isLoadingFriends = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text("Seleccionar amigos para invitar")
                                }
                            }

                            // Diálogo de selección de amigos
                            if (showFriendSelection) {
                                AlertDialog(
                                    onDismissRequest = { showFriendSelection = false },
                                    title = {
                                        Text(
                                            text = "Invitar amigos como colaboradores",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    containerColor = Color(0xFF1E1E1E),
                                    text = {
                                        Column {
                                            if (isLoadingFriends) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator()
                                                }
                                            } else if (friends.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 32.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "No tienes amigos disponibles para invitar",
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                    )
                                                }
                                            } else {
                                                LazyColumn(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 300.dp)
                                                ) {
                                                    var noHayAmigos = false
                                                    items(friends) { friend ->
                                                        // Solo mostrar amigos que no son ya colaboradores
                                                        Log.d("Amigos", "Amigo: ${friend.name}, ID: ${friend.id}")
                                                        if (collaborators.none { it.id == friend.id }) {
                                                            noHayAmigos = true
                                                            FriendCollaboratorItem(
                                                                friend = friend,
                                                                colorManager = UserColorManager(context),
                                                                onClick = {
                                                                    // Invitar al amigo como colaborador
                                                                    if (playlistId != null) {
                                                                        coroutineScope.launch {
                                                                            ApiClient.inviteCollaborator(
                                                                                playlistId,
                                                                                friend.id,
                                                                                context
                                                                            )?.also {
                                                                                // Opcional: recargar pendientes tras invitar
                                                                                ApiClient.getPendingInvitations(playlistId, context)
                                                                                    ?.let { resp ->
                                                                                        pendingInvites.clear()
                                                                                        val arr = resp.optJSONArray("pendingInvitations")
                                                                                        for (i in 0 until (arr?.length() ?: 0)) {
                                                                                            arr!!.optJSONObject(i)?.let { obj ->
                                                                                                pendingInvites += PendingInvitationItem(
                                                                                                    invitationId = obj.optString("id"),
                                                                                                    userId = obj.optString("userId"),
                                                                                                    nickname = obj.optString("nickname"),
                                                                                                    message = obj.optString("message"),
                                                                                                    playlistId = obj.optString("playlistId")
                                                                                                )
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                Toast.makeText(
                                                                                    context,
                                                                                    "Invitación enviada a ${friend.name}",
                                                                                    Toast.LENGTH_SHORT
                                                                                ).show()
                                                                                showFriendSelection =
                                                                                    false
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            )
                                                        }

                                                    }
                                                    item {
                                                        if(!noHayAmigos){
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(vertical = 8.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    "No hay amigos disponibles para invitar",
                                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {},
                                    dismissButton = {
                                        Row(horizontalArrangement = Arrangement.Start) {
                                            Button(
                                                onClick = { showFriendSelection = false },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .height(48.dp)
                                            )
                                            {
                                                Text("Cerrar", color = Color.White, fontWeight = FontWeight.Medium)

                                            }
                                        }
                                    }
                                )
                            }
                        }
                    },
                    // Botones de acción en la parte inferior
                    confirmButton = {},
                    dismissButton = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showCollaboratorsDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                            ) {
                                Text("Cerrar", color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Contenido del Bottom Sheet con las opciones de la playlist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetContent(
    playlistInfo: Playlist,
    playlistImage: String,
    playlistTitle: String,
    playlistAuthor: String,
    playlistDescription: String,
    isLikedPlaylist: Boolean,
    navController: NavController,
    playlistId: String?,
    onRateClick: () -> Unit,
    onDismiss: () -> Unit,  // Llamar a esta función para cerrar
    playlistMeGusta: String,
    onLikeUpdate: (Boolean) -> Unit,
    onPlaylistUpdated: (Playlist) -> Unit,
    collaborators: List<CollaboratorItem>,
    onCollaboratorsUpdated: (List<CollaboratorItem>) -> Unit,
    pendingInvites: SnapshotStateList<PendingInvitationItem>,
    pendingInvitesCollaborate: List<CollaboratorItem>,
    onPendingInvitesUpdated: (List<CollaboratorItem>) -> Unit,
    newInviteUserId: String,
    onNewInviteUserIdChange: (String) -> Unit,
    onShowCollaboratorsDialogChange: (Boolean) -> Unit,
    showCollaboratorsDialog: Boolean
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

    var showPendingInvites by remember { mutableStateOf(false) }

    //Estado para pop up de eliminar la playlist
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Estado para controlar si mostrar las opciones de compartir
    var showShareOptions by remember { mutableStateOf(false) }

    // Estado para mostrar el diálogo de selección de amigos
    var showFriendSelectionDialog by remember { mutableStateOf(false) }

    // ① Flag para disparar la recarga
    var loadCollaborators by remember { mutableStateOf(false) }

    // URL base para compartir
    val baseShareUrl = "https://vibra.eina.unizar.es/playlist/" // Usa HTTPS para compatibilidad universal
    val vibraDeepLink = "vibra://playlist/" // Para abrir directamente en la app si está instalada
    val fullShareUrl = playlistId?.let { "$baseShareUrl$it" } ?: ""
    val fullDeepLink = playlistId?.let { "$vibraDeepLink$it" } ?: ""

    // Función para copiar al portapapeles
    fun copyToClipboard() {
        val clipboardManager = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        val clipData = ClipData.newPlainText("Enlace a playlist", fullDeepLink)
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
            """.trimIndent())
            type = "text/plain"
        }

        context.startActivity(Intent.createChooser(shareIntent, "Compartir playlist"))
    }

    Log.d("BottomSheetContent", "Playlist ID: $playlistId")

    var showEditPlaylistDialog by remember { mutableStateOf(false) } //Editar playlist
    var newPlaylistName by remember { mutableStateOf(playlistTitle) }
    var newPlaylistDescription by remember { mutableStateOf("") }

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

    LaunchedEffect(loadCollaborators) {
        if (loadCollaborators && playlistId != null) {
            Log.d("Collaborators", "🔄 Cargando colaboradores para playlist $playlistId")
            try {
                val response = withContext(Dispatchers.IO) { getPlaylistCollaborators(context, playlistId) }
                val processed = processCollaborators(response)
                Log.d("Collaborators", "✅ Colaboradores obtenidos: $processed")
                onCollaboratorsUpdated(processed)
            } catch (e: Exception) {
                Log.e("Collaborators", "Error recargando colaboradores", e)
            } finally {
                // 3) Resetea la bandera local para no buclear
                loadCollaborators = false
            }
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
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shadow(2.dp, RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = playlistImage + "?t=${System.currentTimeMillis()}", // Añade parámetro para evitar caché
                    contentDescription = "Portada de la playlist",
                    placeholder = painterResource(R.drawable.defaultplaylist),
                    error = painterResource(R.drawable.defaultplaylist),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(playlistTitle, color = textColor, fontSize = 16.sp)
                Text("de $playlistAuthor", color = Color.Gray, fontSize = 12.sp)
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
                        SongOptionItem("Compartir con amigos", onClick = { 
                            showFriendSelectionDialog = true
                        })
                        Spacer(modifier = Modifier.height(8.dp))
                        SongOptionItem(
                            text = "Compartir en otras apps",
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
                        SongOptionItem(
                            text = "Editar colaboradores",
                            onClick = {
                                Log.d("Collaborators", ">> EDITAR COLABORADORES CLICKED y playlistId: $playlistId")

                                coroutineScope.launch {
                                    // Cargar colaboradores actuales
                                    loadCollaborators = true
                                    val responseRecent = playlistId?.let { getPlaylistCollaborators(context, it) }
                                    val colaboradoresProcesados = processCollaborators(responseRecent)
                                    onCollaboratorsUpdated(colaboradoresProcesados)
                                    onShowCollaboratorsDialogChange(true)
                                    onDismiss()
                                }
                            }
                        )

                        Spacer(Modifier.height(8.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        // Opción "Eliminar Playlist" con estilo personalizado
                        SongOptionItem("Editar Playlist",
                            onClick = {
                                newPlaylistName = playlistTitle
                                newPlaylistDescription = playlistDescription ?: ""
                                showEditPlaylistDialog = true
                                //onDismiss()
                            })
                        Spacer(modifier = Modifier.height(8.dp))
                        SongOptionItem(
                            text = "Eliminar Playlist",
                            textColor = Color(0xFFFF6B6B),
                            onClick = {
                                showDeleteConfirmation = true
                            }
                        )
                    }
                    }
                    Spacer(modifier = Modifier.height(38.dp))
                }
            }
        }
        if (showEditPlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showEditPlaylistDialog = false },
                title = {
                    Text(
                        text = "Editar Playlist",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                containerColor = Color(0xFF1E1E1E),
                text = {
                    Column {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text("Nombre", color = MaterialTheme.colorScheme.onSurface) },
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors( // Añadir colores
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = newPlaylistDescription,
                            onValueChange = { newPlaylistDescription = it },
                            label = { Text("Descripción", color = MaterialTheme.colorScheme.onSurface) },
                            singleLine = false,
                            minLines = 2,
                            colors = TextFieldDefaults.outlinedTextFieldColors( // Añadir colores
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.End) {
                        Spacer(modifier = Modifier.width(48.dp))
                        Button(
                            onClick = {
                                if (newPlaylistName.isNotEmpty() && newPlaylistDescription.isNotEmpty()) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            // Manejo seguro del ID
                                            val id = playlistId?.toInt()
                                                ?: throw NumberFormatException("ID inválido")

                                            // Validación de la URL de imagen
                                            val safeImageUrl = if (playlistImage.isNullOrBlank()) {
                                                "defaultplaylist.jpg"
                                            } else {
                                                playlistImage
                                            }
                                            Log.d("Error", "Id de la lista editada: " + id)
                                            val (code, message) = updatePlaylist(
                                                id = id,
                                                name = newPlaylistName,
                                                description = newPlaylistDescription,
                                                frontPage = safeImageUrl,
                                                context = context
                                            )

                                            withContext(Dispatchers.Main) {
                                                when (code) {
                                                    200 -> {
                                                        Toast.makeText(
                                                        context,
                                                        "Playlist actualizada",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                        // Crear una playlist actualizada con los nuevos valores
                                                        // (usamos un Playlist vacío y lo actualizamos con los valores que tenemos)
                                                        val tempPlaylist = Playlist(
                                                            id = playlistId ?: "",
                                                            title = newPlaylistName,
                                                            description = newPlaylistDescription,
                                                            imageUrl = safeImageUrl,
                                                            // Usa valores por defecto o los existentes para los demás campos
                                                            idAutor = "",
                                                            idArtista = "",
                                                            esPublica = "",
                                                            esAlbum = ""
                                                        )

                                                        // Notifica que se actualizó la playlist
                                                        onPlaylistUpdated(tempPlaylist)

                                                        // Indica que necesitamos recargar la información
                                                        //needToRefresh = true
                                                    }

                                                    404 -> Toast.makeText(
                                                        context,
                                                        "Playlist no encontrada",
                                                        Toast.LENGTH_LONG
                                                    ).show()

                                                    else -> Toast.makeText(
                                                        context,
                                                        "Error: ${message ?: "Código $code"}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                                showEditPlaylistDialog = false
                                            }
                                        } catch (e: NumberFormatException) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "ID de playlist inválido",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "Error de conexión",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = newPlaylistName.isNotEmpty() && newPlaylistDescription.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = VibraBlue),
                            modifier = Modifier
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Guardar", color = VibraBlack, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.Start) {
                        Button(
                            onClick = { showEditPlaylistDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                        ) {
                            Text("Cancelar", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            )
        }
    }

    ConfirmationDialog(
        showDialog = showDeleteConfirmation,
        playlistName = playlistTitle,
        onDismiss = {
            // Se ejecuta al pulsar Cancelar o fuera del diálogo
            showDeleteConfirmation = false
        },
        onConfirm = {
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

    FriendSelectionDialog(
        showDialog = showFriendSelectionDialog,
        playlistId = playlistId ?: "",
        playlistTitle = playlistTitle,
        playlistImage = playlistImage,
        onDismiss = { showFriendSelectionDialog = false },
        navigateToChat = { friendId, friendName, message, sharedContent ->
            val encodedName = URLEncoder.encode(friendName, "UTF-8")
            val route = "chat/$friendId/$encodedName"
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
            }
            coroutineScope.launch {
                ApiClient.sendChatMessageWithSharedContent(friendId, message, sharedContent, context)
            }
        }
    )
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

//Función para obtener el autor de la lista
private suspend fun getPlaylistAuthor(playlist: Playlist): String = withContext(Dispatchers.IO) {
    when (playlist.esAlbum) {
        "Vibra" -> "Vibra"
        "album" -> {
            try {
                if (!playlist.idArtista.isNullOrEmpty()) {
                    // Log con información clara de la petición
                    Log.d("PlaylistAuthor", "Solicitando artista con ID: ${playlist.idArtista}")

                    // Obtener respuesta de la API
                    val response = get("artist/${playlist.idArtista}")

                    // Log de la respuesta completa (truncada para no sobrecargar logs)
                    Log.d("PlaylistAuthor", "Respuesta recibida (primeros 200 caracteres): ${response?.take(200)}")

                    if (!response.isNullOrEmpty()) {
                        try {
                            // Parsear el JSON y extraer información vital
                            val responseJson = JSONObject(response)

                            // Extraer y mostrar las claves de nivel superior para diagnóstico
                            val keys = responseJson.keys().asSequence().toList()
                            Log.d("PlaylistAuthor", "Claves en la respuesta JSON: $keys")

                            // Intentar acceder al objeto 'artist'
                            if (responseJson.has("artist")) {
                                val artistObj = responseJson.getJSONObject("artist")

                                // Mostrar las claves dentro del objeto artist
                                val artistKeys = artistObj.keys().asSequence().toList()
                                Log.d("PlaylistAuthor", "Claves en el objeto artist: $artistKeys")

                                // Verificar y extraer el nombre
                                if (artistObj.has("name")) {
                                    val name = artistObj.getString("name")
                                    Log.d("PlaylistAuthor", "Nombre de artista encontrado: $name")
                                    return@withContext name
                                } else {
                                    Log.w("PlaylistAuthor", "El objeto artist NO contiene la clave 'name'")
                                }
                            } else {
                                Log.w("PlaylistAuthor", "La respuesta NO contiene la clave 'artist'")

                                // Intentar obtener el nombre directamente de la raíz (caso alternativo)
                                if (responseJson.has("name")) {
                                    val name = responseJson.getString("name")
                                    Log.d("PlaylistAuthor", "Nombre encontrado en la raíz: $name")
                                    return@withContext name
                                }
                            }

                            // Si llegamos hasta aquí, no pudimos extraer el nombre correctamente
                            Log.e("PlaylistAuthor", "No se pudo extraer el nombre del artista del JSON")
                            "Artista no identificado"
                        } catch (e: Exception) {
                            Log.e("PlaylistAuthor", "Error al procesar el JSON: ${e.javaClass.simpleName}: ${e.message}")
                            "Error en formato de datos"
                        }
                    } else {
                        Log.w("PlaylistAuthor", "Respuesta vacía al solicitar artista")
                        "Sin datos de artista"
                    }
                } else {
                    Log.w("PlaylistAuthor", "ID de artista vacío o nulo")
                    "Artista sin identificador"
                }
            } catch (e: Exception) {
                Log.e("PlaylistAuthor", "Error general: ${e.javaClass.simpleName}: ${e.message}", e)
                "Error de conexión"
            }
        }
        "single" -> "Sencillo"
        null -> "Origen desconocido"
        else -> "Colección personalizada"
    }
}

@Composable
fun StarRatingDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    lastRating: Int = 1
) {
    var selectedRating by remember(showDialog, lastRating) {
        mutableStateOf(lastRating)
    }


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

/*
* Pop up para confirmar si quieres eliminar la lista
*/


@Composable
fun ConfirmationDialog(
    showDialog: Boolean,
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF202020)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Título
                    Text(
                        text = "¿Eliminar playlist?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Nombre de la playlist
                    Text(
                        text = "\"$playlistName\"",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Advertencia
                    Text(
                        text = "Esta acción no se puede deshacer",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Botón cancelar
                        OutlinedButton(
                            onClick = onDismiss,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Cancelar",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Botón eliminar
                        Button(
                            onClick = onConfirm,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6B6B),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Eliminar",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistCoverWithEdit(
    playlistId: String?,
    playlistInfo: Playlist?,
    onUpdateSuccess: (JSONObject) -> Unit = {},
    onUpdateError: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    editEnabled: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var imageAlpha by remember { mutableStateOf(1f) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri ->
            selectedImageUri = imageUri

            coroutineScope.launch {
                isLoading = true
                imageAlpha = 0.5f
                try {
                    val base64Image = uriToBase64(context, imageUri)
                    base64Image?.let {
                        val numericPlaylistId = playlistId?.toIntOrNull() ?: 0
                        val response = updatePlaylistImage(
                            numericPlaylistId,
                            base64Image
                        )

                        response?.let { jsonResponse ->
                            if (jsonResponse.has("error")) {
                                onUpdateError(jsonResponse.getString("error"))
                            } else {
                                onUpdateSuccess(jsonResponse)
                            }
                        } ?: onUpdateError("Error al actualizar la imagen")
                    } ?: onUpdateError("No se pudo convertir la imagen")
                } catch (e: Exception) {
                    onUpdateError("Error: ${e.localizedMessage ?: "Error desconocido"}")
                } finally {
                    isLoading = false
                    imageAlpha = 1f
                }
            }
        }
    }

    // Aquí usamos el modifier recibido para ocupar todo el espacio disponible
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Contenido de imagen según los estados
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val urlAntes = playlistInfo?.imageUrl
                    // Mantener la imagen anterior con opacidad reducida
                    AsyncImage(
                        model = getImageUrl(urlAntes, "/defaultplaylist.jpg"),
                        contentDescription = "Portada",
                        placeholder = painterResource(R.drawable.defaultplaylist),
                        error = painterResource(R.drawable.defaultplaylist),
                        contentScale = ContentScale.Crop, // Asegura que la imagen cubra todo el espacio
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.5f)
                    )
                    CircularProgressIndicator()
                }
            }
            selectedImageUri != null -> {
                // Mostrar imagen recién seleccionada
                Image(
                    painter = rememberAsyncImagePainter(selectedImageUri),
                    contentDescription = "Nueva portada de playlist",
                    contentScale = ContentScale.Crop, // Asegura que la imagen cubra todo el espacio
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(imageAlpha)
                )
            }
            else -> {
                // Mostrar la imagen actual
                val urlAntes = playlistInfo?.imageUrl
                AsyncImage(
                    model = getImageUrl(urlAntes, "/defaultplaylist.jpg"), //+ "?t=${System.currentTimeMillis()}"
                    contentDescription = "Portada",
                    placeholder = painterResource(R.drawable.defaultplaylist),
                    error = painterResource(R.drawable.defaultplaylist),
                    contentScale = ContentScale.Crop, // Asegura que la imagen cubra todo el espacio
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(imageAlpha)
                )
            }
        }

        if (editEnabled) {
            // Botón de edición
            IconButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier
                    .padding(8.dp)
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        shape = CircleShape
                    )
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar portada",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Primero, añadir un nuevo componente para mostrar la lista de amigos
@Composable
fun FriendSelectionDialog(
    showDialog: Boolean,
    playlistId: String,
    playlistTitle: String,
    playlistImage: String,
    onDismiss: () -> Unit,
    navigateToChat: (friendId: String, friendName: String, message: String, sharedContent: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Cargar la lista de amigos
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val friendsArray = ApiClient.getFriendsList(context)
                if (friendsArray != null) {
                    val friendsList = mutableListOf<Friend>()
                    
                    for (i in 0 until friendsArray.length()) {
                        val friend = friendsArray.getJSONObject(i)
                        friendsList.add(
                            Friend(
                                id = friend.getString("friendId"),
                                name = friend.getString("nickname"),
                                photo = friend.optString("user_picture", "")
                            )
                        )
                    }
                    
                    friends = friendsList
                }
            } catch (e: Exception) {
                Log.e("FriendSelection", "Error cargando amigos: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Crear el JSON de contenido compartido (nuevo)
    val sharedContent = JSONObject().apply {
        put("type", "playlist")
        put("id", playlistId)
        put("title", playlistTitle)
        if (playlistImage.isNotEmpty()) {
            put("image", playlistImage)
        }
    }.toString()

    // Mensaje simple
    val messageText = "¡Mira esta playlist!"

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { 
                Text(
                    "Compartir con amigos",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                ) 
            },
            text = {
                Column {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = VibraBlue)
                        }
                    } else if (friends.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No tienes amigos para compartir",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            items(friends) { friend ->
                                FriendShareItem(
                                    friend = friend,
                                    colorManager = UserColorManager(context),
                                    onClick = {
                                        // Navegar al chat y enviar el mensaje con contenido compartido
                                        navigateToChat(friend.id, friend.name, messageText, sharedContent)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

// Componente para mostrar un amigo en la lista
@Composable
fun FriendShareItem(
    friend: Friend,
    colorManager: UserColorManager,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar del amigo
            if (friend.photo == "null" || friend.photo.isEmpty()) {
                val friendProfileColor = remember(friend.id) {
                    colorManager.getUserProfileColor(friend.id)
                }
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(friendProfileColor, CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = friend.name.take(1).uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(ApiClient.getImageUrl(friend.photo))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Nombre del amigo
            Text(
                text = friend.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Icono de enviar mensaje
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Enviar",
                tint = VibraBlue,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun FriendCollaboratorItem(
    friend: Friend,
    invite: Boolean = true,
    colorManager: UserColorManager,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar del amigo
            if (friend.photo == "null" || friend.photo.isEmpty()) {
                val friendProfileColor = remember(friend.id) {
                    colorManager.getUserProfileColor(friend.id)
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(friendProfileColor, CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = friend.name.take(1).uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(ApiClient.getImageUrl(friend.photo))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Nombre del amigo
            Text(
                text = friend.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.weight(1f))


            if (invite){                // Icono de invitar a colaborar
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Invitar como colaborador",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        )
                        .padding(4.dp)
                )
            }
        }
    }
}
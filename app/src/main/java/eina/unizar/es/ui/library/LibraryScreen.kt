package eina.unizar.es.ui.library

import androidx.compose.material.icons.filled.PushPin
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.musicapp.ui.theme.VibraBlack
import com.example.musicapp.ui.theme.VibraBlue
import com.example.musicapp.ui.theme.VibraDarkGrey
import com.example.musicapp.ui.theme.VibraLightGrey
import com.example.musicapp.ui.theme.VibraWhite
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient.get
import eina.unizar.es.data.model.network.ApiClient.getCollaborativePlaylists
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.data.model.network.ApiClient.getLikedPlaylists
import eina.unizar.es.data.model.network.ApiClient.getUserData
import eina.unizar.es.data.model.network.ApiClient.post
import eina.unizar.es.ui.main.Rubik
import eina.unizar.es.ui.navbar.BottomNavigationBar
import eina.unizar.es.ui.player.FloatingMusicPlayer
import eina.unizar.es.ui.player.MusicPlayerViewModel
import eina.unizar.es.data.model.network.ApiClient.getLikedPlaylists
import eina.unizar.es.data.model.network.ApiClient.getUserData
import eina.unizar.es.ui.playlist.Playlist
import eina.unizar.es.ui.song.Song
import eina.unizar.es.ui.user.UserProfileMenu
import eina.unizar.es.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(navController: NavController, playerViewModel: MusicPlayerViewModel) {
    var searchText by remember { mutableStateOf(TextFieldValue("")) } // Estado del texto de búsqueda
    var isSearching by remember { mutableStateOf(false) } // Estado para mostrar la barra de búsqueda

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDescription by remember { mutableStateOf("") }




    // Estado de la barra de navegación inferior
    var selectedItem by remember { mutableStateOf(2) } // 2 es "Biblioteca" por defecto

    val bottomNavItems = listOf(
        Pair("Inicio", Icons.Default.Home),
        Pair("Buscar", Icons.Default.Search),
        Pair("Biblioteca", Icons.Rounded.Menu)
    )

    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var playlistsLike by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var collaborativePlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    val context = LocalContext.current

    // Para poder realizar el post del like/unlike
    val coroutineScope = rememberCoroutineScope()

    // Id del usuario a guardar al darle like
    var userId by remember { mutableStateOf("") }  // Estado inicial

    // Cambiar esta variable para controlar la visibilidad inicial
    var isLoading by remember { mutableStateOf(true) }

    // Contadores para rastrear el progreso de carga
    var totalImagesToLoad by remember { mutableStateOf(0) }
    var loadedImagesCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val userData = getUserData(context)
            if (userData != null) {
                userId =
                    (userData["id"]
                        ?: "Id").toString()  // Si no hay nickname, usa "Usuario"
            }

            val userId3 = getLikedPlaylists(userId)
            userId3?.let {
                playlistsLike = userId3 // Actualizas el estado de las playlists "liked"
            }

            val response = get("playlists") // Llamada a la API
            response?.let {
                val jsonArray = JSONArray(it)
                val fetchedPlaylists = mutableListOf<Playlist>()
                val likedSongsPlaylist = mutableListOf<Playlist>()

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val playlistUserId = jsonObject.getString("user_id")

                    // Solo añadir playlists que coincidan con el userId
                    if (playlistUserId == userId) {
                        val playlist = Playlist(
                            id = jsonObject.getString("id"),
                            title = jsonObject.getString("name"),
                            idAutor = jsonObject.getString("user_id"),
                            idArtista = jsonObject.getString("artist_id"),
                            description = jsonObject.getString("description"),
                            esPublica = jsonObject.getString("type"),
                            esAlbum = jsonObject.getString("typeP"),
                            imageUrl = jsonObject.getString("front_page")
                        )

                        // Separar la playlist de "me gusta" del resto
                        if (playlist.esAlbum == "Vibra_likedSong") {
                            likedSongsPlaylist.add(playlist)
                        } else {
                            fetchedPlaylists.add(playlist)
                        }
                    }
                }

                // Combinar las listas con la playlist "me gusta" al principio
                playlists = likedSongsPlaylist + fetchedPlaylists
            }

            getCollaborativePlaylists(userId)?.let { arr ->

                // Debug: imprime las claves de cada JSONObject
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { obj ->
                        val keys = obj.names()?.run {
                            (0 until length()).map { idx -> optString(idx) }
                        } ?: listOf()
                        Log.d("LibraryScreen", "collab[${i}] keys = $keys")
                    }
                }


                collaborativePlaylists = List(arr.length()) { i ->
                    arr.getJSONObject(i).run {
                        Playlist(
                            id          = optString("id", ""),             // antes: playlistId
                            title       = optString("name", ""),           // antes: title
                            idAutor     = optString("user_id", ""),        // antes: ownerId
                            idArtista   = optString("artist_id", ""),      // o vacío si no te interesa
                            description = optString("description", ""),    // aprovéchalo si quieres
                            esPublica   = optString("type", "public"),     // o revisa typeP
                            esAlbum     = optString("typeP", "playlist"), // antes fijo “playlist”
                            imageUrl    = optString("front_page", "")      // tal cual
                        )
                    }
                }
            }
        }

        // Al finalizar la carga de datos, actualizar el estado
        coroutineScope.launch {
            // Calcular el total de imágenes a cargar antes de mostrar contenido
            totalImagesToLoad = playlists.size

            // Establecer un tiempo límite para la carga
            delay(500)
            if (isLoading) {
                isLoading = false
                Log.d("Loading", "Forced loading complete after timeout")
            }
        }

    }

    // Función para incrementar el contador de imágenes cargadas
    val onImageLoaded = {
        loadedImagesCount++
        Log.d("Loading", "Image loaded: $loadedImagesCount/$totalImagesToLoad")
        // Check if all images are loaded
        if (loadedImagesCount >= totalImagesToLoad && totalImagesToLoad > 0) {
            isLoading = false
            Log.d("Loading", "All images loaded")
        }
    }

    suspend fun createPlaylist(playlistName: String, userId : String, playlistDescription: String) {
        val jsonBody = JSONObject().apply {
            put("name", playlistName)
            put("user_id", userId) // Agregar el ID del usuario
            put("description", playlistDescription)
            put("type", "private")
        }

        coroutineScope  { // Lanza una corrutina en el scope
            val response = withContext(Dispatchers.IO) { // Cambia al hilo IO para la red
                post("playlists", jsonBody)
            }

            response?.let {
                // Manejar la respuesta del servidor (si es necesario)
                println("Playlist creada con éxito: $it")

                // Actualizar la lista de playlists después de crear una nueva
                val getResponse = withContext(Dispatchers.IO) {
                    get("playlists")
                }

                getResponse?.let {
                    val jsonArray = JSONArray(it)
                    val fetchedPlaylists = mutableListOf<Playlist>()

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val playlistUserId = jsonObject.getString("user_id")

                        // Solo añadir playlists que coincidan con el userId actual
                        if (playlistUserId == userId) {
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
                    }
                    playlists = fetchedPlaylists
                }
            } ?: run {
                // Manejar el error si la creación de la playlist falla
                println("Error al crear la playlist")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row (verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(220.dp))

                        IconButton(
                            onClick = { showCreatePlaylistDialog = true },
                            modifier = Modifier
                                .padding(start = 64.dp, top = 10.dp)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Crear Playlist",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(8.dp)
        ) {
            // Lista de elementos filtrados según la búsqueda
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                // Indicador de carga global mientras se cargan todas las imágenes
                if (isLoading) {
                    // Envuelve el Box dentro de un item{}
                    item {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = VibraBlue
                            )
                        }
                    }
                }
                else {
                    // Playlists Creadas section
                    if (playlists.isNotEmpty()) {
                        item {
                            Text(
                                text = "Tus Playlists",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                        }

                        itemsIndexed(playlists) { index, item ->
                            LibraryItem(item, navController)
                        }
                    }

                    // Liked Playlists section
                    if (playlistsLike.isNotEmpty()) {
                        item {
                            Text(
                                text = "Playlists que te gustan",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                //fontFamily = Rubik,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                        }
                        items(playlistsLike) { item2 ->
                            LibraryItem(item2, navController)
                        }
                    }

                    if (collaborativePlaylists.isNotEmpty()) {
                        item {
                            Text(
                                text = "Playlists colaborativas",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                        }
                        items(collaborativePlaylists) { playlist ->
                            LibraryItem(playlist, navController)
                        }
                    }

                    // Optional: Show a message if no playlists exist
                    if (playlists.isEmpty() && playlistsLike.isEmpty() && collaborativePlaylists.isEmpty()) {
                        item {
                            Text(
                                text = "No tienes playlists aún",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .wrapContentHeight()
                                    .clickable { showCreatePlaylistDialog = true }
                            )
                        }
                    }
                }
            }
        }
        // Pop-up para crear playlist
        if (showCreatePlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                title = {
                    Text(
                        text = "Crear Playlist",
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
                        // Campo para el nombre (obligatorio)
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = {
                                Text(
                                    "Nombre",
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )

                        // Campo para la descripción (opcional)
                        OutlinedTextField(
                            value = newPlaylistDescription,
                            onValueChange = { newPlaylistDescription = it },
                            label = {
                                Text(
                                    "Descripción (opcional)",
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            singleLine = false,
                            minLines = 2,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.End) { // Alineamos a la izquierda
                        Spacer(modifier = Modifier.width(48.dp)) // Agregamos un margen izquierdo
                        Button(
                            onClick = {
                                if (newPlaylistName.isNotEmpty()) {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        createPlaylist(newPlaylistName, userId, newPlaylistDescription)
                                    }
                                    showCreatePlaylistDialog = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (newPlaylistName.isNotEmpty()) VibraBlue else VibraLightGrey
                            ),
                            modifier = Modifier
                                .height(48.dp),
                            enabled = newPlaylistName.isNotEmpty()
                        ) {
                            Text("Confirmar", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.Start) { // Alineamos a la izquierda
                        Spacer(modifier = Modifier.width(0.dp)) // Agregamos un margen izquierdo
                        // Botón Cancelar
                        OutlinedButton(
                            onClick = { showCreatePlaylistDialog = false },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp),
                        ) {
                            Text(
                                text = "Cancelar",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            )
        }
    }
}


@Composable
fun LibraryItem(
    playlist: Playlist,
    navController: NavController
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                navController.navigate("playlist/${playlist.id}")
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
            // Imagen de la playlist - Modificada para expandirse completamente
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shadow(2.dp, RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = getImageUrl(playlist.imageUrl, "defaultplaylist.jpg")+ "?t=${System.currentTimeMillis()}",
                    placeholder = painterResource(R.drawable.defaultplaylist),
                    error = painterResource(R.drawable.defaultplaylist),
                    contentDescription = "Portada de playlist",
                    contentScale = ContentScale.Crop, // Esto es clave para que la imagen se expanda
                    modifier = Modifier.fillMaxSize() // Ocupa todo el espacio disponible del Box
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información de la playlist
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = playlist.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (playlist.esAlbum == "album") "Álbum" else "Playlist" +
                            if (playlist.esPublica == "public") " · Público" else " · Privado",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Flecha indicativa
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Ver playlist",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


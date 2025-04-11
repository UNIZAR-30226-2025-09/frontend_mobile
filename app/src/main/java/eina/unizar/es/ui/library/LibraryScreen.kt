package eina.unizar.es.ui.library

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.musicapp.ui.theme.VibraBlack
import com.example.musicapp.ui.theme.VibraBlue
import com.example.musicapp.ui.theme.VibraLightGrey
import com.example.musicapp.ui.theme.VibraWhite
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient.get
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
    val context = LocalContext.current

    // Para poder realizar el post del like/unlike
    val coroutineScope = rememberCoroutineScope()

    // Id del usuario a guardar al darle like
    var userId by remember { mutableStateOf("") }  // Estado inicial

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

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val playlistUserId = jsonObject.getString("user_id")

                    // Solo añadir playlists que coincidan con el userId
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
        }

    }

    suspend fun createPlaylist(playlistName: String, userId : String, playlistDescription: String) {
        val jsonBody = JSONObject().apply {
            put("name", playlistName)
            put("user_id", userId) // Agregar el ID del usuario
            put("description", playlistDescription)
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
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Tu biblioteca", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)

                        Spacer(modifier = Modifier.width(50.dp))

                        IconButton(
                            onClick = { showCreatePlaylistDialog = true },
                            modifier = Modifier
                                .padding(start = 64.dp, top = 10.dp)
                                .size(30.dp)
                                .background(VibraBlue, CircleShape) // Forma circular
                                .clip(CircleShape) // Asegura el recorte

                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Crear Playlist",
                                tint = VibraBlack,
                                modifier = Modifier.size(20.dp)
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
                        UserProfileMenu(navController)
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
                // Playlists Creadas section
                if (playlists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Tus Playlists",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            //fontFamily = Rubik,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }
                    items(playlists) { item ->
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

                // Optional: Show a message if no playlists exist
                if (playlists.isEmpty() && playlistsLike.isEmpty()) {
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
        // Pop-up para crear playlist
        if (showCreatePlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                title = {
                    Text(
                        "Nombre de la playlist",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }, // OnBackground
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
                    Row(horizontalArrangement = Arrangement.Start) { // Alineamos a la izquierda
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
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (newPlaylistName.isNotEmpty()) VibraBlue else VibraLightGrey
                            ),
                            enabled = newPlaylistName.isNotEmpty()
                        ) {
                            Text("Confirmar", color = VibraBlack)
                        }
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.Start) { // Alineamos a la izquierda
                        Spacer(modifier = Modifier.width(0.dp)) // Agregamos un margen izquierdo
                        Button(
                            onClick = { showCreatePlaylistDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = VibraLightGrey)
                        ) {
                            Text("Cancelar", color = VibraBlack)
                        }
                    }
                }
            )
        }
    }
}


@Composable
fun LibraryItem(playlist: Playlist, navController: NavController) {
    var path = ""
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                // Navegar a la lista de canciones
                navController.navigate("playlist/${playlist.id}") // Usamos el id de la playlist
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (playlist?.esAlbum == "Vibra_likedSong"){path = "playlist_images/meGusta.png"} else {path = playlist.imageUrl}
        val playlistImage = getImageUrl(path, "default-playlist.jpg")
        Log.d("ImageURL", "URL final: $playlistImage")
        //if(playlistImage != "default-playlist.jpg"){
            AsyncImage(
                model = getImageUrl(path, "defaultplaylist.jpg"),
                contentDescription = "Imagen",
                modifier = Modifier.size(50.dp),
                placeholder = painterResource(R.drawable.defaultplaylist), // Fallback local
                error = painterResource(R.drawable.defaultplaylist)
            )
        //}
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Log.d("Descripciones", playlist.description)
            Text(
                text = if (playlist.description != "null") {
                    playlist.description
                } else {
                    "Añade aquí una descripción"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}


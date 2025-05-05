package eina.unizar.es.ui.friends

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.ui.player.MusicPlayerViewModel
import eina.unizar.es.ui.user.UserProfileMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray

data class Friend(
    val id: String,
    val name: String,
    val photo: String = "",
    val status: String = "",
    val isPendingRequest: Boolean = false,
    val isSentRequest: Boolean = false,
    val lastMessage: String = "" // Nueva propiedad para último mensaje
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(navController: NavController, playerViewModel: MusicPlayerViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Variables de estado para la UI
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf(listOf<Friend>()) }
    var friendRequests by remember { mutableStateOf(listOf<Friend>()) }
    var sentRequests by remember { mutableStateOf(listOf<Friend>()) } // Nueva variable para solicitudes enviadas
    var searchResults by remember { mutableStateOf(listOf<Friend>()) }
    var expandedReceivedRequests by remember { mutableStateOf(true) }
    var expandedSentRequests by remember { mutableStateOf(true) } // Nueva variable para el desplegable
    var expandedFriends by remember { mutableStateOf(true) }
    
    // Estado para controlar el diálogo de búsqueda de amigos
    var showAddFriendDialog by remember { mutableStateOf(false) }

    // Carga de datos desde la API
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isLoading = true
            try {
                // Obtener solicitudes de amistad recibidas
                val receivedRequestsArray = ApiClient.getReceivedFriendRequests(context)
                if (receivedRequestsArray != null) {
                    val requests = mutableListOf<Friend>()
                    
                    for (i in 0 until receivedRequestsArray.length()) {
                        try {
                            val request = receivedRequestsArray.getJSONObject(i)
                            requests.add(
                                Friend(
                                    id = request.getString("friendId"),
                                    name = request.getString("nickname"),
                                    photo = request.optString("user_picture", ""),
                                    isPendingRequest = true
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("FriendsScreen", "Error procesando solicitud recibida: ${e.message}")
                        }
                    }
                    
                    friendRequests = requests
                    Log.d("FriendsScreen", "Solicitudes de amistad recibidas: ${requests.size}")
                } else {
                    Log.e("FriendsScreen", "Error obteniendo solicitudes de amistad recibidas")
                }
                
                // Obtener solicitudes de amistad enviadas (NUEVO)
                val sentRequestsArray = ApiClient.getSentFriendRequests(context)
                if (sentRequestsArray != null) {
                    val requests = mutableListOf<Friend>()
                    
                    for (i in 0 until sentRequestsArray.length()) {
                        try {
                            val request = sentRequestsArray.getJSONObject(i)
                            requests.add(
                                Friend(
                                    id = request.getString("friendId"),
                                    name = request.getString("nickname"),
                                    photo = request.optString("user_picture", ""),
                                    isPendingRequest = true,
                                    isSentRequest = true // Marcar como solicitud enviada
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("FriendsScreen", "Error procesando solicitud enviada: ${e.message}")
                        }
                    }
                    
                    sentRequests = requests
                    Log.d("FriendsScreen", "Solicitudes de amistad enviadas: ${requests.size}")
                } else {
                    Log.e("FriendsScreen", "Error obteniendo solicitudes de amistad enviadas")
                }
                
                // Obtener lista de amigos
                val friendsArray = ApiClient.getFriendsList(context)
                if (friendsArray != null) {
                    val friendsList = mutableListOf<Friend>()
                    
                    for (i in 0 until friendsArray.length()) {
                        try {
                            val friend = friendsArray.getJSONObject(i)
                            friendsList.add(
                                Friend(
                                    id = friend.getString("friendId"),
                                    name = friend.getString("nickname"),
                                    photo = friend.optString("user_picture", ""),
                                    status = "online" // Asumimos online por defecto
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("FriendsScreen", "Error procesando amigo: ${e.message}")
                        }
                    }
                    
                    friends = friendsList
                    Log.d("FriendsScreen", "Amigos cargados: ${friendsList.size}")
                } else {
                    Log.e("FriendsScreen", "Error obteniendo lista de amigos")
                }
            } catch (e: Exception) {
                Log.e("FriendsScreen", "Error cargando datos sociales: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }
    
    // Función para buscar amigos potenciales
    fun searchFriends(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            searchQuery = query
            isSearching = false
            return
        }
        
        searchQuery = query
        isSearching = true
        
        coroutineScope.launch {
            try {
                Log.d("SearchFriends", "Buscando usuarios con query: $query")
                
                // Obtener todos los usuarios potenciales (no amigos)
                val allPotentialFriendsArray = ApiClient.searchNewFriends(context)
                
                if (allPotentialFriendsArray != null) {
                    val results = mutableListOf<Friend>()
                    
                    // Convertir el JSONArray a una lista de Friend
                    val allPotentialFriends = mutableListOf<Friend>()
                    for (i in 0 until allPotentialFriendsArray.length()) {
                        try {
                            val user = allPotentialFriendsArray.getJSONObject(i)
                            allPotentialFriends.add(
                                Friend(
                                    id = user.getString("id"),
                                    name = user.getString("nickname"),
                                    photo = user.optString("user_picture", ""),
                                    isPendingRequest = false
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("SearchFriends", "Error procesando usuario: ${e.message}", e)
                        }
                    }
                    
                    // Filtrar la lista según el término de búsqueda
                    val filteredResults = allPotentialFriends.filter {
                        it.name.contains(query, ignoreCase = true)
                    }
                    
                    withContext(Dispatchers.Main) {
                        searchResults = filteredResults
                        
                        if (filteredResults.isEmpty()) {
                            Log.d("SearchFriends", "No se encontraron usuarios que coincidan con: $query")
                            Toast.makeText(
                                context,
                                "No se encontraron usuarios que coincidan",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Log.d("SearchFriends", "Se encontraron ${filteredResults.size} usuarios que coinciden con: $query")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        searchResults = emptyList()
                        Log.e("SearchFriends", "Error en la búsqueda de usuarios")
                        Toast.makeText(
                            context,
                            "Error en la búsqueda de usuarios",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("SearchFriends", "Error al buscar amigos: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    searchResults = emptyList()
                    Toast.makeText(
                        context,
                        "Error en la búsqueda: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isSearching = false
                }
            }
        }
    }
    
    // Función para enviar una solicitud de amistad
    fun sendFriendRequest(friendId: String) {
        coroutineScope.launch {
            try {
                Log.d("SendFriendRequest", "Enviando solicitud a: $friendId")
                
                // Usar el nuevo método implementado en ApiClient
                val response = ApiClient.sendFriendRequest(friendId, context)
                
                if (response != null) {
                    withContext(Dispatchers.Main) {
                        // Eliminar el usuario de la lista de resultados para evitar enviar múltiples solicitudes
                        searchResults = searchResults.filter { it.id != friendId }
                        Toast.makeText(
                            context,
                            "Solicitud de amistad enviada",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("SendFriendRequest", "Error al enviar solicitud de amistad")
                        Toast.makeText(
                            context,
                            "Error al enviar solicitud de amistad",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("SendFriendRequest", "Error al enviar solicitud de amistad: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error al enviar solicitud: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    // Función para eliminar un amigo de la lista
    fun removeFriend(friendId: String) {
        coroutineScope.launch {
            try {
                Log.d("RemoveFriend", "Eliminando amigo: $friendId")
                
                val response = ApiClient.unfollowFriend(friendId, context)
                
                if (response != null) {
                    withContext(Dispatchers.Main) {
                        // Actualizar la UI: eliminar de la lista de amigos
                        friends = friends.filter { it.id != friendId }
                        
                        Toast.makeText(
                            context,
                            "Amigo eliminado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("RemoveFriend", "Error al eliminar amigo")
                        Toast.makeText(
                            context,
                            "Error al eliminar amigo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("RemoveFriend", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Función para aceptar una solicitud de amistad
    fun acceptFriendRequest(friendId: String, friendName: String, friendPhoto: String) {
        coroutineScope.launch {
            try {
                Log.d("AcceptFriendRequest", "Aceptando solicitud de: $friendId")
                
                val response = ApiClient.acceptFriendRequest(friendId, context)
                
                if (response != null) {
                    withContext(Dispatchers.Main) {
                        // Actualizar la UI: eliminar de solicitudes y añadir a amigos
                        friendRequests = friendRequests.filter { it.id != friendId }
                        
                        // Añadir a la lista de amigos con datos completos
                        friends = friends + Friend(
                            id = friendId,
                            name = friendName,
                            photo = friendPhoto,
                            status = "online", // Estado por defecto
                            isPendingRequest = false
                        )
                        
                        Toast.makeText(
                            context,
                            "Solicitud aceptada",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("AcceptFriendRequest", "Error al aceptar solicitud")
                        Toast.makeText(
                            context,
                            "Error al aceptar solicitud",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("AcceptFriendRequest", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Función para rechazar una solicitud de amistad
    fun rejectFriendRequest(friendId: String) {
        coroutineScope.launch {
            try {
                Log.d("RejectFriendRequest", "Rechazando solicitud de: $friendId")
                
                val response = ApiClient.rejectFriendRequest(friendId, context)
                
                if (response != null) {
                    withContext(Dispatchers.Main) {
                        // Actualizar la UI: eliminar de solicitudes
                        friendRequests = friendRequests.filter { it.id != friendId }
                        
                        Toast.makeText(
                            context,
                            "Solicitud rechazada",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("RejectFriendRequest", "Error al rechazar solicitud")
                        Toast.makeText(
                            context,
                            "Error al rechazar solicitud",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("RejectFriendRequest", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Función para cancelar una solicitud de amistad enviada
    fun cancelFriendRequest(friendId: String) {
        coroutineScope.launch {
            try {
                Log.d("CancelFriendRequest", "Cancelando solicitud a: $friendId")
                
                val response = ApiClient.rejectFriendRequest(friendId, context)
                
                if (response != null) {
                    withContext(Dispatchers.Main) {
                        // Actualizar la UI: eliminar de solicitudes enviadas
                        sentRequests = sentRequests.filter { it.id != friendId }
                        
                        Toast.makeText(
                            context,
                            "Solicitud cancelada",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("CancelFriendRequest", "Error al cancelar solicitud")
                        Toast.makeText(
                            context,
                            "Error al cancelar solicitud",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CancelFriendRequest", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Diálogo para buscar y añadir amigos
    if (showAddFriendDialog) {
        Dialog(
            onDismissRequest = { 
                showAddFriendDialog = false 
                searchQuery = ""
                searchResults = emptyList()
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Buscar amigos",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Campo de búsqueda
                    val focusManager = LocalFocusManager.current
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchFriends(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar por nombre...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { 
                                    searchQuery = ""
                                    searchResults = emptyList()
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { 
                            focusManager.clearFocus()
                            searchFriends(searchQuery)
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Resultados de búsqueda
                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (searchQuery.isNotEmpty() && searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No se encontraron usuarios",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else if (searchResults.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            items(searchResults) { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Avatar
                                    if (user.photo.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                .clip(CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = user.name.take(1).uppercase(),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(user.photo)
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
                                    
                                    // Nombre
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    // Botón para enviar solicitud
                                    Button(
                                        onClick = { 
                                            sendFriendRequest(user.id)
                                            // Opcional: Mostrar un mensaje de confirmación
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text("Añadir")
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Botón para cerrar el diálogo
                    Button(
                        onClick = { 
                            showAddFriendDialog = false 
                            searchQuery = ""
                            searchResults = emptyList()
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
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
        floatingActionButton = {
            if (!isLoading) {
                FloatingActionButton(
                    onClick = { showAddFriendDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Añadir amigo",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Sección 1: Solicitudes Recibidas (encabezado + contenido)
                ExpandableSection(
                    title = "Solicitudes recibidas${if (friendRequests.isNotEmpty()) " (${friendRequests.size})" else ""}",
                    isExpanded = expandedReceivedRequests,
                    onToggle = { expandedReceivedRequests = !expandedReceivedRequests },
                    icon = Icons.Default.Person
                )
                
                // Contenido de solicitudes recibidas
                AnimatedVisibility(
                    visible = expandedReceivedRequests,
                    enter = expandVertically(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300))
                ) {
                    if (friendRequests.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            items(friendRequests) { friend ->
                                FriendRequestItem(
                                    friend = friend,
                                    onAccept = {
                                        acceptFriendRequest(friend.id, friend.name, friend.photo)
                                    },
                                    onReject = {
                                        rejectFriendRequest(friend.id)
                                    }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No tienes solicitudes de amistad pendientes",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                // Sección 2: Solicitudes Enviadas (encabezado + contenido)
                ExpandableSection(
                    title = "Solicitudes enviadas${if (sentRequests.isNotEmpty()) " (${sentRequests.size})" else ""}",
                    isExpanded = expandedSentRequests,
                    onToggle = { expandedSentRequests = !expandedSentRequests },
                    icon = Icons.Default.Send
                )
                
                // Contenido de solicitudes enviadas
                AnimatedVisibility(
                    visible = expandedSentRequests,
                    enter = expandVertically(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300))
                ) {
                    if (sentRequests.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            items(sentRequests) { friend ->
                                SentRequestItem(
                                    friend = friend,
                                    onCancel = {
                                        cancelFriendRequest(friend.id)
                                    }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No has enviado solicitudes de amistad",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                // Sección 3: Amigos (encabezado + contenido)
                ExpandableSection(
                    title = "Amigos${if (friends.isNotEmpty()) " (${friends.size})" else ""}",
                    isExpanded = expandedFriends,
                    onToggle = { expandedFriends = !expandedFriends },
                    icon = Icons.Default.Group
                )
                
                // Contenido de la lista de amigos
                AnimatedVisibility(
                    visible = expandedFriends,
                    enter = expandVertically(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300))
                ) {
                    if (friends.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(friends) { friend ->
                                FriendItem(
                                    friend = friend,
                                    onClick = {
                                        val encodedName = java.net.URLEncoder.encode(friend.name, "UTF-8")
                                        val encodedPhoto = if (friend.photo.isNotEmpty()) {
                                            java.net.URLEncoder.encode(friend.photo, "UTF-8")
                                        } else {
                                            ""
                                        }
                                        
                                        navController.navigate("chat/${friend.id}/$encodedName/$encodedPhoto")
                                    },
                                    onDeleteConfirmed = {
                                        removeFriend(friend.id)
                                    }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No tienes amigos en tu lista",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Colapsar" else "Expandir",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun FriendRequestItem(
    friend: Friend,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar del amigo
            if (friend.photo.isEmpty()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = friend.name.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(friend.photo)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Información del amigo
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Quiere ser tu amigo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            // Botones de aceptar y rechazar
            Row {
                IconButton(onClick = onAccept) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Aceptar",
                        tint = Color(0xFF4CAF50)
                    )
                }
                IconButton(onClick = onReject) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Rechazar",
                        tint = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

@Composable
fun FriendItem(
    friend: Friend,
    onClick: () -> Unit,
    onDeleteConfirmed: () -> Unit
) {
    // Estado para controlar la visualización del menú desplegable para este amigo
    var showMenu by remember { mutableStateOf(false) }
    
    // Estado para mostrar el diálogo de confirmación para eliminar amigo
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    val statusColor = when (friend.status) {
        "online" -> Color(0xFF4CAF50)
        "busy" -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }
    
    // Diálogo de confirmación para eliminar amigo
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Eliminar amigo") },
            text = { Text("¿Estás seguro de que quieres eliminar a ${friend.name} de tu lista de amigos?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteConfirmed()
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con indicador de estado
            Box {
                if (friend.photo.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = friend.name.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(friend.photo)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Indicador de estado
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Información del amigo
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Solo mostrar el último mensaje si existe
                if (friend.lastMessage.isNotEmpty()) {
                    Text(
                        text = friend.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    // Si no hay último mensaje, mostrar un estado genérico
                    Text(
                        text = if (friend.status == "online") "En línea" else "Desconectado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
            
            // Botones de acción
            Row {
                // Icono para chatear
                IconButton(onClick = onClick) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "Chat",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Menú de tres puntos verticales
                Box {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Menú desplegable
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Eliminar amigo", 
                                    color = MaterialTheme.colorScheme.error
                                ) 
                            },
                            onClick = {
                                showMenu = false
                                showDeleteConfirmDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SentRequestItem(
    friend: Friend,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (friend.photo.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(friend.photo)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = friend.name.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Solicitud pendiente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancelar solicitud",
                    tint = Color.Red
                )
            }
        }
    }
}
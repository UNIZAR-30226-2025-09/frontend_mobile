package eina.unizar.es.ui.friends

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.ui.player.MusicPlayerViewModel
import eina.unizar.es.ui.user.UserColorManager
import eina.unizar.es.ui.user.UserProfileMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

data class Friend(
    val id: String,
    val name: String,
    val photo: String = "",
    val isPendingRequest: Boolean = false,
    val isSentRequest: Boolean = false,
    val lastMessage: String = "",
    val lastActivity: Date = Date(0),
    val unreadCount: Int = 0  // Añadido para rastrear mensajes no leídos
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun FriendsScreen(navController: NavController, playerViewModel: MusicPlayerViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colorManager = remember { UserColorManager(context) }

    // Variables de estado para la UI
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf(listOf<Friend>()) }
    var friendRequests by remember { mutableStateOf(listOf<Friend>()) }
    var sentRequests by remember { mutableStateOf(listOf<Friend>()) }
    var searchResults by remember { mutableStateOf(listOf<Friend>()) }
    var expandedReceivedRequests by remember { mutableStateOf(true) }
    var expandedSentRequests by remember { mutableStateOf(true) }
    var expandedFriends by remember { mutableStateOf(true) }

    // Estado para controlar el diálogo de búsqueda de amigos
    var showAddFriendDialog by remember { mutableStateOf(false) }

    var isRefreshing by remember { mutableStateOf(false) }

    // Función para recargar todos los datos de la pantalla
    fun refreshData() {
        coroutineScope.launch {
            try {
                isLoading = true
                
                // Obtener solicitudes recibidas
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
                }
                
                // Obtener solicitudes enviadas
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
                                    isSentRequest = true
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("FriendsScreen", "Error procesando solicitud enviada: ${e.message}")
                        }
                    }
                    
                    sentRequests = requests
                    Log.d("FriendsScreen", "Solicitudes de amistad enviadas: ${requests.size}")
                }
                
                // Obtener lista de amigos con sus últimos mensajes
                val friendsArray = ApiClient.getFriendsList(context)
                if (friendsArray != null) {
                    val friendsList = mutableListOf<Friend>()
                    
                    for (i in 0 until friendsArray.length()) {
                        try {
                            val friend = friendsArray.getJSONObject(i)
                            val friendId = friend.getString("friendId")
                            
                            // Obtener el último mensaje del chat y su timestamp (si existe)
                            var lastMessage = ""
                            var lastMessageTimestamp: Date? = null
                            var unreadCount = 0  // Contador de mensajes no leídos
                            
                            try {
                                val chatResponse = ApiClient.getChatConversation(friendId, context)
                                if (chatResponse != null && chatResponse.has("messages")) {
                                    val messagesArray = chatResponse.getJSONArray("messages")
                                    
                                    // Contar mensajes no leídos (enviados por el amigo y que no hayamos leído)
                                    for (j in 0 until messagesArray.length()) {
                                        val messageObj = messagesArray.getJSONObject(j)
                                        val senderId = messageObj.getString("user1_id")
                                        val isRead = messageObj.getBoolean("read")
                                        
                                        // Si el mensaje fue enviado por el amigo y no está leído, incrementar contador
                                        if (senderId == friendId && !isRead) {
                                            unreadCount++
                                        }
                                    }
                                    
                                    if (messagesArray.length() > 0) {
                                        // Tomamos el último mensaje
                                        val lastMessageObj = messagesArray.getJSONObject(messagesArray.length() - 1)
                                        lastMessage = lastMessageObj.getString("txt_message")
                                        
                                        // Obtener el timestamp del último mensaje
                                        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                                        lastMessageTimestamp = try {
                                            dateFormat.parse(lastMessageObj.getString("sent_at"))
                                        } catch (e: Exception) {
                                            Log.e("FriendsScreen", "Error parseando fecha del mensaje: ${e.message}")
                                            Date(0) // Fecha muy antigua si no se puede parsear
                                        }
                                        
                                        // Limitar longitud si es necesario
                                        if (lastMessage.length > 30) {
                                            lastMessage = lastMessage.take(27) + "..."
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("FriendsScreen", "Error obteniendo último mensaje: ${e.message}")
                            }
                            
                            friendsList.add(
                                Friend(
                                    id = friendId,
                                    name = friend.getString("nickname"),
                                    photo = friend.optString("user_picture", ""),
                                    lastMessage = lastMessage,
                                    lastActivity = lastMessageTimestamp ?: Date(0), // Usar 0 para amigos sin mensajes
                                    unreadCount = unreadCount  // Añadir el contador de mensajes no leídos
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("FriendsScreen", "Error procesando amigo: ${e.message}")
                        }
                    }
                    
                    // Ordenar la lista de amigos por la fecha del último mensaje (más reciente primero)
                    friends = friendsList.sortedByDescending { it.lastActivity }
                    Log.d("FriendsScreen", "Amigos cargados y ordenados por actividad reciente: ${friendsList.size}")
                }
            } catch (e: Exception) {
                Log.e("FriendsScreen", "Error cargando datos sociales: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun manualRefresh() {
        coroutineScope.launch {
            isRefreshing = true
            refreshData()
            isRefreshing = false
        }
    }
    
    val pullRefreshState = rememberPullRefreshState(isRefreshing, ::manualRefresh)

    // Carga inicial de datos
    LaunchedEffect(Unit) {
        refreshData()
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
                    // Convertir el JSONArray a una lista de Friend
                    val allPotentialFriends = mutableListOf<Friend>()
                    for (i in 0 until allPotentialFriendsArray.length()) {
                        try {
                            val user = allPotentialFriendsArray.getJSONObject(i)
                            allPotentialFriends.add(
                                Friend(
                                    id = user.getString("id"),
                                    name = user.getString("nickname"),
                                    photo = user.getString("user_picture"),
                                    isPendingRequest = false
                                )
                            )
                            Log.d("SearchFriends", "Usuario encontrado: ${user.getString("nickname")} ${user.getString("id")} ${user.getString("user_picture")}")
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
                        } else {
                            Log.d("SearchFriends", "Se encontraron ${filteredResults.size} usuarios que coinciden con: $query")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        searchResults = emptyList()
                        Log.e("SearchFriends", "Error en la búsqueda de usuarios")
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
                
                val response = ApiClient.sendFriendRequest(friendId, context)
                
                if (response != null) {
                    withContext(Dispatchers.Main) {
                        // Eliminar el usuario de la lista de resultados
                        searchResults = searchResults.filter { it.id != friendId }
                        
                        // Buscar el usuario en los resultados para añadirlo a solicitudes enviadas
                        val sentUser = searchResults.find { it.id == friendId }
                        sentUser?.let {
                            sentRequests = sentRequests + Friend(
                                id = it.id,
                                name = it.name,
                                photo = it.photo,
                                isPendingRequest = true,
                                isSentRequest = true
                            )
                        }
                        
                        Toast.makeText(
                            context,
                            "Solicitud de amistad enviada",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Recargar datos después de enviar solicitud
                        refreshData()
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
                        
                        // Recargar datos después de eliminar amigo
                        refreshData()
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
                            isPendingRequest = false
                        )
                        
                        Toast.makeText(
                            context,
                            "Solicitud aceptada",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Recargar datos después de aceptar
                        refreshData()
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
                        
                        // Recargar datos después de rechazar solicitud
                        refreshData()
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
                        
                        // Recargar datos después de cancelar solicitud
                        refreshData()
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
                                    if (user.photo == "null" || user.photo.isEmpty()) {
                                        val userProfileColor = remember(user.id) {
                                            colorManager.getUserProfileColor(user.id)
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(userProfileColor, CircleShape)
                                                .clip(CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Añade un log para depuración
                                            Log.d("FriendsScreen", "Mostrando inicial para ${user.name} con ID ${user.id}, color: $userProfileColor")
                                            
                                            Text(
                                                text = user.name.take(1).uppercase(),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(ApiClient.getImageUrl(user.photo))
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
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
                    .verticalScroll(rememberScrollState())
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {

                    Spacer(modifier = Modifier.height(16.dp))

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
                            Column {
                                friendRequests.forEach { friend ->
                                    FriendRequestItem(
                                        colorManager = colorManager,
                                        context = context,
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
                        icon = Icons.AutoMirrored.Filled.Send
                    )
                    
                    // Contenido de solicitudes enviadas
                    AnimatedVisibility(
                        visible = expandedSentRequests,
                        enter = expandVertically(animationSpec = tween(300)),
                        exit = shrinkVertically(animationSpec = tween(300))
                    ) {
                        if (sentRequests.isNotEmpty()) {
                            Column {
                                sentRequests.forEach { friend ->
                                    SentRequestItem(
                                        colorManager = colorManager,
                                        context = context,
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
                            Column {
                                // Usamos forEachIndexed para obtener el índice
                                friends.forEachIndexed { index, friend ->
                                    FriendItem(
                                        navController = navController,
                                        friend = friend,
                                        colorManager = colorManager,
                                        context = context,
                                        onClick = {
                                            try {
                                                val encodedName = URLEncoder.encode(friend.name, "UTF-8")
                                                val route = if (friend.photo.isNotEmpty()) {
                                                    val encodedPhoto = URLEncoder.encode(friend.photo, "UTF-8")
                                                    "chat/${friend.id}/$encodedName?friendPhoto=$encodedPhoto"
                                                } else {
                                                    "chat/${friend.id}/$encodedName"
                                                }
                                                navController.navigate(route)
                                            } catch (e: Exception) {
                                                Log.e("Navigation", "Error al navegar al chat: ${e.message}", e)
                                                Toast.makeText(
                                                    context,
                                                    "Error al abrir el chat: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        onDeleteConfirmed = {
                                            removeFriend(friend.id)
                                        },
                                        isLastItem = index == friends.size - 1 // Es el último elemento si su índice coincide con el tamaño - 1
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

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
        
        // Indicador de Pull-to-refresh ahora en el Box exterior
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
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
    colorManager: UserColorManager,
    context: Context,
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
            if (friend.photo == "null" || friend.photo.isEmpty()) {
                // Usar UserColorManager para obtener un color persistente
                val friendProfileColor = remember(friend.id) {
                    colorManager.getUserProfileColor(friend.id)
                }
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(friendProfileColor, CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Añade un log para depuración
                    Log.d("FriendsScreen", "Mostrando inicial para ${friend.name} con ID ${friend.id}, color: $friendProfileColor")
                    
                    Text(
                        text = friend.name.take(1).uppercase(),
                        style = MaterialTheme.typography.bodyLarge,
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
                    style = MaterialTheme.typography.titleMedium,
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
    navController: NavController,
    friend: Friend,
    colorManager: UserColorManager,
    context: Context,
    onClick: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    isLastItem: Boolean = false // Nuevo parámetro para identificar si es el último elemento
) {
    // Estado para controlar la visualización del menú desplegable para este amigo
    var showMenu by remember { mutableStateOf(false) }
    
    // Estado para mostrar el diálogo de confirmación para eliminar amigo
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
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
            // Avatar sin indicador de estado
            if (friend.photo == "null" || friend.photo.isEmpty()) {
                val friendProfileColor = remember(friend.id) {
                    colorManager.getUserProfileColor(friend.id)
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(friendProfileColor, CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Log para depuración
                    Log.d("FriendsScreen", "Mostrando inicial para ${friend.name} con ID ${friend.id}, color: $friendProfileColor")

                    Text(
                        text = friend.name.take(1).uppercase(),
                        style = MaterialTheme.typography.bodyLarge,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = friend.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Último mensaje (sin cambios)
                Text(
                    text = friend.lastMessage.ifEmpty { "" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        // Texto más oscuro si hay mensajes sin leer
                        alpha = if (friend.unreadCount > 0) 0.9f else 0.7f
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Texto en negrita si hay mensajes sin leer
                    fontWeight = if (friend.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            
            // Botones de acción
            Row (
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Mostrar indicador de mensajes no leídos si hay alguno
                if (friend.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(
                                color = Color(0xFFFF6B6B),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (friend.unreadCount > 99) "99+" else friend.unreadCount.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Icono para chatear
                IconButton(onClick = {
                    val encodedName = URLEncoder.encode(friend.name, "UTF-8")
                    val route = if (friend.photo.isNotEmpty()) {
                        val encodedPhoto = URLEncoder.encode(friend.photo, "UTF-8")
                        "chat/${friend.id}/$encodedName?friendPhoto=$encodedPhoto"
                    } else {
                        "chat/${friend.id}/$encodedName"
                    }
                    navController.navigate(route)
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
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

        // Solo mostrar el Divider si no es el último elemento
        if (!isLastItem) {
            Divider(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun SentRequestItem(
    colorManager: UserColorManager,
    context: Context,
    friend: Friend,
    onCancel: () -> Unit
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (friend.photo == "null" || friend.photo.isEmpty()) {
                    val friendProfileColor = remember(friend.id) {
                        colorManager.getUserProfileColor(friend.id)
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(friendProfileColor, CircleShape)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Añade un log para depuración
                        Log.d("FriendsScreen", "Mostrando inicial para ${friend.name} con ID ${friend.id}, color: $friendProfileColor")
                        
                        Text(
                            text = friend.name.take(1).uppercase(),
                            style = MaterialTheme.typography.bodyLarge,
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
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Solicitud pendiente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
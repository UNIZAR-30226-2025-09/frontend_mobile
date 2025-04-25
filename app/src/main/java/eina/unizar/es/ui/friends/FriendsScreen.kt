package eina.unizar.es.ui.friends

import android.content.Context
import android.util.Log
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.ui.player.MusicPlayerViewModel
import eina.unizar.es.ui.user.UserProfileMenu
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONArray

data class Friend(
    val id: String,
    val name: String,
    val photo: String,
    val status: String = "offline", // online, offline, busy, etc.
    val lastMessage: String = "",
    val isPendingRequest: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(navController: NavController, playerViewModel: MusicPlayerViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Estado para expandir/colapsar las secciones
    var friendRequestsExpanded by remember { mutableStateOf(true) }
    var friendsExpanded by remember { mutableStateOf(true) }
    
    // Estados para las listas de amigos y solicitudes
    var friendRequests by remember { mutableStateOf(listOf<Friend>()) }
    var friends by remember { mutableStateOf(listOf<Friend>()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Estado para controlar el diálogo de búsqueda de amigos
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<Friend>()) }
    var isSearching by remember { mutableStateOf(false) }

    // Carga de datos desde la API
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isLoading = true
            try {
                // Obtener el token de autenticación
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                
                if (token.isNullOrEmpty()) {
                    Log.e("FriendsScreen", "Token de autenticación no disponible")
                    isLoading = false
                    return@launch
                }
                
                // Crear objeto JSONObject vacío para la petición
                val emptyJson = JSONObject()
                
                // Preparar los headers con el token de autenticación
                val headers = mutableMapOf<String, String>("Authorization" to "Bearer $token")

                // Obtener solicitudes de amistad recibidas
                val receivedRequestsResponse = ApiClient.postWithHeaders("social/getReceivedFriendRequests", emptyJson, context, headers)
                receivedRequestsResponse?.let {
                    val jsonArray = JSONArray(it)
                    val requests = mutableListOf<Friend>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val user = jsonObject.getJSONObject("user1") // El remitente de la solicitud
                        
                        requests.add(
                            Friend(
                                id = user.getString("id"),
                                name = user.getString("nickname"),
                                photo = user.optString("user_picture", ""),
                                isPendingRequest = true
                            )
                        )
                    }
                    
                    friendRequests = requests
                }
                
                // Obtener lista de amigos
                val friendsListResponse = ApiClient.postWithHeaders("social/getFriendsList", emptyJson, context, headers)
                friendsListResponse?.let {
                    val jsonArray = JSONArray(it)
                    val friendsList = mutableListOf<Friend>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        // Determinar qué usuario es el amigo (no el usuario actual)
                        val user = if (jsonObject.has("user2")) {
                            jsonObject.getJSONObject("user2")
                        } else {
                            jsonObject.getJSONObject("user1")
                        }
                        
                        friendsList.add(
                            Friend(
                                id = user.getString("id"),
                                name = user.getString("nickname"),
                                photo = user.optString("user_picture", ""),
                                status = "online" // Por defecto asumimos que está en línea
                            )
                        )
                    }
                    
                    friends = friendsList
                }
            } catch (e: Exception) {
                // Manejar errores
                e.printStackTrace()
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
            return
        }
        
        searchQuery = query
        isSearching = true
        coroutineScope.launch {
            try {
                // Obtener el token de autenticación
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                
                if (token.isNullOrEmpty()) {
                    Log.e("SearchFriends", "Token de autenticación no disponible")
                    isSearching = false
                    return@launch
                }
                
                // Crear objeto JSONObject con el query de búsqueda
                val jsonBody = JSONObject().put("searchText", query)
                
                // Preparar los headers con el token de autenticación
                val headers = mutableMapOf<String, String>("Authorization" to "Bearer $token")
                
                // Llamar a la API con el token de autenticación
                val response = ApiClient.postWithHeaders("social/searchNewFriends", jsonBody, context, headers)
                
                if (response != null) {
                    Log.d("SearchFriends", "Respuesta recibida: $response")
                    val jsonArray = JSONArray(response)
                    val results = mutableListOf<Friend>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val user = jsonArray.getJSONObject(i)
                        
                        results.add(
                            Friend(
                                id = user.getString("id"),
                                name = user.getString("nickname"),
                                photo = user.optString("user_picture", ""),
                                isPendingRequest = false
                            )
                        )
                    }
                    
                    searchResults = results
                } else {
                    Log.e("SearchFriends", "La API devolvió una respuesta nula")
                    searchResults = emptyList()
                }
            } catch (e: Exception) {
                Log.e("SearchFriends", "Error al buscar amigos: ${e.message}", e)
                e.printStackTrace()
                searchResults = emptyList()
            } finally {
                isSearching = false
            }
        }
    }
    
    // Función para enviar una solicitud de amistad
    fun sendFriendRequest(friendId: String) {
        coroutineScope.launch {
            try {
                // Obtener el token de autenticación
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                
                if (token.isNullOrEmpty()) {
                    Log.e("SendFriendRequest", "Token de autenticación no disponible")
                    return@launch
                }
                
                // Crear objeto JSONObject con el ID del amigo
                val jsonBody = JSONObject().put("friendId", friendId)
                
                // Preparar los headers con el token de autenticación
                val headers = mutableMapOf<String, String>("Authorization" to "Bearer $token")
                
                // Llamar a la API con el token de autenticación
                val response = ApiClient.postWithHeaders("social/send", jsonBody, context, headers)
                
                if (response != null) {
                    // Eliminar el usuario de la lista de resultados para evitar enviar múltiples solicitudes
                    searchResults = searchResults.filter { it.id != friendId }
                } else {
                    Log.e("SendFriendRequest", "Error al enviar solicitud de amistad: La API devolvió una respuesta nula")
                }
            } catch (e: Exception) {
                Log.e("SendFriendRequest", "Error al enviar solicitud de amistad: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }
    
    // Función para eliminar un amigo de la lista
    fun removeFriend(friendId: String) {
        coroutineScope.launch {
            try {
                // Obtener el token de autenticación
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                
                if (token.isNullOrEmpty()) {
                    Log.e("RemoveFriend", "Token de autenticación no disponible")
                    return@launch
                }
                
                val jsonBody = JSONObject().put("friendRequestId", friendId)
                
                // Preparar los headers con el token de autenticación
                val headers = mutableMapOf<String, String>("Authorization" to "Bearer $token")
                
                // Llamar a la API con el token de autenticación
                val response = ApiClient.postWithHeaders("social/reject", jsonBody, context, headers)
                
                if (response != null) {
                    // Actualizar la UI solo si la API responde correctamente
                    friends = friends.filter { it.id != friendId }
                } else {
                    Log.e("RemoveFriend", "Error al eliminar amigo: La API devolvió una respuesta nula")
                }
            } catch (e: Exception) {
                Log.e("RemoveFriend", "Error al eliminar amigo: ${e.message}", e)
                e.printStackTrace()
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
                title = {
                    Row (verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Amigos", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                // Mostrar indicador de carga
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // Sección de solicitudes de amistad
                ExpandableSection(
                    title = "Solicitudes de amistad",
                    itemCount = friendRequests.size,
                    isExpanded = friendRequestsExpanded,
                    onToggle = { friendRequestsExpanded = !friendRequestsExpanded }
                )
                
                AnimatedVisibility(
                    visible = friendRequestsExpanded,
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
                                        coroutineScope.launch {
                                            try {
                                                // Llamada a la API para aceptar la solicitud
                                                val jsonBody = JSONObject().put("friendRequestId", friend.id)
                                                val response = ApiClient.post("social/accept", jsonBody)
                                                
                                                if (response != null) {
                                                    // Actualizar la UI
                                                    friendRequests = friendRequests.filter { it.id != friend.id }
                                                    friends = friends + friend.copy(isPendingRequest = false)
                                                }
                                            } catch (e: Exception) {
                                                // Manejar error
                                                e.printStackTrace()
                                            }
                                        }
                                    },
                                    onReject = {
                                        coroutineScope.launch {
                                            try {
                                                // Llamada a la API para rechazar la solicitud
                                                val jsonBody = JSONObject().put("friendRequestId", friend.id)
                                                val response = ApiClient.post("social/reject", jsonBody)
                                                
                                                if (response != null) {
                                                    // Actualizar la UI
                                                    friendRequests = friendRequests.filter { it.id != friend.id }
                                                }
                                            } catch (e: Exception) {
                                                // Manejar error
                                                e.printStackTrace()
                                            }
                                        }
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sección de amigos
                ExpandableSection(
                    title = "Tus amigos",
                    itemCount = friends.size,
                    isExpanded = friendsExpanded,
                    onToggle = { friendsExpanded = !friendsExpanded }
                )
                
                AnimatedVisibility(
                    visible = friendsExpanded,
                    enter = expandVertically(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300))
                ) {
                    if (friends.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f) // Toma el espacio restante
                        ) {
                            items(friends) { friend ->
                                FriendItem(
                                    friend = friend,
                                    onClick = {
                                        // Navegar a la conversación con este amigo
                                        navController.navigate("chat/${friend.id}")
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
                                "Aún no tienes amigos añadidos",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                // Botón para añadir amigo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            // Mostrar el diálogo de búsqueda de amigos
                            showAddFriendDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            Icons.Filled.PersonAdd,
                            contentDescription = "Añadir amigo",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Añadir amigo", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    itemCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
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
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "($itemCount)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                modifier = Modifier
                    .weight(1f)
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
                if (friend.lastMessage.isNotEmpty()) {
                    Text(
                        text = friend.lastMessage,
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
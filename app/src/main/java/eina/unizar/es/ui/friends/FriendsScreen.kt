package eina.unizar.es.ui.friends

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.ui.player.MusicPlayerViewModel
import eina.unizar.es.ui.user.UserProfileMenu
import kotlinx.coroutines.launch

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
    
    // Estados para las listas de amigos y solicitudes (podrían cargarse desde una API)
    var friendRequests by remember { mutableStateOf(listOf<Friend>()) }
    var friends by remember { mutableStateOf(listOf<Friend>()) }

    // Simulación de datos - Reemplazar con llamadas API reales
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            // Aquí se cargarían los datos reales desde la API
            // Por ahora usamos datos de ejemplo
            friendRequests = listOf(
                Friend(
                    id = "1", 
                    name = "Carlos Martínez", 
                    photo = "", 
                    isPendingRequest = true
                ),
                Friend(
                    id = "2", 
                    name = "Ana García", 
                    photo = "", 
                    isPendingRequest = true
                )
            )
            
            friends = listOf(
                Friend(
                    id = "3", 
                    name = "Luis Rodríguez", 
                    photo = "", 
                    status = "online", 
                    lastMessage = "¿Qué tal el concierto de ayer?"
                ),
                Friend(
                    id = "4", 
                    name = "María López", 
                    photo = "", 
                    status = "offline", 
                    lastMessage = "Escucha esta canción"
                ),
                Friend(
                    id = "5", 
                    name = "Javier Sánchez", 
                    photo = "", 
                    status = "busy", 
                    lastMessage = "¿Tienes la nueva playlist?"
                )
            )
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
                        UserProfileMenu(navController)
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
                            .heightIn(max = 300.dp) // Altura máxima para la lista
                    ) {
                        items(friendRequests) { friend ->
                            FriendRequestItem(
                                friend = friend,
                                onAccept = {
                                    // Lógica para aceptar solicitud
                                    friendRequests = friendRequests.filter { it.id != friend.id }
                                    friends = friends + friend.copy(isPendingRequest = false)
                                },
                                onReject = {
                                    // Lógica para rechazar solicitud
                                    friendRequests = friendRequests.filter { it.id != friend.id }
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
            
            // Botón flotante para añadir amigo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        // Lógica para añadir amigo
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
                        tint = Color(0xFF4CAF50)  // Mantenemos colores específicos para estados
                    )
                }
                IconButton(onClick = onReject) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Rechazar",
                        tint = Color(0xFFF44336)  // Mantenemos colores específicos para estados
                    )
                }
            }
        }
    }
}

@Composable
fun FriendItem(
    friend: Friend,
    onClick: () -> Unit
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
                        // Lógica para eliminar amigo
                        // Aquí iría la llamada a la API para eliminar al amigo
                        // Por ejemplo: ApiClient.deleteContact(friend.id)
                        
                        // Cerramos el diálogo
                        showDeleteConfirmDialog = false
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
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(12.dp))

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
                            }
                        )
                    }
                }
            }
        }
    }
}
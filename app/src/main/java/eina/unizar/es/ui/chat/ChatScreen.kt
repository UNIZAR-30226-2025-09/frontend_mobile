package eina.unizar.es.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.ui.player.MusicPlayerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, friendId: String?, playerViewModel: MusicPlayerViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Estado para almacenar la información del amigo
    var friendName by remember { mutableStateOf("") }
    var friendPhoto by remember { mutableStateOf("") }
    var friendStatus by remember { mutableStateOf("offline") }
    
    var messages by remember { mutableStateOf(listOf<String>()) }
    var currentMessage by remember { mutableStateOf(TextFieldValue("")) }
    
    // Estado para controlar la visualización del menú desplegable
    var showMenu by remember { mutableStateOf(false) }
    
    // Estado para mostrar el diálogo de confirmación para eliminar amigo
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    // Carga de datos del amigo
    LaunchedEffect(friendId) {
        coroutineScope.launch {
            // Aquí deberías cargar la información del amigo desde tu API
            // Por ahora usamos datos de ejemplo
            friendId?.let {
                // Simula la carga de datos del amigo
                // Reemplazar con llamada API real
                friendName = "Luis Rodríguez" // Ejemplo
                friendPhoto = "" // URL de la foto
                friendStatus = "online" // Estado del amigo
                
                // También podrías cargar el historial de mensajes
                messages = listOf(
                    "Hola, ¿qué tal?",
                    "¿Has escuchado la nueva canción de Bad Bunny?",
                    "Está genial, te la recomiendo"
                )
            }
        }
    }
    
    // Determina el color de estado
    val statusColor = when (friendStatus) {
        "online" -> Color(0xFF4CAF50) // Mantenemos estos colores específicos para los estados
        "busy" -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }

    // Diálogo de confirmación para eliminar amigo
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Eliminar amigo") },
            text = { Text("¿Estás seguro de que quieres eliminar a $friendName de tu lista de amigos?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Lógica para eliminar amigo
                        coroutineScope.launch {
                            // Aquí iría la llamada a la API para eliminar al amigo
                            // Por ejemplo: ApiClient.deleteContact(friendId)
                            
                            // Cerramos el diálogo y volvemos a la pantalla anterior
                            showDeleteConfirmDialog = false
                            navController.popBackStack()
                        }
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Foto del amigo
                        Box {
                            if (friendPhoto.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = friendName.take(1).uppercase(),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(friendPhoto)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            // Indicador de estado
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                    .align(Alignment.BottomEnd)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Nombre del amigo
                        Text(
                            text = if (friendName.isNotEmpty()) friendName else "Chat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Menú de tres puntos verticales
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Más opciones",
                                tint = MaterialTheme.colorScheme.onBackground
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
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Message list with chat bubbles
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    messages.forEachIndexed { index, message ->
                        val isUserMessage = index % 2 == 1 // Simulamos mensajes alternados
                        ChatBubble(
                            message = message,
                            isUserMessage = isUserMessage
                        )
                    }
                }

                // Input field and send button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = currentMessage,
                        onValueChange = { currentMessage = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .height(56.dp)
                            .fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (currentMessage.text.isEmpty()) {
                                    Text(
                                        text = "Escribe un mensaje...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    
                    IconButton(
                        onClick = {
                            if (currentMessage.text.isNotBlank()) {
                                messages = messages + currentMessage.text
                                currentMessage = TextFieldValue("")
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Enviar",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ChatBubble(
    message: String,
    isUserMessage: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isUserMessage) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isUserMessage) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUserMessage) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}


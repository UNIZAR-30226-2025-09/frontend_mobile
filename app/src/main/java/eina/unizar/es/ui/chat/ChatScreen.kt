package eina.unizar.es.ui.chat

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.ui.player.MusicPlayerViewModel
import eina.unizar.es.ui.user.UserColorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    friendId: String?,
    friendName: String?,
    friendPhoto: String?,
    playerViewModel: MusicPlayerViewModel,
    sharePlaylistId: String? = null,
    sharePlaylistTitle: String? = null,
    sharePlaylistImage: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Decodificar parámetros de la URL
    val decodedFriendName = remember(friendName) {
        try {
            URLDecoder.decode(friendName ?: "", "UTF-8")
        } catch (e: Exception) {
            Log.e("ChatScreen", "Error decodificando nombre del amigo: ${e.message}")
            friendName ?: ""
        }
    }

    val decodedFriendPhoto = remember(friendPhoto) {
        try {
            URLDecoder.decode(friendPhoto ?: "", "UTF-8")
        } catch (e: Exception) {
            Log.e("ChatScreen", "Error decodificando foto del amigo: ${e.message}")
            ""
        }
    }

    // Estados para el chat
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    val processingMap = remember { mutableStateMapOf<String, Boolean>() }

    var currentMessage by remember { mutableStateOf(TextFieldValue("")) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Estado para el usuario actual
    var currentUserId by remember { mutableStateOf("") }

    // Estados para la información del amigo
    var friendStatus by remember { mutableStateOf("offline") }
    val friendInitials = remember(decodedFriendName) {
        decodedFriendName.take(1).uppercase()
    }

    // Estado para menú desplegable y diálogo de confirmación
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Estado para scroll
    val scrollState = rememberLazyListState()
    var isScrollAtBottom by remember { mutableStateOf(true) }

    // Color del perfil del amigo
    val colorManager = remember { UserColorManager(context) }
    val friendProfileColor = remember(friendId) {
        friendId?.let { colorManager.getUserProfileColor(it) } ?: Color(0xFF607D8B)
    }

    // Función para desplazarse al final de la lista
    suspend fun scrollToBottom() {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
            isScrollAtBottom = true
        }
    }

    // Listener para detectar si el scroll está al final
    LaunchedEffect(scrollState, messages) {
        snapshotFlow { scrollState.firstVisibleItemIndex }
            .collect { firstVisibleIndex ->
                val atBottom = messages.isEmpty() ||
                        (firstVisibleIndex + scrollState.layoutInfo.visibleItemsInfo.size >= messages.size - 1)
                isScrollAtBottom = atBottom
            }
    }

    // Obtener ID del usuario actual
    LaunchedEffect(Unit) {
        val userData = ApiClient.getUserData(context)
        if (userData != null) {
            currentUserId = (userData["id"] ?: "").toString()
            Log.d("ChatScreen", "ID de usuario actual: $currentUserId")
        }
    }

    // Función para cargar mensajes con mejora de scroll
    suspend fun loadMessages() {
        if (friendId == null) {
            isLoading = false
            error = "ID de amigo no válido"
            return
        }

        try {
            Log.d("ChatScreen", "Cargando mensajes de chat con amigo ID: $friendId")

            // Guardar el número actual de mensajes para comparación
            val currentMessageCount = messages.size
            val userWasAtBottom = isScrollAtBottom

            val response = ApiClient.getChatConversation(friendId, context)

            if (response != null && response.has("messages")) {
                val messagesArray = response.getJSONArray("messages")
                val newMessages = mutableListOf<ChatMessage>()
                val dateFormat =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

                Log.d("ChatScreen", "Procesando ${messagesArray.length()} mensajes")

                for (i in 0 until messagesArray.length()) {
                    try {
                        val messageObj = messagesArray.getJSONObject(i)
                        val parsedDate = try {
                            dateFormat.parse(messageObj.getString("sent_at"))
                        } catch (e: Exception) {
                            Date()
                        }

                        val message = ChatMessage(
                            id = messageObj.getString("id"),
                            senderId = messageObj.getString("user1_id"),
                            receiverId = messageObj.getString("user2_id"),
                            content = messageObj.getString("txt_message"),
                            timestamp = parsedDate ?: Date(),
                            isRead = messageObj.getBoolean("read"),
                            sharedContent = messageObj.optString("shared_content", null)
                        )

                        newMessages.add(message)
                    } catch (e: JSONException) {
                        Log.e("ChatScreen", "Error procesando mensaje: ${e.message}")
                    }
                }

                // Ordenar mensajes por timestamp
                withContext(Dispatchers.Main) {
                    messages = newMessages.sortedBy { it.timestamp }
                    Log.d("ChatScreen", "Cargados ${messages.size} mensajes")
                    error = null

                    // Desplazarse al final solo si:
                    // 1. No había mensajes antes (primera carga)
                    // 2. Hay nuevos mensajes y el usuario estaba ya en el final
                    // 3. El usuario era quien estaba enviando un mensaje (isScrollAtBottom será true)
                    val shouldScrollToBottom = currentMessageCount == 0 ||
                            (messages.size > currentMessageCount && userWasAtBottom)

                    if (shouldScrollToBottom) {
                        Log.d(
                            "ChatScreen",
                            "Auto-scrolling to bottom: initialLoad=${currentMessageCount == 0}, newMessages=${messages.size > currentMessageCount}, wasAtBottom=$userWasAtBottom"
                        )
                        scrollToBottom()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    error = "No se pudieron cargar los mensajes"
                    Log.e("ChatScreen", "Error cargando mensajes: respuesta nula o sin mensajes")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                error = "Error: ${e.message}"
                Log.e("ChatScreen", "Excepción cargando mensajes: ${e.message}")
            }
        } finally {
            withContext(Dispatchers.Main) {
                isLoading = false
                Log.d("ChatScreen", "Carga de mensajes finalizada, isLoading=$isLoading")
            }
        }
    }

    // Función para enviar un mensaje
    fun sendMessage() {
        if (friendId == null || currentMessage.text.trim().isEmpty()) return

        val messageText = currentMessage.text.trim()
        currentMessage = TextFieldValue("")
        focusManager.clearFocus()

        // Generar ID temporal único
        val tempId = "temp-${System.currentTimeMillis()}"

        // Mensaje temporal
        val tempMessage = ChatMessage(
            id = tempId,
            senderId = currentUserId,
            receiverId = friendId,
            content = messageText,
            timestamp = Date(),
            isRead = false
        )

        // Actualizar UI inmediatamente con mensaje temporal
        messages = messages + tempMessage

        // Scroll al final
        coroutineScope.launch {
            scrollToBottom()
        }

        // Enviar mensaje a la API
        coroutineScope.launch {
            try {
                val response = ApiClient.sendChatMessage(friendId, messageText, context)
                if (response != null) {
                    // En lugar de recargar todos los mensajes, actualizamos solo este mensaje
                    try {
                        // Extraer el ID real del mensaje enviado desde la respuesta
                        val realMessageId = response.optString("messageId", "")
                        if (realMessageId.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                // Reemplazar el mensaje temporal con el mensaje real
                                messages = messages.map {
                                    if (it.id == tempId) {
                                        // Actualizar el mensaje con el ID real y marcar como enviado
                                        it.copy(id = realMessageId, isRead = false)
                                    } else {
                                        it
                                    }
                                }

                                // Programar una recarga completa después de un breve retraso
                                // para sincronizar con otros posibles cambios en el servidor
                                delay(500)
                                loadMessages()
                            }
                        } else {
                            // Si no se obtuvo un ID, recargamos mensajes después de un retraso
                            delay(1000)
                            loadMessages()
                        }
                    } catch (e: Exception) {
                        Log.e("ChatScreen", "Error actualizando mensaje: ${e.message}")
                        // En caso de error, recargar todos los mensajes después de un retraso
                        delay(1000)
                        loadMessages()
                    }
                } else {
                    Log.e("ChatScreen", "Error al enviar mensaje, respuesta nula")
                    // En caso de error, mantener el mensaje temporal pero marcarlo como fallido
                    withContext(Dispatchers.Main) {
                        // Opcionalmente, podrías cambiar el aspecto visual del mensaje para indicar error
                        // Por ejemplo, cambiando el color o añadiendo un icono de error
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatScreen", "Error enviando mensaje: ${e.message}")
            }
        }
        currentMessage = TextFieldValue("")
    }

    // Modifica la función sendPlaylistMessage en ChatScreen.kt
    fun sendPlaylistMessage(playlistId: String, playlistTitle: String, playlistImage: String?) {
        if (friendId == null) return

        // Mensaje más simple y amigable
        val messageText = "¡Mira esta playlist!"

        // Crear JSON con información de la playlist
        val sharedContent = JSONObject().apply {
            put("type", "playlist")
            put("id", playlistId)
            put("title", playlistTitle)
            if (!playlistImage.isNullOrEmpty()) {
                put("image", playlistImage)
            }
        }.toString()

        // Generar ID temporal único
        val tempId = "temp-${System.currentTimeMillis()}"

        // Mensaje temporal con la información estructurada
        val tempMessage = ChatMessage(
            id = tempId,
            senderId = currentUserId,
            receiverId = friendId,
            content = messageText,  // Mensaje simple
            timestamp = Date(),
            isRead = false,
            sharedContent = sharedContent  // Contenido enriquecido con datos de la playlist
        )

        // Actualizar UI inmediatamente con mensaje temporal
        messages = messages + tempMessage

        // Scroll al final
        coroutineScope.launch {
            scrollToBottom()
        }

        // Enviar mensaje a la API
        coroutineScope.launch {
            try {
                val response = ApiClient.sendChatMessageWithSharedContent(
                    friendId, messageText, sharedContent, context
                )

                if (response != null) {
                    try {
                        // Extraer el ID real del mensaje enviado
                        val realMessageId = response.optString("messageId", "")
                        if (realMessageId.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                // Reemplazar el mensaje temporal con el real
                                messages = messages.map {
                                    if (it.id == tempId) {
                                        it.copy(id = realMessageId)
                                    } else {
                                        it
                                    }
                                }

                                // Recargar después de un breve retraso
                                delay(500)
                                loadMessages()
                            }
                        } else {
                            delay(1000)
                            loadMessages()
                        }
                    } catch (e: Exception) {
                        Log.e("ChatScreen", "Error actualizando mensaje compartido: ${e.message}")
                        delay(1000)
                        loadMessages()
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatScreen", "Error enviando mensaje con playlist: ${e.message}")
            }
        }
    }

    // Cargar mensajes iniciales
    LaunchedEffect(friendId) {
        isLoading = true
        loadMessages()
    }

    // Polling para actualizar mensajes automáticamente
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // esperar 1 segundo1
            if (friendId != null && !isLoading) {
                try {
                    loadMessages()
                } catch (e: Exception) {
                    Log.e("ChatScreen", "Error en polling de mensajes: ${e.message}")
                }
            }
        }
    }

    // Efectuar el compartir playlist automáticamente si hay parámetros
    LaunchedEffect(sharePlaylistId, sharePlaylistTitle) {
        if (!sharePlaylistId.isNullOrEmpty() && !sharePlaylistTitle.isNullOrEmpty()) {
            // Esperar a que se cargue el ID del usuario actual
            delay(500) // Pequeña espera para asegurar que currentUserId está cargado
            sendPlaylistMessage(
                playlistId = sharePlaylistId,
                playlistTitle = sharePlaylistTitle,
                playlistImage = sharePlaylistImage
            )
        }
    }

    // Diálogo de confirmación para eliminar amigo
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Eliminar amigo") },
            text = { Text("¿Estás seguro de que quieres eliminar a $decodedFriendName de tu lista de amigos?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val response = ApiClient.unfollowFriend(friendId ?: "", context)
                                if (response != null) {
                                    navController.popBackStack()
                                }
                            } catch (e: Exception) {
                                Log.e("ChatScreen", "Error eliminando amigo: ${e.message}")
                            }
                            showDeleteConfirmDialog = false
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
            }
        )
    }

    // UI principal
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Foto del amigo o iniciales
                        if (decodedFriendPhoto.isEmpty()) {
                            // Mostrar iniciales con color de perfil
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(friendProfileColor, CircleShape)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = friendInitials,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else {
                            // Mostrar foto
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(ApiClient.getImageUrl(decodedFriendPhoto))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Foto de perfil",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Nombre del amigo
                        Text(
                            text = decodedFriendName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    // Menú de opciones
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Más opciones"
                            )
                        }

                        // Menú desplegable
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
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
            )
        },
        bottomBar = {
            // Barra de entrada de mensaje
            Surface(
                modifier = Modifier.fillMaxWidth(),
                //tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Campo de texto
                    OutlinedTextField(
                        value = currentMessage,
                        onValueChange = { currentMessage = it },
                        placeholder = { Text("Escribe un mensaje...") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (currentMessage.text.isNotBlank()) {
                                sendMessage()
                            }
                        }),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Botón de enviar
                    IconButton(
                        onClick = {
                            if (currentMessage.text.isNotBlank()) {
                                sendMessage()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Enviar mensaje",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        // Contenido principal
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading && messages.isEmpty()) {
                // Indicador de carga
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (error != null && messages.isEmpty()) {
                // Mensaje de error
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = error ?: "Error desconocido",
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            error = null
                            loadMessages()
                        }
                    }) {
                        Text("Reintentar")
                    }
                }
            } else if (messages.isEmpty()) {
                // Estado vacío (sin mensajes)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No hay mensajes aún",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Envía un mensaje para iniciar la conversación",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Lista de mensajes
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    state = scrollState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        val sharedJson = remember(message.sharedContent) {
                            message.sharedContent
                                ?.let { runCatching { JSONObject(it) }.getOrNull() }
                        }

                        val processing = processingMap[message.id] ?: false

                        if (sharedJson?.optString("type") == "collaboration_request") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        text = message.content,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (processing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            processingMap[message.id] = true

                                                            val keysList = sharedJson.keys().asSequence().toList()
                                                            Log.d("ChatScreen", "sharedJson keys = $keysList")

                                                            // 1) Extrae y loggea el playlist_id
                                                            val plId = sharedJson.optString("playlist_id")
                                                            Log.d("ChatScreen", ">> attempting rejectCollab with playlist_id='$plId'")

                                                            // 2) Si plId no está vacío, llama a la API
                                                            plId.takeIf(String::isNotEmpty)?.let { id ->
                                                                ApiClient.rejectCollaboration(id, context)
                                                            } ?: run {
                                                                Log.e("ChatScreen", "playlist_id vacío, no llamo a la API")
                                                            }

                                                            // 3) Refresca y quita el flag
                                                            loadMessages()
                                                            processingMap[message.id] = false
                                                        }
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.error
                                                    ),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        "Rechazar",
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }

                                                Spacer(Modifier.width(16.dp))

                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            processingMap[message.id] = true

                                                            val keysList = sharedJson.keys().asSequence().toList()
                                                            Log.d("ChatScreen", "sharedJson keys = $keysList")

                                                            // 1) Extrae y loggea el playlist_id
                                                            val plId = sharedJson.optString("playlist_id")
                                                            Log.d("ChatScreen", ">> attempting acceptCollab with playlist_id='$plId'")

                                                            // 2) Si plId no está vacío, llama a la API
                                                            plId.takeIf(String::isNotEmpty)?.let { id ->
                                                                ApiClient.acceptCollaboration(id, context)
                                                            } ?: run {
                                                                Log.e("ChatScreen", "playlist_id vacío, no llamo a la API")
                                                            }

                                                            // 3) Refresca y quita el flag
                                                            loadMessages()
                                                            processingMap[message.id] = false
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    Text(
                                                        "Aceptar",
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            ChatBubble(
                                navController = navController,
                                message = message,
                                currentUserId = currentUserId,
                                friendName = decodedFriendName,
                                friendPhoto = decodedFriendPhoto,
                                friendProfileColor = friendProfileColor
                            )
                        }
                    }
                }

                // Botón para ir al final (si hay suficientes mensajes)
                if (messages.size > 10 && !isScrollAtBottom) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                scrollToBottom()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(40.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Ir al final",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    navController: NavController,
    message: ChatMessage,
    currentUserId: String,
    friendName: String,
    friendPhoto: String,
    friendProfileColor: Color
) {
    // Obtener el ancho de la pantalla para calcular el 60%
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val maxBubbleWidth = screenWidth * 0.6f  // 60% del ancho de la pantalla

    // Determinar si el mensaje fue enviado por el usuario actual
    val isFromCurrentUser = message.senderId == currentUserId
    
    Log.d("ChatBubble", "Message from user ${message.senderId}, currentUserId=$currentUserId, isFromCurrentUser=$isFromCurrentUser")
    
    // Colores y alineación
    val bubbleColor = if (isFromCurrentUser) 
        MaterialTheme.colorScheme.primary 
    else 
        MaterialTheme.colorScheme.surfaceVariant
    
    val textColor = if (isFromCurrentUser) 
        MaterialTheme.colorScheme.onPrimary 
    else 
        MaterialTheme.colorScheme.onSurfaceVariant
    
    // Formatear hora
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(message.timestamp)
    
    // Estado para controlar si el contenido compartido es válido
    val sharedContentData = remember(message.sharedContent) {
        parseSharedContent(message.sharedContent)
    }
    
    // Burbuja de chat
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar (solo para mensajes recibidos)
            if (!isFromCurrentUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(friendProfileColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (friendPhoto.isEmpty()) {
                        Text(
                            text = friendName.take(1).uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(ApiClient.getImageUrl(friendPhoto))
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Burbuja de mensaje usando el ancho calculado
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = maxBubbleWidth)  // Aquí usamos el ancho relativo
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                            bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .padding(12.dp)
            ) {
                Column {
                    // Renderizar el contenido compartido si es válido
                    if (sharedContentData != null) {
                        SharedPlaylistPreview(
                            navController = navController,
                            playlistId = sharedContentData.id,
                            playlistTitle = sharedContentData.title,
                            playlistImage = sharedContentData.image,
                            contentColor = textColor
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Mostrar el mensaje (texto)
                    Text(
                        text = message.content,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeString,
                            color = textColor.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        if (isFromCurrentUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            Icon(
                                imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                contentDescription = if (message.isRead) "Leído" else "Enviado",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            // Spacer para mantener el espacio en el lado opuesto al avatar
            if (isFromCurrentUser) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

// Clase auxiliar para almacenar información de contenido compartido
data class SharedContentInfo(
    val type: String,
    val id: String,
    val title: String,
    val image: String
)

// Función para procesar el contenido compartido fuera del composable
fun parseSharedContent(sharedContent: String?): SharedContentInfo? {
    if (sharedContent == null || sharedContent.isEmpty() || sharedContent == "null") {
        return null
    }
    
    return try {
        val json = JSONObject(sharedContent)
        
        if (json.has("type") && json.getString("type") == "playlist") {
            SharedContentInfo(
                type = "playlist",
                id = json.getString("id"),
                title = json.getString("title"),
                image = json.optString("image", "")
            )
        } else {
            null
        }
    } catch (e: JSONException) {
        Log.e("ChatBubble", "Error al parsear contenido compartido: ${e.message}")
        null
    }
}

@Composable
fun SharedPlaylistPreview(
    navController: NavController,
    playlistId: String,
    playlistTitle: String,
    playlistImage: String,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                // Navegar a la playlist al hacer clic
                navController.navigate("playlist/$playlistId")
            },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(ApiClient.getImageUrl(playlistImage))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Imagen de playlist",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.defaultplaylist),
                    placeholder = painterResource(R.drawable.defaultplaylist)
                )
            }
            
            // Información de la playlist
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Playlist",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                
                Text(
                    text = playlistTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}


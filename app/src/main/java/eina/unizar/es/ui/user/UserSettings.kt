package eina.unizar.es.ui.user

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.VectorProperty
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import eina.unizar.es.R
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicapp.ui.theme.VibraBlue
import com.example.musicapp.ui.theme.VibraDarkGrey
import com.example.musicapp.ui.theme.VibraLightGrey
import com.example.musicapp.ui.theme.VibraMediumGrey
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.data.model.network.ApiClient.getUserData
import eina.unizar.es.data.model.network.ApiClient.updateUserProfile
import eina.unizar.es.data.model.network.ApiClient.uriToBase64
import eina.unizar.es.ui.main.Rubik
import eina.unizar.es.ui.player.MusicPlayerViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.format.TextStyle

@Composable
fun UserSettings(navController: NavController, isPremium: Boolean, viewModel: MusicPlayerViewModel) {
    val backgroundColor = Color(0xFF000000)   // Fondo negro
    val cardColor = Color(0xFF121212)         // Tarjetas gris oscuro
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isPremium by remember { mutableStateOf(isPremium) }  // Estado para el plan del usuario

    // Cargar datos del usuario cuando se abra la pantalla
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val userData = getUserData(context)
            if (userData != null) {
                isPremium = userData["is_premium"] as Boolean
                Log.d("UserData", "isPremium asignado: $isPremium") // Verifica si is_premium se asigna correctamente
            }
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VibraDarkGrey)
            .verticalScroll(rememberScrollState())
    ) {
        // Cabecera superior con foto de perfil y nombre
        HeaderSection()

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Tarjeta del plan
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VibraMediumGrey),
                modifier = Modifier.fillMaxWidth()
                    .border(1.dp, VibraBlue, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tu plan", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (isPremium) "Premium" else "Gratuito",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        if (isPremium) "Tu próxima factura es de 4,99 € y se emite al inicio de cada mes."
                        else "Plan gratuito disponible para siempre.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sección "Gestión Perfil"
            SettingsSection(title = "Gestión Perfil", items = listOf(
                Pair("Editar Perfil", "perfilEdit"),
            ), navController)

            Spacer(modifier = Modifier.height(24.dp))

            // Sección "Suscripción"
            SettingsSection(title = "Suscripción", items = listOf(
                Pair("Administrar Suscripciones", "plans"),
            ), navController)

            Spacer(modifier = Modifier.height(24.dp))

            // Sección Cerrar Sesión
            ActionButton(
                text = "Cerrar Sesión",
                icon = Icons.Default.ExitToApp,
                onClickAction = {
                    coroutineScope.launch {
                        viewModel.cleanupOnLogout()  // Reinicia el reproductor
                        ApiClient.logoutUser(context, navController)  // Llama a la función logout
                    }
                }
            )

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
fun HeaderSection() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var nickname by remember { mutableStateOf("Cargando...") }  // Estado inicial
    var userPicture by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var profileColor by remember { mutableStateOf(Color(0xFF607D8B)) }
    var initials by remember { mutableStateOf("") }

    // Llamada a la API cuando la pantalla se carga
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val userData = getUserData(context)
            if (userData != null) {
                nickname = (userData["nickname"] ?: "Nickname").toString()
                userPicture = (userData["user_picture"] ?: "").toString()
                userId = (userData["id"] ?: "").toString()

                Log.d("UserData", "userPicture asignado: $userPicture")
                // Genera o recupera el color del perfil
                val colorManager = UserColorManager(context)
                profileColor = colorManager.getUserProfileColor(userId)

                // Obtiene las iniciales
                initials = getInitial(nickname)
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFB3D9FF))
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            // Aqui podemos sacar la URI de la imagen del usuario
            ProfileImagePicker(
                userId = userId,
                userPicture = userPicture,
                initials = initials,
                profileColor = profileColor,
                fromMenu = false,
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Nombre de usuario
            Text(
                text = nickname.capitalize(),
                fontFamily = Rubik,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.3f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                ),
                color = Color(0xFF2E2E2E)
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
    // Efecto de Derrame con Ondas en la parte inferior del Header
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp) // Ajusta la altura del efecto
    ) {
        val width = size.width
        val height = size.height

        val path = Path().apply {
            moveTo(0f, 0f) // Inicio en la esquina izquierda

            cubicTo(
                width * 0.25f, height * 1.5f, // Punto de control 1
                width * 0.75f, height * -0.5f, // Punto de control 2
                width, height.toFloat() // Fin en la esquina derecha
            )

            lineTo(width, 0f)
            lineTo(0f, 0f)
            close()
        }

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFB3D9FF), Color(0xFFB3D9FF)), // Fusión con el fondo negro
                startY = 0f,
                endY = height
            ),
            style = Fill
        )
    }
}

// Botón de acción
@Composable
fun ActionButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClickAction: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VibraMediumGrey),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickAction() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = text, tint = Color(0xFFFF6B6B))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text, color = Color(0xFFFF6B6B), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Sección de ajustes con opciones navegables
@Composable
fun SettingsSection(title: String, items: List<Pair<String, String>>, navController: NavController) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VibraMediumGrey),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            items.forEach { (text, route) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(route) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = text, tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ProfileImagePicker(
    userId: String,
    userPicture: String?,
    initials: String,
    profileColor: Color,
    currentNickname: String = "",
    onUpdateSuccess: (JSONObject) -> Unit = {},
    onUpdateError: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    fromMenu: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var imageLoadFailed by remember { mutableStateOf(false) }
    var currentUserPicture by remember { mutableStateOf(userPicture) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri ->
            // Mostrar inmediatamente la imagen seleccionada
            selectedImageUri = imageUri

            coroutineScope.launch {
                isLoading = true
                try {
                    val base64Image = uriToBase64(context, imageUri)
                    base64Image?.let {
                        val response = updateUserProfile(
                            userId = userId,
                            nickname = currentNickname.takeIf { it.isNotBlank() },
                            profileImage = it
                        )

                        response?.let { jsonResponse ->
                            if (jsonResponse.has("error")) {
                                onUpdateError(jsonResponse.getString("error"))
                            } else {
                                currentUserPicture = jsonResponse.optString("user_picture", null)
                                imageLoadFailed = false
                                onUpdateSuccess(jsonResponse)
                            }
                        } ?: run {
                            onUpdateError("Error desconocido al actualizar el perfil")
                        }
                    } ?: run {
                        onUpdateError("No se pudo procesar la imagen")
                    }
                } catch (e: Exception) {
                    onUpdateError("Error: ${e.localizedMessage ?: "Error desconocido"}")
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Contenido de imagen según los estados
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            selectedImageUri != null -> {
                // Prioridad 1: Mostrar imagen recién seleccionada
                Image(
                    painter = rememberAsyncImagePainter(selectedImageUri),
                    contentDescription = "Nueva imagen de perfil",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            !userPicture.isNullOrBlank() && !imageLoadFailed -> {
                // Prioridad 2: Mostrar imagen del servidor si existe y no ha fallado
                val imageUrl = getImageUrl(userPicture ?: "", "/defaultuser.jpg")

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Imagen de perfil actual",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    onError = { imageLoadFailed = true }
                )
            }
            else -> {
                // Fallback: Mostrar iniciales si no hay imagen o falló la carga
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = profileColor, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = if (fromMenu) 20.sp else 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (!fromMenu) {
            // Botón de edición
            IconButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar imagen",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
package eina.unizar.es.ui.user

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.data.model.network.ApiClient.getUserData
import eina.unizar.es.ui.player.MusicPlayerViewModel
import kotlin.random.Random
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import com.example.musicapp.ui.theme.VibraDarkGrey
import com.example.musicapp.ui.theme.VibraLightGrey

// Función que obtiene la inicial del nickname
fun getInitial(nickname: String?): String {
    return nickname?.take(1)?.toUpperCase() ?: ""
}

@Composable
fun UserProfileMenu(
    navController: NavController,
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var userPicture by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var profileColor by remember { mutableStateOf(Color(0xFF607D8B)) }
    var initials by remember { mutableStateOf("") }
    var isPremium by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val userData = getUserData(context)
            if (userData != null) {
                nickname = (userData["nickname"] ?: "Nickname").toString()
                userPicture = (userData["user_picture"] ?: "").toString()
                userId = (userData["id"] ?: "").toString()
                isPremium = userData["is_premium"] as Boolean
                val colorManager = UserColorManager(context)
                profileColor = colorManager.getUserProfileColor(userId)
            }
            initials = getInitial(nickname)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Botón de perfil con efecto de hover
        CompositionLocalProvider(LocalRippleTheme provides NoRippleTheme) {
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .size(48.dp)
                    .clickable { expanded = !expanded },
            ) {
                ProfileImagePicker(
                    userId = userId,
                    userPicture = userPicture,
                    initials = initials,
                    profileColor = profileColor,
                    fromMenu = true,
                )
            }
        }

        // Menú desplegable mejorado
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(200.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2A2A2A),
                            Color(0xFF1E1E1E)
                        )
                    ),
                    shape = RoundedCornerShape(5.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF404040),
                            Color(0xFF303030)
                        )
                    ),
                    shape = RoundedCornerShape(5.dp)
                ),
                properties = PopupProperties(
                    focusable = true,
                    clippingEnabled = true  // Asegúrate de que esto esté habilitado
                )
        ) {
            // Header con información del usuario
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = nickname,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // User premium status badge - versión profesional y minimalista
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .width(90.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isPremium)
                                Brush.horizontalGradient(listOf(Color(0xFFB0C4DE), Color(0xFFB0C4DF)))
                            else
                                Brush.horizontalGradient(listOf(Color(0xFFFFAFC1), Color(0xFFFFAFC7)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isPremium) Icons.Outlined.Star else Icons.Outlined.Person,
                            contentDescription = null,
                            tint = Color(0xFF2A2A2A).copy(alpha = 0.9f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPremium) "Premium" else "Gratuito",
                            color = Color(0xFF2A2A2A).copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }

            Divider(
                color = Color(0xFF404040),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Items del menú
            val menuItems = listOf(
                MenuItem(
                    label = "Ajustes",
                    icon = Icons.Outlined.Settings,
                    color = Color(0xFFA0A0A0),
                    action = { navController.navigate("settings") }
                ),
                MenuItem(
                    label = "Amigos",
                    icon = Icons.Outlined.People,
                    color = Color(0xFFA0A0A0),
                    action = { navController.navigate("friends") }
                ),
                MenuItem(
                    label = "Cerrar Sesión",
                    icon = Icons.Outlined.Logout,
                    color = Color(0xFFFF6B6B),
                    action = {
                        coroutineScope.launch {
                            viewModel.cleanupOnLogout()
                            ApiClient.logoutUser(context, navController)
                        }
                    }
                )
            )

            menuItems.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            item.label,
                            color = item.color,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        expanded = false
                        item.action()
                    },
                    leadingIcon = {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = item.color,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),
                    colors = MenuDefaults.itemColors(
                        textColor = item.color,
                        leadingIconColor = item.color
                    )
                )
            }
        }
    }
}

// Modelo para los items del menú
private data class MenuItem(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val action: () -> Unit
)

// Tema sin ripple para el botón
private object NoRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = Color.Unspecified

    @Composable
    override fun rippleAlpha() = RippleAlpha(0.0f, 0.0f, 0.0f, 0.0f)
}


class UserColorManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("UserColorPrefs", Context.MODE_PRIVATE)

    fun getUserProfileColor(userId: String): Color {
        // Intenta obtener el color almacenado para este usuario
        val storedColorHex = sharedPreferences.getString("user_color_$userId", null)

        return if (storedColorHex != null) {
            // Si ya existe un color para este usuario, lo convierte y devuelve
            try {
                Log.d("UserColorManager", "Retrieved existing color for user $userId: $storedColorHex")
                Color(android.graphics.Color.parseColor(storedColorHex))
            } catch (e: Exception) {
                // Si hay error al parsear, genera un nuevo color
                Log.w("UserColorManager", "Error parsing stored color for user $userId, generating new color")
                generateAndStoreRandomColor(userId)
            }
        } else {
            // Si es un usuario nuevo, genera y almacena un color completamente aleatorio
            Log.i("UserColorManager", "Generating new color for new user $userId")
            generateAndStoreRandomColor(userId)
        }
    }

    private fun generateAndStoreRandomColor(userId: String): Color {
        // Genera un color completamente aleatorio con algunas restricciones para evitar colores muy claros
        val newColor = generateUniqueRandomColor()

        // Almacena el color
        sharedPreferences.edit().putString("user_color_$userId", newColor).apply()

        // Convierte y devuelve
        return try {
            Log.d("UserColorManager", "Generated and stored new color for user $userId: $newColor")
            Color(android.graphics.Color.parseColor(newColor))
        } catch (e: Exception) {
            Log.e("UserColorManager", "Failed to parse generated color: ${e.message}")
            Color(0xFF607D8B) // Color por defecto si falla
        }
    }

    // Genera un color aleatorio con mayor saturación y brillo
    private fun generateUniqueRandomColor(): String {
        // Genera componentes de color con mayor saturación
        val hue = Random.nextFloat() * 360f
        val saturation = 0.5f + Random.nextFloat() * 0.5f  // Entre 0.5 y 1.0
        val brightness = 0.5f + Random.nextFloat() * 0.5f  // Entre 0.5 y 1.0

        // Convierte HSV a RGB
        val hsv = floatArrayOf(hue, saturation, brightness)
        val color = android.graphics.Color.HSVToColor(hsv)

        // Convierte a formato hexadecimal
        return String.format("#%06X", (0xFFFFFF and color))
    }

    // Método para limpiar colores de usuarios antiguos (opcional)
    fun clearOldUserColors() {
        sharedPreferences.edit().clear().apply()
        Log.i("UserColorManager", "Cleared all stored user colors")
    }
}

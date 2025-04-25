package eina.unizar.es.ui.user

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
import kotlinx.coroutines.launch
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.data.model.network.ApiClient.getLikedPlaylists
import eina.unizar.es.data.model.network.ApiClient.getUserData
import eina.unizar.es.ui.player.MusicPlayerViewModel
import kotlin.random.Random

// Función que obtiene la inicial del nickname
fun getInitial(nickname: String?): String {
    return nickname?.take(1)?.toUpperCase() ?: ""
}

// In your composable, you would use it like this:
@Composable
fun UserProfileMenu(navController: NavController, viewModel: MusicPlayerViewModel, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var userPicture by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var profileColor by remember { mutableStateOf(Color(0xFF607D8B)) }
    var initials by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val userData = getUserData(context)
            if (userData != null) {
                nickname = (userData["nickname"] ?: "Nickname").toString()
                userPicture = (userData["user_picture"] ?: "").toString()
                userId = (userData["id"] ?: "").toString()

                // Get a persistent color for this specific user
                val colorManager = UserColorManager(context)
                profileColor = colorManager.getUserProfileColor(userId)
            }
            Log.d("UserData", "Valores asignados: $userPicture $nickname $userId") // Verifica si is_premium se asigna correctamente
            initials = getInitial(nickname)
        }
    }

    // Rest of the function remains the same as in the original code...
    Box(
        contentAlignment = Alignment.TopStart
    ) {
        IconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .size(48.dp)
        ) {
            if (userPicture.isEmpty()) { // !!! Ojo la negacion para docker!!!
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(userPicture)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color = profileColor)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(165.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF252525))
                .border(
                    width = 1.5.dp,
                    color = Color(0xFF3A3A3A),
                    shape = RoundedCornerShape(6.dp)
                )
        ) {
            // Items con altura compacta
            val itemModifier = Modifier.height(40.dp)

            // Ajustes
            DropdownMenuItem(
                text = {
                    Text(
                        "Ajustes",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 13.sp
                    )
                },
                onClick = {
                    expanded = false
                    navController.navigate("settings")
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,  // Más minimalista sin descripción
                        tint = Color(0xFFA0A0A0),
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = itemModifier.padding(horizontal = 8.dp)
            )

            // Amigos
            DropdownMenuItem(
                text = {
                    Text(
                        "Amigos",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 13.sp
                    )
                },
                onClick = {
                    expanded = false
                    navController.navigate("friends")
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFFA0A0A0),
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = itemModifier.padding(horizontal = 8.dp)
            )

            // Divisor sutil
            Divider(
                color = Color(0xFF3A3A3A),
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // Cerrar sesión
            DropdownMenuItem(
                text = {
                    Text(
                        "Cerrar Sesión",
                        color = Color(0xFFFF6B6B),
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 13.sp
                    )
                },
                onClick = {
                    expanded = false
                    coroutineScope.launch {
                        viewModel.cleanupOnLogout()
                        ApiClient.logoutUser(context, navController)
                    }
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = itemModifier.padding(horizontal = 8.dp)
            )
        }
    }
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

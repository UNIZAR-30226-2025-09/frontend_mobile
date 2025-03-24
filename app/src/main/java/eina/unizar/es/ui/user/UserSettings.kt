package eina.unizar.es.ui.user

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import eina.unizar.es.R
import androidx.compose.ui.platform.LocalContext
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.data.model.network.ApiClient.getUserData
import eina.unizar.es.ui.main.Rubik
import kotlinx.coroutines.launch

@Composable
fun UserSettings(navController: NavController, isPremium: Boolean) {
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
            .background(backgroundColor)
    ) {
        // Cabecera superior con foto de perfil y nombre
        HeaderSection()

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Tarjeta del plan
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier.fillMaxWidth()
                    .border(1.dp, Color(0xFF79E2FF), RoundedCornerShape(16.dp))
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

    // Llamada a la API cuando la pantalla se carga
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val userData = getUserData(context)
            if (userData != null) {
                nickname =
                    (userData["nickname"] ?: "Usuario").toString()  // Si no hay nickname, usa "Usuario"
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
            // Imagen de perfil
            Image(
                painter = painterResource(id = R.drawable.kanyeperfil), // ⚠️ Reemplaza con la imagen de perfil
                contentDescription = "Imagen de perfil",
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Nombre de usuario
            Text(nickname, color = Color(0xFF2E2E2E), fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = Rubik)

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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        modifier = Modifier.fillMaxWidth()
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

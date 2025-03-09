package eina.unizar.es.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun UserProfileMenu(navController: NavController, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopEnd // Icono alineado a la derecha
    ) {
        // Icono de usuario
        IconButton(
            onClick = { expanded = true },
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Usuario",
                tint = Color.White,
                modifier = Modifier.size(50.dp) // Aumentamos el tamaño del icono
            )
        }

        // Menú desplegable con bordes redondeados y gris claro
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp)) // Bordes redondeados
                .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(5.dp)) // 🔹 Borde gris claro
                .background(Color(0xFF1E1E1E)) // Fondo oscuro
        ) {
            DropdownMenuItem(
                text = { Text("Ajustes", color = Color.White) },
                onClick = {
                    expanded = false
                    navController.navigate("settings")
                },
                leadingIcon = {
                    Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = Color.White)
                }
            )
            DropdownMenuItem(
                text = { Text("Amigos", color = Color.White) },
                onClick = {
                    expanded = false
                    navController.navigate("friends")
                },
                leadingIcon = {
                    Icon(Icons.Default.Face, contentDescription = "Amigos", tint = Color.White)
                }
            )
            Divider(
                color = Color(0x33CCCCCC), // Línea separadora en gris claro
                thickness = 1.dp,
                modifier = Modifier
                    .width(120.dp) // La línea es más corta, dejando los extremos vacíos
                    .align(Alignment.CenterHorizontally) // Centra la línea dentro del menú
            )

            DropdownMenuItem(
                text = { Text("Cerrar Sesión", color = Color.Red) },
                onClick = {
                    expanded = false
                    // Lógica para cerrar sesión
                },
                leadingIcon = {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar Sesión", tint = Color.Red)
                }
            )
        }
    }
}

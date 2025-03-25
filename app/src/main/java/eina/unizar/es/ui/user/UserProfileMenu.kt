package eina.unizar.es.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import eina.unizar.es.data.model.network.ApiClient

@Composable
fun UserProfileMenu(navController: NavController, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        contentAlignment = Alignment.TopStart
    ) {
        IconButton(
            onClick = { expanded = true },
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Usuario",
                tint = Color.White,
                modifier = Modifier.size(50.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(5.dp))
                .background(Color(0xFF1E1E1E))
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
                    //navController.navigate("friends")
                    navController.navigate("chat")
                },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = "Amigos", tint = Color.White)
                }
            )
            Divider(
                color = Color(0x33CCCCCC),
                thickness = 1.dp,
                modifier = Modifier
                    .width(120.dp)
                    .align(Alignment.CenterHorizontally)
            )

            DropdownMenuItem(
                text = { Text("Cerrar Sesión", color = Color(0xFFFF6B6B)) },
                onClick = {
                    expanded = false
                    coroutineScope.launch {
                        ApiClient.logoutUser(context, navController)
                    }
                },
                leadingIcon = {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar Sesión", tint = Color(0xFFFF6B6B))
                }
            )
        }
    }
}

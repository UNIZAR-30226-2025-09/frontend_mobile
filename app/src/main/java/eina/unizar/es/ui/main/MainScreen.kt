package eina.unizar.es.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun MainScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bienvenido a Vibra",
            fontSize = 24.sp,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Custom button with a theme
        Button(
            onClick = { navController.navigate("player") },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Blue, // Custom background color
                contentColor = Color.White// Custom text color
            ),
            shape = MaterialTheme.shapes.medium, // Custom shape
            modifier = Modifier.padding(8.dp) // Padding for the button
        ) {
            Text(text = "Comenzar")
        }
    }
}

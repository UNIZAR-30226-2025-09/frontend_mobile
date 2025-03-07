package eina.unizar.es.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import eina.unizar.es.R

@Composable
fun PlansScreen(navController: NavController) {
    // Fondo oscuro para la pantalla completa
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)), // Fondo oscuro
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Tarjeta Gratuita (arriba)
            PlanCard(
                title = "Gratuito",
                price = "0 € durante toda la vida",
                monthlyPrice = "0 €/mes",
                features = listOf(
                    "Reproducción con anuncios",
                    "Anuncios visuales y de audio",
                    "Podrás saltar 5 canciones al día"
                ),
                buttonText = "Pasar a Gratuito",
                buttonColor = Color(0xFFFFAFC1), // Rosado
                textColor = Color(0xFFFFAFC1),
                iconResId = R.drawable.vibrarosa, // Logo rosa
                onClick = { navController.navigate("login") }
            )

            Spacer(modifier = Modifier.height(24.dp)) // Espaciado entre las tarjetas

            // Tarjeta Premium (abajo)
            PlanCard(
                title = "Premium",
                price = "4,99 € al mes",
                monthlyPrice = "4,99 €/mes",
                features = listOf(
                    "Experiencia libre de anuncios",
                    "Saltar ilimitadas canciones al día",
                    "Cancela cuando quieras"
                ),
                buttonText = "Pasar a Premium",
                buttonColor = Color(0xFFB0C4DE), // Azul
                textColor = Color(0xFFB0C4DE),
                iconResId = R.drawable.vibraazul, // Logo azul
                onClick = { navController.navigate("register") }
            )
        }
    }
}

@Composable
fun PlanCard(
    title: String,
    price: String,
    monthlyPrice: String,
    features: List<String>,
    buttonText: String,
    buttonColor: Color,
    textColor: Color,
    iconResId: Int, // 🔹 Se agrega el logo como parámetro
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp), // 🔹 Esquinas redondeadas
        modifier = Modifier
            .fillMaxWidth(0.9f) // 🔹 Ancho de la tarjeta
            .height(350.dp) // 🔹 Altura fija
            .border(2.dp, Color.Gray, RoundedCornerShape(16.dp)) // 🔹 Borde con esquinas redondeadas
            .background(Color(0xFF2E2E2E), shape = RoundedCornerShape(16.dp)), // 🔹 Asegura que el fondo respete las esquinas
        colors = CardDefaults.cardColors(containerColor = Color.Transparent) // 🔹 Evita fondo adicional de la Card
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔹 Icono del plan (más grande)
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = "Plan Icon",
                tint = textColor,
                modifier = Modifier.size(90.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Título del plan
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Precio
            Text(
                text = price,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Precio mensual
            Text(
                text = monthlyPrice,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de características
            features.forEach { feature ->
                Text(
                    text = feature,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = buttonText, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

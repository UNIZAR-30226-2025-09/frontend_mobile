package eina.unizar.es.ui.plans

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.data.model.network.getUserData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun PlansScreen(paymentSheet: PaymentSheet, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPremium by remember { mutableStateOf(false) }
    var isPaymentReady by remember { mutableStateOf(false) }
    var paymentIntentClientSecret by remember { mutableStateOf<String?>(null) }
    val previousRoute = navController.previousBackStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        if (previousRoute == "settings") {
            PaymentConfiguration.init(
                context,
                "pk_test_51R0pjqP1jnBE1veqsiXWTUll0H44mEoupgzDAnrFyjZ9pUPNHZ3aGViTzT49nYDchBr0F6UhI6V7kMA3DV2OFi3Z00XUhmPX1A"
            )

            coroutineScope.launch {
                val clientSecret = eina.unizar.es.ui.payments.fetchPaymentIntent()
                if (clientSecret != null) {
                    paymentIntentClientSecret = clientSecret
                    isPaymentReady = true
                } else {
                    isPaymentReady = false
                }
                val userData = getUserData(context)
                if (userData != null) {
                    isPremium = userData["is_premium"] as Boolean
                    Log.d("UserData", "isPremium asignado: $isPremium") // Verifica si is_premium se asigna correctamente
                }
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Tarjeta Gratuita
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
                buttonColor = Color(0xFFFFAFC1),
                textColor = Color(0xFFFFAFC1),
                iconResId = R.drawable.logorosa,
                onClick = {
                    if (previousRoute == "settings") {
                        if (!isPremium) {
                            Toast.makeText(context, "Ya eres usuario Gratuito", Toast.LENGTH_LONG)
                                .show()
                        } else {
                            coroutineScope.launch {
                                val response = ApiClient.post("user/premium", JSONObject().apply {
                                    put("is_premium", false)
                                })
                                if (response != null) {
                                    isPremium = false
                                    Toast.makeText(context, "Has cambiado a Plan Gratuito", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Error al cambiar a gratuito", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                    navController.popBackStack()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tarjeta Premium
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
                buttonColor = Color(0xFFB0C4DE),
                textColor = Color(0xFFB0C4DE),
                iconResId = R.drawable.logoazul,
                onClick = {
                    if (previousRoute == "settings") {
                        if (isPremium) {
                            Toast.makeText(context, "Ya eres usuario Premium", Toast.LENGTH_LONG)
                                .show()
                        } else {
                            paymentIntentClientSecret?.let { clientSecret ->
                                paymentSheet.presentWithPaymentIntent(
                                    clientSecret,
                                    PaymentSheet.Configuration("Vibra Music")
                                )
                            } ?: Toast.makeText(context, "Error al obtener clientSecret", Toast.LENGTH_LONG).show()
                        }
                    }
                    navController.popBackStack()
                }
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
            // 🔹 Icono del plan
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
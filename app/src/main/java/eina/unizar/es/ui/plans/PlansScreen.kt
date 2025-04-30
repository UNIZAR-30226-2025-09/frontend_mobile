package eina.unizar.es.ui.plans

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.musicapp.ui.theme.VibraBlue
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient.getUserData
import eina.unizar.es.data.model.network.ApiClient.postTokenPremium
import eina.unizar.es.ui.main.MainActivity
import eina.unizar.es.ui.player.MusicPlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.min

@Composable
fun PlansScreen(paymentSheet: PaymentSheet, navController: NavController,
                isPremium: Boolean = false, playerViewModel: MusicPlayerViewModel,
                isViewOnly: Boolean = false) {


    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPremiumLocal by remember { mutableStateOf(isPremium) }
    var isViewOnlyLocal by remember { mutableStateOf(isViewOnly) }
    var isPaymentReady by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var changingToPremium by remember { mutableStateOf(false) }
    var paymentIntentClientSecret by remember { mutableStateOf<String?>(null) }
    val previousRoute = navController.previousBackStackEntry?.destination?.route


    LaunchedEffect(Unit) {
        isPremiumLocal = getPremiumStatus(context)!!
    }

    LaunchedEffect(Unit) {
        if (previousRoute == "settings") {
            PaymentConfiguration.init(
                context,
                "pk_test_51R0pjqP1jnBE1veqsiXWTUll0H44mEoupgzDAnrFyjZ9pUPNHZ3aGViTzT49nYDchBr0F6UhI6V7kMA3DV2OFi3Z00XUhmPX1A"
            )

            coroutineScope.launch {
                paymentIntentClientSecret = eina.unizar.es.ui.payments.fetchPaymentIntent()
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
                .padding(40.dp)
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
                buttonText = if (isPremiumLocal) "Pasar a Gratuito" else "Plan Actual",
                buttonColor = Color(0xFFFFAFC1),
                textColor = Color(0xFFFFAFC1),
                iconResId = R.drawable.logorosa,
                isCurrentPlan = !isPremiumLocal && !isViewOnlyLocal,
                onClick = {
                    if (previousRoute == "settings") {
                        if (!isPremiumLocal) {
                            Toast.makeText(context, "Ya eres usuario Gratuito", Toast.LENGTH_LONG)
                                .show()
                        } else {
                            changingToPremium = false
                            showDialog = true


                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                isViewOnly = isViewOnlyLocal
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
                buttonText = if (!isPremiumLocal) "Pasar a Premium" else "Plan Actual",
                buttonColor = Color(0xFFB0C4DE),
                textColor = Color(0xFFB0C4DE),
                iconResId = R.drawable.logoazul,
                isCurrentPlan = isPremiumLocal && !isViewOnlyLocal,
                onClick = {
                    if (previousRoute == "settings") {
                        if (isPremiumLocal) {
                            Toast.makeText(context, "Ya eres usuario Premium", Toast.LENGTH_LONG).show()
                        } else {
                            changingToPremium = true
                            showDialog = true
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                isViewOnly = isViewOnlyLocal
            )
        }
        // Diálogo de confirmación
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color(0xFF1E1E1E),
                title = {
                    Text(
                        "Confirmar cambio de plan",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                },
                text = {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "¿Estás seguro de que quieres cambiar al plan ${if (changingToPremium) "Premium" else "Gratuito"}?",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDialog = false
                            if (changingToPremium) {
                                // Lógica para Premium (pago)
                                paymentIntentClientSecret?.let { clientSecret ->
                                    paymentSheet.presentWithPaymentIntent(
                                        clientSecret,
                                        PaymentSheet.Configuration("Vibra Music")
                                    )

                                } ?: Toast.makeText(context, "Error al procesar el pago", Toast.LENGTH_LONG).show()
                                /*coroutineScope.launch {
                                    delay(5000)
                                    navController.navigate("settings")
                                }*/
                            } else {
                                // Lógica MEJORADA para Gratuito
                                coroutineScope.launch {
                                    try {
                                        val jsonBody = JSONObject().apply {
                                            put("is_premium", false)
                                        }
                                        val response = postTokenPremium("user/premium", jsonBody, context)

                                        if (response != null /*&& response.getBoolean("success")*/) {
                                            // Actualizar estado local y SharedPreferences
                                            isPremiumLocal = false
                                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                                .edit()
                                                .putBoolean("is_premium", false)
                                                .apply()

                                            Toast.makeText(
                                                context,
                                                "Plan cambiado a Gratuito correctamente",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            navController.navigate("settings") {
                                                popUpTo("settings") { inclusive = true }
                                            }
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Error en la respuesta del servidor",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ChangeToFree", "Error: ${e.message}")
                                        Toast.makeText(
                                            context,
                                            "Error al cambiar a Gratuito: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF79E2FF),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text("Confirmar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDialog = false }, // Solo cierra el diálogo
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color(0xFF79E2FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 24.dp)
                    ) {
                        Text("Cancelar")
                    }
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
    iconResId: Int, // Se agrega el logo como parámetro
    isCurrentPlan: Boolean, //Plan actual
    isViewOnly: Boolean,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier.fillMaxWidth(1f)
            .padding(start = 24.dp)
    ) {
        // Cartelito "Tu plan actual" (solo visible si isCurrentPlan es true)
        if (isCurrentPlan && !isViewOnly) {
            Text(
                text = "Tu plan actual",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = (-8).dp, x = (10.dp))
                    .background(
                        color = Color(0xFF4CAF50), // Verde para destacar
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .zIndex(1f) // Asegura que esté por encima
            )
        }

        Card(
            shape = RoundedCornerShape(16.dp), // Esquinas redondeadas
            modifier = Modifier
                .fillMaxWidth(0.9f) // Ancho de la tarjeta
                .height(350.dp) // Altura fija
                .border(
                    width = 2.dp,
                    color = Color.Gray,
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    Color(0xFF2E2E2E),
                    shape = RoundedCornerShape(16.dp)
                ), // Asegura que el fondo respete las esquinas
            colors = CardDefaults.cardColors(containerColor = Color.Transparent) // Evita fondo adicional de la Card
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

                if (!isCurrentPlan && !isViewOnly) { // Botón
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor
                        ),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(text = buttonText, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Función para determinar el plan del usuario
suspend fun getPremiumStatus(context: Context) : Boolean? {
    val userData = getUserData(context)
    if (userData != null) {
        return userData["is_premium"] as Boolean
    } else {
        return false
    }
}
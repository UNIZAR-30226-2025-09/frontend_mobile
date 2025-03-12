package eina.unizar.es.ui.payments

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.PaymentConfiguration
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun PaymentScreen(onDismiss: () -> Unit, paymentSheet: PaymentSheet) {
    val context = LocalContext.current as ComponentActivity
    var paymentIntentClientSecret by remember { mutableStateOf<String?>(null) }
    var isPaymentReady by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Animación infinita para el borde
    val infiniteTransition = rememberInfiniteTransition()
    val animatedBorderColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF7596A8), // Azul claro inicial
        targetValue = Color(0xFFFFFFFF), // Azul neón vibrante
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing), // Transición más rápida y dinámica
            repeatMode = RepeatMode.Reverse
        )
    )


    LaunchedEffect(Unit) {
        PaymentConfiguration.init(
            context,
            "pk_test_51R0pjqP1jnBE1veqsiXWTUll0H44mEoupgzDAnrFyjZ9pUPNHZ3aGViTzT49nYDchBr0F6UhI6V7kMA3DV2OFi3Z00XUhmPX1A"
        )

        coroutineScope.launch {
            val clientSecret = fetchPaymentIntent()
            if (clientSecret != null) {
                paymentIntentClientSecret = clientSecret
                isPaymentReady = true
            } else {
                isPaymentReady = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent, // Fondo oscuro
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, animatedBorderColor, RoundedCornerShape(16.dp))
                    .background(Color(0xFF2E2E2E), shape = RoundedCornerShape(16.dp)), // Fondo oscuro premium
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ICONO PREMIUM
                    Icon(
                        painter = painterResource(id = R.drawable.logoazul), // Icono azul premium
                        contentDescription = "Premium Icon",
                        tint = Color(0xFFB0C4DE), // Azul claro premium
                        modifier = Modifier.size(90.dp),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Título del plan
                    Text(
                        text = "Premium",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB0C4DE) // Azul clarito premium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Precio
                    Text(
                        text = "4,99 € al mes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Precio mensual pequeño
                    Text(
                        text = "4,99 €/mes",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Características del plan
                    listOf(
                        "Experiencia libre de anuncios",
                        "Saltar ilimitadas canciones al día",
                        "Cancela cuando quieras"
                    ).forEach { feature ->
                        Text(
                            text = feature,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Botón "Pasar a Premium"
                    Button(
                        onClick = {
                            onDismiss()
                            paymentIntentClientSecret?.let { clientSecret ->
                                paymentSheet.presentWithPaymentIntent(
                                    clientSecret,
                                    PaymentSheet.Configuration("Vibra Music")
                                )
                            } ?: Toast.makeText(context, "Error al obtener clientSecret", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0C4DE)), // Azul claro premium
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pasar a Premium", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Botón "Cancelar"
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

/**
 * Obtiene el `clientSecret` para el pago desde la API de Stripe utilizando `ApiClient`.
 */
suspend fun fetchPaymentIntent(): String? {
    return ApiClient.post("stripe/create-payment-intent", JSONObject())?.let {
        JSONObject(it).getString("clientSecret")
    }
}

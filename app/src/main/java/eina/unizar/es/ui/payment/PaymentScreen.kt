package eina.unizar.es.ui.payments

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.PaymentConfiguration
import eina.unizar.es.data.model.network.ApiClient
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(navController: NavController, paymentSheet: PaymentSheet) {
    val context = LocalContext.current as ComponentActivity
    var paymentIntentClientSecret by remember { mutableStateOf<String?>(null) }
    var isPaymentReady by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Inicializar configuración de Stripe
        PaymentConfiguration.init(
            context,
            "pk_test_51R0pjqP1jnBE1veqsiXWTUll0H44mEoupgzDAnrFyjZ9pUPNHZ3aGViTzT49nYDchBr0F6UhI6V7kMA3DV2OFi3Z00XUhmPX1A"
        )

        // Obtener el Client Secret desde la API de Stripe
        coroutineScope.launch {
            val clientSecret = fetchPaymentIntent()
            println("🔑 ClientSecret recibido: $clientSecret")

            if (clientSecret != null) {
                paymentIntentClientSecret = clientSecret
                isPaymentReady = true
            } else {
                isPaymentReady = false // Si falla, evitar mostrar "loading" indefinido
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Pago Seguro con Stripe 💳",
            color = Color.White,
            fontSize = 22.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isPaymentReady) {
            Button(
                onClick = {
                    paymentIntentClientSecret?.let { clientSecret ->
                        paymentSheet.presentWithPaymentIntent(
                            clientSecret,
                            PaymentSheet.Configuration("Vibra Music")
                        )
                    } ?: Toast.makeText(context, "Error al obtener clientSecret ❌", Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ED760)),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text("Pagar Ahora 💰", color = Color.Black)
            }
        } else {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

/**
 * Obtiene el `clientSecret` para el pago desde la API de Stripe utilizando `ApiClient`.
 */
suspend fun fetchPaymentIntent(): String? {
    return ApiClient.post("stripe/create-payment-intent", JSONObject())?.let {
        JSONObject(it).getString("clientSecret")
    }
}
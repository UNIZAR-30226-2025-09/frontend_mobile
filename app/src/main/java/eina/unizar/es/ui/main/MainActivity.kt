package eina.unizar.es.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import eina.unizar.es.ui.navigation.AppNavigator
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import eina.unizar.es.ui.theme.VibraAppTheme
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.*
import org.json.JSONObject
import eina.unizar.es.data.model.network.getUserData
import eina.unizar.es.data.model.network.postTokenPremium

class MainActivity : ComponentActivity() {
    private lateinit var paymentSheet: PaymentSheet  // Declaramos PaymentSheet
    private val coroutineScope = CoroutineScope(Dispatchers.IO) // Definimos un Scope para operaciones de red

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializamos PaymentSheet con un callback para manejar los resultados de pago
        paymentSheet = PaymentSheet(this) { result ->
            handlePaymentResult(result)
        }

        setContent {
            VibraAppTheme {
                val navController = rememberNavController()
                AppNavigator(navController, paymentSheet) // Pasamos PaymentSheet a la pantalla
            }
        }
    }

    // Método separado para manejar el resultado del pago
    private fun handlePaymentResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                Log.d("Payment", "Pago completado")

                // Actualizar is_premium en el servidor
                coroutineScope.launch {
                    try {
                        val jsonBody = JSONObject().apply {
                            put("is_premium", true)  // Cambiar a Premium
                        }

                        val response = postTokenPremium("user/premium", jsonBody, this@MainActivity)

                        if (response != null) {
                            Log.d("Payment", "Estado actualizado a Premium en el servidor")

                            // Actualizar localmente is_premium
                            val userData = getUserData(this@MainActivity)
                            if (userData != null) {
                                Log.d("UserData", "isPremium asignado: ${userData["is_premium"]}")
                            }

                            // Notificar al usuario en la UI
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Pago completado, disfruta de Vibra",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Log.e("Payment", "Error al actualizar el estado de premium en el servidor")
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Error al actualizar el estado de premium",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("PlanChangeError", "Error al cambiar a Premium: ${e.message}")
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Error al procesar la solicitud",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

            is PaymentSheetResult.Canceled -> {
                Log.d("Payment", "Pago cancelado")
                runOnUiThread {
                    Toast.makeText(this, "Pago cancelado", Toast.LENGTH_LONG).show()
                }
            }

            is PaymentSheetResult.Failed -> {
                Log.e("Payment", "⚠ Error en el pago: ${result.error.message}")
                runOnUiThread {
                    Toast.makeText(this, "⚠ Error en el pago: ${result.error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

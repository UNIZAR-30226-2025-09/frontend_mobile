package eina.unizar.es.ui.main

import android.content.Context
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
import eina.unizar.es.data.model.network.postTokenPremium

class MainActivity : ComponentActivity() {
    private lateinit var paymentSheet: PaymentSheet
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar PaymentSheet
        paymentSheet = PaymentSheet(this) { result -> handlePaymentResult(result) }

        setContent {
            VibraAppTheme {
                val navController = rememberNavController()
                val isPremium = getPremiumStatus(this)  // Obtener el estado actual
                AppNavigator(navController, paymentSheet, isPremium) // Pasamos el estado
            }
        }
    }

    private fun handlePaymentResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                Log.d("Payment", "Pago completado")

                coroutineScope.launch {
                    try {
                        val jsonBody = JSONObject().apply { put("is_premium", true) }
                        val response = postTokenPremium("user/premium", jsonBody, this@MainActivity)

                        if (response != null) {
                            Log.d("Payment", "Estado actualizado a Premium en el servidor")

                            setPremiumStatus(this@MainActivity, true) // Guardar en SharedPreferences
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Pago completado, disfruta de Vibra", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Log.e("Payment", "Error al actualizar el estado en el servidor")
                        }
                    } catch (e: Exception) {
                        Log.e("PlanChangeError", "Error al cambiar a Premium: ${e.message}")
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
                    Toast.makeText(this, "Error en el pago: ${result.error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getPremiumStatus(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_premium", false)
    }

    private fun setPremiumStatus(context: Context, isPremium: Boolean) {
        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("is_premium", isPremium)
            apply()
        }
    }
}

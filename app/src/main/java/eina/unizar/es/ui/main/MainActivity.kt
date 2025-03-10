package eina.unizar.es.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import eina.unizar.es.ui.navigation.AppNavigator
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult

class MainActivity : ComponentActivity() {
    private lateinit var paymentSheet: PaymentSheet  // ✅ Declaramos el PaymentSheet aquí

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Inicializamos PaymentSheet en onCreate()
        paymentSheet = PaymentSheet(this) { result ->
            when (result) {
                is PaymentSheetResult.Completed -> println("✅ Pago completado")
                is PaymentSheetResult.Canceled -> println("❌ Pago cancelado")
                is PaymentSheetResult.Failed -> println("⚠ Error en el pago: ${result.error}")
            }
        }

        setContent {
            val navController = rememberNavController()
            AppNavigator(navController, paymentSheet) // ✅ Pasamos PaymentSheet a la pantalla
        }
    }
}

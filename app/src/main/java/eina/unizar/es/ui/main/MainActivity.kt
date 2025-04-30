package eina.unizar.es.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import eina.unizar.es.ui.navigation.AppNavigator
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import eina.unizar.es.ui.theme.VibraAppTheme
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import eina.unizar.es.data.model.network.ApiClient.postTokenPremium
import kotlinx.coroutines.*
import org.json.JSONObject
//import eina.unizar.es.data.model.network.postTokenPremium

class MainActivity : ComponentActivity() {
    private lateinit var paymentSheet: PaymentSheet
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var navigateToSettings = mutableStateOf(false)

    private var pendingIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Guardar el intent inicial si contiene uri
        if (intent?.data != null) {
            pendingIntent = intent
        }

        // Inicializar PaymentSheet
        paymentSheet = PaymentSheet(this) { result -> handlePaymentResult(result)}

        setContent {
            VibraAppTheme {
                val navController = rememberNavController()
                val isPremium = getPremiumStatus(this)

                // Procesamos el intent pendiente una vez que navController está disponible
                pendingIntent?.let { intent ->
                    LaunchedEffect(navController) {
                        handleIntent(intent, navController)
                        pendingIntent = null
                    }
                }

                // Controlador de navegación
                if (navigateToSettings.value) {
                    LaunchedEffect(Unit) {
                        val previousRoute = navController.previousBackStackEntry?.destination?.route
                        navController.navigate("settings") {
                            if (previousRoute == "settings") {
                                // Si venimos de settings, eliminar toda la pila hasta plans
                                popUpTo("settings") {
                                    inclusive = true
                                }
                            } else {
                                // Si venimos de otra pantalla, comportamiento normal
                                popUpTo("settings") {
                                    inclusive = false
                                }
                            }
                        }
                        navigateToSettings.value = false
                    }
                }

                AppNavigator(navController, paymentSheet, isPremium)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Procesamos inmediatamente el intent si tiene datos
        if (intent.data != null) {
            // En este punto, el Compose ya está configurado, por lo que debemos
            // encontrar el NavController actual o almacenar el intent para procesarlo más tarde
            pendingIntent = intent

            // Obligamos a refrescar la actividad para que se procese el pendingIntent
            recreate()
        }
    }

    private fun handleIntent(intent: Intent, navController: NavController) {
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                val scheme = uri.scheme
                val host = uri.host
                val path = uri.pathSegments

                // Verificar autenticación
                val isAuthenticated = isUserAuthenticated(this)

                when {
                    // Enlace tipo vibra://playlist/123
                    scheme == "vibra" && host == "playlist" && path.isNotEmpty() -> {
                        val playlistId = path[0]
                        handleDeepLinkNavigation(navController, isAuthenticated, "playlist/$playlistId")
                    }

                    // Enlace tipo https://vibra.eina.unizar.es/playlist/123
                    (scheme == "http" || scheme == "https") &&
                            host == "vibra.eina.unizar.es" &&
                            path.size >= 2 &&
                            path[0] == "playlist" -> {

                        val playlistId = path[1]
                        Log.d("TAG","Ha entrado donde toca y el id es: " + playlistId)
                        handleDeepLinkNavigation(navController, isAuthenticated, "playlist/$playlistId")
                    }
                }
            }
        }
    }

    private fun handleDeepLinkNavigation(navController: NavController, isAuthenticated: Boolean, destination: String) {
        if (isAuthenticated) {
            navController.navigate(destination) {
                // Limpiar la pila de navegación
                popUpTo(0)
            }
        } else {
            navController.navigate("login?returnTo=$destination") {
                // Limpiar la pila de navegación
                popUpTo(0)
            }
        }
    }

    private fun isUserAuthenticated(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return !sharedPreferences.getString("auth_token", null).isNullOrEmpty()
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
                                navigateToSettings.value = true
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

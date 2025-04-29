package eina.unizar.es.ui.permissions

import android.content.Context
import androidx.navigation.NavController

fun NavController.navigateWithAuth(
    route: String,
    isAuthenticated: Boolean,
    context: Context
) {
    if (isAuthenticated) {
        navigate(route)
    } else {
        // Guardamos la ruta destino en los argumentos
        navigate("login?returnToRoute=$route") {
            popUpTo(0)
        }
    }
}

fun isUserAuthenticated(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    return !sharedPreferences.getString("auth_token", null).isNullOrEmpty()
}
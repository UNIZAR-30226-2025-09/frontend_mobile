package eina.unizar.es.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stripe.android.paymentsheet.PaymentSheet
import eina.unizar.es.ui.payments.PaymentScreen

@Composable
fun AppNavigator(navController: NavController, paymentSheet: PaymentSheet, ) {
    val navController: NavHostController = rememberNavController()

    Scaffold { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "main",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("main") { PaymentScreen(navController, paymentSheet) }
                /*
                composable("main") { MainScreen(navController) }
                composable("player") { PlayerScreen() }
                composable("plans") { PlansScreen(navController) }
                composable("login") { UserLoginScreen(navController) }
                composable("register") { UserRegisterScreen(navController)  }
                composable("menu") {HomeScreen(navController)}
                composable("playlist") { PlaylistScreen(navController) }
                composable("settings") { UserSettings(navController) }
                composable("library") { LibraryScreen(navController) }
                composable("perfilEdit") { EditProfileScreen(navController) }
                composable("search") { SearchScreen(navController) }
                */
            }
        }
    }
}






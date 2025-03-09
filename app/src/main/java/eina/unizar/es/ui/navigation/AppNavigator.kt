package eina.unizar.es.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import eina.unizar.es.ui.auth.UserLoginScreen
import eina.unizar.es.ui.auth.UserRegisterScreen
import eina.unizar.es.ui.main.MainScreen
import eina.unizar.es.ui.plans.PlansScreen
import eina.unizar.es.ui.player.PlayerScreen
import eina.unizar.es.ui.menu.HomeScreen

@Composable
fun AppNavigator() {
    val navController: NavHostController = rememberNavController()

    Scaffold { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "main",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("main") { MainScreen(navController) }
                composable("player") { PlayerScreen() }
                composable("plans") { PlansScreen(navController) }
                composable("login") { UserLoginScreen(navController) }
                composable("register") { UserRegisterScreen(navController)  }
                composable("menu") {HomeScreen(navController)}
            }
        }
    }
}






package eina.unizar.es.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.musicapp.ui.song.SongScreen
import com.stripe.android.paymentsheet.PaymentSheet
import eina.unizar.es.ui.auth.UserLoginScreen
import eina.unizar.es.ui.auth.UserRegisterScreen
import eina.unizar.es.ui.library.LibraryScreen
import eina.unizar.es.ui.main.MainScreen
import eina.unizar.es.ui.menu.MenuScreen
import eina.unizar.es.ui.plans.PlansScreen
import eina.unizar.es.ui.player.FloatingMusicPlayer
import eina.unizar.es.ui.playlist.PlaylistScreen
import eina.unizar.es.ui.search.SearchScreen
import eina.unizar.es.ui.user.EditProfileScreen
import eina.unizar.es.ui.user.UserSettings

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
                composable("main") { MainScreen(navController) }
                composable("player") { FloatingMusicPlayer("tituloPrueba", "AlbumPrueba", 1, true) }
                composable("plans") { PlansScreen(navController) }
                composable("login") { UserLoginScreen(navController) }
                composable("register") { UserRegisterScreen(navController) }
                composable("menu") { MenuScreen(navController, paymentSheet) }
                composable(
                    "playlist/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId")
                    PlaylistScreen(navController, playlistId)
                }
                composable("settings") { UserSettings(navController) }
                composable("library") { LibraryScreen(navController) }
                composable("perfilEdit") { EditProfileScreen(navController) }
                composable("search") { SearchScreen(navController) }
                composable(
                    "song/{songId}",
                    arguments = listOf(navArgument("songId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val songId = backStackEntry.arguments?.getString("songId")
                    SongScreen(navController, songId)
                }
            }
        }
    }
}






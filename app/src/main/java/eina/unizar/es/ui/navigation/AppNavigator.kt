package eina.unizar.es.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.musicapp.ui.song.SongScreen
import com.stripe.android.paymentsheet.PaymentSheet
import eina.unizar.es.ui.artist.ArtistScreen
import eina.unizar.es.ui.auth.UserLoginScreen
import eina.unizar.es.ui.auth.UserRegisterScreen
import eina.unizar.es.ui.library.LibraryScreen
import eina.unizar.es.ui.main.MainScreen
import eina.unizar.es.ui.menu.MenuScreen
import eina.unizar.es.ui.navbar.BottomNavigationBar
import eina.unizar.es.ui.plans.PlansScreen
import eina.unizar.es.ui.player.FloatingMusicPlayer
import eina.unizar.es.ui.player.MusicPlayerViewModel
import eina.unizar.es.ui.playlist.PlaylistScreen
import eina.unizar.es.ui.search.SearchScreen
import eina.unizar.es.ui.user.EditProfileScreen
import eina.unizar.es.ui.user.UserSettings

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun AppNavigator(navController: NavController, paymentSheet: PaymentSheet, isPremium: Boolean) {
    val navController: NavHostController = rememberNavController()
    val playerViewModel: MusicPlayerViewModel = viewModel()

    Scaffold (
        bottomBar = {
            // Mostrar el reproductor solo si no estamos en la pantalla de canción
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute !in listOf("song/{songId}", "login", "register", "perfilEdit", "settings", "plans", "main")) {
                Column {
                    FloatingMusicPlayer(navController, playerViewModel)
                    BottomNavigationBar(navController)
                }
            }
            else if (currentRoute in listOf("song/{songId}")) {
                BottomNavigationBar(navController)
            }
        }
    ){ innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "main",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("main") { MainScreen(navController) }
                composable("menu") { MenuScreen(navController, paymentSheet, isPremium, playerViewModel) }
                composable("buscar") { SearchScreen(navController, playerViewModel) }
                composable("tu biblioteca") { LibraryScreen(navController, playerViewModel) }
                composable("login") { UserLoginScreen(navController) }
                composable("register") { UserRegisterScreen(navController) }
                composable("perfilEdit") { EditProfileScreen(navController) }
                composable("settings") { UserSettings(navController, isPremium) }
                composable("plans") { PlansScreen(paymentSheet,navController,isPremium, playerViewModel) }
                composable("artist") { ArtistScreen(navController, playerViewModel) }
                //composable("player") { FloatingMusicPlayer(navController, playerViewModel) }

                composable(
                    "playlist/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.StringType } )
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId")
                    PlaylistScreen(navController, playlistId, playerViewModel)
                }
                composable(
                    "song/{songId}",
                    arguments = listOf(navArgument("songId") { type = NavType.StringType } )
                ) { backStackEntry ->
                    val songId = backStackEntry.arguments?.getString("songId")
                    SongScreen(navController, songId, playerViewModel)
                }
            }
        }
    }
}





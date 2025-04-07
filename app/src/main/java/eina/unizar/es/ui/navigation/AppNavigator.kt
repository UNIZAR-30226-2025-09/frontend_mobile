package eina.unizar.es.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHost
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
import eina.unizar.es.ui.chat.ChatScreen
import eina.unizar.es.ui.friends.FriendsScreen
import eina.unizar.es.ui.search.ADSongs

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun AppNavigator(navController: NavHostController, paymentSheet: PaymentSheet, isPremium: Boolean) {
    val playerViewModel: MusicPlayerViewModel = viewModel()

    // Estado para ruta actual observada
    val currentRoute = remember(navController) {
        mutableStateOf("")
    }

    // Observa cambios de ruta (mejor que currentBackStackEntry?.destination?.route)
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentRoute.value = destination.route ?: ""
        }
    }

    // Rutas que no deben mostrar bottomBar
    val routesWithoutBottomBar = listOf(
        "login", "register", "perfilEdit", "settings", "plans", "main", "song"
    )

    // Extrae el prefijo de la ruta para detectar dinámicas como "song/{id}" => "song"
    fun getBaseRoute(route: String): String {
        return route.substringBefore("/")
    }

    val baseRoute = getBaseRoute(currentRoute.value)
    val showFloatingPlayer = baseRoute !in routesWithoutBottomBar

    Scaffold(
        bottomBar = {
            when {
                showFloatingPlayer -> {
                    Column {
                        FloatingMusicPlayer(navController, playerViewModel)
                        BottomNavigationBar(navController)
                    }
                }
                // Si no queremos nada, no ponemos nada
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
                composable("friends") { FriendsScreen(navController, playerViewModel) }
                composable("ADSongs") { ADSongs() }
                composable(
                    "chat/{friendId}",
                    arguments = listOf(navArgument("friendId") { type = NavType.StringType } )
                ) { backStackEntry ->
                    val friendId = backStackEntry.arguments?.getString("friendId")
                    ChatScreen(navController, friendId, playerViewModel) 
                }

                composable(
                    "artist/{artistId}",
                    arguments = listOf(navArgument("artistId") { type = NavType.IntType } )
                ) { backStackEntry ->
                    val artistId = backStackEntry.arguments?.getInt("artistId")
                    ArtistScreen(navController, playerViewModel, artistId)
                }

                // Para playlists normales
                composable(
                    "playlist/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId")
                    PlaylistScreen(
                        navController = navController,
                        playlistId = playlistId,
                        playerViewModel = playerViewModel,
                        isSencillo = false,
                        singleId = null
                    )
                }

                // Para sencillos
                composable(
                    "single/{songId}",
                    arguments = listOf(navArgument("songId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val songId = backStackEntry.arguments?.getString("songId")
                    PlaylistScreen(
                        navController = navController,
                        playlistId = null,
                        playerViewModel = playerViewModel,
                        isSencillo = true,
                        singleId = songId
                    )
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





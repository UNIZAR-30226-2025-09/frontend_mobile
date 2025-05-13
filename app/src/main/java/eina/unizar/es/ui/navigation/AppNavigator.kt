package eina.unizar.es.ui.navigation

import android.annotation.SuppressLint
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
import eina.unizar.es.ui.user.UserStyleScreen
import kotlinx.coroutines.delay

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun AppNavigator(navController: NavHostController, paymentSheet: PaymentSheet, isPremium: Boolean) {
    val playerViewModel: MusicPlayerViewModel = viewModel()
    val context = LocalContext.current

    // Restaurar el estado de reproducción cuando se inicia la aplicación
    LaunchedEffect(Unit) {
        // Asegurarse de que el userId está configurado primero
        playerViewModel.setUserId(context)
        
        // Esperar un momento para que el userId se cargue completamente
        delay(1000)

        Log.d("RetomarSong", "Restaurando estado de reproducción")
        // Restaurar el estado de reproducción guardado anteriormente
        playerViewModel.restorePlaybackState(context)
        
        // Inicializar las canciones con "me gusta"
        playerViewModel.initializeLikedSongs(playerViewModel.getUserId())
        
        // Configurar el estado premium del usuario
        playerViewModel.setPremiumUser(context)


        //
        //playerViewModel.setApplicationContext(context)

        Log.d("RetomarSong", "Estado de reproducción restaurado")
    }

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
        "login", "register", "perfilEdit", "plans", "main", "song", "styles"
    )

    // Extrae el prefijo de la ruta para detectar dinámicas como "song/{id}" => "song"
    fun getBaseRoute(route: String): String {
        return route
            .substringBefore("/")
            .substringBefore("?")
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
                composable(
                    "login?returnTo={returnTo}",
                    arguments = listOf(
                        navArgument("returnTo") {
                            type = NavType.StringType
                            defaultValue = ""
                            nullable = true
                        }
                    )
                ) { backStackEntry ->
                    val returnTo = backStackEntry.arguments?.getString("returnTo") ?: ""
                    UserLoginScreen(navController, returnTo)
                }
                composable("register") { UserRegisterScreen(navController) }
                composable("perfilEdit") { EditProfileScreen(navController) }
                composable("settings") { UserSettings(navController, isPremium, playerViewModel) }
                composable("friends") { FriendsScreen(navController, playerViewModel) }
                composable("ADSongs") { ADSongs(playerViewModel) }
                composable("styles") { UserStyleScreen(navController, context) }
                composable(
                    route = "plans?isViewOnly={isViewOnly}",
                    arguments = listOf(
                        navArgument("isViewOnly") {
                            type = NavType.BoolType
                            defaultValue = false // Valor por defecto si no se pasa
                        }
                    )
                ) { backStackEntry ->
                    val isViewOnly = backStackEntry.arguments?.getBoolean("isViewOnly") ?: false
                    PlansScreen(
                        paymentSheet = paymentSheet,
                        navController = navController,
                        isPremium = isPremium,
                        playerViewModel = playerViewModel,
                        isViewOnly = isViewOnly
                    )
                }
                composable(
                    "chat/{friendId}/{friendName}?friendPhoto={friendPhoto}",
                    arguments = listOf(
                        navArgument("friendId") { type = NavType.StringType },
                        navArgument("friendName") { type = NavType.StringType },
                        navArgument("friendPhoto") { 
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val friendId = backStackEntry.arguments?.getString("friendId")
                    val friendName = backStackEntry.arguments?.getString("friendName")
                    val friendPhoto = backStackEntry.arguments?.getString("friendPhoto")
                    
                    ChatScreen(navController, friendId, friendName, friendPhoto, playerViewModel)
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
                    PlaylistScreen(navController, playlistId, playerViewModel, false, null)
                }

                // Para sencillos
                composable(
                    "single/{songId}",
                    arguments = listOf(navArgument("songId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val songId = backStackEntry.arguments?.getString("songId")
                    PlaylistScreen(navController, null, playerViewModel, true, songId)
                }
                composable(
                    "song/{songId}",
                    arguments = listOf(navArgument("songId") { type = NavType.StringType } )
                ) {
                    SongScreen(navController, playerViewModel)
                }
            }
        }
    }
}





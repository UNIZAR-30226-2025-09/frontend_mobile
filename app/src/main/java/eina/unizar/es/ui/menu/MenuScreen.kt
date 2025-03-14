package eina.unizar.es.ui.menu

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.stripe.android.paymentsheet.PaymentSheet
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient.get
import eina.unizar.es.ui.components.UserProfileMenu
import eina.unizar.es.ui.navbar.BottomNavigationBar
import eina.unizar.es.ui.player.FloatingMusicPlayer
import eina.unizar.es.ui.payments.PaymentScreen
import eina.unizar.es.ui.playlist.Playlist
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(navController: NavController, paymentSheet: PaymentSheet) {
    var showPaymentDialog by remember { mutableStateOf(false) } // Estado para mostrar pop-up

    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }

    // Cargar playlists desde el backend
    LaunchedEffect(Unit) {
        val response = get("playlists") // Llamada a la API
        response?.let {
            val jsonArray = JSONArray(it)
            val fetchedPlaylists = mutableListOf<Playlist>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                fetchedPlaylists.add(
                    Playlist(
                        id = jsonObject.getString("id"),
                        title = jsonObject.getString("name"),
                        //imageUrl = jsonObject.getString("image_url")
                    )
                )
            }
            playlists = fetchedPlaylists
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserProfileMenu(navController)

                        Spacer(modifier = Modifier.weight(0.9f))

                        // Hacerse Premium activa el pop-up en lugar de cambiar de pantalla
                        VibraBanner(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(end = 16.dp, top = 16.dp),
                            premium = false,
                            onPremiumClick = { showPaymentDialog = true } // Mostrar pop-up
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = { BottomNavigationBar(navController) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize() // Aseguramos que la columna ocupa todo el espacio
            ) {
                // Canciones recomendadas en Grid
                Column {
                    Text(
                        "Canciones recomendadas",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f) // La grilla ocupa la mayor parte del espacio
                    ) {
                        items(
                            listOf(
                                "Canción A",
                                "Canción B",
                                "Canción C",
                                "Canción D",
                                "Canción E",
                                "Canción F",
                                "Canción G",
                                "Canción H",
                                "Canción I",
                                "Canción J"
                            )
                        ) { cancion ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clickable { navController.navigate("song") },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        cancion,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Álbumes populares
                    Text(
                        "Álbumes populares",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(
                            listOf(
                                "DeBÍ TiRAR MáS FOToS",
                                "Easy Money Baby",
                                "Gaxur Gang",
                                "Marietuco Tiradera",
                                "Buenas Noches",
                                "Buenos Dias",
                                "Buenas Tardes"
                            )
                        ) { album ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Card(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clickable { /* navController.navigate("album") */ },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.kanyeperfil),
                                            contentDescription = "Álbum",
                                            modifier = Modifier.size(120.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    album,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Listas de reproducción recomendadas
                    Text(
                        "Listas para ti",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(playlists) { playlist -> // Cambiado listOf(playlists) a playlists
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Card(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clickable { navController.navigate("playlist") },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Aquí debes cargar la imagen de la playlist desde playlist.imageUrl
                                        // Por ahora, usamos una imagen de ejemplo
                                        Image(
                                            painter = painterResource(id = R.drawable.kanyeperfil), // Reemplaza con la imagen de la playlist
                                            contentDescription = "Lista",
                                            modifier = Modifier.size(120.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    playlist.title, // Cambiado lista a playlist.title
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Reproductor Flotante
                        val isPlaying = remember { mutableStateOf(false) }
                        FloatingMusicPlayer(
                            title = "Mi canción",
                            artist = "Artista",
                            albumArt = R.drawable.kanyeperfil,
                            isPlaying = isPlaying.value,
                            // onPlayPauseClick = { isPlaying.value = !isPlaying.value },
                            // onFavoriteClick = { /*TODO*/ },
                            // onComputerClick = { /*TODO*/ }
                        )
                    }
                }
            }

            // Mostrar pop-up si el usuario pulsa en "Hacerse Premium" + Logica de esPremium?
            if (showPaymentDialog) {
                PaymentScreen(
                    onDismiss = { showPaymentDialog = false },
                    paymentSheet = paymentSheet
                )
            }
        }
    }
}


/**
 * Composable que dibuja un banner degradado con un logo y el texto "Vibra".
 * El banner se muestra a la derecha. Reemplaza R.drawable.my_logo por el recurso de tu logo.
 */
@Composable
fun VibraBanner(modifier: Modifier = Modifier, premium: Boolean, onPremiumClick: () -> Unit) {
    val bannerWidth = 160.dp
    val bannerHeight = 50.dp
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF004aad), Color(0xFF00a0d7))
    )
    // Animación infinita para el borde
    val infiniteTransition = rememberInfiniteTransition()
    val animatedBorderColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFB0C4DE), // Azul claro inicial
        targetValue = Color(0xFF00D4FF), // Azul neón vibrante
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing), // Transición más rápida y dinámica
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .size(width = bannerWidth, height = bannerHeight)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, animatedBorderColor, RoundedCornerShape(16.dp))
            .background(gradientBrush, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp)
            .clickable {
                if (!premium) {
                    onPremiumClick() // Desplegar Pop-up
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
                .clip(RoundedCornerShape(16)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(id = R.drawable.vibrablanco),
                contentDescription = "Logo",
                modifier = Modifier.size(45.dp)
            )
            Text(
                text = if (premium) "Vibra" else "Hacerse Premium",
                color = Color.White,
                fontSize = if (premium) 8.sp else 20.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
        }
    }
}
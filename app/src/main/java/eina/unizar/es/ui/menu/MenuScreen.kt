package eina.unizar.es.ui.menu

import android.util.Log
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.stripe.android.paymentsheet.PaymentSheet
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient.get
import eina.unizar.es.ui.main.Rubik
import eina.unizar.es.ui.user.UserProfileMenu
import eina.unizar.es.ui.navbar.BottomNavigationBar
import eina.unizar.es.ui.player.FloatingMusicPlayer
import eina.unizar.es.ui.payments.PaymentScreen
import eina.unizar.es.ui.playlist.Playlist
import org.json.JSONArray
import coil.compose.AsyncImage
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.data.model.network.ApiClient.getUserData
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import eina.unizar.es.ui.artist.Artist


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(navController: NavController, paymentSheet: PaymentSheet, isPremium: Boolean) {
    val context = LocalContext.current
    var showPaymentDialog by remember { mutableStateOf(false) } // Estado para mostrar pop-up
    var isPremium by remember { mutableStateOf(isPremium) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
            coroutineScope.launch {
                val userData = getUserData(context)
                if (userData != null) {
                    isPremium = userData["is_premium"] as Boolean
                    Log.d("UserData", "isPremium asignado: $isPremium") // Verifica si is_premium se asigna correctamente
                }
            }
    }

    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var albums by remember { mutableStateOf<List<Playlist>>(emptyList()) }

    var artists by remember { mutableStateOf<List<Artist>>(emptyList()) }

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
                        idAutor = jsonObject.getString("user_id"),
                        idArtista = jsonObject.getString("artist_id"),
                        description = jsonObject.getString("description"),
                        esPublica = jsonObject.getString("type"),
                        esAlbum = jsonObject.getString("typeP"),
                        imageUrl = jsonObject.getString("front_page")
                    )
                )
            }
            playlists = fetchedPlaylists
        }

        val albumsAux = mutableListOf<Playlist>() // Lista para álbumes
        val listasDeReproduccionAux = mutableListOf<Playlist>() // Lista para playlists

        for (playlist in playlists) {
            if (playlist.esAlbum == "album") {
                albumsAux.add(playlist)
            } else {
                listasDeReproduccionAux.add(playlist)
            }
        }

        albums = albumsAux;
        playlists = listasDeReproduccionAux;


        // Canciones de la parte superior de recomendaciones
        val responseS = get("artist/artists") // Llamada a la API para obtener canciones
        responseS?.let {
            val jsonArray = JSONArray(it)
            val fetchedArtists = mutableListOf<Artist>()

            for (i in 0 until 8) {
                val jsonObject = jsonArray.getJSONObject(i)
                fetchedArtists.add(
                    Artist(
                        id = jsonObject.getInt("id"),
                        name = jsonObject.getString("name"),
                        biography = "Prueba",//jsonObject.getString("bio")//
                        photo = jsonObject.getString("photo"),
                    )
                )

            }
            Log.d("Artista", "Artistas: + " + fetchedArtists)
            artists = fetchedArtists
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
                        //if (!isPremium) {
                            // Hacerse Premium activa el pop-up en lugar de cambiar de pantalla
                            VibraBanner(
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(end = 16.dp, top = 16.dp, bottom = 16.dp),
                                premium = isPremium,
                                onPremiumClick = { showPaymentDialog = true } // Mostrar pop-up
                            )
                        //}
                    }
                },


                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

        },
        bottomBar = {
            Column {
                val isPlaying = remember { mutableStateOf(false) }
                FloatingMusicPlayer("Sensualidad", "god", R.drawable.kanyeperfil, isPlaying.value)
                BottomNavigationBar(navController)
            }
        },
    )
    { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(16.dp))
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
                // Artistas recomendados en Grid
                Column {
                    Text(
                        "Artistas recomendados",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(artists) { artist ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Card(
                                    modifier = Modifier
                                        .size(100.dp) // Aumentamos el tamaño del Card para que la imagen sea más grande
                                        .clickable {
                                            // navController.navigate("playlist/${album.id}")
                                            navController.navigate("artist")
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)

                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val urlAntes = artist?.photo
                                        val playlistImage = getImageUrl(urlAntes, "/default-playlist.jpg")
                                        AsyncImage(
                                            model = playlistImage,
                                            contentDescription = "Foto del artista",
                                            modifier = Modifier
                                                .size(100.dp) // Igualamos el tamaño de la imagen al del Card
                                                .clip(CircleShape) // Usamos CircleShape para hacer la imagen redonda
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = artist.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
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
                        items(playlists) { album ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Card(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clickable {
                                            navController.navigate("playlist/${album.id}")
                                        }, // PASAMOS EL ID
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Cargar la imagen desde la URL
                                        val urlAntes = album?.imageUrl
                                        val playlistImage =
                                            getImageUrl(urlAntes, "/default-playlist.jpg")
                                        AsyncImage(
                                            model = playlistImage,
                                            contentDescription = "Portada de la playlist",
                                            modifier = Modifier
                                                //.size(imageSize)
                                                //.alpha(imageAlpha)
                                                .clip(RoundedCornerShape(8.dp)) // Opcional: añade esquinas redondeadas
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = album.title,
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
                        items(playlists) { playlist ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Card(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clickable {
                                            navController.navigate("playlist/${playlist.id}") // PASAMOS EL ID
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                    //elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Cargar la imagen desde la URL
                                        val urlAntes = playlist?.imageUrl
                                        val playlistImage =
                                            getImageUrl(urlAntes, "/default-playlist.jpg")
                                        AsyncImage(
                                            model = playlistImage,
                                            contentDescription = "Portada de la playlist",
                                            modifier = Modifier
                                                //.size(imageSize)
                                                //.alpha(imageAlpha)
                                                .clip(RoundedCornerShape(8.dp)) // Opcional: añade esquinas redondeadas
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
                animation = tween(
                    durationMillis = 1200,
                    easing = FastOutSlowInEasing
                ), // Transición más rápida y dinámica
                repeatMode = RepeatMode.Reverse
            )
        )

        Box(
            modifier = modifier
                .size(width = bannerWidth, height = bannerHeight)
                .clip(RoundedCornerShape(16.dp))
                .border(3.dp, animatedBorderColor, RoundedCornerShape(16.dp))
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
                    text = if (premium) "VIBRA" else "Hacerse Premium",
                    color = Color.White,
                    fontSize = if (premium) 25.sp else 20.sp,
                    modifier = Modifier.then(if (premium) Modifier.padding(end = 26.dp)
                        else Modifier.padding(end = 6.dp)),
                    fontFamily = Rubik
                )
            }
        }
    }


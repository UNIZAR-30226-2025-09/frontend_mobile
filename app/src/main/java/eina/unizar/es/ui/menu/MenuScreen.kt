package eina.unizar.es.ui.menu

import android.annotation.SuppressLint
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.lifecycle.viewmodel.compose.viewModel
import eina.unizar.es.data.model.network.ApiClient.getImageUrl
import eina.unizar.es.data.model.network.ApiClient.getUserData
import eina.unizar.es.ui.player.MusicPlayerViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.musicapp.ui.theme.VibraBlue
import com.example.musicapp.ui.theme.VibraDarkGrey
import com.example.musicapp.ui.theme.VibraLightGrey
import com.example.musicapp.ui.theme.VibraWhite
import eina.unizar.es.ui.artist.Artist
import eina.unizar.es.ui.song.Song


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(navController: NavController, paymentSheet: PaymentSheet, isPremium: Boolean) {
    val context = LocalContext.current
    var showPaymentDialog by remember { mutableStateOf(false) } // Estado para mostrar pop-up
    var isPremium by remember { mutableStateOf(isPremium) }
    val coroutineScope = rememberCoroutineScope()
    //val parentEntry = remember(navController) { navController.getBackStackEntry("main") }
    //val playerViewModel = viewModel<MusicPlayerViewModel>(parentEntry)

    val playerViewModel: MusicPlayerViewModel = viewModel() // ✅ Crea aquí el viewModel


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
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }

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

    // Mostrar el pop-up de publicidad al entrar a la pantalla
    var showAdvertPopup by remember { mutableStateOf(!isPremium) } // Cambiado a true inicialmente

    // Mostrar el pop-up de publicidad
    AdvertPopup(
        showPopup = showAdvertPopup,
        onDismiss = { showAdvertPopup = false },
        onConfirm = {
            showAdvertPopup = false
            showPaymentDialog = true
            // Lógica al confirmar la publicidad
        }
    )


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
                FloatingMusicPlayer(viewModel = playerViewModel, navController = navController)
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
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = Rubik // Quitar si no convence
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f) // La grilla ocupa la mayor parte del espacio
                    ) {
                        items(songs) { cancion ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clickable {
                                        playerViewModel.loadSongsFromApi(
                                            songId = cancion.id.toString(),
                                            context = context,
                                            albumArtResId = R.drawable.kanyeperfil
                                        )
                                        navController.navigate("song/${cancion.id}")
                                    }, // PASAMOS EL ID
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cancion.name,
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
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = Rubik // Quitar si no convence
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
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = Rubik // Quitar si no convence
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvertPopup(
    showPopup: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    imageResId: Int = R.drawable.vibrablanco
) {
    if (showPopup) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.padding(20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2196F3), Color(0xFF0D47A1)
                                )
                            )
                        )
                ) {
                    Text(
                        text = "PREMIUM EXCLUSIVO",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = VibraWhite,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )


                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = "Premium Benefits",
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(16.dp))
                    )


                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        FeatureItem("✅ Sin anuncios molestos")
                        FeatureItem("✅ Contenido exclusivo")
                        FeatureItem("✅ Acceso prioritario")
                        FeatureItem("✅ Soporte 24/7")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "SOLO 5.99€/mes",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE91E63),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(0.7f)
                                .height(48.dp)
                                .border(2.dp, Color.Black, RoundedCornerShape(10.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VibraLightGrey
                            )
                        ) {
                            Text(
                                "Ahora no",
                                color = VibraDarkGrey,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onConfirm,
                            shape = RoundedCornerShape(12.dp),

                            modifier = Modifier
                                .weight(0.7f)
                                .height(48.dp)
                                .border(2.dp, Color.Black, RoundedCornerShape(10.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VibraBlue
                            )

                        ) {
                            Text(
                                "QUIERO PREMIUM",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable  // <-- No olvidar esta anotación
private fun FeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {

        Text(
            text = text,
            color = VibraLightGrey,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
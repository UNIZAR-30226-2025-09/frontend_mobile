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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.musicapp.ui.theme.VibraBlue
import com.example.musicapp.ui.theme.VibraDarkGrey
import com.example.musicapp.ui.theme.VibraLightGrey
import com.example.musicapp.ui.theme.VibraMediumGrey
import com.example.musicapp.ui.theme.VibraWhite
import eina.unizar.es.ui.artist.Artist
import eina.unizar.es.ui.song.Song
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(navController: NavController, paymentSheet: PaymentSheet, isPremium: Boolean, playerViewModel: MusicPlayerViewModel) {
    val context = LocalContext.current
    var showPaymentDialog by remember { mutableStateOf(false) }
    var isPremium by remember { mutableStateOf(isPremium) }
    val coroutineScope = rememberCoroutineScope()

    // Cambiar esta variable para controlar la visibilidad inicial
    var isLoadingMain by remember { mutableStateOf(true) }
    var isLoadingRecent by remember { mutableStateOf(true) }

    // Contadores para rastrear el progreso de carga
    var totalImagesToLoad by remember { mutableStateOf(0) }
    var loadedImagesCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val userData = getUserData(context)
            if (userData != null) {
                isPremium = userData["is_premium"] as Boolean
                Log.d("UserData", "isPremium asignado: $isPremium")
            }
        }
    }

    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var albums by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var artists by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var recentPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }

    // Cargar las playlists recientes (en un LaunchedEffect separado)
    LaunchedEffect(playerViewModel.getUserId()) {
        val userId = playerViewModel.getUserId()
        if (userId.isNotEmpty()) {
            Log.d("RecentPlaylists", "Cargando playlists recientes para usuario: $userId")
            val responseRecent = get("/playlists/recent/$userId")
            responseRecent?.let {
                val jsonArray = JSONArray(it)
                val fetchedRecentPlaylists = mutableListOf<Playlist>()

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    fetchedRecentPlaylists.add(
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
                recentPlaylists = fetchedRecentPlaylists
                isLoadingRecent = false
                Log.d("RecentPlaylists", "Cargadas ${fetchedRecentPlaylists.size} playlists recientes")
            }
        } else {
            isLoadingRecent = false
            Log.d("RecentPlaylists", "No se pudo cargar playlists recientes: userId vacío")
        }
    }

    // Cargar playlists desde el backend
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            // Inicializar el ViewModel con el ID de usuario
            if (playerViewModel.getUserId().isEmpty()) {
                playerViewModel.setUserId(context)
            }

            // Cargar explícitamente el estado de "me gusta"
            playerViewModel.initializeLikedSongs(playerViewModel.getUserId())
            playerViewModel.setPremiumUser(context)
        }

        val response = get("playlists")
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

        // Canciones de la parte superior de recomendaciones
        val responseSS = get("songs")
        responseSS?.let {
            val jsonArray = JSONArray(it)
            val fetchedSongs = mutableListOf<Song>()

            for (i in 0 until 8) {
                val jsonObject = jsonArray.getJSONObject(i)
                fetchedSongs.add(
                    Song(
                        id = jsonObject.getInt("id"),
                        name = jsonObject.getString("name"),
                        duration = jsonObject.getInt("duration"),
                        photo_video = jsonObject.getString("photo_video"),
                        url_mp3 = jsonObject.getString("url_mp3"),
                        letra = jsonObject.getString("lyrics")
                    )
                )
            }
            songs = fetchedSongs
        }

        val albumsAux = mutableListOf<Playlist>()
        val listasDeReproduccionAux = mutableListOf<Playlist>()

        for (playlist in playlists) {
            if (playlist.esAlbum == "album") {
                albumsAux.add(playlist)
            } else {
                listasDeReproduccionAux.add(playlist)
            }
        }

        albums = albumsAux
        playlists = listasDeReproduccionAux

        // Canciones de la parte superior de recomendaciones
        val responseS = get("artist/artists")
        responseS?.let {
            val jsonArray = JSONArray(it)
            val fetchedArtists = mutableListOf<Artist>()

            for (i in 0 until 8) {
                val jsonObject = jsonArray.getJSONObject(i)
                fetchedArtists.add(
                    Artist(
                        id = jsonObject.getString("id"),
                        name = jsonObject.getString("name"),
                        biography = "Prueba",
                        photo = jsonObject.getString("photo"),
                    )
                )
            }
            Log.d("Artista", "Artistas: + " + fetchedArtists)
            artists = fetchedArtists
        }

        // Al finalizar la carga de datos, actualizar el estado
        coroutineScope.launch {
            // Calcular el total de imágenes a cargar antes de mostrar contenido
            totalImagesToLoad = artists.size +
                    albums.filter { it.esPublica == "public" }.size +
                    playlists.filter { it.esAlbum == "Vibra" }.size

            // Establecer un tiempo límite para la carga
            delay(100)
            if (isLoadingMain) {
                isLoadingMain = false
                Log.d("Loading", "Forced loading complete after timeout")
            }
        }
    }

    // Mostrar el pop-up de publicidad al entrar a la pantalla
    var showAdvertPopup by remember { mutableStateOf(!isPremium) }

    // Función para incrementar el contador de imágenes cargadas
    val onImageLoaded = {
        loadedImagesCount++
        Log.d("Loading", "Image loaded: $loadedImagesCount/$totalImagesToLoad")
        // Check if all images are loaded
        if (loadedImagesCount >= totalImagesToLoad && totalImagesToLoad > 0) {
            isLoadingMain = false
            Log.d("Loading", "All images loaded")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        if(showAdvertPopup) {
                            LaunchedEffect(Unit) {
                                while (true) {
                                    delay(13000) // 13 segundos (media)
                                    showPaymentDialog = true
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(0.9f))
                        if (!isPremium) {
                            VibraBanner(
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(end = 16.dp, top = 16.dp, bottom = 16.dp),
                                premium = isPremium,
                                onPremiumClick = { showPaymentDialog = true }
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 4.dp)) {
                        UserProfileMenu(navController, playerViewModel)
                    }
                }
            )
        }
    ) { innerPadding ->
        // Envolver todo con verticalScroll
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Estructura corregida - usando una única Column como contenedor
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    /*
                    // 1. Sección de Playlists recientemente visitadas
                    if (isLoadingRecent) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = VibraBlue
                            )
                        }
                    } else */
                    if (recentPlaylists.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp)
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Primera fila con 2 elementos
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                recentPlaylists.take(2).forEach { playlist ->
                                    HorizontalPlaylistCard(
                                        playlist = playlist,
                                        modifier = Modifier.weight(1f),
                                        onClick = { navController.navigate("playlist/${playlist.id}") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Segunda fila con 2 elementos
                            if (recentPlaylists.size > 2) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    recentPlaylists.drop(2).take(2).forEach { playlist ->
                                        HorizontalPlaylistCard(
                                            playlist = playlist,
                                            modifier = Modifier.weight(1f),
                                            onClick = { navController.navigate("playlist/${playlist.id}") }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Tercera fila con 2 elementos
                            if (recentPlaylists.size > 4) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    recentPlaylists.drop(4).take(2).forEach { playlist ->
                                        HorizontalPlaylistCard(
                                            playlist = playlist,
                                            modifier = Modifier.weight(1f),
                                            onClick = { navController.navigate("playlist/${playlist.id}") }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Cuarta fila con 2 elementos
                            if (recentPlaylists.size > 6) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    recentPlaylists.drop(6).take(2).forEach { playlist ->
                                        HorizontalPlaylistCard(
                                            playlist = playlist,
                                            modifier = Modifier.weight(1f),
                                            onClick = { navController.navigate("playlist/${playlist.id}") }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Resto del contenido (con su propio estado de carga)
                    if (isLoadingMain) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = VibraBlue
                            )
                        }
                    } else {
                        // 3. Artistas recomendados
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            "Artistas recomendados",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(artists) { artist ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Card(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clickable { navController.navigate("artist/${artist.id}") },
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            var isImageLoading by remember { mutableStateOf(true) }

                                            if (isImageLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.align(Alignment.Center),
                                                    color = VibraBlue
                                                )
                                            }

                                            AsyncImage(
                                                model = getImageUrl(
                                                    artist.photo,
                                                    "defaultartist.jpg"
                                                ),
                                                contentDescription = null,
                                                placeholder = painterResource(R.drawable.defaultartist),
                                                error = painterResource(R.drawable.defaultartist),
                                                modifier = Modifier
                                                    .size(100.dp)
                                                    .clip(CircleShape),
                                                onLoading = { isImageLoading = true },
                                                onSuccess = {
                                                    isImageLoading = false
                                                    onImageLoaded()
                                                },
                                                onError = {
                                                    isImageLoading = false
                                                    onImageLoaded()
                                                }
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

                        Spacer(modifier = Modifier.height(24.dp))

                        // 4. Álbumes populares
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Álbumes populares",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(albums) { album ->
                                if (album.esPublica == "public") {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Card(
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clickable {
                                                    navController.navigate("playlist/${album.id}")
                                                },
                                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                var isImageLoading by remember { mutableStateOf(true) }

                                                if (isImageLoading) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(40.dp),
                                                        color = VibraBlue
                                                    )
                                                }

                                                val urlAntes = album?.imageUrl
                                                val playlistImage =
                                                    getImageUrl(urlAntes, "/defaultx.jpg")
                                                AsyncImage(
                                                    model = playlistImage,
                                                    placeholder = painterResource(R.drawable.defaultx),
                                                    error = painterResource(R.drawable.defaultx),
                                                    contentDescription = "Portada de la playlist",
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    onLoading = { isImageLoading = true },
                                                    onSuccess = {
                                                        isImageLoading = false
                                                        onImageLoaded()
                                                    },
                                                    onError = {
                                                        isImageLoading = false
                                                        onImageLoaded()
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (album.title.length > 20) {
                                                val midpoint = album.title.substring(0, 20).lastIndexOf(" ").let {
                                                    if (it == -1) 20 else it
                                                }
                                                album.title.substring(0, midpoint) + "\n" + album.title.substring(midpoint)
                                            } else {
                                                album.title
                                            },
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 5. Listas para ti
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Listas para ti",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(playlists) { playlist ->
                                if (playlist?.esAlbum == "Vibra") {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Card(
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clickable {
                                                    navController.navigate("playlist/${playlist.id}")
                                                },
                                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                var isImageLoading by remember { mutableStateOf(true) }

                                                if (isImageLoading) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(40.dp),
                                                        color = VibraBlue
                                                    )
                                                }

                                                val urlAntes = playlist?.imageUrl
                                                val playlistImage =
                                                    getImageUrl(urlAntes, "/defaultplaylist.jpg")
                                                AsyncImage(
                                                    model = playlistImage,
                                                    contentDescription = "Portada de la playlist",
                                                    placeholder = painterResource(R.drawable.defaultplaylist),
                                                    error = painterResource(R.drawable.defaultplaylist),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    onLoading = { isImageLoading = true },
                                                    onSuccess = {
                                                        isImageLoading = false
                                                        onImageLoaded()
                                                    },
                                                    onError = {
                                                        isImageLoading = false
                                                        onImageLoaded()
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            playlist.title,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
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
            modifier = Modifier
                .fillMaxSize()
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

@Composable
fun HorizontalPlaylistCard(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(60.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = VibraMediumGrey
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Imagen pequeña (40x40)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                var isImageLoading by remember { mutableStateOf(true) }

                if (isImageLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = VibraBlue
                    )
                }

                AsyncImage(
                    model = getImageUrl(playlist.imageUrl, "/defaultplaylist.jpg"),
                    contentDescription = "Portada de ${playlist.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = painterResource(id = R.drawable.defaultplaylist),
                    error = painterResource(id = R.drawable.defaultplaylist),
                    onLoading = { isImageLoading = true },
                    onSuccess = { isImageLoading = false },
                    onError = { isImageLoading = false }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Texto de la playlist
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Nombre principal (negrita)
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtítulo (si existe)
                if (playlist.description.isNotBlank()) {
                    Text(
                        text = playlist.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
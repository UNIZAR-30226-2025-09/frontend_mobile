package eina.unizar.es.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import eina.unizar.es.R
import eina.unizar.es.ui.components.UserProfileMenu // Asegúrate de tener este import y el Composable definido
import eina.unizar.es.ui.navbar.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {

    val albumes = listOf("DeBÍ TiRAR MáS FOToS", "Easy Money Baby", "BUENAS NOCHES", "Un Verano Sin Ti")
    val listas = listOf("Éxitos España", "Los 2000 España", "Top 50: Global")

    val cancionesRecomendadas = listOf("Canción A", "Canción B", "Canción C", "Canción D", "Canción E", "Canción F", "Canción G", "Canción H")

    val bottomNavItems = listOf(
        Pair("Inicio", Icons.Default.Home),
        Pair("Buscar", Icons.Default.Search),
        Pair("Biblioteca", Icons.Rounded.Menu),
    )
    var selectedItem by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserProfileMenu(navController)

                        // 🔹 Spacer para empujar el banner hacia la derecha
                        Spacer(modifier = Modifier.weight(0.9f))

                        VibraBanner(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(end = 16.dp, top = 16.dp),
                            premium = false,  // Asumimos que el usuario NO es premium
                            navController = navController // Pasamos el `navController`
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column {
                // Canciones recomendadas en Grid
                Text("Canciones recomendadas", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(cancionesRecomendadas) { cancion ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clickable { navController.navigate("song") },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(cancion, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Álbumes populares
                Text("Álbumes populares", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(albumes.size) { idx ->
                        val album = albumes[idx]
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
                                        painter = painterResource(id = R.drawable.kanyeperfil), // 📌 Imagen dentro del cuadrado
                                        contentDescription = "Álbum",
                                        modifier = Modifier.size(120.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(album, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Listas de reproducción recomendadas
                Text("Listas para ti", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(listas.size) { idx ->
                        val lista = listas[idx]
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
                                    Image(
                                        painter = painterResource(id = R.drawable.kanyeperfil), // 📌 Imagen dentro del cuadrado
                                        contentDescription = "Lista",
                                        modifier = Modifier.size(120.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(lista, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

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
fun VibraBanner(modifier: Modifier = Modifier, premium: Boolean, navController: NavController) {
    val bannerWidth = 160.dp
    val bannerHeight = 50.dp
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF004aad), Color(0xFF00a0d7))
    )

    Box(
        modifier = modifier
            .size(width = bannerWidth, height = bannerHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(gradientBrush)
            .padding(horizontal = 6.dp)
            .clickable {
                if (!premium) {
                    navController.navigate("payment") // Navegar a la pantalla de pago si NO es premium
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
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
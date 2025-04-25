package eina.unizar.es.ui.main

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.webkit.WebView
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient
import eina.unizar.es.ui.player.MusicPlayerViewModel
import kotlinx.coroutines.delay


// Creamos variable para importar a la UI la nueva FontFamily
val Rubik = FontFamily(
    Font(R.font.rubik_font)
)

@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current
    var lock by remember { mutableStateOf(false) }
    var animationFinished by remember { mutableStateOf(false) }

    // Musica en segundo plano y verificación de autenticación
    LaunchedEffect(Unit) {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (!token.isNullOrEmpty()) {
            lock = true
            delay(2000)
            navController.navigate("menu") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    if (!lock) {
        // Efecto para navegar después de que termine la animación
        LaunchedEffect(Unit) {
            // Esperar 3 segundos para que termine la animación
            delay(2000)
            // Establecer que la animación ha terminado
            animationFinished = true
            // Navegar a la pantalla de login
            navController.navigate("login")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    Glide.with(ctx)
                        .asGif()
                        .load(R.raw.inicio_gif) // GIF desde res/raw/
                        .diskCacheStrategy(DiskCacheStrategy.NONE) // Se evita que se save en cache
                        .into(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Contenido encima del GIF
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xAA000000))
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animación SVG con WebView
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        loadDataWithBaseURL(
                            null,
                            """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <style>
                                    body { margin: 0; padding: 0; overflow: hidden; background-color: transparent; }
                                    svg { width: 100%; height: 100%; }
                                </style>
                            </head>
                            <body>
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 400">
                                  <defs>
                                    <style>
                                      @keyframes revealLine {
                                        0% { width: 0; }
                                        30% { width: 100%; }
                                      }
                                      
                                      @keyframes revealLetter {
                                        0% { opacity: 0; transform: translateY(20px); }
                                        100% { opacity: 1; transform: translateY(0); }
                                      }
                                      
                                      @keyframes subtlePulse {
                                        0% { filter: drop-shadow(0 0 0px rgba(255,255,255,0.1)); }
                                        50% { filter: drop-shadow(0 0 4px rgba(255,255,255,0.4)); }
                                        100% { filter: drop-shadow(0 0 0px rgba(255,255,255,0.1)); }
                                      }
                                      
                                      .background {
                                        fill: transparent;
                                      }
                                      
                                      .letter {
                                        font-family: 'Arial', sans-serif;
                                        font-weight: 900;
                                        font-size: 120px;
                                        fill: #ffffff;
                                        opacity: 0;
                                      }
                                      
                                      .letter-v { animation: revealLetter 0.4s ease-out forwards 0.1s; }
                                      .letter-i { animation: revealLetter 0.4s ease-out forwards 0.2s; }
                                      .letter-b { animation: revealLetter 0.4s ease-out forwards 0.3s; }
                                      .letter-r { animation: revealLetter 0.4s ease-out forwards 0.4s; }
                                      .letter-a { animation: revealLetter 0.4s ease-out forwards 0.5s; }
                                      
                                      .line {
                                        stroke: #ffffff;
                                        stroke-width: 2px;
                                        height: 2px;
                                        stroke-dasharray: 1000;
                                        stroke-dashoffset: 1000;
                                        animation: subtlePulse 3s infinite 2s;
                                      }
                                      
                                      .top-line {
                                        animation: revealLine 1s ease-out forwards 0.7s;
                                        width: 0;
                                        height: 2px;
                                        background-color: white;
                                      }
                                      
                                      .bottom-line {
                                        animation: revealLine 1s ease-out forwards 0.9s;
                                        width: 0;
                                        height: 2px;
                                        background-color: white;
                                      }
                                      
                                      .container {
                                        animation: subtlePulse 3s infinite 2s;
                                      }
                                    </style>
                                  </defs>
                                  
                                  <rect class="background" width="800" height="400" />
                                  
                                  <!-- Contenedor para el texto y las líneas -->
                                  <g class="container">
                                    <!-- Línea superior -->
                                    <rect x="250" y="170" width="300" height="2" class="top-line" />
                                    
                                    <!-- Letras individuales animadas -->
                                    <text x="250" y="250" class="letter letter-v">V</text>
                                    <text x="320" y="250" class="letter letter-i">I</text>
                                    <text x="350" y="250" class="letter letter-b">B</text>
                                    <text x="420" y="250" class="letter letter-r">R</text>
                                    <text x="490" y="250" class="letter letter-a">A</text>
                                    
                                    <!-- Línea inferior -->
                                    <rect x="250" y="270" width="300" height="2" class="bottom-line" />
                                  </g>
                                </svg>
                            </body>
                            </html>
                            """,
                            "text/html",
                            "UTF-8",
                            null
                        )
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}
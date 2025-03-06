package eina.unizar.es.ui.main

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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eina.unizar.es.R

// Creamos variable para importar a la UI la nueva FontFamily
val Rubik = FontFamily(
    Font(R.font.rubik_font)
)

@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current

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
        ) { // Y dentro ...
            Image(
                painter = painterResource(id = R.drawable.vibrablanco), // Logo
                contentDescription = "Logo de Vibra",
                modifier = Modifier
                    .size(150.dp)
                    .padding(bottom = 16.dp)
            )

            // Texto de Bienvenida
            Text(
                text = "Bienvenido a Vibra",
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = Rubik
            )

            // Separador
            Spacer(modifier = Modifier.height(20.dp))

            // Boton de comenzar
            Button(
                onClick = { navController.navigate("player") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(android.graphics.Color.parseColor("#79e2ff")),
                    contentColor = Color.Black
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(8.dp)
            ) { // Dentro del boton el siguiente texto
                Text(
                    text = "Comenzar",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

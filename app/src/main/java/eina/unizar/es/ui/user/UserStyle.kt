package eina.unizar.es.ui.user

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.bumptech.glide.Glide
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import eina.unizar.es.data.model.network.ApiClient
import kotlinx.coroutines.launch
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.musicapp.ui.theme.VibraBlue
import com.example.musicapp.ui.theme.VibraLightGrey
import eina.unizar.es.R
import eina.unizar.es.ui.auth.registerUser

class UserStyleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Deshabilitar el botón de retroceso
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // No hacer nada para impedir volver atrás
            }
        })

        setContent {
            val navController = rememberNavController()
            UserStyleScreen(
                navController = navController,
                context = this
            )
        }
    }
}

@Composable
fun UserStyleScreen(
    navController: NavController,
    context: Context
) {
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Obtener datos del usuario de SharedPreferences
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val username = sharedPreferences.getString("temp_username", "") ?: ""
    val email = sharedPreferences.getString("temp_email", "") ?: ""
    val password = sharedPreferences.getString("temp_password", "") ?: ""
    val confirmPassword = sharedPreferences.getString("temp_confirm_password", "") ?: ""

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // GIF de fondo (sin cambios)
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    Glide.with(ctx)
                        .asGif()
                        .load(R.raw.inicio_gif)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay (sin cambios)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212).copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Contenido existente sin cambios...
                    Text(
                        text = "Selecciona tu estilo favorito",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Botones de género (sin cambios)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GenreButton(
                            genre = "Pop",
                            isSelected = selectedGenre == "Pop",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedGenre = "Pop" }
                        )

                        GenreButton(
                            genre = "Rock",
                            isSelected = selectedGenre == "Rock",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedGenre = "Rock" }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GenreButton(
                            genre = "Reggaeton",
                            isSelected = selectedGenre == "Reggaeton",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedGenre = "Reggaeton" }
                        )

                        GenreButton(
                            genre = "Flamenco",
                            isSelected = selectedGenre == "Flamenco",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedGenre = "Flamenco" }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GenreButton(
                            genre = "Rap",
                            isSelected = selectedGenre == "Rap",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedGenre = "Rap" }
                        )

                        GenreButton(
                            genre = "Trap",
                            isSelected = selectedGenre == "Trap",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedGenre = "Trap" }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mensaje de error
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Botón Continuar MODIFICADO para usar registerUser
                    Button(
                        onClick = {
                            selectedGenre?.let { genre ->
                                isLoading = true
                                errorMessage = null

                                coroutineScope.launch {
                                    try {
                                        // Llamar a registerUser con el género seleccionado
                                        val result = registerUser(
                                            username,
                                            email,
                                            password,
                                            confirmPassword,
                                            context,
                                            genre // Enviar el género seleccionado
                                        )

                                        // Verificar el resultado y mostrar mensaje de error si existe
                                        if (result.first) {
                                            // Éxito - navegar a login
                                            navController.navigate("login")
                                            Toast.makeText(
                                                context,
                                                "Registro exitoso",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            // Mostrar el mensaje de error específico
                                            errorMessage = result.second ?: "No se pudo completar el registro"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error: ${e.message}"
                                        Log.e("UserStyle", "Error registrando: ${e.message}", e)
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VibraBlue,
                            disabledContainerColor = Color.DarkGray.copy(alpha = 0.5f)
                        ),
                        enabled = selectedGenre != null && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "Continuar",
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GenreButton(
    genre: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(70.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) VibraLightGrey else Color.DarkGray
        )
    ) {
        Text(
            text = genre,
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
package eina.unizar.es.ui.auth

import android.content.Context
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import eina.unizar.es.R
import eina.unizar.es.data.model.network.ApiClient
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRegisterScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) } // Estado para el mensaje de error


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)), // Fondo oscuro
        contentAlignment = Alignment.Center
    ) {
        // GIF de fondo
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

        // Overlay para mejorar legibilidad
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212).copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), // Color de la tarjeta
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Logo de Vibra
                    Icon(
                        painter = painterResource(id = R.drawable.vibrablanco),
                        contentDescription = "Logo de Vibra",
                        tint = Color.White,
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Título
                    Text(
                        text = "Regístrate en Vibra",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 0.dp, bottom = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = { Text("Nombre de usuario") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "User Icon"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = Color.White,
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Correo electrónico") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Email Icon"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = Color.White,
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("********") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Password Icon"
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = Color.White,
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = { Text("********") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Confirm Password Icon"
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = Color.White,
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Mensaje de error (si existe)
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFFF6B6B),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Botón de Registrarse
                    Button(
                        onClick = {
                            if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                                errorMessage = "Completa todos los campos"
                            } else if (!isEmailValid(email)) {
                                errorMessage = "El formato de correo no es valido"
                            } else {
                                errorMessage = null

                                // Guardar datos en SharedPreferences
                                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                sharedPreferences.edit().apply {
                                    putString("temp_username", username)
                                    putString("temp_email", email)
                                    putString("temp_password", password)
                                    putString("temp_confirm_password", confirmPassword)
                                    apply()
                                }

                                // Navegar a la pantalla de selección de estilo
                                navController.navigate("styles")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF79E2FF),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Registrarse",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "o",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Texto de inicio de sesión
                    val loginText = buildAnnotatedString {
                        append("¿Ya tienes cuenta? ")
                        pushStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold))
                        append("Inicia sesión en Vibra")
                        pop()
                    }

                    Text(
                        text = loginText,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.clickable { navController.navigate("login") }
                            .padding(bottom = 8.dp)
                    )

                    val subscriptionText = buildAnnotatedString {
                        append("Si quieres saber más sobre nuestras subs, visita")
                    }

                    val subs = buildAnnotatedString {
                        pushStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold))
                        append("Vibra Suscripciones.")
                        pop()
                    }

                    Text(
                        text = subscriptionText,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )

                    Text(
                        text = subs,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable { navController.navigate("plans?isViewOnly=true") }
                            .padding(top = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * Realiza la petición de registro a la API y devuelve un par con:
 * - Boolean: true si el registro fue exitoso, false en caso contrario
 * - String?: mensaje de error en caso de fallo, null si fue exitoso
 */
suspend fun registerUser(
    username: String,
    email: String,
    password: String,
    confirmPassword: String,
    context: Context,
    styleFav: String,
): Pair<Boolean, String?> {
    if (password != confirmPassword) {
        return Pair(false, "Las contraseñas no coinciden")
    }

    val jsonBody = JSONObject().apply {
        put("nickname", username)
        put("mail", email)
        put("password", password)
        put("style_fav", styleFav)
    }

    Log.d("RegisterRequest", "JSON enviado: $jsonBody")

    return try {
        val (statusCode, response) = ApiClient.postWithCode("user/register", jsonBody)

        when (statusCode) {
            201 -> {
                Log.d("RegisterSuccess", "Registro exitoso")
                Pair(true, null)
            }
            400 -> {
                Log.e("RegisterError", "El correo ya está en uso")
                Pair(false, "El correo electrónico ya está en uso")
            }
            409 -> {
                Log.e("RegisterError", "El nombre de usuario ya está en uso")
                Pair(false, "El nombre de usuario ya está en uso")
            }
            in 500..599 -> {
                val errorMessage = response ?: "Error del servidor"
                Log.e("RegisterError", "Error del servidor ($statusCode): $errorMessage")
                Pair(false, "Error del servidor: $errorMessage")
            }
            else -> {
                val errorMessage = response ?: "Error desconocido"
                Log.e("RegisterError", "Error inesperado ($statusCode): $errorMessage")
                Pair(false, "Error inesperado: $errorMessage")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("RegisterError", "Error de conexión: ${e.message}")
        Pair(false, "Error de conexión: ${e.message}")
    }
}

/*
* Funcion para comprobar que el formato del correo es válido
*/
fun isEmailValid(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    return emailRegex.matches(email)
}
package eina.unizar.es.ui.auth

import android.content.Context
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserLoginScreen(navController: NavController, returnTo: String = "") {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var dialogEmail by remember { mutableStateOf("") }

    val returnToRoute = navController
        .currentBackStackEntry
        ?.arguments
        ?.getString("returnTo") ?: ""

    LaunchedEffect(Unit) {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (!token.isNullOrEmpty()) {
            navController.navigate("menu") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
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
                    // Logo
                    Icon(
                        painter = painterResource(id = R.drawable.logoblanco),
                        contentDescription = "Logo de Vibra",
                        tint = Color.White,
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Iniciar Sesión en Vibra",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 0.dp, bottom = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null // Limpiar error cuando el usuario escribe
                        },
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

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null // Limpiar error cuando el usuario escribe
                        },
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


                    errorMessage?.let {
                        Text(
                            text = it,
                            color = Color(0xFFFF5252), // Rojo más suave
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Botón Continuar con ancho reducido
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val (loginSuccess, error) = loginUser(context, email, password)
                            if (loginSuccess) {
                                if (returnToRoute.isNotEmpty()) {
                                    navController.navigate(returnToRoute) {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("menu") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                                Toast.makeText(
                                    context,
                                    "Sesión iniciada con éxito",
                                    Toast.LENGTH_LONG
                                ).show()
                                errorMessage = null
                            } else {
                                errorMessage = error
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF79E2FF),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f) // Reducir el ancho al 70%
                        .height(50.dp)
                        .align(Alignment.CenterHorizontally) // Centrar horizontalmente
                ) {
                    Text(
                        text = "Continuar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "o",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally) // Centrar horizontalmente
                )

                Spacer(modifier = Modifier.height(12.dp))

                val registerText = buildAnnotatedString {
                    append("¿No tienes una cuenta? ")
                    pushStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold))
                    append("Regístrate en Vibra")
                    pop()
                }

                Text(
                    text = registerText,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center, // Alinear texto centrado
                    modifier = Modifier
                        .clickable { navController.navigate("register") }
                        .padding(8.dp)
                        .fillMaxWidth() // Ocupar todo el ancho para poder centrarlo
                        .align(Alignment.CenterHorizontally), // Centrar horizontalmente
                )

                Text(
                    text = "¿Has olvidado tu contraseña?",
                    color = Color.White,
                    textAlign = TextAlign.Center, // Ya tenía alineación centrada
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            dialogEmail = email // Pre-carga el email si ya estaba escrito
                            showForgotPasswordDialog = true
                        }
                        .padding(top = 4.dp)
                        .fillMaxWidth() // Ocupar todo el ancho para poder centrarlo
                        .align(Alignment.CenterHorizontally) // Centrar horizontalmente
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Diálogo de recuperación de contraseña
    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            onDismiss = {
                showForgotPasswordDialog = false
                dialogEmail = ""
            },
            onConfirm = { emailValue ->
                dialogEmail = emailValue
                coroutineScope.launch {
                    handleForgotPassword(context, emailValue)
                    showForgotPasswordDialog = false
                }
            },
            initialEmail = dialogEmail
        )
    }
}

/**
 * Realiza la petición de login a la API y devuelve un par con:
 * - Boolean: true si las credenciales son correctas, false en caso contrario
 * - String?: mensaje de error en caso de fallo, null si fue exitoso
 */
suspend fun loginUser(context: Context, email: String, password: String): Pair<Boolean, String?> {
    return withContext(Dispatchers.IO) {
        if (email.isBlank()) {
            return@withContext Pair(false, "El correo electrónico no puede estar vacío")
        }

        if (password.isBlank()) {
            return@withContext Pair(false, "La contraseña no puede estar vacía")
        }

        val jsonBody = JSONObject().apply {
            put("mail", email)
            put("password", password)
        }

        Log.d("LoginRequest", "JSON enviado: $jsonBody")

        try {
            val responseHeaders = mutableMapOf<String, String>()
            val (statusCode, response) = ApiClient.postWithCode("user/login", jsonBody)

            when (statusCode) {
                in 200..299 -> {
                    // Intentar recuperar el token desde la cabecera
                    var token = responseHeaders["Authorization"]?.replace("Bearer ", "")

                    // Si no está en la cabecera, buscarlo en el JSON de respuesta
                    if (token.isNullOrEmpty() && response != null) {
                        val jsonResponse = JSONObject(response)
                        if (jsonResponse.has("token")) {
                            token = jsonResponse.getString("token")
                        }
                    }

                    if (!token.isNullOrEmpty()) {
                        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit().putString("auth_token", token).apply()
                        Log.d("Login", "Token guardado correctamente: $token")
                        return@withContext Pair(true, null)
                    } else {
                        Log.e("LoginError", "No se recibió el token ni en la cabecera ni en el JSON")
                        return@withContext Pair(false, "Error al procesar la respuesta del servidor")
                    }
                }
                400 -> {
                    Log.e("LoginError", "Credenciales inválidas")
                    return@withContext Pair(false, "Correo o contraseña incorrectos")
                }
                401 -> {
                    Log.e("LoginError", "No autorizado")
                    return@withContext Pair(false, "Correo o contraseña incorrectos")
                }
                403 -> {
                    Log.e("LoginError", "Usuario no encontrado")
                    return@withContext Pair(false, "Esta cuenta está conectada en otro dispositivo")
                }
                in 500..599 -> {
                    val errorMessage = response ?: "Error del servidor"
                    Log.e("LoginError", "Error del servidor ($statusCode): $errorMessage")
                    return@withContext Pair(false, "Error del servidor: $errorMessage")
                }
                else -> {
                    val errorMessage = response ?: "Error desconocido"
                    Log.e("LoginError", "Error inesperado ($statusCode): $errorMessage")
                    return@withContext Pair(false, "Error inesperado: $errorMessage")
                }
            }
        } catch (e: Exception) {
            Log.e("LoginError", "Error en loginUser: ${e.message}", e)
            return@withContext Pair(false, "Error de conexión: ${e.message}")
        }
    }
}

// Gestion cuando el usuario ha olvidad la contraseña de su cuenta
private suspend fun handleForgotPassword(context: Context, email: String) {
    val success = ApiClient.forgotPassword(email, context)

    withContext(Dispatchers.Main) {
        if (success) {
            Toast.makeText(
                context,
                "Se ha enviado un correo para restablecer tu contraseña",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "Error al enviar el correo. Verifica tu dirección.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initialEmail: String
) {
    var email by remember { mutableStateOf(initialEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Recuperar contraseña",
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = {
                        Text(
                            "Correo electrónico",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Color.White,
                        focusedBorderColor = Color(0xFF79E2FF),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Botón Cancelar
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Gray
                    ),
                    border = BorderStroke(1.dp, Color.Gray),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar",
                        color = Color.White,)
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Botón Enviar
                Button(
                    onClick = { onConfirm(email) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF79E2FF),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Enviar")
                }
            }
        },
        dismissButton = {} // Eliminamos el dismissButton por defecto
    )
}
package eina.unizar.es.ui.auth

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserLoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) } // Nuevo estado para el error
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var dialogEmail by remember { mutableStateOf("") }

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
            .background(Color(0xFF121212)),
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
                    onValueChange = { email = it },
                    placeholder = { Text("Correo electrónico") },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon") },
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
                    onValueChange = { password = it },
                    placeholder = { Text("********") },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Color.White,
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                if (showError) { // Mostrar el error si showError es true
                    Text(
                        text = "Correo o contraseña incorrectos.",
                        color = Color(0xFFFF6B6B),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val loginSuccess = loginUser(context, email, password)
                            if (loginSuccess) {
                                navController.navigate("menu")
                                Toast.makeText(context, "Sesión iniciada con éxito", Toast.LENGTH_LONG).show()
                                showError = false // Ocultar el mensaje de error si el login es correcto.
                            } else {
                                showError = true // Mostrar el mensaje de error si el login falla.
                            }
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
                        text = "Continuar",
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
                    modifier = Modifier.clickable { navController.navigate("register") }
                        .padding(8.dp),
                )

                Text(
                    text = "¿Has olvidado tu contraseña?",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        //.align(Alignment.End)
                        .clickable {
                            coroutineScope.launch {
                                Log.d("Email", "El email de recuperacion es: " + email)
                                dialogEmail = email // Pre-carga el email si ya estaba escrito
                                showForgotPasswordDialog = true
                            }
                        }
                        .padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        // Diálogo de recuperación
        if (showForgotPasswordDialog) {
            ForgotPasswordDialog(
                onDismiss = {
                    showForgotPasswordDialog = false
                    dialogEmail = ""
                },
                onConfirm = { email ->
                    dialogEmail = email
                    coroutineScope.launch {
                        handleForgotPassword(context, email)
                        showForgotPasswordDialog = false
                    }
                },
                initialEmail = dialogEmail
            )
        }
    }
}

/**
 * Realiza la petición de login a la API y devuelve `true` si las credenciales son correctas.
 */
suspend fun loginUser(context: Context, email: String, password: String): Boolean {
    return withContext(Dispatchers.IO) {
        val jsonBody = JSONObject().apply {
            put("mail", email)
            put("password", password)
        }

        Log.d("LoginRequest", "JSON enviado: $jsonBody")

        try {
            val responseHeaders = mutableMapOf<String, String>()
            val response = ApiClient.postWithHeaders("user/login", jsonBody, context, responseHeaders)

            if (response != null) {
                val jsonResponse = JSONObject(response)
                val httpStatus = jsonResponse.optInt("status", 200) // Por si el backend devuelve código HTTP en JSON

                // Verificamos si el servidor ha respondido correctamente
                if (httpStatus in 200..299) {

                    // Intentar recuperar el token desde la cabecera
                    var token = responseHeaders["Authorization"]?.replace("Bearer ", "")

                    // Si no está en la cabecera, buscarlo en el JSON de respuesta
                    if (token.isNullOrEmpty() && jsonResponse.has("token")) {
                        token = jsonResponse.getString("token")
                    }

                    if (!token.isNullOrEmpty()) {
                        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit().putString("auth_token", token).apply()  // Cambiado a apply() para mejor rendimiento

                        Log.d("Login", "Token guardado correctamente: $token")

                        /*withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Inicio de Sesión Exitoso", Toast.LENGTH_LONG).show()
                        }*/

                        return@withContext true
                    } else {
                        Log.e("LoginError", "No se recibió el token ni en la cabecera ni en el JSON")
                    }
                } else {
                    Log.e("LoginError", "Respuesta del servidor con error: Código HTTP $httpStatus")
                }
            } else {
                Log.e("LoginError", "Respuesta nula del servidor")
            }

            return@withContext false
        } catch (e: Exception) {
            Log.e("LoginError", "Error en loginUser: ${e.message}", e)
            return@withContext false
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
                    Text("Cancelar")
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
package eina.unizar.es.ui.auth

import android.content.Context
import android.util.Log
import android.widget.Toast
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Correo Electrónico",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Start)
                        .padding(top = 0.dp, bottom = 4.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Contraseña",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Start)
                        .padding(top = 0.dp, bottom = 4.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

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

                Spacer(modifier = Modifier.height(20.dp))

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

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "o",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(20.dp))

                val registerText = buildAnnotatedString {
                    append("¿No tienes una cuenta? ")
                    pushStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold))
                    append("Regístrate en Vibra.")
                    pop()
                }

                Text(
                    text = registerText,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.clickable { navController.navigate("register") }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
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
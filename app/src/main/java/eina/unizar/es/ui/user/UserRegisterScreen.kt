package eina.unizar.es.ui.auth

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRegisterScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)), // Fondo oscuro
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

                // Campo de Nombre de Usuario
                Text(
                    text = "Nombre de Usuario",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Start)
                        .padding(top = 0.dp, bottom = 4.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("Nombre de usuario") },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User Icon") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Color.White,
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campo de Correo Electrónico
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

                // Campo de Contraseña
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

                Spacer(modifier = Modifier.height(12.dp))

                // Campo de Confirmar Contraseña
                Text(
                    text = "Confirmar Contraseña",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Start)
                        .padding(top = 0.dp, bottom = 4.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = { Text("********") },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password Icon") },
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

                // Botón de Registrarse
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val loginSuccess = registerUser(username, email, password, confirmPassword)
                            if (loginSuccess) {
                                navController.navigate("login")
                                Toast.makeText(context, "Cuenta creada correctamente", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Error al registrarse", Toast.LENGTH_LONG).show()
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
                        text = "Registrarse",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Texto de inicio de sesión
                val loginText = buildAnnotatedString {
                    append("¿Ya tienes cuenta? ")
                    pushStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold))
                    append("Inicia sesión en Vibra.")
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
                    modifier = Modifier.clickable { navController.navigate("plans") }
                        .padding(top = 3.dp)
                )
            }
        }
    }
}

/**
 * Realiza la petición de registro a la API y devuelve `true` si el registro fue exitoso.
 * Retorna `false` si hay un error o si las contraseñas no coinciden.
 */
suspend fun registerUser(username: String, email: String, password: String, confirmPassword: String): Boolean {
    if (password != confirmPassword) {
        Log.e("RegisterError", "Las contraseñas no coinciden")
        return false
    }

    val jsonBody = JSONObject().apply {
        put("nickname", username)
        put("mail", email)
        put("password", password)
        put("style_fav", "rock") // Ajusta según la lógica de la app
    }

    Log.d("RegisterRequest", "JSON enviado: $jsonBody")

    return try {
        val response = ApiClient.post("user/register", jsonBody)
        if (response != null) {
            val jsonResponse = JSONObject(response)
            val message = jsonResponse.optString("message", "")

            if (message.contains("registrado con éxito", ignoreCase = true)) {
                Log.d("RegisterSuccess", "Registro exitoso")
                return true
            } else {
                Log.e("RegisterError", "Error en el registro: $message")
                return false
            }
        } else {
            Log.e("RegisterError", "El servidor respondió con null")
            return false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("RegisterError", "Error de conexión: ${e.message}")
        return false
    }
}
package eina.unizar.es.ui.user

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.Log
import androidx.navigation.NavController
import com.example.musicapp.ui.theme.VibraDarkGrey
import eina.unizar.es.data.model.network.ApiClient.getUserData
import eina.unizar.es.data.model.network.ApiClient.postTokenPremium
import eina.unizar.es.data.model.network.ApiClient.updateUserProfile
import eina.unizar.es.ui.auth.loginUser
import eina.unizar.es.ui.main.Rubik
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun EditProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backgroundColor = Color(0xFF000000)
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var originalEmail by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    val errorColor = Color(0xFFFF5252)
    var usernameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var currentPasswordError by remember { mutableStateOf(false) }
    var passwordMismatchError by remember { mutableStateOf(false) }

    // Cargar datos del usuario
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val userData = getUserData(context)
            if (userData != null) {
                username = (userData["nickname"] ?: "").toString()
                email = (userData["mail"] ?: "").toString()
                originalEmail = email
            }
            isLoading = false
        }
    }

    // Diálogo de confirmación
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text(
                    "Confirmar cambios",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Rubik,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            },
            text = {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2D2D2D)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Resumen de cambios:",
                                color = Color(0xFF79E2FF),
                                fontFamily = Rubik,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Divider(color = Color(0xFF444444), thickness = 1.dp)

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Nombre: ",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontFamily = Rubik,
                                    fontSize = 14.sp,
                                    modifier = Modifier.width(120.dp)
                                )
                                Spacer(modifier = Modifier.width(60.dp))
                                Text(
                                    username,
                                    color = Color.White,
                                    fontFamily = Rubik,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Correo electrónico: ",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontFamily = Rubik,
                                    fontSize = 14.sp,
                                    modifier = Modifier.width(120.dp)
                                )
                                Spacer(modifier = Modifier.width(60.dp))
                                Text(
                                    email,
                                    color = Color.White,
                                    fontFamily = Rubik,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }

                            if (newPassword.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Contraseña: ",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontFamily = Rubik,
                                        fontSize = 14.sp,
                                        modifier = Modifier.width(120.dp)
                                    )
                                    Spacer(modifier = Modifier.width(60.dp))
                                    Text(
                                        "••••••",
                                        color = Color.White,
                                        fontFamily = Rubik,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "¿Confirmas que deseas guardar estos cambios?",
                        color = Color.White.copy(alpha = 0.9f),
                        fontFamily = Rubik,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        coroutineScope.launch {
                            val jsonBody = JSONObject().apply {
                                put("nickname", username)
                                put("mail", email)
                                if (newPassword.isNotBlank()) put("password", newPassword)
                            }

                            val response = postTokenPremium("user/update", jsonBody, context)

                            if (response != null) {
                                Toast.makeText(
                                    context,
                                    "Perfil actualizado correctamente",
                                    Toast.LENGTH_LONG
                                ).show()
                                navController.popBackStack()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error al actualizar el perfil",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF79E2FF),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text("Confirmar", fontFamily = Rubik, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFF79E2FF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(end = 24.dp)
                ) {
                    Text("Cancelar", fontFamily = Rubik)
                }
            }
        )
    }


    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fondo con olas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VibraDarkGrey)
            ) {
                val width = size.width
                val height = size.height
                val path = Path().apply {
                    moveTo(0f, height * 0.8f)
                    quadraticBezierTo(width * 0.25f, height * 0.95f, width * 0.5f, height * 0.85f)
                    quadraticBezierTo(width * 0.75f, height * 0.75f, width, height * 0.9f)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF339CFF), Color(0xFFB3D9FF)),
                        startY = height * 0.7f,
                        endY = height
                    ),
                    style = Fill
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Editar perfil",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontFamily = Rubik,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 85.dp, bottom = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Campos obligatorios
                TextFieldWithLabel(
                    label = "Nombre de usuario",
                    value = username,
                    onValueChange = {
                        username = it
                        usernameError = it.isEmpty()
                    },
                    isUsername = true,
                    isError = usernameError
                )


                if (usernameError) {
                    Text(
                        "Este campo es obligatorio",
                        color = errorColor,
                        fontSize = 12.sp,
                        fontFamily = Rubik,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                TextFieldWithLabel(
                    label = "Correo electrónico",
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = it.isEmpty()
                    },
                    isEmail = true,
                    isError = emailError
                )
                if (emailError) {
                    Text(
                        "Este campo es obligatorio",
                        color = errorColor,
                        fontSize = 12.sp,
                        fontFamily = Rubik,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                TextFieldWithLabel(
                    label = "Contraseña Actual",
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        currentPasswordError = it.isEmpty()
                    },
                    isPassword = true,
                    isError = currentPasswordError
                )
                if (currentPasswordError) {
                    Text(
                        "Este campo es obligatorio",
                        color = errorColor,
                        fontSize = 12.sp,
                        fontFamily = Rubik,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Nueva contraseña (opcional)
                TextFieldWithLabel(
                    label = "Nueva Contraseña (opcional)",
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        passwordMismatchError = false
                    },
                    isPassword = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Cancelar", color = Color.White)
                    }
                    Button(
                        onClick = {
                            // Validar solo campos obligatorios
                            usernameError = username.isEmpty()
                            emailError = email.isEmpty()
                            currentPasswordError = currentPassword.isEmpty()

                            if (!usernameError && !emailError && !currentPasswordError) {
                                coroutineScope.launch {
                                    val (code, message) = updateUserProfile(
                                        currentPassword = currentPassword,
                                        nickname = username,
                                        email = email,
                                        password = newPassword,
                                        context = context
                                    )
                                    when (code) {
                                        200 -> {
                                            Toast.makeText(
                                                context,
                                                "Perfil actualizado",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            showDialog = false
                                        }

                                        400 -> Toast.makeText(
                                            context,
                                            "Correo ya registrado",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        404 -> Toast.makeText(
                                            context,
                                            "Usuario no encontrado",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        409 -> Toast.makeText(
                                            context,
                                            "Nombre en uso",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        500 -> Toast.makeText(
                                            context,
                                            "Error del servidor",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        else -> Toast.makeText(
                                            context,
                                            "Error inesperado",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Por favor, complete los campos obligatorios",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF79E2FF)),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            "Guardar perfil",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFieldWithLabel(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    isEmail: Boolean = false,
    isUsername: Boolean = false,
    isError: Boolean = false
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    label,
                    color = Color(0xFFBBBBBB),
                    fontFamily = Rubik
                )
            },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontFamily = Rubik),
            leadingIcon = {
                if (isEmail) Icon(
                    Icons.Default.Email,
                    contentDescription = "Email Icon",
                    tint = if (isError) Color(0xFFFF5252) else Color.White
                )
                if (isPassword) Icon(
                    Icons.Default.Lock,
                    contentDescription = "Password Icon",
                    tint = if (isError) Color(0xFFFF5252) else Color.White
                )
                if (isUsername) Icon(
                    Icons.Default.Person,
                    contentDescription = "Username Icon",
                    tint = if (isError) Color(0xFFFF5252) else Color.White
                )
            },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                cursorColor = Color.White,
                focusedBorderColor = if (isError) Color(0xFFFF5252) else Color.Gray,
                unfocusedBorderColor = if (isError) Color(0xFFFF5252) else Color.DarkGray
                //leadingIconColor = if (isError) Color(0xFFFF5252) else Color.White
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = when {
                    isPassword -> KeyboardType.Password
                    isEmail -> KeyboardType.Email
                    else -> KeyboardType.Text
                }
            ),
            isError = isError
        )
    }
}



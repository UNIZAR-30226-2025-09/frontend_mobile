package eina.unizar.es.ui.user

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import eina.unizar.es.ui.main.Rubik

@Composable
fun EditProfileScreen(navController: NavController) {
    val backgroundColor = Color(0xFF000000)   // Negro
    val textColor = Color(0xFFFFFFFF)         // Blanco
    val buttonColor = Color(0xFF79E2FF)       // Azul claro

    var username by remember { mutableStateOf("warapmamn") }
    var email by remember { mutableStateOf("warapmamn@gmail.com") }
    var password by remember { mutableStateOf("******") }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Fondo con olas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            val width = size.width
            val height = size.height

            val path = Path().apply {
                moveTo(0f, height * 0.8f) // Comienza abajo

                // Varias ondas para más efecto visual
                quadraticBezierTo(width * 0.25f, height * 0.95f, width * 0.5f, height * 0.85f)
                quadraticBezierTo(width * 0.75f, height * 0.75f, width, height * 0.9f)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF339CFF), Color(0xFFB3D9FF)), // Degradado azul
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
            // Título
            Text(
                "Editar perfil",
                color = textColor,
                fontSize = 26.sp,
                fontFamily = Rubik,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 85.dp, bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Campos de texto
            TextFieldWithLabel(label = "Nombre de usuario", value = username, onValueChange = { username = it }, isUsername = true)
            Spacer(modifier = Modifier.height(16.dp))
            TextFieldWithLabel(label = "Correo electrónico", value = email, onValueChange = { email = it }, isEmail = true)
            Spacer(modifier = Modifier.height(16.dp))
            TextFieldWithLabel(label = "Contraseña", value = password, onValueChange = { password = it }, isPassword = true)

            Spacer(modifier = Modifier.height(16.dp))

            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Cancelar", color = Color.White)
                }
                Button(
                    onClick = { /* Guardar cambios */ },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = RoundedCornerShape(50.dp),
                ) {
                    Text("Guardar perfil", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Composable para los campos de texto
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFieldWithLabel(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    isEmail: Boolean = false,
    isUsername: Boolean = false
) {
    Column {
        Text(label, color = Color.White, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(label, color = Color(0xFFBBBBBB)) }, // Placeholder en gris claro
            singleLine = true,
            textStyle = TextStyle(color = Color.White), // Texto que escribe el usuario en blanco
            leadingIcon = {
                if (isEmail) Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = Color.White)
                if (isPassword) Icon(Icons.Default.Lock, contentDescription = "Password Icon", tint = Color.White)
                if (isUsername) Icon(Icons.Default.Person, contentDescription = "Username Icon", tint = Color.White)
            },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                cursorColor = Color.White, // Cursor en blanco
                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = Color.DarkGray
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = when {
                    isPassword -> KeyboardType.Password
                    isEmail -> KeyboardType.Email
                    else -> KeyboardType.Text
                }
            )
        )
    }
}

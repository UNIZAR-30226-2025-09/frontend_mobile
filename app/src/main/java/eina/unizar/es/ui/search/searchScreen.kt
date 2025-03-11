package eina.unizar.es.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import eina.unizar.es.ui.navbar.BottomNavigationBar // Importamos la NavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    // Fondo ligeramente más claro que el negro puro
    val backgroundColor = Color(0xFF1E1E1E)
    // Colores para la barra de búsqueda
    val searchBarUnfocusedColor = Color.White
    val searchTextUnfocusedColor = Color.Black
    val searchBarFocusedColor = Color.Black
    val searchTextFocusedColor = Color.White

    // Otros colores
    val textColor = Color.White
    val buttonColor = Color(0xFF0D47A1)
    val cardBackgroundColor = Color(0xFF121212)

    // Estado de la consulta de búsqueda
    var searchQuery by remember { mutableStateOf("") }
    // Historial simulado de búsquedas
    val searchHistory = remember {
        mutableStateListOf("Canción 1", "Canción 2", "Playlist 1", "Canción 3", "Playlist 2")
    }

    // InteractionSource para detectar foco en el TextField
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Definimos colores según el foco
    val currentContainerColor = if (isFocused) searchBarFocusedColor else searchBarUnfocusedColor
    val currentTextColor = if (isFocused) searchTextFocusedColor else searchTextUnfocusedColor

    // ✅ Usamos Scaffold para manejar la barra inferior correctamente
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(backgroundColor)
                .padding(16.dp)
        ) {
            // Título "Buscar" en grande
            Text(
                text = "Buscar",
                fontSize = 32.sp,
                color = textColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Barra de búsqueda ocupando todo el ancho
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar...", color = currentTextColor) },
                textStyle = TextStyle(color = currentTextColor),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = currentContainerColor,
                    focusedBorderColor = buttonColor,
                    unfocusedBorderColor = currentTextColor,
                    cursorColor = currentTextColor,
                    focusedLabelColor = currentTextColor,
                    unfocusedLabelColor = currentTextColor
                ),
                interactionSource = interactionSource
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ LazyColumn con `Modifier.weight(1f)` para que termine antes y no tape la navbar
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Título de historial, si hay búsquedas
                if (searchHistory.isNotEmpty()) {
                    item {
                        Text(
                            text = "Últimas búsquedas",
                            fontSize = 20.sp,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                // Lista de búsquedas recientes presentadas como banners (Cards)
                items(searchHistory) { result ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Imagen placeholder (cuadrado)
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(Color.DarkGray)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = result,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                // Si es "Canción", mostramos "Artista X", si no, "Playlist"
                                if (result.startsWith("Canción")) {
                                    Text(
                                        text = "Artista X",
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        text = "Playlist",
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ✅ Botón de limpiar historial (estrecho y centrado)
            if (searchHistory.isNotEmpty()) {
                Button(
                    onClick = { searchHistory.clear() },
                    modifier = Modifier
                        .width(200.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                ) {
                    Text("Limpiar historial", color = Color.White)
                }
            }
        }
    }
}

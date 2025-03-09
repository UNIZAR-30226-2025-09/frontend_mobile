package eina.unizar.es.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import eina.unizar.es.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(navController : NavController) {
    var selectedFilter by remember { mutableStateOf("Playlists") }
    val libraryItems = remember { getLibraryItems(selectedFilter) }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Top Bar
        TopAppBar(
            title = { Text("Tu Biblioteca", color = Color.White, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Black
            )
        )

        // Lista de elementos de la biblioteca
        LazyColumn(modifier = Modifier.padding(8.dp)) {
            items(libraryItems) { item ->
                LibraryItem(item)
            }
        }
    }
}

@Composable
fun LibraryItem(item: LibraryItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp).clickable {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.kanyeperfil),
            contentDescription = "Imagen",
            modifier = Modifier.size(50.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = item.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = item.type, color = Color.Gray, fontSize = 14.sp)
        }
    }
}

// Datos simulados
data class LibraryItem(val name: String, val type: String)

fun getLibraryItems(filter: String): List<LibraryItem> {
    return when (filter) {
        "Playlists" -> listOf(
            LibraryItem("Canciones que te gustan", "Playlist"),
            LibraryItem("Playlist prueba", "Playlist")
        )
        else -> emptyList()
    }
}
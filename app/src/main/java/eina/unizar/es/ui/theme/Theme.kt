package eina.unizar.es.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.musicapp.ui.theme.DarkColorScheme
import com.example.musicapp.ui.theme.LightColorScheme
import com.example.musicapp.ui.theme.Typography


@Composable
fun VibraAppTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

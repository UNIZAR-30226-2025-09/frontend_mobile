package com.example.musicapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 🎨 Definir la familia tipográfica (Sans-Serif del sistema)
val SansSerifFont = FontFamily.SansSerif

// ✨ Configurar la tipografía global
val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = SansSerifFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SansSerifFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SansSerifFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)

package com.example.musicapp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

val VibraBlack = Color(0xFF121212)
val VibraYellow = Color(0xFFEAD137)//0xFF79E2FF
val VibraWhite = Color(0xFFFFFFFF)
val VibraGrey = Color(0xFFB3B3B3)


val LightColorScheme = lightColorScheme(
    primary = VibraYellow,
    background = VibraWhite,
    onBackground = VibraBlack,
    surface = VibraGrey,
    onSurface = VibraBlack,
)

val DarkColorScheme = darkColorScheme(
    primary = VibraYellow,
    background = VibraBlack,
    onBackground = VibraWhite,
    surface = VibraGrey,
    onSurface = VibraWhite,
)

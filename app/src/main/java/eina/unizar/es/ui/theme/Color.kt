package com.example.musicapp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

val VibraBlack = Color(0xFF121212)
val VibraBlue = Color(0xFF79E2FF)//0xFF1ED760   0xFFEAD137
val VibraWhite = Color(0xFFFFFFFF)
val VibraDarkGrey = Color(0xFF121212)
val VibraMediumGrey = Color(0xFF1c1c1c)
val VibraLightGrey = Color(0xFFB3B3B3)

val DarkColorScheme = darkColorScheme(
    primary = VibraBlue,
    background = VibraBlack,
    onBackground = VibraWhite,
    surface = VibraDarkGrey,
    onSurface = VibraWhite,
    inverseSurface = VibraLightGrey,
    primaryContainer = VibraMediumGrey,
)

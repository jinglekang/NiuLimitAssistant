package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = NiuRed,
        secondary = SafeGreen,
        tertiary = AccentTeal,
        background = DeepDarkBlue,
        surface = CardBackgroundDark,
        surfaceVariant = Color(0xFF222B3F),
        secondaryContainer = Color(0xFF123329),
        outline = Color(0xFF3A465F),
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = TextLightGray,
        onSurface = Color.White
    )

private val LightColorScheme =
    lightColorScheme(
        primary = NiuRed,
        secondary = SafeGreen,
        tertiary = AccentTeal,
        background = Color(0xFFF1F5F9),
        surface = Color.White,
        surfaceVariant = Color(0xFFE2E8F0),
        secondaryContainer = Color(0xFFD9FBE8),
        outline = Color(0xFFCBD5E1),
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = Color(0xFF0F172A),
        onSurface = Color(0xFF1E293B)
    )

@Composable
fun NiuLimitAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

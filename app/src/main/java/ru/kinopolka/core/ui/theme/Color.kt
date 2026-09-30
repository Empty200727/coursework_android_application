package ru.kinopolka.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette: cinema red with warm amber accents. Used when dynamic color is unavailable.
internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFFA4262C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD7),
    onPrimaryContainer = Color(0xFF410006),
    secondary = Color(0xFF775653),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD7),
    onSecondaryContainer = Color(0xFF2C1513),
    tertiary = Color(0xFF745B0C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE08C),
    onTertiaryContainer = Color(0xFF241A00),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF231918),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF231918),
    surfaceVariant = Color(0xFFF5DDDB),
    onSurfaceVariant = Color(0xFF534342),
    outline = Color(0xFF857371),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB3AE),
    onPrimary = Color(0xFF68000F),
    primaryContainer = Color(0xFF850D19),
    onPrimaryContainer = Color(0xFFFFDAD7),
    secondary = Color(0xFFE7BDB8),
    onSecondary = Color(0xFF442927),
    secondaryContainer = Color(0xFF5D3F3C),
    onSecondaryContainer = Color(0xFFFFDAD7),
    tertiary = Color(0xFFE5C36C),
    onTertiary = Color(0xFF3D2E00),
    tertiaryContainer = Color(0xFF594400),
    onTertiaryContainer = Color(0xFFFFE08C),
    background = Color(0xFF1A1111),
    onBackground = Color(0xFFF1DEDC),
    surface = Color(0xFF1A1111),
    onSurface = Color(0xFFF1DEDC),
    surfaceVariant = Color(0xFF534342),
    onSurfaceVariant = Color(0xFFD8C2BF),
    outline = Color(0xFFA08C8A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

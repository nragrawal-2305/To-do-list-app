package com.example.todolist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============================================================
// LIGHT AESTHETIC YELLOW THEME
// ============================================================

private val LightColorScheme = lightColorScheme(

    primary = Color(0xFFD99A00),
    secondary = Color(0xFF9A6700),
    tertiary = Color(0xFF718355),

    background = Color(0xFFFFF8E7),
    surface = Color(0xFFFFFDF5),

    primaryContainer = Color(0xFFFFF1B8),
    surfaceVariant = Color(0xFFFFFDF5),

    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF3D321F),

    onBackground = Color(0xFF3D321F),
    onSurface = Color(0xFF3D321F),
    onSurfaceVariant = Color(0xFF796B50)
)

// ============================================================
// DARK GALAXY THEME
// ============================================================
// 🔒 GALAXY COLORS UNCHANGED
// ============================================================

private val DarkColorScheme = darkColorScheme(

    primary = DarkGalaxyPrimary,
    secondary = DarkGalaxySecondary,
    tertiary = DarkGalaxyTertiary,

    background = DarkGalaxyBackground,
    surface = DarkGalaxyBackground,

    primaryContainer = DarkGalaxyAddTask,
    surfaceVariant = DarkGalaxyTaskCard,

    onPrimary = Color(0xFF29243A),
    onPrimaryContainer = Color(0xFFF0EAF7),

    onBackground = Color(0xFFF0EAF7),
    onSurface = Color(0xFFF0EAF7),
    onSurfaceVariant = Color(0xFFC2B7CF)
)

// ============================================================
// APP THEME
// ============================================================

@Composable
fun ToDoListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val colorScheme =
        if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


package com.example.todolist

import android.Manifest
import android.net.Uri
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.Button
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

import com.example.todolist.ui.theme.ToDoListTheme

import java.util.Calendar


// ================================================================
// CONSTANTS
// ================================================================

private const val CHANNEL_ID = "todo_reminder_channel_v3"
private const val PREFS_NAME = "todo_prefs"
private const val TASK_COUNT_KEY = "task_count"


// ================================================================
// DAILY NOTIFICATION CODES
// ================================================================

private const val MORNING_NOTIFICATION_CODE = 1001
private const val NOON_NOTIFICATION_CODE = 1002
private const val AFTERNOON_NOTIFICATION_CODE = 1003
private const val EVENING_NOTIFICATION_CODE = 1004
private const val NIGHT_NOTIFICATION_CODE = 1005


// ================================================================
// LABELS
// ================================================================

private const val LABEL_STUDY = "Study"
private const val LABEL_WORK = "Work"
private const val LABEL_PERSONAL = "Personal"
private const val LABEL_IMPORTANT = "Important"
private const val LABEL_HOME = "Home"
private const val LABEL_OTHER = "Other"

private val availableLabels = listOf(
    LABEL_STUDY,
    LABEL_WORK,
    LABEL_PERSONAL,
    LABEL_IMPORTANT,
    LABEL_HOME,
    LABEL_OTHER
)


// ================================================================
// LABEL COLORS
// ================================================================

private fun getLabelColor(label: String): Color {

    return when (label) {

        LABEL_STUDY ->
            Color(0xFF2563EB)

        LABEL_WORK ->
            Color(0xFF7C3AED)

        LABEL_PERSONAL ->
            Color(0xFF16A34A)

        LABEL_IMPORTANT ->
            Color(0xFFDC2626)

        LABEL_HOME ->
            Color(0xFFF97316)

        else ->
            Color(0xFF64748B)
    }
}


// ================================================================
// LABEL EMOJIS
// ================================================================

private fun getLabelEmoji(label: String): String {

    return when (label) {

        LABEL_STUDY ->
            "📚"

        LABEL_WORK ->
            "💼"

        LABEL_PERSONAL ->
            "👤"

        LABEL_IMPORTANT ->
            "🔥"

        LABEL_HOME ->
            "🏠"

        else ->
            "🏷️"
    }
}


// ================================================================
// TASK DATA CLASS
// ================================================================

data class Task(
    val name: String,
    val completed: Boolean = false,
    val priority: Boolean = false,
    val label: String = LABEL_OTHER
)


// ================================================================
// SAVE TASKS
// ================================================================

private fun saveTasks(
    preferences: SharedPreferences,
    tasks: List<Task>
) {

    val editor = preferences.edit()

    val oldCount = preferences.getInt(
        TASK_COUNT_KEY,
        0
    )

    for (i in 0 until oldCount) {

        editor.remove("task_${i}_name")
        editor.remove("task_${i}_completed")
        editor.remove("task_${i}_priority")
        editor.remove("task_${i}_label")
    }

    editor.putInt(
        TASK_COUNT_KEY,
        tasks.size
    )

    tasks.forEachIndexed { index, task ->

        editor.putString(
            "task_${index}_name",
            task.name
        )

        editor.putBoolean(
            "task_${index}_completed",
            task.completed
        )

        editor.putBoolean(
            "task_${index}_priority",
            task.priority
        )

        editor.putString(
            "task_${index}_label",
            task.label
        )
    }

    editor.apply()
}


// ================================================================
// LOAD TASKS
// ================================================================

private fun loadTasks(
    preferences: SharedPreferences
): List<Task> {

    val count = preferences.getInt(
        TASK_COUNT_KEY,
        0
    )

    val loadedTasks = mutableListOf<Task>()

    for (i in 0 until count) {

        val name = preferences.getString(
            "task_${i}_name",
            null
        )

        if (!name.isNullOrEmpty()) {

            val completed = preferences.getBoolean(
                "task_${i}_completed",
                false
            )

            val priority = preferences.getBoolean(
                "task_${i}_priority",
                false
            )

            val label = preferences.getString(
                "task_${i}_label",
                LABEL_OTHER
            ) ?: LABEL_OTHER

            loadedTasks.add(
                Task(
                    name = name,
                    completed = completed,
                    priority = priority,
                    label = label
                )
            )
        }
    }

    return loadedTasks
}



// ================================================================
// BACKGROUND / THEME OPTIONS
// ================================================================

private const val BACKGROUND_MODE_KEY = "background_mode"
private const val BACKGROUND_COLOR_KEY = "background_color"
private const val BACKGROUND_URI_KEY = "background_uri"
private const val GALAXY_DARK_KEY = "galaxy_dark_theme"
private val GALAXY_BACKGROUND = Color(0xFF090B24)

private val darkThemeOptions = listOf(
    AppThemeOption("Galaxy", "🌌", Color(0xFF090B24)),
    AppThemeOption("Midnight Ocean", "🌊", Color(0xFF082F49)),
    AppThemeOption("Forest Night", "🌲", Color(0xFF102A22)),
    AppThemeOption("Purple Nebula", "🪐", Color(0xFF24113D)),
    AppThemeOption("Graphite", "🌑", Color(0xFF20242C))
)

private data class AppThemeOption(
    val name: String,
    val emoji: String,
    val color: Color
)

private val appThemeOptions = listOf(
    AppThemeOption("Default Blush", "🌸", Color(0xFFFFF3F5)),
    AppThemeOption("Morning Sky", "☁️", Color(0xFFE3F2FD)),
    AppThemeOption("Soft Meadow", "🌿", Color(0xFFE8F5E9)),
    AppThemeOption("Lavender Haze", "💜", Color(0xFFF3E5F5)),
    AppThemeOption("Warm Sand", "🏖️", Color(0xFFFFF0E6)),
    AppThemeOption("Sunlit Cream", "☀️", Color(0xFFFFFDE7)),
    AppThemeOption("Rose Garden", "🌹", Color(0xFFFCE4EC)),
    AppThemeOption("Glacier", "🩵", Color(0xFFE0F7FA)),
    AppThemeOption("Sage Home", "🍃", Color(0xFFE7EFE5)),
    AppThemeOption("Cloud Grey", "☁️", Color(0xFFF1F5F9))
)

private fun colorFromStoredValue(value: Int): Color {
    return Color(value)
}

// Each light theme gets its own layered background palette, not just a single tint.
private fun lightThemeGradientColors(themeName: String, animatedBase: Color): List<Color> {
    val transparentWhite = Color.White.copy(alpha = 0.62f)
    return when (themeName) {
        "Morning Sky" -> listOf(animatedBase, Color(0xFFB9E4FF), Color(0xFFF5FBFF))
        "Soft Meadow" -> listOf(Color(0xFFD8F3DC), Color(0xFFB7E4C7), Color(0xFFF4FFF4))
        "Lavender Haze" -> listOf(animatedBase, Color(0xFFE4D7FF), Color(0xFFFFF7FF))
        "Warm Sand" -> listOf(Color(0xFFFFE0C2), Color(0xFFFFF0D8), Color(0xFFFFFAF3))
        "Sunlit Cream" -> listOf(Color(0xFFFFF4B8), Color(0xFFFFFBE2), Color(0xFFFFFEF5))
        "Rose Garden" -> listOf(Color(0xFFFFD8E5), Color(0xFFFFEEF3), Color(0xFFFFFAFC))
        "Glacier" -> listOf(Color(0xFFBDEFF4), Color(0xFFE0FAFC), Color(0xFFF8FFFF))
        "Sage Home" -> listOf(Color(0xFFD5E5D0), Color(0xFFEAF2E5), Color(0xFFFAFFF7))
        "Cloud Grey" -> listOf(Color(0xFFDDE6F0), Color(0xFFF0F4F9), Color(0xFFFFFFFF))
        else -> listOf(animatedBase, transparentWhite, Color(0xFFFFE5EC)) // Default Blush
    }
}

private fun lightThemeAccentColor(themeName: String): Color = when (themeName) {
    "Morning Sky" -> Color(0xFF60A5FA)
    "Soft Meadow" -> Color(0xFF55A879)
    "Lavender Haze" -> Color(0xFF9B87D5)
    "Warm Sand" -> Color(0xFFD99A62)
    "Sunlit Cream" -> Color(0xFFE0B83F)
    "Rose Garden" -> Color(0xFFE879A4)
    "Glacier" -> Color(0xFF55BFCB)
    "Sage Home" -> Color(0xFF7E9B75)
    "Cloud Grey" -> Color(0xFF91A4BA)
    else -> Color(0xFFF08CA5)
}

// ================================================================
// MAIN ACTIVITY
// ================================================================

class MainActivity : ComponentActivity() {

    private var pendingTaskName: String? = null

    private var pendingReminderTime: Long = 0L


    // ============================================================
    // ON CREATE
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        try {
            createNotificationChannel()
        } catch (_: Exception) {
            // Continue launching even if notification setup fails.
        }


        // ========================================================
        // ANDROID 13+ NOTIFICATION PERMISSION
        // ========================================================

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    2001
                )
            }
        }


        // ========================================================
        // DAILY NOTIFICATIONS
        // ========================================================
        // Daily reminders are scheduled from onResume(), where failures are
        // caught so an alarm/permission issue cannot prevent the UI opening.


        // ========================================================
        // UI
        // ========================================================

        setContent {

            ToDoListTheme {

                val context = LocalContext.current

                var taskText by remember {
                    mutableStateOf("")
                }

                var searchText by remember {
                    mutableStateOf("")
                }

                var editingIndex by remember {
                    mutableStateOf(-1)
                }

                var selectedFilter by remember {
                    mutableStateOf("All")
                }

                // ==================================================
                // SHARED PREFERENCES
                // ==================================================

                val sharedPreferences = remember {

                    context.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                    )
                }


                // One-time migration: force the app's initial appearance to Light.
                // After this migration, any theme selected by the user is remembered.
                val lightDefaultMigrationDone = remember {
                    val done = sharedPreferences.getBoolean("light_default_migration_v1", false)
                    if (!done) {
                        sharedPreferences.edit()
                            .putBoolean("light_default_migration_v1", true)
                            .putBoolean(GALAXY_DARK_KEY, false)
                            .putString(BACKGROUND_MODE_KEY, "color")
                            .putInt(BACKGROUND_COLOR_KEY, appThemeOptions.first().color.toArgb())
                            .remove(BACKGROUND_URI_KEY)
                            .apply()
                    }
                    done
                }

                var menuExpanded by remember {
                    mutableStateOf(false)
                }

                var appearanceMenuExpanded by remember {
                    mutableStateOf(false)
                }

                var galaxyDarkTheme by remember {
                    mutableStateOf(
                        if (!lightDefaultMigrationDone) false
                        else sharedPreferences.getBoolean(GALAXY_DARK_KEY, false)
                    )
                }

                var backgroundMode by remember {
                    mutableStateOf(
                        if (!lightDefaultMigrationDone) "color"
                        else sharedPreferences.getString(BACKGROUND_MODE_KEY, "color") ?: "color"
                    )
                }

                var backgroundColorValue by remember {
                    mutableStateOf(
                        if (!lightDefaultMigrationDone) appThemeOptions.first().color.toArgb()
                        else sharedPreferences.getInt(
                            BACKGROUND_COLOR_KEY,
                            appThemeOptions.first().color.toArgb()
                        )
                    )
                }

                var backgroundUri by remember {
                    mutableStateOf(
                        sharedPreferences.getString(
                            BACKGROUND_URI_KEY,
                            null
                        )
                    )
                }

                var themePickerExpanded by remember {
                    mutableStateOf(false)
                }
                // Keep Light and Dark theme lists in separate sections.
                var themePickerMode by remember {
                    mutableStateOf("light")
                }

                // Buddy can be added/removed from the three-dot menu.
                var buddyEnabled by remember {
                    mutableStateOf(
                        sharedPreferences.getBoolean("buddy_enabled", true)
                    )
                }
                var buddyCelebrating by remember {
                    mutableStateOf(false)
                }
                var buddyMotionToggle by remember {
                    mutableStateOf(false)
                }
                var buddyThemeMessage by remember {
                    mutableStateOf("")
                }

                // ==================================================
                // LABEL STATE
                // ==================================================

                var selectedLabel by remember {
                    mutableStateOf(LABEL_OTHER)
                }

                var labelMenuExpanded by remember {
                    mutableStateOf(false)
                }


                // ==================================================
                // TASK LIST
                // ==================================================

                val tasks = remember {

                    mutableStateListOf<Task>().apply {

                        addAll(
                            loadTasks(
                                sharedPreferences
                            )
                        )
                    }
                }


                // ==================================================
                // NOTIFICATION PERMISSION LAUNCHER
                // ==================================================

                val notificationPermissionLauncher =
                    rememberLauncherForActivityResult(

                        contract =
                            ActivityResultContracts.RequestPermission()

                    ) { granted ->

                        if (granted) {

                            continueSettingReminder(
                                context
                            )

                        } else {

                            Toast.makeText(
                                context,
                                "Notification permission is required",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }


                // ==================================================
                // MAIN SURFACE
                // ==================================================

                val selectedBackgroundColor =
                    colorFromStoredValue(backgroundColorValue)
                val selectedLightThemeName = appThemeOptions.firstOrNull {
                    it.color.toArgb() == backgroundColorValue
                }?.name ?: "Default Blush"
                val lightThemeAccent = lightThemeAccentColor(selectedLightThemeName)
                val animatedBackgroundColor by animateColorAsState(
                    targetValue = selectedBackgroundColor,
                    animationSpec = tween(durationMillis = 700),
                    label = "themeBackgroundAnimation"
                )
                val ambientMotion = rememberInfiniteTransition(label = "lightThemeAmbientMotion")
                val ambientDriftX by ambientMotion.animateFloat(
                    initialValue = -18f,
                    targetValue = 22f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 6500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "lightThemeAmbientDriftX"
                )
                val ambientDriftY by ambientMotion.animateFloat(
                    initialValue = 16f,
                    targetValue = -14f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 7200),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "lightThemeAmbientDriftY"
                )

                MaterialTheme(
                    colorScheme = if (galaxyDarkTheme) {
                        darkColorScheme(
                            primary = Color(0xFFB9A7FF),
                            onPrimary = Color(0xFF20134F),
                            secondary = Color(0xFF80D8FF),
                            onSecondary = Color(0xFF002B3D),
                            background = selectedBackgroundColor,
                            onBackground = Color(0xFFF4F1FF),
                            surface = Color(0xFF151735),
                            onSurface = Color(0xFFF4F1FF),
                            surfaceVariant = Color(0xFF282A4D),
                            onSurfaceVariant = Color(0xFFD0CEE8),
                            primaryContainer = Color(0xFF30285B),
                            onPrimaryContainer = Color(0xFFF4F1FF),
                            secondaryContainer = Color(0xFF24364D),
                            onSecondaryContainer = Color(0xFFEAF7FF)
                        )
                    } else {
                        // Use an explicit light palette. Reusing MaterialTheme.colorScheme here
                        // can keep the app on the previous/default scheme instead of switching.
                        lightColorScheme(
                            primary = Color(0xFF6750A4),
                            onPrimary = Color.White,
                            secondary = Color(0xFF52647A),
                            onSecondary = Color.White,
                            background = selectedBackgroundColor,
                            onBackground = Color(0xFF24212A),
                            surface = Color(0xFFFFFBFF),
                            onSurface = Color(0xFF24212A),
                            surfaceVariant = Color(0xFFECE6F0),
                            onSurfaceVariant = Color(0xFF49454F),
                            primaryContainer = Color(0xFFEADDFF),
                            onPrimaryContainer = Color(0xFF21005D),
                            secondaryContainer = Color(0xFFE0E8F2),
                            onSecondaryContainer = Color(0xFF172B40)
                        )
                    }
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = selectedBackgroundColor
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Static, layered backgrounds only: no moving stars or pulsing.
                            // The selected palette updates immediately when a theme is tapped.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (galaxyDarkTheme) {
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    animatedBackgroundColor,
                                                    Color(0xFF17143B),
                                                    Color(0xFF070918)
                                                )
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                colors = lightThemeGradientColors(
                                                    selectedLightThemeName,
                                                    animatedBackgroundColor
                                                )
                                            )
                                        }
                                    )
                            )
                            if (!galaxyDarkTheme) {
                                // Soft moving colour blooms make each light theme feel alive
                                // while keeping the task content readable above this layer.
                                Box(
                                    modifier = Modifier
                                        .offset(x = ambientDriftX.dp, y = 58.dp)
                                        .size(230.dp)
                                        .alpha(0.28f)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(lightThemeAccent.copy(alpha = 0.72f), Color.Transparent)
                                            ),
                                            CircleShape
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = ambientDriftY.dp, y = (-115).dp)
                                        .size(270.dp)
                                        .alpha(0.22f)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(lightThemeAccent.copy(alpha = 0.62f), Color.Transparent)
                                            ),
                                            CircleShape
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .offset(x = 80.dp, y = ambientDriftY.dp)
                                        .size(130.dp)
                                        .alpha(0.14f)
                                        .background(lightThemeAccent, CircleShape)
                                )
                            }
                            if (galaxyDarkTheme) {
                                // Fixed decorative galaxy glows; intentionally not animated.
                                Box(
                                    modifier = Modifier
                                        .offset(x = 220.dp, y = 90.dp)
                                        .size(180.dp)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF7865D8).copy(alpha = 0.25f),
                                                    Color.Transparent
                                                )
                                            ),
                                            CircleShape
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .offset(x = (-35).dp, y = 330.dp)
                                        .size(210.dp)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF3285C7).copy(alpha = 0.18f),
                                                    Color.Transparent
                                                )
                                            ),
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = "✦      ·     ✧          ·       ✦      ·     ✧",
                                    color = Color.White.copy(alpha = 0.58f),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 95.dp),
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "·       ✧          ✦      ·        ✧       ·",
                                    color = Color(0xFFB8C8FF).copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 12.dp),
                                    fontSize = 14.sp
                                )
                            }

                            Column(

                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .verticalScroll(
                                            rememberScrollState()
                                        )
                                        .padding(20.dp)

                            ) {


                                // =================================================
                                // HEADER
                                // =================================================

                                Row(

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    horizontalArrangement =
                                        Arrangement.SpaceBetween,

                                    verticalAlignment =
                                        Alignment.CenterVertically

                                ) {

                                    Column {

                                        Text(
                                            text = "✨ My Tasks",
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.height(4.dp)
                                        )

                                        Text(
                                            text =
                                                "Stay organized and get things done!",
                                            fontSize = 14.sp,
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurfaceVariant
                                        )
                                    }


                                    // =================================================
                                    // FILTER MENU
                                    // =================================================

                                    Box {

                                        Text(

                                            text = "⋮",

                                            fontSize = 30.sp,

                                            modifier =
                                                Modifier
                                                    .clickable {
                                                        menuExpanded = true
                                                    }
                                                    .padding(8.dp)
                                        )


                                        DropdownMenu(

                                            expanded =
                                                menuExpanded,

                                            onDismissRequest = {
                                                menuExpanded = false
                                            }

                                        ) {

                                            DropdownMenuItem(

                                                text = {
                                                    Text("All Tasks")
                                                },

                                                onClick = {

                                                    selectedFilter = "All"
                                                    menuExpanded = false
                                                }
                                            )


                                            DropdownMenuItem(

                                                text = {
                                                    Text("Completed")
                                                },

                                                onClick = {

                                                    selectedFilter =
                                                        "Completed"

                                                    menuExpanded = false
                                                }
                                            )


                                            DropdownMenuItem(

                                                text = {
                                                    Text("Pending")
                                                },

                                                onClick = {

                                                    selectedFilter =
                                                        "Pending"

                                                    menuExpanded = false
                                                }
                                            )


                                            DropdownMenuItem(

                                                text = {
                                                    Text(
                                                        "Most Important Tasks"
                                                    )
                                                },

                                                onClick = {

                                                    selectedFilter = "High"

                                                    menuExpanded = false
                                                }
                                            )

                                            DropdownMenuItem(

                                                text = {
                                                    Text("🎨 Appearance")
                                                },

                                                onClick = {
                                                    appearanceMenuExpanded = true
                                                    menuExpanded = false
                                                }
                                            )


                                            DropdownMenuItem(
                                                text = {
                                                    Text(if (buddyEnabled) "🐻 Remove Buddy" else "🐻 Add Buddy")
                                                },
                                                onClick = {
                                                    buddyEnabled = !buddyEnabled
                                                    sharedPreferences.edit()
                                                        .putBoolean("buddy_enabled", buddyEnabled)
                                                        .apply()
                                                    menuExpanded = false
                                                }
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = appearanceMenuExpanded,
                                            onDismissRequest = {
                                                appearanceMenuExpanded = false
                                            }
                                        ) {

                                            DropdownMenuItem(
                                                text = { Text("☀️ Light Themes (10)") },
                                                onClick = {
                                                    themePickerMode = "light"
                                                    themePickerExpanded = true
                                                    appearanceMenuExpanded = false
                                                }
                                            )

                                            DropdownMenuItem(
                                                text = { Text("🌙 Dark Themes (5)") },
                                                onClick = {
                                                    themePickerMode = "dark"
                                                    themePickerExpanded = true
                                                    appearanceMenuExpanded = false
                                                }
                                            )

                                            DropdownMenuItem(
                                                text = { Text("🔄 Reset Background") },
                                                onClick = {
                                                    galaxyDarkTheme = false
                                                    backgroundMode = "color"
                                                    backgroundColorValue = appThemeOptions.first().color.toArgb()
                                                    backgroundUri = null
                                                    buddyMotionToggle = !buddyMotionToggle
                                                    buddyThemeMessage = "Back to Default Blush! 🌸"

                                                    sharedPreferences.edit()
                                                        .putBoolean(GALAXY_DARK_KEY, false)
                                                        .putString(BACKGROUND_MODE_KEY, "color")
                                                        .putInt(BACKGROUND_COLOR_KEY, backgroundColorValue)
                                                        .remove(BACKGROUND_URI_KEY)
                                                        .apply()

                                                    appearanceMenuExpanded = false
                                                }
                                            )
                                        }

                                        if (themePickerExpanded) {
                                            androidx.compose.ui.window.Dialog(
                                                onDismissRequest = { themePickerExpanded = false }
                                            ) {
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(8.dp),
                                                    shape = RoundedCornerShape(20.dp),
                                                    color = if (themePickerMode == "dark") Color(0xFF20242C) else Color(0xFFFFFFFF),
                                                    tonalElevation = 8.dp
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(max = 560.dp)
                                                            .verticalScroll(rememberScrollState())
                                                            .padding(20.dp)
                                                    ) {
                                                        Text(
                                                            text = if (themePickerMode == "light") "☀️ Light Themes (10)" else "🌙 Dark Themes (5)",
                                                            color = if (themePickerMode == "dark") Color.White else Color(0xFF252525),
                                                            fontSize = 20.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(Modifier.height(12.dp))
                                                        val visibleThemes = if (themePickerMode == "light") appThemeOptions else darkThemeOptions
                                                        visibleThemes.forEach { theme ->
                                                            val isDarkChoice = themePickerMode == "dark"
                                                            val isSelected = galaxyDarkTheme == isDarkChoice &&
                                                                    backgroundColorValue == theme.color.toArgb()
                                                            val cardScale by animateFloatAsState(
                                                                targetValue = if (isSelected) 1.025f else 1f,
                                                                animationSpec = tween(220),
                                                                label = "themeCardScale_${theme.name}"
                                                            )
                                                            Card(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(vertical = 4.dp)
                                                                    .graphicsLayer {
                                                                        scaleX = cardScale
                                                                        scaleY = cardScale
                                                                    }
                                                                    .clickable {
                                                                        galaxyDarkTheme = isDarkChoice
                                                                        backgroundMode = "color"
                                                                        backgroundColorValue = theme.color.toArgb()
                                                                        backgroundUri = null
                                                                        buddyMotionToggle = !buddyMotionToggle
                                                                        buddyThemeMessage = "Yay! ${theme.name} looks lovely! ${theme.emoji}"
                                                                        sharedPreferences.edit()
                                                                            .putBoolean(GALAXY_DARK_KEY, isDarkChoice)
                                                                            .putString(BACKGROUND_MODE_KEY, "color")
                                                                            .putInt(BACKGROUND_COLOR_KEY, backgroundColorValue)
                                                                            .remove(BACKGROUND_URI_KEY)
                                                                            .apply()
                                                                        themePickerExpanded = false
                                                                    },
                                                                shape = RoundedCornerShape(14.dp),
                                                                colors = CardDefaults.cardColors(
                                                                    containerColor = if (isSelected) theme.color.copy(alpha = 0.55f)
                                                                    else theme.color.copy(alpha = 0.22f)
                                                                ),
                                                                elevation = CardDefaults.cardElevation(
                                                                    defaultElevation = if (isSelected) 5.dp else 1.dp
                                                                )
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(34.dp)
                                                                            .background(theme.color, CircleShape)
                                                                    )
                                                                    Spacer(Modifier.width(12.dp))
                                                                    Text(
                                                                        text = "${theme.emoji} ${theme.name}",
                                                                        modifier = Modifier.weight(1f),
                                                                        color = if (themePickerMode == "dark") Color.White else Color(0xFF252525),
                                                                        fontSize = 16.sp,
                                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                                    )
                                                                    if (isSelected) {
                                                                        Text("✓", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                                                            color = if (themePickerMode == "dark") Color.White else Color(0xFF3F6B50))
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        Spacer(Modifier.height(8.dp))
                                                        TextButton(
                                                            onClick = { themePickerExpanded = false },
                                                            modifier = Modifier.align(Alignment.End)
                                                        ) {
                                                            Text("Close")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }


                                Spacer(
                                    modifier =
                                        Modifier.height(20.dp)
                                )


                                // =================================================
                                // BUDDY COMPANION (OPTIONAL)
                                // =================================================

                                if (buddyEnabled) {
                                    BuddyCard(
                                        totalTasks = tasks.size,
                                        completedTasks = tasks.count { it.completed },
                                        celebrating = buddyCelebrating,
                                        motionToggle = buddyMotionToggle,
                                        themeMessage = buddyThemeMessage
                                    )
                                    Spacer(Modifier.height(20.dp))
                                }


                                // =================================================
                                // SEARCH
                                // =================================================

                                OutlinedTextField(

                                    value =
                                        searchText,

                                    onValueChange = {
                                        searchText = it
                                        // Buddy reacts when the user searches for a task.
                                        buddyMotionToggle = !buddyMotionToggle
                                    },

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    label = {
                                        Text("Search tasks")
                                    },

                                    placeholder = {
                                        Text("Search...")
                                    },

                                    singleLine = true
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(16.dp)
                                )


                                // =================================================
                                // ADD TASK CARD
                                // =================================================

                                Card(

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor =
                                                MaterialTheme
                                                    .colorScheme
                                                    .primaryContainer
                                        ),

                                    shape =
                                        RoundedCornerShape(18.dp)

                                ) {

                                    Column(

                                        modifier =
                                            Modifier.padding(18.dp)

                                    ) {

                                        Text(
                                            text =
                                                "📝 Add a new task",
                                            fontSize =
                                                18.sp,
                                            fontWeight =
                                                FontWeight.Bold
                                        )


                                        Spacer(
                                            modifier =
                                                Modifier.height(12.dp)
                                        )


                                        OutlinedTextField(

                                            value =
                                                taskText,

                                            onValueChange = {
                                                taskText = it
                                            },

                                            modifier =
                                                Modifier.fillMaxWidth(),

                                            label = {
                                                Text("Task name")
                                            },

                                            singleLine = true
                                        )


                                        Spacer(
                                            modifier =
                                                Modifier.height(12.dp)
                                        )


                                        // =================================================
                                        // LABEL SELECTOR
                                        // =================================================

                                        Box(
                                            modifier =
                                                Modifier.fillMaxWidth()
                                        ) {

                                            OutlinedTextField(

                                                value =
                                                    "${getLabelEmoji(selectedLabel)}  $selectedLabel",

                                                onValueChange = {},

                                                modifier =
                                                    Modifier.fillMaxWidth(),

                                                label = {
                                                    Text("Task label")
                                                },

                                                readOnly = true
                                            )


                                            Box(

                                                modifier =
                                                    Modifier
                                                        .matchParentSize()
                                                        .clickable {
                                                            labelMenuExpanded = true
                                                        }
                                            )


                                            DropdownMenu(

                                                expanded =
                                                    labelMenuExpanded,

                                                onDismissRequest = {
                                                    labelMenuExpanded = false
                                                }

                                            ) {

                                                availableLabels.forEach { label ->

                                                    DropdownMenuItem(

                                                        text = {

                                                            Row(

                                                                verticalAlignment =
                                                                    Alignment.CenterVertically

                                                            ) {

                                                                Box(

                                                                    modifier =
                                                                        Modifier
                                                                            .size(12.dp)
                                                                            .background(
                                                                                getLabelColor(
                                                                                    label
                                                                                ),
                                                                                CircleShape
                                                                            )
                                                                )

                                                                Spacer(
                                                                    modifier =
                                                                        Modifier.width(10.dp)
                                                                )

                                                                Text(
                                                                    "${getLabelEmoji(label)}  $label"
                                                                )
                                                            }
                                                        },

                                                        onClick = {

                                                            selectedLabel =
                                                                label

                                                            labelMenuExpanded =
                                                                false
                                                        }
                                                    )
                                                }
                                            }
                                        }


                                        Spacer(
                                            modifier =
                                                Modifier.height(12.dp)
                                        )


                                        // =================================================
                                        // ADD / UPDATE BUTTON
                                        // =================================================

                                        Button(

                                            onClick = {

                                                if (
                                                    taskText.trim().isEmpty()
                                                ) {

                                                    Toast.makeText(
                                                        context,
                                                        "Please enter a task",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                } else {

                                                    if (
                                                        editingIndex >= 0
                                                    ) {

                                                        val oldTask =
                                                            tasks[editingIndex]

                                                        val newTaskName =
                                                            taskText.trim()

                                                        tasks[editingIndex] =
                                                            oldTask.copy(
                                                                name =
                                                                    newTaskName,
                                                                label =
                                                                    selectedLabel
                                                            )


                                                        if (
                                                            oldTask.name !=
                                                            newTaskName
                                                        ) {

                                                            val oldReminder =
                                                                sharedPreferences
                                                                    .getLong(
                                                                        "reminder_${oldTask.name}",
                                                                        0L
                                                                    )

                                                            if (
                                                                oldReminder > 0L
                                                            ) {

                                                                sharedPreferences
                                                                    .edit()
                                                                    .putLong(
                                                                        "reminder_$newTaskName",
                                                                        oldReminder
                                                                    )
                                                                    .remove(
                                                                        "reminder_${oldTask.name}"
                                                                    )
                                                                    .apply()
                                                            }
                                                        }


                                                        editingIndex = -1


                                                        Toast.makeText(
                                                            context,
                                                            "Task updated",
                                                            Toast.LENGTH_SHORT
                                                        ).show()

                                                    } else {

                                                        tasks.add(

                                                            Task(
                                                                name =
                                                                    taskText.trim(),
                                                                label =
                                                                    selectedLabel
                                                            )
                                                        )

                                                        // Buddy reacts whenever a new task is added.
                                                        buddyCelebrating = false
                                                        buddyMotionToggle = !buddyMotionToggle

                                                        Toast.makeText(
                                                            context,
                                                            "Task added",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }


                                                    saveTasks(
                                                        sharedPreferences,
                                                        tasks
                                                    )

                                                    taskText = ""

                                                    selectedLabel =
                                                        LABEL_OTHER
                                                }
                                            },

                                            modifier =
                                                Modifier.fillMaxWidth(),

                                            shape =
                                                RoundedCornerShape(12.dp)

                                        ) {

                                            Text(

                                                text =
                                                    if (
                                                        editingIndex >= 0
                                                    )
                                                        "Update Task"
                                                    else
                                                        "Add Task"
                                            )
                                        }
                                    }
                                }


                                Spacer(
                                    modifier =
                                        Modifier.height(20.dp)
                                )


                                // =================================================
                                // TASK TITLE
                                // =================================================

                                Text(

                                    text =

                                        when (
                                            selectedFilter
                                        ) {

                                            "Completed" ->
                                                "Completed Tasks"

                                            "Pending" ->
                                                "Pending Tasks"

                                            "High" ->
                                                "Most Important Tasks"

                                            else ->
                                                "Your Tasks"
                                        },

                                    fontSize =
                                        20.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(10.dp)
                                )


                                // =================================================
                                // FILTER TASKS
                                // =================================================

                                val filteredTasks =

                                    tasks
                                        .filter { task ->

                                            val matchesSearch =
                                                task.name.contains(
                                                    searchText,
                                                    ignoreCase = true
                                                )

                                            val matchesFilter =

                                                when (
                                                    selectedFilter
                                                ) {

                                                    "Completed" ->
                                                        task.completed

                                                    "Pending" ->
                                                        !task.completed

                                                    "High" ->
                                                        task.priority

                                                    else ->
                                                        true
                                                }

                                            matchesSearch &&
                                                    matchesFilter
                                        }
                                        .sortedByDescending {
                                            it.priority
                                        }


                                // =================================================
                                // EMPTY TASKS
                                // =================================================

                                if (
                                    filteredTasks.isEmpty()
                                ) {

                                    Card(

                                        modifier =
                                            Modifier.fillMaxWidth(),

                                        shape =
                                            RoundedCornerShape(16.dp)

                                    ) {

                                        Column(

                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(30.dp),

                                            horizontalAlignment =
                                                Alignment.CenterHorizontally

                                        ) {

                                            Text(
                                                text = "📋",
                                                fontSize = 40.sp
                                            )

                                            Spacer(
                                                modifier =
                                                    Modifier.height(8.dp)
                                            )

                                            Text(
                                                text =
                                                    "No tasks found",
                                                fontSize =
                                                    16.sp,
                                                fontWeight =
                                                    FontWeight.Medium
                                            )
                                        }
                                    }

                                } else {

                                    // =================================================
                                    // TASK LIST
                                    // =================================================

                                    filteredTasks.forEach { taskItem ->

                                        val originalIndex =
                                            tasks.indexOf(taskItem)


                                        val reminderTime =
                                            sharedPreferences.getLong(
                                                "reminder_${taskItem.name}",
                                                0L
                                            )


                                        // =================================================
                                        // TASK CARD
                                        // =================================================

                                        Card(

                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp),

                                            shape =
                                                RoundedCornerShape(16.dp),

                                            colors =
                                                CardDefaults.cardColors(

                                                    containerColor =
                                                        getLabelColor(
                                                            taskItem.label
                                                        ).copy(
                                                            alpha = 0.16f
                                                        )
                                                )

                                        ) {

                                            Column(

                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp)

                                            ) {

                                                // Main row: keep the task text wide so long names
                                                // wrap like a paragraph instead of one word per line.
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {


                                                    // =========================================
                                                    // CHECKBOX
                                                    // =========================================

                                                    Checkbox(

                                                        checked =
                                                            taskItem.completed,

                                                        onCheckedChange = { isCompleted ->

                                                            tasks[originalIndex] =
                                                                taskItem.copy(
                                                                    completed = isCompleted
                                                                )

                                                            // Celebrate only when a task is marked complete.
                                                            buddyCelebrating = isCompleted
                                                            buddyMotionToggle = !buddyMotionToggle

                                                            saveTasks(
                                                                sharedPreferences,
                                                                tasks
                                                            )
                                                        }
                                                    )


                                                    Spacer(
                                                        modifier =
                                                            Modifier.width(6.dp)
                                                    )


                                                    // =========================================
                                                    // PRIORITY
                                                    // =========================================

                                                    Box(

                                                        modifier =
                                                            Modifier
                                                                .size(24.dp)
                                                                .clickable {

                                                                    tasks[originalIndex] =
                                                                        taskItem.copy(
                                                                            priority =
                                                                                !taskItem.priority
                                                                        )

                                                                    saveTasks(
                                                                        sharedPreferences,
                                                                        tasks
                                                                    )
                                                                }
                                                                .padding(5.dp)
                                                                .background(

                                                                    if (
                                                                        taskItem.priority
                                                                    )
                                                                        Color(
                                                                            0xFFE53935
                                                                        )
                                                                    else
                                                                        Color.Gray,

                                                                    CircleShape
                                                                )
                                                    )


                                                    Spacer(
                                                        modifier =
                                                            Modifier.width(10.dp)
                                                    )


                                                    // =========================================
                                                    // TASK DETAILS
                                                    // =========================================

                                                    Column(

                                                        modifier =
                                                            Modifier
                                                                .weight(1f)
                                                                .padding(
                                                                    horizontal = 4.dp
                                                                )

                                                    ) {

                                                        // =====================================
                                                        // TASK NAME
                                                        // Allow long task names to wrap and show fully.
                                                        // =====================================

                                                        Text(

                                                            text =
                                                                taskItem.name,

                                                            fontSize =
                                                                16.sp,

                                                            fontWeight =
                                                                FontWeight.SemiBold,

                                                            // Do not cut long task names to one line.
                                                            maxLines =
                                                                Int.MAX_VALUE,

                                                            softWrap =
                                                                true,

                                                            overflow =
                                                                TextOverflow.Clip,

                                                            modifier =
                                                                Modifier.fillMaxWidth().alpha(

                                                                    if (
                                                                        taskItem.completed
                                                                    )
                                                                        0.5f
                                                                    else
                                                                        1f
                                                                )
                                                        )


                                                        Spacer(
                                                            modifier =
                                                                Modifier.height(5.dp)
                                                        )


                                                        // =====================================
                                                        // LABEL
                                                        // =====================================

                                                        Row(

                                                            verticalAlignment =
                                                                Alignment.CenterVertically

                                                        ) {

                                                            Box(

                                                                modifier =
                                                                    Modifier
                                                                        .size(7.dp)
                                                                        .background(
                                                                            getLabelColor(
                                                                                taskItem.label
                                                                            ),
                                                                            CircleShape
                                                                        )
                                                            )


                                                            Spacer(
                                                                modifier =
                                                                    Modifier.width(5.dp)
                                                            )


                                                            Text(

                                                                text =
                                                                    "${getLabelEmoji(taskItem.label)} ${taskItem.label}",

                                                                fontSize =
                                                                    11.sp,

                                                                fontWeight =
                                                                    FontWeight.Bold,

                                                                color =
                                                                    getLabelColor(
                                                                        taskItem.label
                                                                    ),

                                                                maxLines =
                                                                    1,

                                                                softWrap =
                                                                    false,

                                                                overflow =
                                                                    TextOverflow.Ellipsis
                                                            )
                                                        }


                                                        // =====================================
                                                        // REMINDER
                                                        // =====================================

                                                        if (
                                                            reminderTime > 0L
                                                        ) {

                                                            Spacer(
                                                                modifier =
                                                                    Modifier.height(4.dp)
                                                            )


                                                            Text(

                                                                text =
                                                                    "⏰ Reminder: ${
                                                                        formatReminderTime(
                                                                            reminderTime
                                                                        )
                                                                    }",

                                                                fontSize =
                                                                    12.sp,

                                                                fontWeight =
                                                                    FontWeight.Medium,

                                                                color =
                                                                    Color(0xFF16A34A),

                                                                maxLines =
                                                                    1,

                                                                softWrap =
                                                                    false,

                                                                overflow =
                                                                    TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }


                                                    // =========================================
                                                }

                                                // Keep action buttons on their own row so they
                                                // do not squeeze the task text into a narrow column.
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {

                                                    // REMINDER BUTTON
                                                    // =========================================

                                                    TextButton(

                                                        onClick = {

                                                            pendingTaskName =
                                                                taskItem.name


                                                            if (
                                                                Build.VERSION.SDK_INT >=
                                                                Build.VERSION_CODES.TIRAMISU
                                                            ) {

                                                                if (

                                                                    ContextCompat
                                                                        .checkSelfPermission(
                                                                            context,
                                                                            Manifest.permission.POST_NOTIFICATIONS
                                                                        ) !=
                                                                    PackageManager.PERMISSION_GRANTED

                                                                ) {

                                                                    notificationPermissionLauncher
                                                                        .launch(
                                                                            Manifest.permission.POST_NOTIFICATIONS
                                                                        )

                                                                } else {

                                                                    setReminder(
                                                                        context
                                                                    )
                                                                }

                                                            } else {

                                                                setReminder(
                                                                    context
                                                                )
                                                            }
                                                        }

                                                    ) {

                                                        Text("⏰")
                                                    }


                                                    // =========================================
                                                    // EDIT BUTTON
                                                    // =========================================

                                                    TextButton(

                                                        onClick = {

                                                            taskText =
                                                                taskItem.name

                                                            selectedLabel =
                                                                taskItem.label

                                                            editingIndex =
                                                                originalIndex
                                                        }

                                                    ) {

                                                        Text("✏️")
                                                    }


                                                    // =========================================
                                                    // DELETE BUTTON
                                                    // =========================================

                                                    TextButton(

                                                        onClick = {

                                                            sharedPreferences
                                                                .edit()
                                                                .remove(
                                                                    "reminder_${taskItem.name}"
                                                                )
                                                                .apply()


                                                            tasks.removeAt(
                                                                originalIndex
                                                            )


                                                            saveTasks(
                                                                sharedPreferences,
                                                                tasks
                                                            )


                                                            Toast.makeText(
                                                                context,
                                                                "Task deleted",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }

                                                    ) {

                                                        Text("🗑️")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }


                                Spacer(
                                    modifier =
                                        Modifier.height(30.dp)
                                )
                            }

                        }
                    }
                }
            }
        }
    }


    // ============================================================
    // ON RESUME
    // ============================================================

    override fun onResume() {

        super.onResume()

        if (

            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||

            (
                    getSystemService(
                        Context.ALARM_SERVICE
                    ) as AlarmManager
                    ).canScheduleExactAlarms()

        ) {
            try {
                scheduleDailyNotifications(this)
            } catch (_: Exception) {
                // The main app remains usable if notifications cannot be scheduled.
            }
        }
    }


    // ============================================================
    // SETUP DAILY NOTIFICATIONS
    // ============================================================

    private fun setupDailyNotifications() {
        // Never force-open Android settings during app startup.
        // On Android 12+, daily alarms are scheduled only if exact alarms are allowed.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) return
            }
            scheduleDailyNotifications(this)
        } catch (_: Exception) {
            // Notifications are optional; a scheduling problem must not close the app.
        }
    }

    // ============================================================
    // FORMAT REMINDER TIME
    // ============================================================

    private fun formatReminderTime(
        timeMillis: Long
    ): String {

        val calendar =
            Calendar.getInstance().apply {
                timeInMillis = timeMillis
            }

        val hour =
            calendar.get(Calendar.HOUR)

        val minute =
            calendar.get(Calendar.MINUTE)

        val amPm =

            if (
                calendar.get(Calendar.AM_PM) ==
                Calendar.AM
            )
                "AM"
            else
                "PM"

        val displayHour =

            if (hour == 0)
                12
            else
                hour

        return String.format(
            "%02d:%02d %s",
            displayHour,
            minute,
            amPm
        )
    }


    // ============================================================
    // SET REMINDER
    // ============================================================

    private fun setReminder(
        context: Context
    ) {

        val taskName =
            pendingTaskName

        if (taskName == null) {
            return
        }

        val calendar =
            Calendar.getInstance()


        DatePickerDialog(

            context,

            { _, year, month, dayOfMonth ->

                TimePickerDialog(

                    context,

                    { _, hourOfDay, minute ->

                        val reminderCalendar =
                            Calendar.getInstance().apply {

                                set(
                                    Calendar.YEAR,
                                    year
                                )

                                set(
                                    Calendar.MONTH,
                                    month
                                )

                                set(
                                    Calendar.DAY_OF_MONTH,
                                    dayOfMonth
                                )

                                set(
                                    Calendar.HOUR_OF_DAY,
                                    hourOfDay
                                )

                                set(
                                    Calendar.MINUTE,
                                    minute
                                )

                                set(
                                    Calendar.SECOND,
                                    0
                                )

                                set(
                                    Calendar.MILLISECOND,
                                    0
                                )
                            }


                        if (

                            reminderCalendar.timeInMillis <=
                            System.currentTimeMillis()

                        ) {

                            Toast.makeText(
                                context,
                                "Please select a future time",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@TimePickerDialog
                        }


                        pendingReminderTime =
                            reminderCalendar.timeInMillis


                        continueSettingReminder(
                            context
                        )
                    },

                    calendar.get(
                        Calendar.HOUR_OF_DAY
                    ),

                    calendar.get(
                        Calendar.MINUTE
                    ),

                    false

                ).show()
            },

            calendar.get(
                Calendar.YEAR
            ),

            calendar.get(
                Calendar.MONTH
            ),

            calendar.get(
                Calendar.DAY_OF_MONTH
            )

        ).show()
    }


    // ============================================================
    // CONTINUE SETTING REMINDER
    // ============================================================

    private fun continueSettingReminder(
        context: Context
    ) {

        val taskName =
            pendingTaskName

        val reminderTime =
            pendingReminderTime

        if (
            taskName == null ||
            reminderTime == 0L
        ) {
            return
        }


        val sharedPreferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )


        sharedPreferences
            .edit()
            .putLong(
                "reminder_$taskName",
                reminderTime
            )
            .apply()


        scheduleReminder(
            context,
            taskName,
            reminderTime
        )


        Toast.makeText(

            context,

            "Reminder set for ${
                formatReminderTime(
                    reminderTime
                )
            }",

            Toast.LENGTH_SHORT

        ).show()


        pendingTaskName = null
        pendingReminderTime = 0L
    }


    // ============================================================
    // SCHEDULE TASK REMINDER
    // ============================================================

    private fun scheduleReminder(

        context: Context,

        taskName: String,

        reminderTime: Long

    ) {

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager


        val uniqueTaskId =
            taskName.hashCode()


        val intent =
            Intent(
                context,
                ReminderReceiver::class.java
            ).apply {

                action =
                    "com.example.todolist.TASK_REMINDER_$uniqueTaskId"

                putExtra(
                    "task_name",
                    taskName
                )

                putExtra(
                    "notification_type",
                    "task"
                )
            }


        val requestCode =
            20000 +
                    (uniqueTaskId and 0x7fff)


        val pendingIntent =
            PendingIntent.getBroadcast(

                context,

                requestCode,

                intent,

                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )


        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            if (
                !alarmManager.canScheduleExactAlarms()
            ) {

                try {

                    context.startActivity(

                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        ).apply {

                            data =
                                android.net.Uri.parse(
                                    "package:${context.packageName}"
                                )

                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                        }
                    )

                } catch (
                    e: Exception
                ) {

                    Toast.makeText(
                        context,
                        "Please allow exact alarms",
                        Toast.LENGTH_LONG
                    ).show()
                }

                return
            }
        }


        try {

            alarmManager.setExactAndAllowWhileIdle(

                AlarmManager.RTC_WAKEUP,

                reminderTime,

                pendingIntent
            )

        } catch (
            e: SecurityException
        ) {

            Toast.makeText(
                context,
                "Unable to schedule reminder",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // ============================================================
    // CREATE NOTIFICATION CHANNEL
    // ============================================================

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val notificationManager =
                getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager


            val channel =
                NotificationChannel(

                    CHANNEL_ID,

                    "To-Do Reminders",

                    NotificationManager.IMPORTANCE_HIGH

                ).apply {

                    description =
                        "Task and daily reminders"

                    enableVibration(true)

                    vibrationPattern =
                        longArrayOf(
                            0,
                            500,
                            250,
                            500
                        )

                    setShowBadge(true)

                    enableLights(true)
                }


            notificationManager
                .createNotificationChannel(
                    channel
                )
        }
    }
}


// ====================================================================
// DAILY NOTIFICATION SCHEDULING
// ====================================================================

fun scheduleDailyNotifications(
    context: Context
) {

    scheduleDailyNotification(
        context,
        9,
        0,
        MORNING_NOTIFICATION_CODE
    )

    scheduleDailyNotification(
        context,
        12,
        0,
        NOON_NOTIFICATION_CODE
    )

    scheduleDailyNotification(
        context,
        15,
        0,
        AFTERNOON_NOTIFICATION_CODE
    )

    scheduleDailyNotification(
        context,
        18,
        0,
        EVENING_NOTIFICATION_CODE
    )

    scheduleDailyNotification(
        context,
        21,
        0,
        NIGHT_NOTIFICATION_CODE
    )
}


// ====================================================================
// SCHEDULE ONE DAILY NOTIFICATION
// ====================================================================

private fun scheduleDailyNotification(

    context: Context,

    hour: Int,

    minute: Int,

    requestCode: Int

) {

    val alarmManager =
        context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager


    val calendar =
        Calendar.getInstance().apply {

            set(
                Calendar.HOUR_OF_DAY,
                hour
            )

            set(
                Calendar.MINUTE,
                minute
            )

            set(
                Calendar.SECOND,
                0
            )

            set(
                Calendar.MILLISECOND,
                0
            )


            if (
                timeInMillis <=
                System.currentTimeMillis()
            ) {

                add(
                    Calendar.DAY_OF_YEAR,
                    1
                )
            }
        }


    val notificationType =

        when (requestCode) {

            MORNING_NOTIFICATION_CODE ->
                "morning"

            NOON_NOTIFICATION_CODE ->
                "noon"

            AFTERNOON_NOTIFICATION_CODE ->
                "afternoon"

            EVENING_NOTIFICATION_CODE ->
                "evening"

            NIGHT_NOTIFICATION_CODE ->
                "night"

            else ->
                "daily"
        }


    val intent =
        Intent(
            context,
            ReminderReceiver::class.java
        ).apply {

            action =
                "com.example.todolist.DAILY_NOTIFICATION_$requestCode"

            putExtra(
                "notification_type",
                notificationType
            )
        }


    val pendingIntent =
        PendingIntent.getBroadcast(

            context,

            requestCode,

            intent,

            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )


    if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.S
    ) {

        if (
            !alarmManager.canScheduleExactAlarms()
        ) {

            return
        }
    }


    try {

        alarmManager.setExactAndAllowWhileIdle(

            AlarmManager.RTC_WAKEUP,

            calendar.timeInMillis,

            pendingIntent
        )

    } catch (
        e: SecurityException
    ) {
        // Permission unavailable
    }
}


// ====================================================================
// RANDOM NOTIFICATION MESSAGE
// ====================================================================

private fun getNotificationMessage(
    type: String
): String {

    val messages =

        when (type) {

            "morning" ->

                listOf(

                    "Start your day with one small task. 🚀",

                    "Good morning! Let's make today productive. ✨",

                    "Your goals are waiting. Start now! 💪",

                    "A fresh day means a fresh chance to get things done. 🌅"
                )


            "noon" ->

                listOf(

                    "It's noon! Take a quick look at your tasks. ☀️",

                    "Half the day is here. Let's get one more thing done! 💪",

                    "Don't forget your goals today. Keep moving forward! 🚀",

                    "A small task now can make your evening easier. ✨"
                )


            "afternoon" ->

                listOf(

                    "3 PM check-in! How are your tasks going? 🔥",

                    "You've still got time to accomplish something great today. 💪",

                    "Afternoon reminder: finish one important task. ⚡",

                    "Keep the momentum going! One more task. 🚀"
                )


            "evening" ->

                listOf(

                    "How much did you accomplish today? Keep going! 🔥",

                    "Evening check-in: finish one more task. 💪",

                    "Don't let today's goals wait until tomorrow. ⚡",

                    "You've got time for one more productive task! ✨"
                )


            "night" ->

                listOf(

                    "Before you sleep, check your pending tasks. 🌙",

                    "Wrap up your day and prepare for tomorrow. ✨",

                    "A little planning tonight makes tomorrow easier. 🚀",

                    "Good night! Don't forget your unfinished tasks. 🌙"
                )


            else ->

                listOf(
                    "Don't forget to check your tasks! 🔔"
                )
        }


    return messages.random()
}


// ====================================================================
// REMINDER RECEIVER
// ====================================================================

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(

        context: Context,

        intent: Intent

    ) {

        createReceiverNotificationChannel(
            context
        )


        val notificationType =

            intent.getStringExtra(
                "notification_type"
            ) ?: "task"


        val taskName =

            intent.getStringExtra(
                "task_name"
            )


        val notificationManager =

            androidx.core.app
                .NotificationManagerCompat
                .from(context)


        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (

                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) !=
                PackageManager.PERMISSION_GRANTED

            ) {

                return
            }
        }


        val title: String
        val message: String


        if (
            notificationType == "task"
        ) {

            title =
                "⏰ Task Reminder"

            message =
                taskName
                    ?: "You have a task to complete!"

        } else {

            title =

                when (
                    notificationType
                ) {

                    "morning" ->
                        "🌅 Good Morning!"

                    "noon" ->
                        "☀️ Noon Reminder"

                    "afternoon" ->
                        "🌤️ Afternoon Reminder"

                    "evening" ->
                        "🌆 Evening Reminder"

                    "night" ->
                        "🌙 Night Reminder"

                    else ->
                        "🔔 Daily Reminder"
                }


            message =
                getNotificationMessage(
                    notificationType
                )
        }


        val notification =

            NotificationCompat
                .Builder(
                    context,
                    CHANNEL_ID
                )

                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )

                .setContentTitle(
                    title
                )

                .setContentText(
                    message
                )

                .setStyle(

                    NotificationCompat
                        .BigTextStyle()
                        .bigText(
                            message
                        )
                )

                .setPriority(
                    NotificationCompat.PRIORITY_MAX
                )

                .setCategory(
                    NotificationCompat.CATEGORY_REMINDER
                )

                .setDefaults(
                    NotificationCompat.DEFAULT_ALL
                )

                .setAutoCancel(
                    true
                )

                .setVisibility(
                    NotificationCompat.VISIBILITY_PUBLIC
                )

                .build()


        notificationManager.notify(

            System.currentTimeMillis()
                .toInt(),

            notification
        )


        if (
            notificationType != "task"
        ) {

            val requestCode =

                when (
                    notificationType
                ) {

                    "morning" ->
                        MORNING_NOTIFICATION_CODE

                    "noon" ->
                        NOON_NOTIFICATION_CODE

                    "afternoon" ->
                        AFTERNOON_NOTIFICATION_CODE

                    "evening" ->
                        EVENING_NOTIFICATION_CODE

                    "night" ->
                        NIGHT_NOTIFICATION_CODE

                    else ->
                        return
                }


            val hour =

                when (
                    notificationType
                ) {

                    "morning" ->
                        9

                    "noon" ->
                        12

                    "afternoon" ->
                        15

                    "evening" ->
                        18

                    "night" ->
                        21

                    else ->
                        return
                }


            scheduleDailyNotification(

                context,

                hour,

                0,

                requestCode
            )
        }
    }


    // ================================================================
    // RECEIVER NOTIFICATION CHANNEL
    // ================================================================

    private fun createReceiverNotificationChannel(

        context: Context

    ) {

        if (

            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O

        ) {

            val manager =

                context.getSystemService(

                    Context.NOTIFICATION_SERVICE

                ) as NotificationManager


            val channel =

                NotificationChannel(

                    CHANNEL_ID,

                    "To-Do Reminders",

                    NotificationManager.IMPORTANCE_HIGH

                ).apply {

                    description =
                        "Task and daily reminders"

                    enableVibration(
                        true
                    )

                    vibrationPattern =
                        longArrayOf(
                            0,
                            500,
                            250,
                            500
                        )

                    setShowBadge(
                        true
                    )

                    enableLights(
                        true
                    )
                }


            manager.createNotificationChannel(
                channel
            )
        }
    }
}


// ================================================================
// BUDDY: FRIENDLY, ANIMATED TASK COMPANION
// ================================================================

@Composable
private fun BuddyCard(
    totalTasks: Int,
    completedTasks: Int,
    celebrating: Boolean,
    motionToggle: Boolean,
    themeMessage: String = ""
) {
    // Buddy keeps gently floating all the time, not only when a task changes.
    val buddyAnimation = rememberInfiniteTransition(label = "buddyIdleAnimation")
    val bob by buddyAnimation.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buddyFloat"
    )
    val wiggle by buddyAnimation.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buddyIdleWiggle"
    )
    val scale by animateFloatAsState(
        targetValue = if (motionToggle || celebrating) 1.08f else 1.0f,
        label = "buddyBounce"
    )
    val reactionRotation by animateFloatAsState(
        targetValue = if (motionToggle || celebrating) 6f else 0f,
        label = "buddyReactionWiggle"
    )
    val rotation = wiggle + reactionRotation

    val message = when {
        celebrating -> "YAYYY! Task completed! You're doing amazing! 🎉"
        themeMessage.isNotBlank() -> themeMessage
        totalTasks == 0 -> "Let's add your first task and start the day!"
        completedTasks == totalTasks -> "All tasks done! I'm so proud of you! 🥳"
        completedTasks > 0 -> "Nice progress! Let's finish the next one! 💪"
        else -> "A new task? Let's do this together! 🚀"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (celebrating) Color(0xFFFFE082)
            else MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = if (celebrating) Color(0xFFFFF8E1)
                        else MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (celebrating) "🥳" else if (motionToggle) "🐻‍❄️" else "🐻",
                    fontSize = 40.sp,
                    modifier = Modifier
                        .offset(y = bob.dp)
                        .scale(scale)
                        .rotate(rotation)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (celebrating) "Buddy is celebrating!" else "Buddy",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (celebrating) Color(0xFF7A3E00)
                    else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = if (celebrating) Color(0xFF7A3E00)
                    else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "$completedTasks / $totalTasks tasks completed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (celebrating) Color(0xFF7A3E00)
                    else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}



package com.example.todolist

import android.Manifest
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.Button
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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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


        // ========================================================
        // CREATE NOTIFICATION CHANNEL
        // ========================================================

        createNotificationChannel()


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

        setupDailyNotifications()


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

                var menuExpanded by remember {
                    mutableStateOf(false)
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
                // SHARED PREFERENCES
                // ==================================================

                val sharedPreferences = remember {

                    context.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                    )
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

                Surface(

                    modifier =
                        Modifier.fillMaxSize(),

                    color =
                        MaterialTheme
                            .colorScheme
                            .background

                ) {

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

                                    text =
                                        "✨ My Tasks",

                                    fontSize =
                                        30.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(4.dp)
                                )

                                Text(

                                    text =
                                        "Stay organized and get things done!",

                                    fontSize =
                                        14.sp,

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

                                    text =
                                        "⋮",

                                    fontSize =
                                        30.sp,

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
                                }
                            }
                        }


                        Spacer(
                            modifier =
                                Modifier.height(20.dp)
                        )


                        // =================================================
                        // SEARCH
                        // =================================================

                        OutlinedTextField(

                            value =
                                searchText,

                            onValueChange = {
                                searchText = it
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


                                // =================================================
                                // TASK NAME
                                // =================================================

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


                                    // Transparent clickable layer
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

                                                // ================================
                                                // UPDATE TASK
                                                // ================================

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


                                                // ================================
                                                // MOVE REMINDER IF NAME CHANGED
                                                // ================================

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

                                                // ================================
                                                // ADD TASK
                                                // ================================

                                                tasks.add(

                                                    Task(
                                                        name =
                                                            taskText.trim(),
                                                        label =
                                                            selectedLabel
                                                    )
                                                )


                                                Toast.makeText(
                                                    context,
                                                    "Task added",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }


                                            // ================================
                                            // SAVE
                                            // ================================

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
                                // FULL LABEL COLOURED TASK CARD
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

                                    Row(

                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),

                                        verticalAlignment =
                                            Alignment.CenterVertically

                                    ) {

                                        // =========================================
                                        // CHECKBOX
                                        // =========================================

                                        Checkbox(

                                            checked =
                                                taskItem.completed,

                                            onCheckedChange = {

                                                tasks[originalIndex] =
                                                    taskItem.copy(
                                                        completed = it
                                                    )

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
                                                Modifier.weight(1f)

                                        ) {

                                            // =====================================
                                            // TASK NAME
                                            // =====================================

                                            Text(

                                                text =
                                                    taskItem.name,

                                                fontSize =
                                                    16.sp,

                                                fontWeight =
                                                    FontWeight.SemiBold,

                                                modifier =
                                                    Modifier.alpha(

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
                                            // NO PILL BACKGROUND
                                            // =====================================

                                            Row(

                                                verticalAlignment =
                                                    Alignment.CenterVertically

                                            ) {

                                                // Small colored dot

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
                                                        )
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
                                                        Color(0xFF16A34A)
                                                )
                                            }
                                        }


                                        // =========================================
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


                        Spacer(
                            modifier =
                                Modifier.height(30.dp)
                        )
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

            scheduleDailyNotifications(this)
        }
    }


    // ============================================================
    // SETUP DAILY NOTIFICATIONS
    // ============================================================

    private fun setupDailyNotifications() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            val alarmManager =
                getSystemService(
                    Context.ALARM_SERVICE
                ) as AlarmManager


            if (
                !alarmManager.canScheduleExactAlarms()
            ) {

                try {

                    startActivity(

                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        ).apply {

                            data =
                                android.net.Uri.parse(
                                    "package:$packageName"
                                )
                        }
                    )

                } catch (
                    e: Exception
                ) {

                    Toast.makeText(
                        this,
                        "Please allow exact alarms",
                        Toast.LENGTH_LONG
                    ).show()
                }

                return
            }
        }


        scheduleDailyNotifications(this)
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


        // ========================================================
        // EXACT ALARM PERMISSION
        // ========================================================

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


        // ========================================================
        // SET EXACT ALARM
        // ========================================================

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

    // 🌅 MORNING — 9:00 AM

    scheduleDailyNotification(
        context,
        9,
        0,
        MORNING_NOTIFICATION_CODE
    )


    // ☀️ NOON — 12:00 PM

    scheduleDailyNotification(
        context,
        12,
        0,
        NOON_NOTIFICATION_CODE
    )


    // 🌤️ AFTERNOON — 3:00 PM

    scheduleDailyNotification(
        context,
        15,
        0,
        AFTERNOON_NOTIFICATION_CODE
    )


    // 🌆 EVENING — 6:00 PM

    scheduleDailyNotification(
        context,
        18,
        0,
        EVENING_NOTIFICATION_CODE
    )


    // 🌙 NIGHT — 9:00 PM

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


    // ================================================================
    // NOTIFICATION TYPE
    // ================================================================

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


    // ================================================================
    // EXACT ALARM PERMISSION
    // ================================================================

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


    // ================================================================
    // SET ALARM
    // ================================================================

    try {

        alarmManager.setExactAndAllowWhileIdle(

            AlarmManager.RTC_WAKEUP,

            calendar.timeInMillis,

            pendingIntent
        )

    } catch (
        e: SecurityException
    ) {

        // Exact alarm permission unavailable
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

            // ========================================================
            // MORNING
            // ========================================================

            "morning" ->

                listOf(

                    "Start your day with one small task. 🚀",

                    "Good morning! Let's make today productive. ✨",

                    "Your goals are waiting. Start now! 💪",

                    "A fresh day means a fresh chance to get things done. 🌅"
                )


            // ========================================================
            // NOON
            // ========================================================

            "noon" ->

                listOf(

                    "It's noon! Take a quick look at your tasks. ☀️",

                    "Half the day is here. Let's get one more thing done! 💪",

                    "Don't forget your goals today. Keep moving forward! 🚀",

                    "A small task now can make your evening easier. ✨"
                )


            // ========================================================
            // AFTERNOON
            // ========================================================

            "afternoon" ->

                listOf(

                    "3 PM check-in! How are your tasks going? 🔥",

                    "You've still got time to accomplish something great today. 💪",

                    "Afternoon reminder: finish one important task. ⚡",

                    "Keep the momentum going! One more task. 🚀"
                )


            // ========================================================
            // EVENING
            // ========================================================

            "evening" ->

                listOf(

                    "How much did you accomplish today? Keep going! 🔥",

                    "Evening check-in: finish one more task. 💪",

                    "Don't let today's goals wait until tomorrow. ⚡",

                    "You've got time for one more productive task! ✨"
                )


            // ========================================================
            // NIGHT
            // ========================================================

            "night" ->

                listOf(

                    "Before you sleep, check your pending tasks. 🌙",

                    "Wrap up your day and prepare for tomorrow. ✨",

                    "A little planning tonight makes tomorrow easier. 🚀",

                    "Good night! Don't forget your unfinished tasks. 🌙"
                )


            // ========================================================
            // DEFAULT
            // ========================================================

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

        // ============================================================
        // CREATE CHANNEL
        // ============================================================

        createReceiverNotificationChannel(
            context
        )


        // ============================================================
        // GET DATA
        // ============================================================

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


        // ============================================================
        // ANDROID 13+ PERMISSION
        // ============================================================

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


        // ============================================================
        // TITLE + MESSAGE
        // ============================================================

        val title: String

        val message: String


        // ============================================================
        // TASK REMINDER
        // ============================================================

        if (
            notificationType == "task"
        ) {

            title =
                "⏰ Task Reminder"


            message =
                taskName
                    ?: "You have a task to complete!"

        } else {

            // ========================================================
            // DAILY NOTIFICATION
            // ========================================================

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


        // ============================================================
        // NOTIFICATION
        // ============================================================

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


        // ============================================================
        // SHOW NOTIFICATION
        // ============================================================

        notificationManager.notify(

            System.currentTimeMillis()
                .toInt(),

            notification
        )


        // ============================================================
        // RESCHEDULE NEXT DAY
        // ============================================================

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



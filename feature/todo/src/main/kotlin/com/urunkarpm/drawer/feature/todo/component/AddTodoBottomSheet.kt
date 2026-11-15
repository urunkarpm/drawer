package com.urunkarpm.drawer.feature.todo.component

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.urunkarpm.drawer.core.designsystem.haptics.LocalCrispHaptics
import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoPriority
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// ponytail: modern unified task canvas with cohesive pill tokens, contextual cursor positioning, and direct preset triggers.
// ceiling: manual dialog-based scheduling without natural language parser;
// upgrade path: on-device natural language parsing (e.g. "dentist next Tuesday at 4pm").
private data class QuickIdea(
    val id: String,
    val label: String,
    val prefix: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoBottomSheet(
    onDismiss: () -> Unit,
    onAddTodo: (
        title: String,
        note: String?,
        placeName: String?,
        latitude: Double?,
        longitude: Double?,
        radiusM: Float,
        deadlineUtc: Long?,
        deadlineZoneId: String?,
        priority: TodoPriority,
        repeatRule: String?
    ) -> Unit,
    onUpdateTodo: ((TodoItem) -> Unit)? = null,
    onDeleteTodo: ((TodoItem) -> Unit)? = null,
    editingTodo: TodoItem? = null,
    onPickPlaceClick: () -> Unit = {},
    presetPlaceName: String? = null,
    presetLatitude: Double? = null,
    presetLongitude: Double? = null,
    presetRadiusM: Float = 200f,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val crispHaptics = LocalCrispHaptics.current
    val isEditMode = editingTodo != null

    var titleValue by remember(editingTodo) {
        val initialText = editingTodo?.title.orEmpty()
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length)
            )
        )
    }
    var note by remember(editingTodo) { mutableStateOf(editingTodo?.note.orEmpty()) }

    var selectedPlaceName by remember(editingTodo, presetPlaceName) {
        mutableStateOf(presetPlaceName ?: editingTodo?.placeName)
    }
    var selectedLat by remember(editingTodo, presetLatitude) {
        mutableStateOf(presetLatitude ?: editingTodo?.latitude)
    }
    var selectedLon by remember(editingTodo, presetLongitude) {
        mutableStateOf(presetLongitude ?: editingTodo?.longitude)
    }
    var selectedRadius by remember(editingTodo, presetRadiusM) {
        mutableFloatStateOf(if (presetLatitude != null) presetRadiusM else (editingTodo?.radiusM ?: 200f))
    }

    var deadlineUtc by remember(editingTodo) { mutableStateOf(editingTodo?.deadlineUtc) }
    var priority by remember(editingTodo) { mutableStateOf(editingTodo?.priority ?: TodoPriority.NORMAL) }
    var repeatRule by remember(editingTodo) { mutableStateOf(editingTodo?.repeatRule) }
    var showDateTimePicker by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!isEditMode) {
            focusRequester.requestFocus()
        }
    }

    fun openDateTimePicker() {
        crispHaptics.click()
        showDateTimePicker = true
    }

    fun submitForm() {
        val trimmedTitle = titleValue.text.trim()
        if (trimmedTitle.isBlank()) return
        crispHaptics.confirm()

        if (editingTodo != null) {
            val updated = editingTodo.copy(
                title = trimmedTitle,
                note = note.trim().takeIf { it.isNotBlank() },
                placeName = selectedPlaceName?.trim()?.takeIf { it.isNotBlank() },
                latitude = selectedLat,
                longitude = selectedLon,
                radiusM = selectedRadius,
                deadlineUtc = deadlineUtc,
                deadlineZoneId = ZoneId.systemDefault().id,
                priority = priority,
                repeatRule = repeatRule?.trim()?.takeIf { it.isNotBlank() }
            )
            onUpdateTodo?.invoke(updated)
        } else {
            onAddTodo(
                trimmedTitle,
                note.trim().takeIf { it.isNotBlank() },
                selectedPlaceName,
                selectedLat,
                selectedLon,
                selectedRadius,
                deadlineUtc,
                ZoneId.systemDefault().id,
                priority,
                repeatRule
            )
        }
        onDismiss()
    }

    val quickIdeas = remember {
        listOf(
            QuickIdea("call", "Call", "Call ", Icons.Outlined.Phone),
            QuickIdea("buy", "Buy", "Buy ", Icons.Outlined.ShoppingCart),
            QuickIdea("pay", "Pay", "Pay ", Icons.Outlined.Payments),
            QuickIdea("meds", "Meds", "Take ", Icons.Outlined.Medication),
            QuickIdea("email", "Email", "Email ", Icons.Outlined.Email),
            QuickIdea("meet", "Meet", "Meeting with ", Icons.Outlined.Groups)
        )
    }

    val activeIdea = quickIdeas.firstOrNull { idea ->
        titleValue.text.startsWith(idea.prefix, ignoreCase = true)
    }

    if (showDateTimePicker) {
        TaskDateTimePickerDialog(
            initialUtc = deadlineUtc,
            onConfirm = { newUtc ->
                crispHaptics.confirm()
                deadlineUtc = newUtc
            },
            onDismiss = { showDateTimePicker = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditMode) "Edit Task" else "New Task",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (editingTodo != null && onDeleteTodo != null) {
                        IconButton(
                            onClick = {
                                crispHaptics.reject()
                                onDeleteTodo(editingTodo)
                                onDismiss()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete task",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. Unified Task Canvas (Fluid borderless card)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextField(
                        value = titleValue,
                        onValueChange = { titleValue = it },
                        placeholder = {
                            Text(
                                "What needs to be done?",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { submitForm() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        thickness = 0.75.dp
                    )

                    TextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = {
                            Text(
                                "Add details, notes or steps... (optional)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 3,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. Proactive Quick Ideas (Toggleable & swappable, vector icons, no emojis)
            if (!isEditMode) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (activeIdea != null) "Idea selected (tap to swap or remove):" else "Quick ideas:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (activeIdea != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        quickIdeas.forEach { idea ->
                            val isSelected = activeIdea?.id == idea.id
                            Surface(
                                onClick = {
                                    val currentText = titleValue.text
                                    if (isSelected) {
                                        crispHaptics.tick()
                                        val newText = currentText.substring(idea.prefix.length).trimStart()
                                        titleValue = TextFieldValue(
                                            text = newText,
                                            selection = TextRange(0)
                                        )
                                    } else {
                                        crispHaptics.click()
                                        val suffix = if (activeIdea != null) {
                                            currentText.substring(activeIdea.prefix.length)
                                        } else {
                                            currentText.trimStart()
                                        }
                                        val newText = idea.prefix + suffix
                                        // Cursor is placed immediately after the clicked idea word
                                        val cursorPosition = idea.prefix.length
                                        titleValue = TextFieldValue(
                                            text = newText,
                                            selection = TextRange(cursorPosition)
                                        )
                                        focusRequester.requestFocus()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.50f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = idea.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = idea.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Symmetrical Contextual Triggers: Date & Time, and Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date & Time Picker Trigger
                val hasDeadline = deadlineUtc != null
                Surface(
                    onClick = { openDateTimePicker() },
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasDeadline) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.50f),
                    border = BorderStroke(
                        1.dp,
                        if (hasDeadline) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (hasDeadline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (hasDeadline) formatDeadlineLabel(deadlineUtc!!) else "Date & Time",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (hasDeadline) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (hasDeadline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (hasDeadline) {
                            IconButton(
                                onClick = {
                                    crispHaptics.tick()
                                    deadlineUtc = null
                                },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear deadline",
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Location Picker Trigger
                val hasPlace = !selectedPlaceName.isNullOrBlank()
                Surface(
                    onClick = {
                        crispHaptics.click()
                        onPickPlaceClick()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasPlace) MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.50f),
                    border = BorderStroke(
                        1.dp,
                        if (hasPlace) MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (hasPlace) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (hasPlace) selectedPlaceName.orEmpty() else "Location",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (hasPlace) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (hasPlace) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (hasPlace) {
                            IconButton(
                                onClick = {
                                    crispHaptics.tick()
                                    selectedPlaceName = null
                                    selectedLat = null
                                    selectedLon = null
                                },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear location",
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            // 4. Priority Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TodoPriority.entries.forEach { p ->
                        val isSelected = priority == p
                        val priorityColor = when (p) {
                            TodoPriority.HIGH -> MaterialTheme.colorScheme.error
                            TodoPriority.NORMAL -> MaterialTheme.colorScheme.primary
                            TodoPriority.LOW -> MaterialTheme.colorScheme.outline
                        }
                        Surface(
                            onClick = {
                                crispHaptics.tick()
                                priority = p
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) priorityColor.copy(alpha = 0.20f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.50f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) priorityColor.copy(alpha = 0.50f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            color = if (isSelected) priorityColor else priorityColor.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                )
                                Text(
                                    text = p.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) priorityColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 5. Repeat Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Repeat",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val repeatOptions = listOf(
                        "Never" to null,
                        "Daily" to "DAILY",
                        "Weekdays" to "WEEKDAYS",
                        "Weekly" to "WEEKLY"
                    )
                    repeatOptions.forEach { (label, rule) ->
                        val isSelected = repeatRule == rule
                        Surface(
                            onClick = {
                                crispHaptics.tick()
                                repeatRule = rule
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.50f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.50f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 6. Submit Button
            Button(
                onClick = { submitForm() },
                enabled = titleValue.text.trim().isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEditMode) "Save Changes" else "Add Task",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDateTimePickerDialog(
    initialUtc: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val crispHaptics = LocalCrispHaptics.current

    val initialZdt = remember(initialUtc) {
        if (initialUtc != null) {
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(initialUtc), ZoneId.systemDefault())
        } else {
            ZonedDateTime.now(ZoneId.systemDefault()).plusHours(1).withMinute(0).withSecond(0)
        }
    }

    var isPickingTime by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialZdt.toLocalDate()
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
    )

    val timePickerState = rememberTimePickerState(
        initialHour = initialZdt.hour,
        initialMinute = initialZdt.minute,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Tabs: Date & Time step switchers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = {
                                crispHaptics.tick()
                                isPickingTime = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (!isPickingTime) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (!isPickingTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Date",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (!isPickingTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                crispHaptics.tick()
                                isPickingTime = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPickingTime) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isPickingTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Time",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isPickingTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    thickness = 0.8.dp
                )

                // Body: AnimatedContent between DatePicker and TimePicker
                AnimatedContent(
                    targetState = isPickingTime,
                    label = "DatePickerStepTransition"
                ) { pickingTime ->
                    if (!pickingTime) {
                        DatePicker(
                            state = datePickerState,
                            showModeToggle = false,
                            title = null,
                            headline = null,
                            colors = DatePickerDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TimePicker(
                                state = timePickerState,
                                colors = TimePickerDefaults.colors()
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    thickness = 0.8.dp
                )

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isPickingTime) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                crispHaptics.click()
                                isPickingTime = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Next: Time")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                crispHaptics.click()
                                isPickingTime = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back")
                        }
                        Button(
                            onClick = {
                                crispHaptics.confirm()
                                val selectedDate = datePickerState.selectedDateMillis?.let { utcMillis ->
                                    Instant.ofEpochMilli(utcMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                                } ?: LocalDate.now()

                                val finalZdt = ZonedDateTime.of(
                                    selectedDate,
                                    LocalTime.of(timePickerState.hour, timePickerState.minute),
                                    ZoneId.systemDefault()
                                )
                                onConfirm(finalZdt.toInstant().toEpochMilli())
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set Deadline")
                        }
                    }
                }
            }
        }
    }
}

private fun formatDeadlineLabel(utc: Long): String {
    val zoneId = ZoneId.systemDefault()
    val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(utc), zoneId)
    val now = ZonedDateTime.now(zoneId)
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    return when {
        zdt.toLocalDate().isEqual(now.toLocalDate()) -> "Today ${zdt.format(timeFmt)}"
        zdt.toLocalDate().isEqual(now.toLocalDate().plusDays(1)) -> "Tomorrow ${zdt.format(timeFmt)}"
        else -> zdt.format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
    }
}

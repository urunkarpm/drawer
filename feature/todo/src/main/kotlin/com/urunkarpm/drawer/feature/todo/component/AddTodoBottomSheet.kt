package com.urunkarpm.drawer.feature.todo.component

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoPriority
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
    val context = LocalContext.current
    val isEditMode = editingTodo != null

    var title by remember(editingTodo) { mutableStateOf(editingTodo?.title.orEmpty()) }
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

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!isEditMode) {
            focusRequester.requestFocus()
        }
    }

    fun openDateTimePicker() {
        showNativeDateTimePicker(context, deadlineUtc) { newUtc ->
            deadlineUtc = newUtc
        }
    }

    fun submitForm() {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) return

        if (isEditMode && editingTodo != null) {
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
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    if (isEditMode && onDeleteTodo != null && editingTodo != null) {
                        IconButton(
                            onClick = {
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

            // 1. Title field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What needs to be done?") },
                placeholder = { Text("e.g. Buy groceries") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitForm() }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            // Optional note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Place & Deadline Interactive Chips (Both Optional)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Place Chip
                val hasPlace = !selectedPlaceName.isNullOrBlank()
                InputChip(
                    selected = hasPlace,
                    onClick = onPickPlaceClick,
                    label = {
                        Text(
                            text = selectedPlaceName ?: "Add place",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (hasPlace) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = if (hasPlace) {
                        {
                            IconButton(
                                onClick = {
                                    selectedPlaceName = null
                                    selectedLat = null
                                    selectedLon = null
                                },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear place",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else null,
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Deadline Time Chip
                val formattedDeadline = remember(deadlineUtc) {
                    deadlineUtc?.let { formatDeadlineLabel(it) }
                }
                val hasDeadline = deadlineUtc != null

                InputChip(
                    selected = hasDeadline,
                    onClick = { openDateTimePicker() },
                    label = {
                        Text(
                            text = formattedDeadline ?: "Add date & time",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (hasDeadline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = if (hasDeadline) {
                        {
                            IconButton(
                                onClick = { deadlineUtc = null },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear deadline",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else null,
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Quick Deadline Shortcuts row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick time:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    onClick = {
                        val nowZdt = ZonedDateTime.now(ZoneId.systemDefault())
                        val later = nowZdt.plusHours(3).withMinute(0).withSecond(0)
                        deadlineUtc = later.toInstant().toEpochMilli()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "+3 Hours",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    onClick = {
                        val zoneId = ZoneId.systemDefault()
                        val tonight = ZonedDateTime.of(LocalDate.now(), LocalTime.of(21, 0), zoneId)
                        deadlineUtc = tonight.toInstant().toEpochMilli()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "Tonight (21:00)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    onClick = {
                        val zoneId = ZoneId.systemDefault()
                        val tomorrowMorning = ZonedDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(9, 0), zoneId)
                        deadlineUtc = tomorrowMorning.toInstant().toEpochMilli()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "Tomorrow (09:00)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    onClick = { openDateTimePicker() },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "Pick Custom...",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Priority and Repeat row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Priority:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TodoPriority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (p) {
                                TodoPriority.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            }
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Repeat:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf("None" to null, "Daily" to "DAILY", "Weekdays" to "WEEKDAYS", "Weekly" to "WEEKLY").forEach { (label, rule) ->
                    FilterChip(
                        selected = repeatRule == rule,
                        onClick = { repeatRule = rule },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Submit Button
            Button(
                onClick = { submitForm() },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEditMode) "Save Changes" else "Add Task",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun showNativeDateTimePicker(
    context: Context,
    initialUtc: Long?,
    onDateTimeSelected: (Long) -> Unit
) {
    val currentZdt = if (initialUtc != null) {
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(initialUtc), ZoneId.systemDefault())
    } else {
        ZonedDateTime.now(ZoneId.systemDefault()).plusHours(1).withMinute(0).withSecond(0)
    }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val chosenZdt = ZonedDateTime.of(
                        LocalDate.of(year, month + 1, dayOfMonth),
                        LocalTime.of(hourOfDay, minute),
                        ZoneId.systemDefault()
                    )
                    onDateTimeSelected(chosenZdt.toInstant().toEpochMilli())
                },
                currentZdt.hour,
                currentZdt.minute,
                android.text.format.DateFormat.is24HourFormat(context)
            ).show()
        },
        currentZdt.year,
        currentZdt.monthValue - 1,
        currentZdt.dayOfMonth
    ).show()
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

package com.ian.garagejournal.ui.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ian.garagejournal.domain.ReminderLevel
import com.ian.garagejournal.domain.ReminderWithStatus
import com.ian.garagejournal.domain.summaryText

@Composable
fun ReminderCard(
    item: ReminderWithStatus,
    onDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val status = item.status
    val color = levelColor(status.level)

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.reminder.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(levelLabel(status.level), style = MaterialTheme.typography.labelLarge, color = color)
            }
            LinearProgressIndicator(
                progress = { status.progress },
                modifier = Modifier.fillMaxWidth(),
                color = color
            )
            Text(status.summaryText(), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalButton(onClick = onDone) { Text("Sudah diservis") }
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Hapus") }
            }
        }
    }
}

@Composable
private fun levelColor(level: ReminderLevel): Color = when (level) {
    ReminderLevel.OK -> MaterialTheme.colorScheme.primary
    ReminderLevel.DUE_SOON -> MaterialTheme.colorScheme.tertiary
    ReminderLevel.OVERDUE -> MaterialTheme.colorScheme.error
}

// Warna tidak jadi satu-satunya penanda: selalu ada label teks
private fun levelLabel(level: ReminderLevel): String = when (level) {
    ReminderLevel.OK -> "Aman"
    ReminderLevel.DUE_SOON -> "Mendekati"
    ReminderLevel.OVERDUE -> "Jatuh tempo"
}


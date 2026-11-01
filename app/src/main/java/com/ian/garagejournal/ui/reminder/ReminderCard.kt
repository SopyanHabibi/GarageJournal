package com.ian.garagejournal.ui.reminder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    val level = status.level
    val barColor = levelColor(level)
    val icon = levelIcon(level)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: ikon + nama + badge status
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(barColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = barColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                item.reminder.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(barColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    levelLabel(level),
                    style = MaterialTheme.typography.labelSmall,
                    color = barColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Target odometer
        val targetOdometer = item.reminder.intervalKm
            ?.let { item.reminder.lastDoneOdometer + it }

        if (targetOdometer != null) {
            Text(
                "%,d km".format(targetOdometer),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            // Target progress
            Text(
                "Target odometer berikutnya",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Progress bar
        // Progress bar custom — tanpa dot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(status.progress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(barColor)
            )
        }

        status.kmRemaining?.let { remaining ->
            val currentOdo = targetOdometer?.minus(remaining)
            if (currentOdo != null) {
                Text(
                    "%,d km sekarang".format(currentOdo),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            status.summaryText(),
            style = MaterialTheme.typography.labelSmall,
            color = barColor,
            fontWeight = FontWeight.SemiBold
        )

        // Actions
        // CTA utama — full width
        ElevatedButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = barColor,
                contentColor = Color.Black
            )
        ) {
            Text("Sudah diservis", style = MaterialTheme.typography.labelMedium)
        }

// Secondary actions
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onEdit) { Text("Edit") }
            TextButton(onClick = onDelete) { Text("Hapus") }
        }
    }
}

@Composable
private fun levelColor(level: ReminderLevel): Color = when (level) {
    ReminderLevel.OK -> MaterialTheme.colorScheme.primary          // Lime
    ReminderLevel.DUE_SOON -> MaterialTheme.colorScheme.tertiary   // Amber
    ReminderLevel.OVERDUE -> MaterialTheme.colorScheme.error       // Merah
}

private fun levelIcon(level: ReminderLevel): ImageVector = when (level) {
    ReminderLevel.OK -> Icons.Default.OilBarrel
    ReminderLevel.DUE_SOON -> Icons.Default.Warning
    ReminderLevel.OVERDUE -> Icons.Default.LocalFireDepartment
}

private fun levelLabel(level: ReminderLevel): String = when (level) {
    ReminderLevel.OK -> "Terjadwal"
    ReminderLevel.DUE_SOON -> "Mendekati"
    ReminderLevel.OVERDUE -> "Jatuh Tempo"
}
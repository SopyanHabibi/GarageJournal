package com.ian.garagejournal.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ian.garagejournal.domain.ReminderWithStatus
import com.ian.garagejournal.ui.reminder.ReminderCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    viewModel: VehicleDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenServiceLog: () -> Unit,
    onOpenFuelLog: () -> Unit,
    onAddReminder: () -> Unit,
    onEditReminder: (Long) -> Unit,
    onOpenStats: () -> Unit
) {
    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val avgKmPerDay by viewModel.avgKmPerDay.collectAsStateWithLifecycle()
    var toMarkDone by remember { mutableStateOf<ReminderWithStatus?>(null) }
    var toDelete by remember { mutableStateOf<ReminderWithStatus?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vehicle?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { padding ->
        vehicle?.let { v ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("${v.brand} ${v.model} (${v.year})", style = MaterialTheme.typography.titleLarge)
                Text("Plat: ${v.plateNumber}")
                Text("Odometer: %,d km".format(v.currentOdometer))
                avgKmPerDay?.let {
                    Text(
                        "Rata-rata pemakaian: ±${"%,.0f".format(it)} km/hari",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                Button(onClick = onOpenServiceLog, modifier = Modifier.fillMaxWidth()) {
                    Text("Log Servis")
                }
                Button(onClick = onOpenFuelLog, modifier = Modifier.fillMaxWidth()) {
                    Text("Log BBM")
                }
                OutlinedButton(onClick = onOpenStats, modifier = Modifier.fillMaxWidth()) {
                    Text("Statistik")
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Pengingat Servis",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onAddReminder) { Text("+ Tambah") }
                }

                if (reminders.isEmpty()) {
                    Text("Belum ada pengingat. Tambahkan, misalnya \"Ganti oli\" tiap beberapa ribu km.")
                } else {
                    reminders.forEach { item ->
                        ReminderCard(
                            item = item,
                            onDone = { toMarkDone = item },
                            onEdit = { onEditReminder(item.reminder.id) },
                            onDelete = { toDelete = item }
                        )
                    }
                }
            }
        }
    }

    toMarkDone?.let { item ->
        AlertDialog(
            onDismissRequest = { toMarkDone = null },
            title = { Text("Tandai \"${item.reminder.name}\" sudah diservis?") },
            text = {
                Text("Dihitung dari odometer saat ini dan hari ini. Ini hanya me-reset pengingat, tidak membuat catatan di Log Servis.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.markDone(item.reminder); toMarkDone = null }) {
                    Text("Ya, tandai")
                }
            },
            dismissButton = { TextButton(onClick = { toMarkDone = null }) { Text("Batal") } }
        )
    }

    toDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Hapus pengingat \"${item.reminder.name}\"?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteReminder(item.reminder); toDelete = null }) {
                    Text("Hapus")
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Batal") } }
        )
    }
}
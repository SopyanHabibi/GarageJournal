package com.ian.garagejournal.ui.fuel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.ian.garagejournal.data.local.FuelLog
import com.ian.garagejournal.util.formatDate
import com.ian.garagejournal.util.formatEfficiency
import com.ian.garagejournal.util.formatLiters
import com.ian.garagejournal.util.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelListScreen(
    viewModel: FuelListViewModel,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<FuelLog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log BBM") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Tambah BBM")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SummaryCard(summary) }
            if (logs.isEmpty()) {
                item { Text("Belum ada catatan BBM. Tekan + untuk menambah.") }
            } else {
                items(logs, key = { it.id }) { log ->
                    FuelLogCard(
                        log = log,
                        onClick = { onEdit(log.id) },
                        onDelete = { toDelete = log }
                    )
                }
            }
        }
    }

    toDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Hapus catatan BBM ini?") },
            text = { Text("KM/L pengisian lain akan dihitung ulang otomatis.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(log); toDelete = null }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun SummaryCard(summary: FuelSummary) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Rata-rata", formatEfficiency(summary.overallEfficiency))
                Stat("Terakhir", formatEfficiency(summary.latestEfficiency))
            }
            Text(
                "Total BBM: ${formatRupiah(summary.totalCost)} • ${formatLiters(summary.totalLiters)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun FuelLogCard(log: FuelLog, onClick: () -> Unit, onDelete: () -> Unit) {
    val efficiencyText = when {
        log.kmPerLiter != null -> formatEfficiency(log.kmPerLiter)
        !log.isFullTank -> "Isi sebagian"
        else -> "Belum ada pembanding"
    }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(efficiencyText, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${formatDate(log.date)} • ${"%,d".format(log.odometer)} km",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "${formatLiters(log.liters)} × ${formatRupiah(log.pricePerLiter)} = ${formatRupiah(log.totalCost)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    if (log.isFullTank) "Penuh" else "Sebagian",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus")
            }
        }
    }
}
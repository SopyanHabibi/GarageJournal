package com.ian.garagejournal.ui.vehicle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ian.garagejournal.ui.vehicle.VehicleListViewModel
import com.ian.garagejournal.data.local.Vehicle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    viewModel: VehicleListViewModel,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onEdit: (Long) -> Unit
) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<Vehicle?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Garage Journal") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Tambah motor")
            }
        }
    ) { padding ->
        if (vehicles.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada motor. Tekan + untuk menambah.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vehicles, key = { it.id }) { v ->
                    VehicleCard(
                        vehicle = v,
                        onClick = { onOpen(v.id) },
                        onEdit = { onEdit(v.id) },
                        onDelete = { toDelete = v }
                    )
                }
            }
        }
    }

    toDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Hapus ${v.name}?") },
            text = { Text("Semua log servis dan BBM motor ini ikut terhapus.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(v); toDelete = null }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(vehicle.name, style = MaterialTheme.typography.titleMedium)
                Text("${vehicle.brand} ${vehicle.model} • ${vehicle.year}", style = MaterialTheme.typography.bodyMedium)
                Text(vehicle.plateNumber, style = MaterialTheme.typography.bodySmall)
                Text("%,d km".format(vehicle.currentOdometer), style = MaterialTheme.typography.labelLarge)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Hapus") }
        }
    }
}
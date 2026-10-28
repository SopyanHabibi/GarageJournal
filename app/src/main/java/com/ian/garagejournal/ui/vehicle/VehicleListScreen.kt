package com.ian.garagejournal.ui.vehicle

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ian.garagejournal.data.backup.ExportSummary
import com.ian.garagejournal.data.backup.ImportSummary
import com.ian.garagejournal.data.backup.defaultBackupFileName
import com.ian.garagejournal.data.local.Vehicle
import com.ian.garagejournal.ui.backup.BackupState
import com.ian.garagejournal.ui.backup.BackupViewModel
import com.ian.garagejournal.util.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    viewModel: VehicleListViewModel,
    backupViewModel: BackupViewModel,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onEdit: (Long) -> Unit
) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val backupState by backupViewModel.state.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<Vehicle?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var showImportInfo by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) backupViewModel.export(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) backupViewModel.import(uri) }

    LaunchedEffect(backupState) {
        val message = when (val s = backupState) {
            is BackupState.ExportDone -> s.summary.describe()
            is BackupState.ImportDone -> s.summary.describe()
            is BackupState.Failed -> s.message
            else -> null
        }
        if (message != null) {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
            backupViewModel.dismiss()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Garage Journal") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        val idle = backupState !is BackupState.Running
                        DropdownMenuItem(
                            text = { Text("Ekspor data (ZIP)") },
                            enabled = idle,
                            onClick = {
                                menuOpen = false
                                exportLauncher.launch(defaultBackupFileName())
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Impor data (ZIP)") },
                            enabled = idle,
                            onClick = {
                                menuOpen = false
                                showImportInfo = true
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Tambah motor")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (backupState is BackupState.Running) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (vehicles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada motor. Tekan + untuk menambah.")
                }
            } else {
                val pagerState = rememberPagerState(pageCount = { vehicles.size })

                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val v = vehicles[page]
                    VehicleCard(
                        vehicle = v,
                        onClick = { onOpen(v.id) },
                        onEdit = { onEdit(v.id) },
                        onDelete = { toDelete = v },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(vehicles.size) { i ->
                        val selected = i == pagerState.currentPage
                        Box(
                            Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (selected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }
            }
        }
    }

    if (showImportInfo) {
        AlertDialog(
            onDismissRequest = { showImportInfo = false },
            title = { Text("Impor data dari backup") },
            text = {
                Text(
                    "Pilih berkas ZIP hasil ekspor Garage Journal. Data dari backup akan DITAMBAHKAN ke app ini. " +
                            "Motor yang plat nomornya sudah ada di app dilewati, jadi data yang sudah ada tidak diubah atau ditimpa."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportInfo = false
                    importLauncher.launch(
                        arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")
                    )
                }) { Text("Pilih berkas") }
            },
            dismissButton = { TextButton(onClick = { showImportInfo = false }) { Text("Batal") } }
        )
    }

    toDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Hapus ${v.displayName()}?") },
            text = { Text("Semua log servis, BBM, dan pengingat motor ini ikut terhapus.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(v); toDelete = null }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Batal") } }
        )
    }
}

private fun ExportSummary.describe(): String {
    val base = "Ekspor selesai: $vehicles motor, $serviceLogs servis, $fuelLogs BBM, " +
            "$reminders pengingat, $receipts foto nota"
    return if (missingReceipts > 0) "$base ($missingReceipts foto nota tidak ditemukan)" else base
}

private fun ImportSummary.describe(): String {
    val parts = mutableListOf("$vehicles motor ditambahkan")
    if (skippedVehicles > 0) parts += "$skippedVehicles dilewati karena sudah ada"
    parts += "$serviceLogs servis"
    parts += "$fuelLogs BBM"
    parts += "$reminders pengingat"
    parts += "$receipts foto nota"
    if (missingReceipts > 0) parts += "$missingReceipts foto nota tidak ada di backup"
    return "Impor selesai: " + parts.joinToString(", ")
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.TwoWheeler,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        vehicle.displayName(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus")
                    }
                }
                Text(
                    "${vehicle.brand} ${vehicle.model} • ${vehicle.year}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(vehicle.plateNumber, style = MaterialTheme.typography.bodySmall)
                Text(
                    "%,d km".format(vehicle.currentOdometer),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
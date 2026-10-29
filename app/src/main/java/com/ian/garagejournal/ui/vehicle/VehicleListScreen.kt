package com.ian.garagejournal.ui.vehicle

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ian.garagejournal.data.backup.ExportSummary
import com.ian.garagejournal.data.backup.ImportSummary
import com.ian.garagejournal.data.backup.defaultBackupFileName
import com.ian.garagejournal.data.local.Vehicle
import com.ian.garagejournal.data.repository.GarageRepository
import com.ian.garagejournal.domain.ReminderWithStatus
import com.ian.garagejournal.ui.backup.BackupState
import com.ian.garagejournal.ui.backup.BackupViewModel
import com.ian.garagejournal.ui.reminder.ReminderCard
import com.ian.garagejournal.util.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    viewModel: VehicleListViewModel,
    backupViewModel: BackupViewModel,
    repository: GarageRepository,
    onAdd: () -> Unit,
    onEditVehicle: (Long) -> Unit,
    onOpenServiceLog: (Long) -> Unit,
    onOpenFuelLog: (Long) -> Unit,
    onAddReminder: (Long) -> Unit,
    onEditReminder: (Long, Long) -> Unit,
    onOpenStats: (Long) -> Unit
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

    val pagerState = rememberPagerState(pageCount = { vehicles.size })
    val currentVehicle = vehicles.getOrNull(pagerState.currentPage)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentVehicle?.displayName() ?: "Garage Journal") },
                actions = {
                    currentVehicle?.let { v ->
                        IconButton(onClick = { onEditVehicle(v.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit motor")
                        }
                        IconButton(onClick = { toDelete = v }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus motor")
                        }
                    }
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
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val v = vehicles[page]
                    val detailViewModel = viewModel<VehicleDetailViewModel>(
                        key = "vehicle_detail_${v.id}",
                        factory = VehicleDetailViewModel.factory(repository, v.id)
                    )
                    VehicleDetailContent(
                        viewModel = detailViewModel,
                        onOpenServiceLog = { onOpenServiceLog(v.id) },
                        onOpenFuelLog = { onOpenFuelLog(v.id) },
                        onAddReminder = { onAddReminder(v.id) },
                        onEditReminder = { reminderId -> onEditReminder(v.id, reminderId) },
                        onOpenStats = { onOpenStats(v.id) }
                    )
                }

                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
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
private fun VehicleDetailContent(
    viewModel: VehicleDetailViewModel,
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
    var toDeleteReminder by remember { mutableStateOf<ReminderWithStatus?>(null) }

    vehicle?.let { v ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
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
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(v.displayName(), style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${v.brand} ${v.model} • ${v.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        icon = Icons.Default.Speed,
                        label = "Odometer",
                        value = "%,d km".format(v.currentOdometer),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        label = "Rata-rata/hari",
                        value = avgKmPerDay?.let { "±${"%,.0f".format(it)} km" } ?: "-",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        icon = Icons.Default.DateRange,
                        label = "Tahun",
                        value = v.year.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.Badge,
                        label = "Plat Nomor",
                        value = v.plateNumber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    icon = Icons.Default.Speed,
                    label = "Log Servis",
                    onClick = onOpenServiceLog,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.TwoWheeler,
                    label = "Log BBM",
                    onClick = onOpenFuelLog,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = "Statistik",
                    onClick = onOpenStats,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                            onDelete = { toDeleteReminder = item }
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

    toDeleteReminder?.let { item ->
        AlertDialog(
            onDismissRequest = { toDeleteReminder = null },
            title = { Text("Hapus pengingat \"${item.reminder.name}\"?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteReminder(item.reminder); toDeleteReminder = null }) {
                    Text("Hapus")
                }
            },
            dismissButton = { TextButton(onClick = { toDeleteReminder = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
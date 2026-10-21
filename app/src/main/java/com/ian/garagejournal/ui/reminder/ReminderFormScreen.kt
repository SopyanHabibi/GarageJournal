package com.ian.garagejournal.ui.reminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ian.garagejournal.ui.components.LocalDatePickerDialog
import com.ian.garagejournal.util.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderFormScreen(viewModel: ReminderFormViewModel, onDone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onDone() } // hasilnya tidak perlu dicek: kalau ditolak, status di dalam app tetap jalan

    LaunchedEffect(state.saved) {
        if (!state.saved) return@LaunchedEffect
        val needsPermission = !state.isEditing &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        if (needsPermission) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Pengingat" else "Tambah Pengingat") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name, onValueChange = viewModel::onNameChange,
                label = { Text("Nama (mis. Ganti oli)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            Text("Ingatkan setiap...", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = state.intervalKm, onValueChange = viewModel::onIntervalKmChange,
                label = { Text("Jarak (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.intervalMonths, onValueChange = viewModel::onIntervalMonthsChange,
                label = { Text("Waktu (bulan)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Isi salah satu atau keduanya. Kalau keduanya diisi, pengingat aktif begitu salah satunya tercapai.",
                style = MaterialTheme.typography.bodySmall
            )

            Text("Terakhir dilakukan", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = state.lastDoneOdometer, onValueChange = viewModel::onLastOdometerChange,
                label = { Text("Odometer saat itu (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Tanggal: ${formatDate(state.lastDoneDateMillis)}") }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("Simpan")
            }
        }
    }

    if (showDatePicker) {
        LocalDatePickerDialog(
            initialMillis = state.lastDoneDateMillis,
            onConfirm = viewModel::onDateChange,
            onDismiss = { showDatePicker = false }
        )
    }
}
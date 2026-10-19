package com.ian.garagejournal.ui.service

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ian.garagejournal.ui.components.LocalDatePickerDialog
import com.ian.garagejournal.util.formatDate
import com.ian.garagejournal.util.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceFormScreen(viewModel: ServiceFormViewModel, onDone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    // rememberSaveable: nama file tidak hilang kalau layar berputar saat kamera terbuka
    var pendingCameraFile by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        pendingCameraFile?.let { viewModel.onCameraResult(it, success) }
        pendingCameraFile = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onGalleryPicked(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Servis" else "Tambah Servis") },
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
                value = state.title, onValueChange = viewModel::onTitleChange,
                label = { Text("Judul (mis. Ganti oli)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Tanggal: ${formatDate(state.dateMillis)}") }

            OutlinedTextField(
                value = state.odometer, onValueChange = viewModel::onOdometerChange,
                label = { Text("Odometer (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.cost, onValueChange = viewModel::onCostChange,
                label = { Text("Biaya (Rp)") },
                supportingText = { state.cost.toLongOrNull()?.let { Text(formatRupiah(it)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.description, onValueChange = viewModel::onDescriptionChange,
                label = { Text("Catatan (opsional)") },
                minLines = 3, modifier = Modifier.fillMaxWidth()
            )

            Text("Nota", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val name = viewModel.prepareCameraFile()
                    pendingCameraFile = name
                    cameraLauncher.launch(viewModel.cameraUri(name))
                }) { Text("Foto nota") }

                OutlinedButton(onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) { Text("Pilih dari galeri") }
            }

            state.receiptFileName?.let { name ->
                AsyncImage(
                    model = viewModel.receiptFile(name),
                    contentDescription = "Nota",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                )
                TextButton(onClick = viewModel::removeReceipt) { Text("Hapus nota") }
            }

            if (!state.isEditing && state.reminders.isNotEmpty()) {
                Text("Servis ini me-reset pengingat:", style = MaterialTheme.typography.titleSmall)
                state.reminders.forEach { r ->
                    Row(
                        Modifier.fillMaxWidth().clickable { viewModel.onReminderToggle(r.id) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = r.id in state.selectedReminderIds,
                            onCheckedChange = { viewModel.onReminderToggle(r.id) }
                        )
                        Text(r.name)
                    }
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("Simpan")
            }
        }
    }

    if (showDatePicker) {
        LocalDatePickerDialog(
            initialMillis = state.dateMillis,
            onConfirm = viewModel::onDateChange,
            onDismiss = { showDatePicker = false }
        )
    }
}
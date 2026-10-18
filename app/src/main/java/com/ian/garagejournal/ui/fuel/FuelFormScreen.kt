package com.ian.garagejournal.ui.fuel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ian.garagejournal.ui.components.LocalDatePickerDialog
import com.ian.garagejournal.util.formatDate
import com.ian.garagejournal.util.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelFormScreen(viewModel: FuelFormViewModel, onDone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit BBM" else "Tambah BBM") },
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
                value = state.liters, onValueChange = viewModel::onLitersChange,
                label = { Text("Jumlah (liter)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.pricePerLiter, onValueChange = viewModel::onPriceChange,
                label = { Text("Harga per liter (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            state.totalCost?.let {
                Text("Total: ${formatRupiah(it)}", style = MaterialTheme.typography.titleMedium)
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Isi sampai penuh", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Matikan kalau cuma isi sebagian. KM/L dihitung antara dua pengisian penuh.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = state.isFullTank, onCheckedChange = viewModel::onFullTankChange)
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
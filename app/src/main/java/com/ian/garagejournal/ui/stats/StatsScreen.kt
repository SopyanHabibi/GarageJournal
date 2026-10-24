package com.ian.garagejournal.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ian.garagejournal.util.formatEfficiency
import com.ian.garagejournal.util.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val months by viewModel.months.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistik") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Rata-rata", formatEfficiency(state.overallEfficiency), Modifier.weight(1f))
                StatCard("Terakhir", formatEfficiency(state.latestEfficiency), Modifier.weight(1f))
                StatCard("Terbaik", formatEfficiency(state.bestEfficiency), Modifier.weight(1f))
            }

            Text("Tren KM/L", style = MaterialTheme.typography.titleMedium)
            if (state.efficiencyPoints.size < 2) {
                Text("Butuh minimal 3 kali isi penuh supaya tren KM/L bisa ditampilkan.")
            } else {
                EfficiencyLineChart(
                    points = state.efficiencyPoints,
                    average = state.overallEfficiency,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text("Biaya per bulan", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = months == 6,
                    onClick = { viewModel.selectMonths(6) },
                    label = { Text("6 bulan") }
                )
                FilterChip(
                    selected = months == 12,
                    onClick = { viewModel.selectMonths(12) },
                    label = { Text("12 bulan") }
                )
            }
            MonthlyCostBarChart(state.monthly, Modifier.fillMaxWidth())

            Text(
                "12 bulan terakhir: ${formatRupiah(state.yearTotal)} • " +
                        "rata-rata ${formatRupiah(state.averagePerMonth)} per bulan",
                style = MaterialTheme.typography.bodyMedium
            )

            val breakdown = state.monthly.filter { it.total > 0 }.asReversed()
            if (breakdown.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Rincian", style = MaterialTheme.typography.titleSmall)
                        breakdown.forEach { m ->
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(monthTitle(m.month), style = MaterialTheme.typography.bodyMedium)
                                    Text(formatRupiah(m.total), style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    "Servis ${formatRupiah(m.serviceCost)} • BBM ${formatRupiah(m.fuelCost)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
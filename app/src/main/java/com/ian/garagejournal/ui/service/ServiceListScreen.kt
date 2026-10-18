package com.ian.garagejournal.ui.service

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ian.garagejournal.data.local.ServiceLog
import com.ian.garagejournal.util.formatDate
import com.ian.garagejournal.util.formatRupiah
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceListScreen(
    viewModel: ServiceListViewModel,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val total by viewModel.totalCost.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<ServiceLog?>(null) }
    var viewingReceipt by remember { mutableStateOf<File?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Servis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Tambah servis")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Total biaya servis", style = MaterialTheme.typography.labelMedium)
                        Text(formatRupiah(total), style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
            if (logs.isEmpty()) {
                item { Text("Belum ada riwayat servis. Tekan + untuk menambah.") }
            } else {
                items(logs, key = { it.id }) { log ->
                    val file = log.receiptPath?.let { viewModel.receiptFile(it) }
                    ServiceLogCard(
                        log = log,
                        receiptFile = file,
                        onReceiptClick = { viewingReceipt = file },
                        onEdit = { onEdit(log.id) },
                        onDelete = { toDelete = log }
                    )
                }
            }
        }
    }

    toDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Hapus \"${log.title}\"?") },
            text = { Text("Foto nota (kalau ada) ikut terhapus.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(log); toDelete = null }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Batal") } }
        )
    }

    viewingReceipt?.let { file ->
        Dialog(
            onDismissRequest = { viewingReceipt = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().clickable { viewingReceipt = null },
                color = Color.Black
            ) {
                AsyncImage(
                    model = file,
                    contentDescription = "Nota",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ServiceLogCard(
    log: ServiceLog,
    receiptFile: File?,
    onReceiptClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(log.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${formatDate(log.date)} • ${"%,d".format(log.odometer)} km",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(formatRupiah(log.cost), style = MaterialTheme.typography.labelLarge)
                if (log.description.isNotBlank()) {
                    Text(
                        log.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (receiptFile != null) {
                AsyncImage(
                    model = receiptFile,
                    contentDescription = "Nota",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onReceiptClick)
                )
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus")
            }
        }
    }
}
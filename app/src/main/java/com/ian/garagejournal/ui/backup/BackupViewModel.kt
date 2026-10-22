package com.ian.garagejournal.ui.backup

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ian.garagejournal.GarageApp
import com.ian.garagejournal.data.backup.BackupExporter
import com.ian.garagejournal.data.backup.ExportSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

sealed interface BackupState {
    data object Idle : BackupState
    data object Running : BackupState
    data class Done(val summary: ExportSummary) : BackupState
    data class Failed(val message: String) : BackupState
}

class BackupViewModel(
    private val exporter: BackupExporter,
    private val resolver: ContentResolver
) : ViewModel() {

    private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
    val state: StateFlow<BackupState> = _state.asStateFlow()

    fun export(uri: Uri) {
        if (_state.value is BackupState.Running) return // cegah ekspor ganda
        _state.value = BackupState.Running

        viewModelScope.launch {
            _state.value = try {
                val summary = withContext(Dispatchers.IO) {
                    val output = resolver.openOutputStream(uri)
                        ?: throw IllegalStateException("Tidak bisa menulis ke lokasi yang dipilih")
                    output.use { exporter.export(it) }
                }
                BackupState.Done(summary)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                runCatching { DocumentsContract.deleteDocument(resolver, uri) } // buang berkas setengah jadi
                BackupState.Failed(e.message ?: "kesalahan tidak diketahui")
            }
        }
    }

    fun dismiss() {
        _state.value = BackupState.Idle
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GarageApp
                BackupViewModel(app.backupExporter, app.contentResolver)
            }
        }
    }
}
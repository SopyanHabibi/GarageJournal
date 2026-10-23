package com.ian.garagejournal.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID

class ReceiptStorage(private val context: Context) {

    private val dir: File
        get() = File(context.filesDir, "receipts").also { it.mkdirs() }

    private fun newFileName(): String = "receipt_${UUID.randomUUID()}.jpg"

    fun resolve(fileName: String): File = File(dir, fileName)

    fun uriFor(fileName: String): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", resolve(fileName))

    /** Siapkan file kosong untuk tujuan hasil kamera */
    fun prepareCameraFile(): String {
        val name = newFileName()
        resolve(name).createNewFile()
        return name
    }

    /** Salin gambar dari galeri ke penyimpanan app */
    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        val name = newFileName()
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Gambar tidak bisa dibaca")
        input.use { src -> resolve(name).outputStream().use { dst -> src.copyTo(dst) } }
        name
    }

    fun delete(fileName: String?) {
        if (fileName == null) return
        runCatching { resolve(fileName).delete() }
    }

    /** Salin gambar dari stream (mis. isi ZIP backup) ke penyimpanan app; kembalikan nama file baru. */
    fun importStream(input: InputStream): String {
        val name = newFileName()
        val target = resolve(name)
        try {
            target.outputStream().use { input.copyTo(it) }
        } catch (e: Exception) {
            target.delete() // jangan tinggalkan berkas setengah jadi
            throw e
        }
        return name
    }
}
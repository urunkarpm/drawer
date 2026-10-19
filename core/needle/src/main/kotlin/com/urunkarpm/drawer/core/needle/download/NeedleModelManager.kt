package com.urunkarpm.drawer.core.needle.download

import android.content.Context
import com.urunkarpm.drawer.core.needle.model.ModelStatus
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NeedleModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _status = MutableStateFlow<ModelStatus>(ModelStatus.NotDownloaded)
    val status: StateFlow<ModelStatus> = _status.asStateFlow()

    private val modelDirectory: File
        get() = File(context.filesDir, "models").apply { if (!exists()) mkdirs() }

    val modelFile: File
        get() = File(modelDirectory, MODEL_FILENAME)

    init {
        checkCurrentStatus()
    }

    fun checkCurrentStatus() {
        val file = modelFile
        if (file.exists() && file.length() > 0) {
            val sizeMb = file.length() / (1024f * 1024f)
            _status.value = ModelStatus.Ready(file.absolutePath, sizeMb)
        } else {
            _status.value = ModelStatus.NotDownloaded
        }
    }

    suspend fun downloadModel(client: HttpClient = HttpClient()): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _status.value = ModelStatus.Downloading(0.0f)
            val tempFile = File(modelDirectory, "$MODEL_FILENAME.tmp")
            if (tempFile.exists()) tempFile.delete()

            client.prepareGet(MODEL_URL).execute { response ->
                if (response.status.value !in 200..299) {
                    throw IOException("HTTP error: ${response.status}")
                }
                val channel: ByteReadChannel = response.bodyAsChannel()
                val totalBytes = response.headers["Content-Length"]?.toLongOrNull() ?: EXPECTED_BYTE_SIZE
                var bytesRead = 0L

                FileOutputStream(tempFile).use { output ->
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                        while (!packet.exhausted()) {
                            val bytes = packet.readByteArray()
                            output.write(bytes)
                            bytesRead += bytes.size
                            if (totalBytes > 0) {
                                val progress = (bytesRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                _status.value = ModelStatus.Downloading(progress)
                            }
                        }
                    }
                }
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                if (modelFile.exists()) modelFile.delete()
                tempFile.renameTo(modelFile)
                val sizeMb = modelFile.length() / (1024f * 1024f)
                _status.value = ModelStatus.Ready(modelFile.absolutePath, sizeMb)
                Result.success(Unit)
            } else {
                _status.value = ModelStatus.Error("Empty download output")
                Result.failure(IllegalStateException("Downloaded model file is empty"))
            }
        } catch (e: Exception) {
            _status.value = ModelStatus.Error(e.message ?: "Download failed")
            Result.failure(e)
        }
    }

    fun deleteModel(): Boolean {
        val deleted = modelFile.delete()
        _status.value = ModelStatus.NotDownloaded
        return deleted
    }

    companion object {
        const val MODEL_FILENAME = "needle_3_cq2.bin"
        const val MODEL_URL = "https://huggingface.co/Cactus-Compute/needle-3-quant/resolve/main/needle-3-cq2.bin"
        const val EXPECTED_BYTE_SIZE = 10_800_000L // ~10.3 MB
    }
}

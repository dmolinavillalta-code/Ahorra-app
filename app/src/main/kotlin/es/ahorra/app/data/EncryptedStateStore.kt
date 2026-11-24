package es.ahorra.app.data

import es.ahorra.core.AppState
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

sealed interface LoadResult {
    data object Empty : LoadResult
    data class Data(val state: AppState) : LoadResult

    /** Hay un archivo pero no se puede leer (clave perdida, contenido alterado o corrupto). Nunca se sobrescribe solo. */
    data object Unreadable : LoadResult
}

/** Un único archivo cifrado. Escritura atómica (archivo temporal + sincronización + renombrado). */
class EncryptedStateStore(private val file: File, private val cipher: PayloadCipher) {

    private val tmp = File(file.parentFile, file.name + ".tmp")

    fun load(): LoadResult {
        if (!file.exists()) return LoadResult.Empty
        return try {
            val json = String(cipher.decrypt(file.readBytes()), Charsets.UTF_8)
            StorageFormat.decode(json)?.let { LoadResult.Data(it) } ?: LoadResult.Unreadable
        } catch (e: Exception) {
            LoadResult.Unreadable
        }
    }

    fun save(state: AppState) {
        val bytes = cipher.encrypt(StorageFormat.encode(state).toByteArray(Charsets.UTF_8))
        FileOutputStream(tmp).use { out ->
            out.write(bytes)
            out.fd.sync()
        }
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    fun delete() {
        file.delete()
        tmp.delete()
    }
}

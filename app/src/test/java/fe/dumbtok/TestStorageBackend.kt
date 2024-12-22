package fe.dumbtok

import fe.dumbtok.module.storage.StorageBackend
import java.io.File
import java.io.OutputStream

object TestStorageBackend : StorageBackend {
    override fun storeFile(name: String): OutputStream {
        return File(name).outputStream()
    }
}

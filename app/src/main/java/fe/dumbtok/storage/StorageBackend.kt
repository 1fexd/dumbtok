package fe.dumbtok.storage

import android.content.Context
import java.io.OutputStream

interface StorageBackend {
    fun storeFile(name: String): OutputStream
}

class AndroidPrivateStorageBackend(private val context: Context) : StorageBackend {
    override fun storeFile(name: String): OutputStream {
        return context.openFileOutput(name, Context.MODE_PRIVATE)
    }
}

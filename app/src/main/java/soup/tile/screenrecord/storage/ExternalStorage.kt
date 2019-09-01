package soup.tile.screenrecord.storage

import android.content.Context
import android.os.Environment
import java.io.File

class ExternalStorage(private val context: Context) {

    fun getDir(type: String): File {
        return Environment.getExternalStoragePublicDirectory(type)
    }
}

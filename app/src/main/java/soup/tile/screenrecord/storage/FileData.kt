package soup.tile.screenrecord.storage

import android.util.Size
import java.io.File

data class FileData(
    val file: File,
    val mimeType: String,
    val size: Size,
    val timestamp: Long
)

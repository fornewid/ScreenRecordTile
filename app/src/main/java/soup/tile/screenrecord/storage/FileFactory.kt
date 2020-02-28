package soup.tile.screenrecord.storage

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileFactory {

    private const val NAME_DIR = "ScreenRecords"
    private const val NAME_FILE_TEMPLATE = "ScreenRecord_%s.mp4"
    private const val FILE_DATE_FORMAT = "yyyyMMdd-HHmmss"

    fun createNewFile(context: Context, timestamp: Long): File {
        return context
            .getExternalDir(Environment.DIRECTORY_MOVIES)
            .childDir(fileName = NAME_DIR)
            .child(fileName = fileName(timestamp))
    }

    private fun Context.getExternalDir(type: String): File {
        return Environment.getExternalStoragePublicDirectory(type)
    }

    private fun fileName(timestamp: Long): String {
        return String.format(
            NAME_FILE_TEMPLATE,
            SimpleDateFormat(FILE_DATE_FORMAT, Locale.US).format(Date(timestamp))
        )
    }

    private fun File.childDir(fileName: String): File {
        return File(this, fileName).apply { mkdirs() }
    }

    private fun File.child(fileName: String): File {
        return File(this, fileName)
    }
}

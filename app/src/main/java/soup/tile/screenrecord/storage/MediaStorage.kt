package soup.tile.screenrecord.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore.Video
import android.provider.MediaStore.Video.VideoColumns
import androidx.core.content.contentValuesOf

class MediaStorage(private val context: Context) {

    fun insertVideo(data: FileData): Uri? {
        return context.contentResolver.insert(
            Video.Media.EXTERNAL_CONTENT_URI,
            data.toContentValues()
        )
    }

    private fun FileData.toContentValues(): ContentValues {
        val fileName = file.name
        val dateSeconds = timestamp / 1000
        return contentValuesOf(
            VideoColumns.DATA to file.absolutePath,
            VideoColumns.TITLE to fileName,
            VideoColumns.DISPLAY_NAME to fileName,
            VideoColumns.DATE_TAKEN to timestamp,
            VideoColumns.DATE_ADDED to dateSeconds,
            VideoColumns.DATE_MODIFIED to dateSeconds,
            VideoColumns.MIME_TYPE to mimeType,
            VideoColumns.WIDTH to size.width,
            VideoColumns.HEIGHT to size.height,
            VideoColumns.SIZE to file.length()
        )
    }
}

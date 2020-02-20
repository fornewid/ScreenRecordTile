package soup.tile.screenrecord.storage

import android.content.Context
import android.net.Uri
import android.provider.MediaStore.Video
import android.provider.MediaStore.Video.VideoColumns
import androidx.core.content.contentValuesOf

class MediaStorage(private val context: Context) {

    fun insertVideo(): Uri? {
        val timeMillis = System.currentTimeMillis()
        return context.contentResolver.insert(
            Video.Media.EXTERNAL_CONTENT_URI,
            contentValuesOf(
                VideoColumns.DISPLAY_NAME to FileFactory.createNewFile(context, timeMillis).name,
                VideoColumns.DATE_TAKEN to timeMillis,
                VideoColumns.DATE_ADDED to timeMillis,
                VideoColumns.MIME_TYPE to "video/mp4"
            )
        )
    }
}

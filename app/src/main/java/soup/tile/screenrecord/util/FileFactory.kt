package soup.tile.screenrecord.util

import java.text.SimpleDateFormat
import java.util.*

object FileFactory {
    private const val NAME_FILE_TEMPLATE = "ScreenRecord_%s.mp4"
    private const val FILE_DATE_FORMAT = "yyyyMMdd-HHmmss"

    fun fileName(timestamp: Long): String {
        return String.format(
            NAME_FILE_TEMPLATE,
            SimpleDateFormat(FILE_DATE_FORMAT, Locale.US).format(Date(timestamp))
        )
    }
}

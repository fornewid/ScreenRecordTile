package soup.tile.screenrecord.util

import android.os.Build
import android.provider.MediaStore

object MediaStoreCompat {

    object MediaColumns {
        val DATE_TAKEN = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.MediaColumns.DATE_TAKEN
        } else {
            "datetaken"
        }
    }
}

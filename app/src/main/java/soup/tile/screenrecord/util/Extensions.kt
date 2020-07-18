@file:Suppress("NOTHING_TO_INLINE")

package soup.tile.screenrecord.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast

inline fun Context.toast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    return Toast.makeText(this, text, duration).show()
}

inline fun Context.toast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    return Toast.makeText(this, resId, duration).show()
}

fun Context?.hasMicrophoneFeature(): Boolean {
    return if (this == null) false
    else packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
}

fun Context.startForegroundServiceCompat(intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        // Pre-O behavior.
        startService(intent)
    }
}

object MediaStore_MediaColumns {
    val DATE_TAKEN = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.MediaColumns.DATE_TAKEN
    } else {
        "datetaken"
    }
}

package soup.tile.screenrecord.util

import android.content.Context
import android.content.pm.PackageManager

fun Context?.hasMicrophoneFeature(): Boolean {
    return if (this == null) false
    else packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
}

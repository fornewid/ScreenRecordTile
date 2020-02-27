@file:Suppress("NOTHING_TO_INLINE")

package soup.tile.screenrecord.util

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

inline fun Context.toast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    return Toast.makeText(this, text, duration).show()
}

inline fun Context.toast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    return Toast.makeText(this, resId, duration).show()
}

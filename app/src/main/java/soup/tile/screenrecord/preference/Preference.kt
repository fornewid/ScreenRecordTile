package soup.tile.screenrecord.preference

import android.content.Context
import android.content.SharedPreferences

val Context.defaultSharedPreference: SharedPreferences
    get() = getSharedPreferences(packageName, Context.MODE_PRIVATE)

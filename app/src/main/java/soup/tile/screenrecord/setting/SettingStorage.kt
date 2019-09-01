package soup.tile.screenrecord.setting

import android.content.Context
import androidx.preference.PreferenceManager
import soup.tile.screenrecord.BuildConfig

class SettingStorage(context: Context) {

    private val prefs = lazy {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    }

    val isFakeStatusBarEnabled: Boolean
            by BooleanPreference(prefs, BuildConfig.PREF_FAKE_STATUS_BAR, false)

    val isFakeStatusBarTimerEnabled: Boolean
            by BooleanPreference(prefs, BuildConfig.PREF_FAKE_STATUS_BAR_TIMER, false)
}

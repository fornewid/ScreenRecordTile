package soup.tile.screenrecord.setting

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.preference.PreferenceFragmentCompat
import soup.tile.screenrecord.R

class SettingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.setting_activity)
        supportFragmentManager.commit {
            replace(R.id.setting_container, SettingFragment())
        }
    }

    class SettingFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }
    }
}

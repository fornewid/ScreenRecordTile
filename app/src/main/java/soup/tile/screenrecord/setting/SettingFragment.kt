package soup.tile.screenrecord.setting

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import soup.tile.screenrecord.*

class SettingFragment : PreferenceFragmentCompat() {

    private val listener = object : OnRecordStateListener {

        override fun onRecordStateChanged(isRecording: Boolean) {
            updateStartRecordButton(isRecording)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference?.key == BuildConfig.PREF_START_RECORD) {
            val useAudioPref = findPreference<SwitchPreference>(BuildConfig.PREF_USE_AUDIO)
            val useAudio = useAudioPref?.isChecked == true
            startActivity(ScreenRecordActivity.getStartIntent(requireContext(), useAudio))
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onResume() {
        super.onResume()
        updateStartRecordButton(RecordingStateManager.isRecording())
        RecordingStateManager.addListener(listener)
    }

    override fun onPause() {
        super.onPause()
        RecordingStateManager.removeListener(listener)
    }

    private fun updateStartRecordButton(isRecording: Boolean) {
        val startRecordPref = findPreference<Preference>(BuildConfig.PREF_START_RECORD)
        if (startRecordPref != null) {
            if (isRecording) {
                startRecordPref.setIcon(R.drawable.ic_setting_stop)
                startRecordPref.setTitle(R.string.screenrecord_stop_label)
            } else {
                startRecordPref.setIcon(R.drawable.ic_setting_record)
                startRecordPref.setTitle(R.string.screenrecord_start_label)
            }
            val useAudioPref = findPreference<SwitchPreference>(BuildConfig.PREF_USE_AUDIO)
            useAudioPref?.isEnabled = isRecording.not()
        }
    }
}

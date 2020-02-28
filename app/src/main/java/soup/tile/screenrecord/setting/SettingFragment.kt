package soup.tile.screenrecord.setting

import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import soup.tile.screenrecord.*
import soup.tile.screenrecord.BuildConfig.*
import soup.tile.screenrecord.util.hasMicrophoneFeature

class SettingFragment : PreferenceFragmentCompat() {

    private val listener = object : OnRecordStateListener {

        override fun onRecordStateChanged(isRecording: Boolean) {
            updateStartRecordButton(isRecording)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        findPreference<Preference>(PREF_USE_AUDIO)?.isVisible = context.hasMicrophoneFeature()
        findPreference<Preference>(PREF_CURRENT_VERSION)?.summary = VERSION_NAME
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference?.key == PREF_START_RECORD) {
            if (RecordingStateManager.isRecording()) {
                context?.run {
                    ActivityCompat.startForegroundService(this, RecordingService.getStopIntent(this))
                }
            } else {
                val useAudioPref = findPreference<SwitchPreference>(PREF_USE_AUDIO)
                val useAudio = useAudioPref?.isChecked == true
                startActivity(ScreenRecordActivity.getStartIntent(requireContext(), useAudio))
            }
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
        val startRecordPref = findPreference<Preference>(PREF_START_RECORD)
        if (startRecordPref != null) {
            if (isRecording) {
                startRecordPref.setIcon(R.drawable.ic_setting_stop)
                startRecordPref.setTitle(R.string.screenrecord_stop_label)
            } else {
                startRecordPref.setIcon(R.drawable.ic_setting_record)
                startRecordPref.setTitle(R.string.screenrecord_start_label)
            }
            val useAudioPref = findPreference<SwitchPreference>(PREF_USE_AUDIO)
            useAudioPref?.isEnabled = isRecording.not()
        }
    }
}

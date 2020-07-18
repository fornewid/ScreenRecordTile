package soup.tile.screenrecord.setting

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import soup.tile.screenrecord.*
import soup.tile.screenrecord.BuildConfig.VERSION_NAME
import soup.tile.screenrecord.preference.PreferenceView
import soup.tile.screenrecord.preference.SwitchPreferenceView
import soup.tile.screenrecord.util.hasMicrophoneFeature
import soup.tile.screenrecord.util.setOnDebounceClickListener
import soup.tile.screenrecord.util.startForegroundServiceCompat

class SettingActivity : Activity() {

    private val listener = object : OnRecordStateListener {

        override fun onRecordStateChanged(isRecording: Boolean) {
            updateStartRecordButton(isRecording)
        }
    }

    private var startRecordPref: PreferenceView? = null
    private var useAudioPref: SwitchPreferenceView? = null
    private var returnToSettingsPref: SwitchPreferenceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.setting_activity)

        startRecordPref = findViewById(R.id.start_record)
        startRecordPref?.setOnDebounceClickListener {
            if (RecordingStateManager.isRecording()) {
                startForegroundServiceCompat(RecordingService.getStopIntent(this))
            } else {
                startActivity(
                    ScreenRecordActivity.getStartIntent(
                        context = this,
                        useAudio = useAudioPref?.isChecked() == true,
                        fromSettings = true,
                        returnToSettings = returnToSettingsPref?.isChecked() == true
                    )
                )
                finish()
            }
        }

        useAudioPref = findViewById(R.id.use_audio)
        useAudioPref?.apply {
            visibility = if (hasMicrophoneFeature()) View.VISIBLE else View.GONE
            setOnClickListener {
                toggle()
            }
        }

        returnToSettingsPref = findViewById(R.id.return_to_settings)
        returnToSettingsPref?.apply {
            setOnClickListener {
                toggle()
            }
        }

        val demoMode: PreferenceView = findViewById(R.id.demo_mode)
        demoMode.setOnDebounceClickListener {
            val intent = Intent("com.android.settings.action.DEMO_MODE").apply {
                setPackage("com.android.systemui")
            }
            startActivity(intent)
        }

        val currentVersion: PreferenceView = findViewById(R.id.current_version)
        currentVersion.setSummary(VERSION_NAME)
        currentVersion.setOnDebounceClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=soup.tile.screenrecord")
            )
            startActivity(intent)
        }

        val github: PreferenceView = findViewById(R.id.github)
        github.setOnDebounceClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/fornewid/ScreenRecordTile")
            )
            startActivity(intent)
        }

        val author: PreferenceView = findViewById(R.id.author)
        author.setOnDebounceClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/fornewid")
            )
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        RecordingStateManager.addListener(listener)
        updateStartRecordButton(RecordingStateManager.isRecording())
    }

    override fun onPause() {
        super.onPause()
        RecordingStateManager.removeListener(listener)
    }

    private fun updateStartRecordButton(isRecording: Boolean) {
        startRecordPref?.run {
            if (isRecording) {
                setIcon(R.drawable.ic_setting_stop)
                setTitle(getString(R.string.screenrecord_stop_label))
            } else {
                setIcon(R.drawable.ic_setting_record)
                setTitle(getString(R.string.screenrecord_start_label))
            }
            useAudioPref?.isEnabled = isRecording.not()
        }
    }
}

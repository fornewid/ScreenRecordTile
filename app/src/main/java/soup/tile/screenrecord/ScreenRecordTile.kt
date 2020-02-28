package soup.tile.screenrecord

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import soup.tile.screenrecord.BuildConfig.PREF_USE_AUDIO
import soup.tile.screenrecord.util.hasMicrophoneFeature

class ScreenRecordTile : TileService() {

    private val listener = object : OnRecordStateListener {

        override fun onRecordStateChanged(isRecording: Boolean) {
            updateTileUi(isRecording)
        }
    }

    private val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTileUi()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        if (RecordingStateManager.isRecording()) {
            ActivityCompat.startForegroundService(this, RecordingService.getStopIntent(this))
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileUi()
        RecordingStateManager.addListener(listener)
    }

    override fun onStopListening() {
        super.onStopListening()
        updateTileUi()
        RecordingStateManager.removeListener(listener)
    }

    private fun updateTileUi(isRecording: Boolean = RecordingStateManager.isRecording()) {
        val oldState = qsTile.state
        val newState = if (isRecording) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        if (oldState != newState) {
            qsTile.state = newState
            qsTile.label = if (isRecording) {
                getString(R.string.tile_label_recording)
            } else {
                getString(R.string.tile_label_default)
            }
            qsTile.updateTile()
        }
    }

    override fun onClick() {
        if (RecordingStateManager.isRecording()) {
            ActivityCompat.startForegroundService(this, RecordingService.getStopIntent(this))
            return
        }

        val executeAction = {
            val useAudio = hasMicrophoneFeature() && prefs.getBoolean(PREF_USE_AUDIO, false)
            startActivityAndCollapse(ScreenRecordActivity.getStartIntent(this, useAudio = useAudio))
        }
        if (isLocked || isSecure) {
            unlockAndRun(executeAction)
        } else {
            executeAction()
        }
    }

    // 00:00
    // val time = DateUtils.formatElapsedTime(timeMs / 1000)
}

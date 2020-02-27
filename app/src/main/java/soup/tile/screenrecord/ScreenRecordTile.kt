package soup.tile.screenrecord

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.app.ActivityCompat
import dagger.android.AndroidInjection

class ScreenRecordTile : TileService() {

    private val listener = object : OnRecordStateListener {

        override fun onRecordStateChanged(isRecording: Boolean) {
            updateTileUi(isRecording)
        }
    }

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
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
            startActivityAndCollapse(ScreenRecordActivity.getStartIntent(this, useAudio = true))
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

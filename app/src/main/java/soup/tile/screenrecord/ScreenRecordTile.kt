package soup.tile.screenrecord

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.android.AndroidInjection
import soup.tile.screenrecord.record.ScreenRecordActivity
import soup.tile.screenrecord.record.ScreenRecordManager
import soup.tile.screenrecord.setting.SettingStorage
import soup.tile.screenrecord.storage.MediaStorage
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

class ScreenRecordTile : TileService() {

    @Inject
    lateinit var notifications: Notifications

    @Inject
    lateinit var settingStorage: SettingStorage

    @Inject
    lateinit var mediaStorage: MediaStorage

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
        ScreenRecordManager.setListener(object : ScreenRecordManager.Listener {

            override fun onScreenRecordStateChanged(isRecording: Boolean) {
                updateTileUi(isRecording)
            }

            override fun onScreenRecordFileSaved(output: File) {
                val uri = mediaStorage.insertVideo()
                if (uri != null) {
                    try { // Add to the mediastore
                        contentResolver.openOutputStream(uri, "w")?.use { os ->
                            output.inputStream().copyTo(os)
                        }
                        output.delete()

                        notifications.showSaveState(uri)

                        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                            .addCategory(Intent.CATEGORY_DEFAULT)
                            .setData(uri)
                        sendBroadcast(intent)
                    } catch (e: IOException) {
                        Timber.e("Error saving screen recording: " + e.message)
                    }
                }
            }
        })
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTileUi()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        if (ScreenRecordManager.isRecording()) {
            ScreenRecordManager.stop()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileUi()
    }

    override fun onStopListening() {
        super.onStopListening()
        updateTileUi()
    }

    private fun updateTileUi(isRecording: Boolean = ScreenRecordManager.isRecording()) {
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
        if (ScreenRecordManager.isRecording()) {
            ScreenRecordManager.stop()
            return
        }

        val executeAction = {
            val intent = Intent(this, ScreenRecordActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivityAndCollapse(intent)
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

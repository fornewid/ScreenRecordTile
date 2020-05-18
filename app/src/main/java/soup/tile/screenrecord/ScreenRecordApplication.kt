package soup.tile.screenrecord

import android.app.Application
import soup.tile.screenrecord.notification.NotificationInfo
import soup.tile.screenrecord.util.ReleaseTree
import timber.log.Timber

class ScreenRecordApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
        NotificationInfo.createChannels(this)
    }
}

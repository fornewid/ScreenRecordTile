package soup.tile.screenrecord

import android.app.Application
import soup.tile.screenrecord.notification.NotificationInfo

class ScreenRecordApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        BuildType.init(this)
        NotificationInfo.createChannels(this)
    }
}

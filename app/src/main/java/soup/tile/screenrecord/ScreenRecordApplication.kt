package soup.tile.screenrecord

import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import soup.tile.screenrecord.di.DaggerApplicationComponent

class ScreenRecordApplication : DaggerApplication() {

    override fun onCreate() {
        super.onCreate()
        BuildType.init(this)
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerApplicationComponent.factory().create(this)
    }
}

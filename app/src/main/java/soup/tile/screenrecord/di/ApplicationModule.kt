package soup.tile.screenrecord.di

import android.content.Context
import dagger.Module
import dagger.Provides
import soup.tile.screenrecord.Notifications
import soup.tile.screenrecord.ScreenRecordApplication
import soup.tile.screenrecord.storage.MediaStorage
import javax.inject.Singleton

@Module
class ApplicationModule {

    @Provides
    fun provideContext(
        application: ScreenRecordApplication
    ): Context = application.applicationContext

    @Singleton
    @Provides
    fun provideNotifications(
        context: Context
    ): Notifications = Notifications(context)

    @Singleton
    @Provides
    fun provideMediaStorage(
        context: Context
    ): MediaStorage = MediaStorage(context)
}

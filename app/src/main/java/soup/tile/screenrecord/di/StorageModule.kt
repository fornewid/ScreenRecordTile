package soup.tile.screenrecord.di

import android.content.Context
import dagger.Module
import dagger.Provides
import soup.tile.screenrecord.setting.SettingStorage
import soup.tile.screenrecord.storage.MediaStorage
import javax.inject.Singleton

@Module
class StorageModule {

    @Singleton
    @Provides
    fun provideSettingStorage(
        context: Context
    ): SettingStorage = SettingStorage(context)

    @Singleton
    @Provides
    fun provideMediaStorage(
        context: Context
    ): MediaStorage = MediaStorage(context)
}

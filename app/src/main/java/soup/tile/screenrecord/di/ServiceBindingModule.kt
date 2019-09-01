package soup.tile.screenrecord.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import soup.tile.screenrecord.Notifications
import soup.tile.screenrecord.ScreenRecordTile
import soup.tile.screenrecord.di.scope.ServiceScope

@Module
abstract class ServiceBindingModule {

    @ServiceScope
    @ContributesAndroidInjector
    abstract fun bindService(): ScreenRecordTile

}

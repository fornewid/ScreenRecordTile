package soup.tile.screenrecord.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import soup.tile.screenrecord.RecordingService
import soup.tile.screenrecord.di.scope.ServiceScope

@Module
interface ServiceBindingModule {

    @ServiceScope
    @ContributesAndroidInjector
    fun bindRecordingService(): RecordingService
}

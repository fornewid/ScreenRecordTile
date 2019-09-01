package soup.tile.screenrecord.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import soup.tile.screenrecord.record.ScreenRecordActivity
import soup.tile.screenrecord.di.scope.ActivityScope

@Module
abstract class ActivityBindingModule {

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindScreenRecordActivity(): ScreenRecordActivity
}

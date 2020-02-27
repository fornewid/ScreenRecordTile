package soup.tile.screenrecord.di

import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import soup.tile.screenrecord.ScreenRecordApplication
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AndroidSupportInjectionModule::class,
    ApplicationModule::class,
    ActivityBindingModule::class,
    ServiceBindingModule::class
])
interface ApplicationComponent : AndroidInjector<ScreenRecordApplication> {

    @Component.Factory
    interface Factory : AndroidInjector.Factory<ScreenRecordApplication>
}

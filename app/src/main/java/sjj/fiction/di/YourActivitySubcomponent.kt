package sjj.fiction.di

import android.app.Activity
import android.app.Application
import dagger.*
import dagger.android.ActivityKey
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import sjj.fiction.main.MainActivity
import dagger.android.ContributesAndroidInjector
import sjj.fiction.App


@Subcomponent()
interface MainActivitySubComponent : AndroidInjector<MainActivity> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<MainActivity>()
}


@Module(subcomponents = [MainActivitySubComponent::class])
abstract class YourActivityModule {
    @Binds
    @IntoMap
    @ActivityKey(MainActivity::class)
    abstract fun bindYourActivityInjectorFactory(builder: MainActivitySubComponent.Builder): AndroidInjector.Factory<out Activity>
}

@Component(modules = [YourActivityModule::class])
interface YourApplicationComponent {
    //        @ActivityScope
//    @ContributesAndroidInjector(modules = arrayOf())
//    fun contributeYourActivityInjector(): MainActivity
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: App): Builder

        fun build(): YourApplicationComponent
    }
    fun inject(application: App)
}
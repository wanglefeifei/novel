package sjj.fiction

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.TrafficStats
import android.os.StrictMode
import com.facebook.drawee.backends.pipeline.Fresco
import com.raizlabs.android.dbflow.config.DatabaseConfig
import com.raizlabs.android.dbflow.config.FlowConfig
import com.raizlabs.android.dbflow.config.FlowManager
import com.raizlabs.android.dbflow.kotlinextensions.insert
import dagger.android.AndroidInjector
import dagger.android.HasActivityInjector
import io.reactivex.plugins.RxJavaPlugins
import org.jetbrains.anko.newTask
import sjj.alog.Config
import sjj.alog.Log
import java.util.*
import dagger.android.DispatchingAndroidInjector
import sjj.fiction.di.DaggerYourApplicationComponent
import sjj.fiction.di.YourApplicationComponent
import javax.inject.Inject



/**
 * Created by SJJ on 2017/9/3.
 */
class App : Application(), HasActivityInjector {
    companion object {
        lateinit var app: App
    }

    @Inject
    lateinit var dispatchingActivityInjector: DispatchingAndroidInjector<Activity>

    val activitys = LinkedList<BaseActivity>()
    override fun onCreate() {
        super.onCreate()
        DaggerYourApplicationComponent.builder().application(this).build().inject(this)

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Log.e("UncaughtException",e)
            val intent = Intent(this, CrashActivity::class.java)
            intent.putExtra(CrashActivity.THREAD_INFO, "线程：${t.name} ID：${t.id}")
            intent.putExtra(CrashActivity.CRASH_DATA, e)
            intent.newTask()
            startActivity(intent)
            finishAll()
        }
        app = this
        val logConfig = Config()
        logConfig.hold = true
        logConfig.holdMultiple = false
        logConfig.holdLev = Config.ERROR
        Config.init(logConfig)
        FlowManager.init(FlowConfig.builder(this)
                .addDatabaseConfig(DatabaseConfig.builder(BookDataBase::class.java).build())
                .build())
        Fresco.initialize(this);

        // 分别为MainThread和VM设置Strict Mode
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()

                    //                    .detectResourceMismatches()
                    .detectCustomSlowCalls().detectAll()
                    .penaltyLog()
//                    .penaltyDeath()
                    .build())

            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .detectActivityLeaks().detectAll()
                    .penaltyLog()
//                    .penaltyDeath()
                    .build())
        }

        RxJavaPlugins.setErrorHandler { Log.e("error ",it) }
    }

    fun exit() {
        System.exit(0)
    }

    fun finishAll() {
        activitys.forEach { it.finish() }
        activitys.clear()
    }

    override fun activityInjector(): AndroidInjector<Activity> {
        return dispatchingActivityInjector
    }

}
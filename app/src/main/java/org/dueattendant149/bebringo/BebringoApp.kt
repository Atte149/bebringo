package org.dueattendant149.bebringo

import android.app.Activity
import android.app.Application
import android.os.Bundle
import org.dueattendant149.bebringo.di.ServiceLocator
import org.dueattendant149.bebringo.domain.entry.EventTypes

/** Точка инициализации зависимостей. */
class BebringoApp : Application() {

    /** Счётчик запущенных активностей: 0 → 1 = вход, 1 → 0 = выход. */
    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        registerActivityLifecycleCallbacks(EntryLifecycleTracker())
    }

    /** Фиксирует входы и выходы из приложения — ядро модели привычки. */
    private inner class EntryLifecycleTracker : ActivityLifecycleCallbacks {

        override fun onActivityStarted(activity: Activity) {
            if (startedActivities == 0) {
                ServiceLocator.logEvent(EventTypes.APP_OPEN)
            }
            startedActivities++
        }

        override fun onActivityStopped(activity: Activity) {
            startedActivities--
            if (startedActivities == 0) {
                ServiceLocator.logEvent(EventTypes.APP_CLOSE)
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}

package fr.driffaud.band2fit

import android.app.Application
import timber.log.Timber

class Application : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
        Timber.plant(FileLoggingTree(this))
    }
}
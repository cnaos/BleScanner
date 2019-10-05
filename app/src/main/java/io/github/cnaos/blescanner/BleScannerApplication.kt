package io.github.cnaos.blescanner

import android.app.Application
import timber.log.Timber

class BleScannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // TODO リリース用のログ出力を作る
    }
}
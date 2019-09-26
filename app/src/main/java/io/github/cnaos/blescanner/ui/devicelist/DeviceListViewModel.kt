package io.github.cnaos.blescanner.ui.devicelist

import android.os.Handler
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.verbose
import java.util.concurrent.TimeUnit

class DeviceListViewModel : ViewModel(),AnkoLogger {
    // 定数
    companion object {
        private val SCAN_PERIOD: Long =
            TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS)// スキャン時間
    }

    val scanning = MutableLiveData<Boolean>(false)

    private val handler: Handler = Handler()

    fun startDeviceScan() {
        verbose("startDeviceScan")


        handler.postDelayed({
            scanning.value = false
            // TODO BLEデバイスのスキャン停止
        }, SCAN_PERIOD)

        scanning.value = true
        // TODO BLEデバイスのスキャン開始
    }

    fun stopDeviceScan() {
        // 一定期間後にスキャン停止するためのHandlerのRunnableの削除
        handler.removeCallbacksAndMessages(null)

        scanning.value = false

        // TODO BLEデバイスのScan停止
    }

}

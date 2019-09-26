package io.github.cnaos.blescanner.ui.devicelist

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Handler
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.markodevcic.peko.PermissionsLiveData
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.verbose
import org.jetbrains.anko.warn
import java.util.concurrent.TimeUnit

class DeviceListViewModel : ViewModel(),AnkoLogger {
    // 定数
    companion object {
        private val SCAN_PERIOD: Long =
            TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS)// スキャン時間
    }

    val scanning = MutableLiveData<Boolean>(false)
    lateinit var bluetoothAdapter: BluetoothAdapter

    val permissionLiveData = PermissionsLiveData()
    var isGrantedBLEPermission = false
    private val handler: Handler = Handler()

    // デバイススキャンコールバック
    private val mLeScanCallback = object : ScanCallback() {
        // スキャンに成功（アドバタイジングは一定間隔で常に発行されているため、本関数は一定間隔で呼ばれ続ける）
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            verbose("BLE scanCallback:onScanResult result=$result")
            // TODO デバイスリストに追加
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            verbose("BLE scanCallback:onBatchScanResults results=$results")
            // TODO デバイスリストに追加
        }
    }


    fun startDeviceScan() {
        verbose("startDeviceScan")

        if (!isGrantedBLEPermission) {
            permissionLiveData.checkPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
            return
        }

        // BluetoothLeScannerの取得
        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            warn("No BluetoothLeScanner available. Is Bluetooth turned on?")
            return
        }

        handler.postDelayed({
            stopDeviceScanInner()
        }, SCAN_PERIOD)

        scanning.value = true
        // BLEデバイスのスキャン開始
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        val scanfilters = listOf<ScanFilter>()
        scanner.startScan(scanfilters, scanSettings, mLeScanCallback)
    }

    fun stopDeviceScan() {
        // 一定期間後にスキャン停止するためのHandlerのRunnableの削除
        handler.removeCallbacksAndMessages(null)
        stopDeviceScanInner()
    }

    private fun stopDeviceScanInner() {
        verbose("stopDeviceScan")
        scanning.value = false

        // BLEデバイスのScan停止
        val scanner = bluetoothAdapter.bluetoothLeScanner ?: return
        scanner.stopScan(mLeScanCallback)
    }

}

package io.github.cnaos.blescanner.ui.devicelist

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Handler
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.markodevcic.peko.PermissionsLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.verbose
import org.jetbrains.anko.warn
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class DeviceListViewModel(application: Application) : AndroidViewModel(application),
    CoroutineScope, AnkoLogger {
    // 定数
    companion object {
        private val SCAN_PERIOD: Long =
            TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS)// スキャン時間
    }

    private val scannedDeviceMap = ConcurrentHashMap<String, BleDeviceData>()

    /**
     * 表示用のデバイスリスト
     */
    val bleDeviceDataList = MutableLiveData<List<BleDeviceData>>(listOf<BleDeviceData>())

    val scanning = MutableLiveData<Boolean>(false)
    lateinit var bluetoothAdapter: BluetoothAdapter

    val permissionLiveData = PermissionsLiveData()
    var isGrantedBLEPermission = false
    private val handler: Handler = Handler()

    private val bleDeviceComparator =
        compareBy<BleDeviceData, String?>(nullsLast())
        { it.name }
            .thenBy { it.address }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCleared() {
        job.cancel()
    }

    // デバイススキャンコールバック
    private val mLeScanCallback = object : ScanCallback() {
        // スキャンに成功（アドバタイジングは一定間隔で常に発行されているため、本関数は一定間隔で呼ばれ続ける）
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            verbose("BLE scanCallback:onScanResult result=$result")
            addDevice(result.device)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            verbose("BLE scanCallback:onBatchScanResults results=$results")
            results?.forEach { addDevice(it.device) }
        }
    }

    @Synchronized
    private fun addDevice(device: BluetoothDevice?) {
        device ?: return
        val tmpBleDeviceData = BleDeviceData(device.name, device.address)

        if (scannedDeviceMap.putIfAbsent(device.address, tmpBleDeviceData) != null) {
            return
        }

        val tmpList = scannedDeviceMap.values.sortedWith(bleDeviceComparator)
        bleDeviceDataList.value = tmpList
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


//    suspend fun readDeviceName(bleDeviceData: BleDeviceData){
//        val device = bluetoothAdapter.getRemoteDevice(bleDeviceData.address)
//        if (device == null) {
//            warn("Device not found.  Unable to scanServices.")
//            return
//        }
//
//        // If ViewModel is destroyed during connection attempt, then `result` will contain
//        // `ConnectGattResult.Canceled`.
//        //val gattResult = device.connectGatt(getApplication<Application>(), autoConnect = false)
//        log("actor connecting: $bleDeviceData")
//        val gatt = device.connectGatt(getApplication(), autoConnect = false).let { result ->
//            when (result) {
//                is ConnectGattResult.Success -> {
//                    result.gatt
//                }
//                is ConnectGattResult.Canceled -> {
//                    return
//                }
//                is ConnectGattResult.Failure -> {
//                    warn("Gatt Connect failed. result=$result")
//                    return
//                }
//                else -> {
//                    throw IllegalStateException("unknown GattResult. result=$result")
//                }
//            }
//        }
//
//        log("actor connected: $bleDeviceData")
//
//        gatt.use { gattHandler ->
//            if (gattHandler.discoverServices() != BluetoothGatt.GATT_SUCCESS) {
//                // discover services failed
//                warn("discover GATT services failed")
//                return
//            }
//
//            val gattModel =
//                GattDeviceModel(gattHandler.services)
//
//            // デバイス名取得
//            val deviceNameCharacteristicUUID =
//                UUID.fromString(GattGenericAccessUUID.DeviceName.uuid)
//
//            val deviceNameCharacteristic =
//                gattModel.gattCharacteristicsMap[deviceNameCharacteristicUUID.toString()]
//
//            log("actor reading: $bleDeviceData")
//            val result = gattHandler.readCharacteristic(deviceNameCharacteristic!!)
//            log("actor read: result=$result")
//            if (result.status == BluetoothGatt.GATT_SUCCESS) {
//                val data =
//                    MyGattStringData(
//                        deviceNameCharacteristicUUID.toString(),
//                        String(result.value)
//                    )
//                gattModel.gattDeviceName = data.data
//                bleDeviceData.name = data.data
//                log("actor: GATT device Name = ${data.data}")
//                launch(Dispatchers.Main) {
//                    deviceReadingCount.value = deviceChannel.count
//                }
//            }
//
//            gattHandler.disconnect()
//        }
//    }
}

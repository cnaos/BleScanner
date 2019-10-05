package io.github.cnaos.blescanner.ui.devicelist

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.markodevcic.peko.PermissionsLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DeviceListViewModel(application: Application) : AndroidViewModel(application) {
    // 定数
    companion object {
        private val SCAN_PERIOD: Long =
            TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS)// スキャン時間
    }

    private lateinit var scanJob: Job
    private val scannedDeviceMap = ConcurrentHashMap<String, BleDeviceData>()

    /**
     * 表示用のデバイスリスト
     */
    val bleDeviceDataList = MutableLiveData<List<BleDeviceData>>(listOf<BleDeviceData>())

    val scanning = MutableLiveData<Boolean>(false)
    lateinit var bluetoothAdapter: BluetoothAdapter

    val permissionLiveData = PermissionsLiveData()
    var isGrantedBLEPermission = false

    private val bleDeviceComparator =
        compareBy<BleDeviceData, String?>(nullsLast())
        { it.name }
            .thenBy { it.address }


    fun log(msg: String) = Timber.v("[${Thread.currentThread().name}] $msg")

    fun deviceScanFlow(
        scanner: BluetoothLeScanner,
        scanfilters: List<ScanFilter>,
        scanSettings: ScanSettings
    ): Flow<ScanResult> = callbackFlow {
        val mLeScanCallback = object : ScanCallback() {
            // スキャンに成功（アドバタイジングは一定間隔で常に発行されているため、本関数は一定間隔で呼ばれ続ける）
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                // log("BLE scanCallback:onScanResult result=$result")
                offer(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                // log("BLE scanCallback:onBatchScanResults results=$results")
                results?.forEach { offer(it) }
            }
        }
        scanner.startScan(scanfilters, scanSettings, mLeScanCallback)

        // 一定時間経過したらchannelをcloseするタイマーを仕掛ける
        launch {
            delay(SCAN_PERIOD)
            log("channel close delay")
            channel.close()
        }

        // Suspend until either onCompleted or external cancellation are invoked
        awaitClose {
            log("channel closed")
            scanner.stopScan(mLeScanCallback)
            stopDeviceScan()
        }
    }

    @Synchronized
    private fun addDevice(device: BluetoothDevice?) {
        device ?: return
        val tmpBleDeviceData = BleDeviceData(device.name, device.address)

        if (scannedDeviceMap.putIfAbsent(device.address, tmpBleDeviceData) != null) {
            return
        }
        log("add device: $tmpBleDeviceData")

        val tmpList = scannedDeviceMap.values.sortedWith(bleDeviceComparator)
        bleDeviceDataList.postValue(tmpList)
    }


    fun startDeviceScan() {
        Timber.v("startDeviceScan")

        if (!isGrantedBLEPermission) {
            permissionLiveData.checkPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
            return
        }

        // BluetoothLeScannerの取得
        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            Timber.w("No BluetoothLeScanner available. Is Bluetooth turned on?")
            return
        }

        scanning.value = true
        // BLEデバイスのスキャン開始
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        val scanfilters = listOf<ScanFilter>()

        // デバイススキャンの結果を受け取るコルーチンを起動する
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            val scanFlow = deviceScanFlow(scanner, scanfilters, scanSettings)
            scanFlow.collect {
                addDevice(it.device)
            }
        }
    }

    fun stopDeviceScan() {
        Timber.v("stopDeviceScan")
        scanning.postValue(false)
        scanJob.cancel()
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

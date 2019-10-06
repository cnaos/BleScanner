package io.github.cnaos.blescanner.ui.devicelist

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.*
import androidx.lifecycle.*
import com.juul.able.experimental.ConnectGattResult
import com.juul.able.experimental.android.connectGatt
import com.markodevcic.peko.PermissionsLiveData
import io.github.cnaos.blescanner.gatt.generic.GattGenericAccessUUID
import io.github.cnaos.blescanner.gattmodel.GattDeviceModel
import io.github.cnaos.blescanner.gattmodel.MyGattStringData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DeviceListViewModel(application: Application) : AndroidViewModel(application) {
    // 定数
    companion object {
        private val SCAN_PERIOD: Long =
            TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS)// スキャン時間
    }

    private var scanJob: Job? = null
    private val scannedDeviceMap = ConcurrentHashMap<String, BleDeviceData>()

    /**
     * 表示用のデバイスリスト
     */
    val bleDeviceDataList = MutableLiveData<List<BleDeviceData>>(listOf<BleDeviceData>())

    val scanning = MutableLiveData<Boolean>(false)

    val deviceNameScanCount: LiveData<Int> = Transformations.map(bleDeviceDataList) {
        bleDeviceDataList.value?.count { it.gapScanState != BleDeviceData.Companion.ScanState.NOT_YET }
    }

    val deviceNameReadActor =
        viewModelScope.actor<BleDeviceData>(capacity = 20, context = Dispatchers.IO) {
            for (it in channel) {
                log("deviceNameReadActor()", "actor ReadDeviceName: $it")
                readDeviceName(it)
                log("deviceNameReadActor()", "actor updateListOrder: $it")
                updateListOrder()
            }
        }

    lateinit var bluetoothAdapter: BluetoothAdapter

    val permissionLiveData = PermissionsLiveData()
    var isGrantedBLEPermission = false

    fun log(functionName: String, msg: String) =
        Timber.v("[${Thread.currentThread().name}] $functionName $msg")

    fun deviceScanFlow(
        scanner: BluetoothLeScanner,
        scanfilters: List<ScanFilter>,
        scanSettings: ScanSettings
    ): Flow<ScanResult> = callbackFlow {
        val functionName = "deviceScanFlow()"
        val mLeScanCallback = object : ScanCallback() {
            // スキャンに成功（アドバタイジングは一定間隔で常に発行されているため、本関数は一定間隔で呼ばれ続ける）
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                // log("BLE scanCallback:onScanResult result=$result")
                if (channel.isClosedForSend) {
                    return
                }
                offer(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                // log("BLE scanCallback:onBatchScanResults results=$results")
                results?.forEach {
                    if (channel.isClosedForSend) {
                        return
                    }
                    offer(it)
                }
            }
        }
        scanner.startScan(scanfilters, scanSettings, mLeScanCallback)

        // 一定時間経過したらchannelをcloseするタイマーを仕掛ける
        launch {
            delay(SCAN_PERIOD)
            log(functionName, "channel close delay")
            channel.close()
        }

        // Suspend until either onCompleted or external cancellation are invoked
        awaitClose {
            log(functionName, "channel closed")
            scanner.stopScan(mLeScanCallback)
            stopDeviceScan()
        }
    }

    @Synchronized
    suspend fun addDevice(device: BluetoothDevice?) {
        val functionName = "addDevice()"
        device ?: return
        val tmpBleDeviceData = BleDeviceData(device.address, device.name)

        if (scannedDeviceMap.putIfAbsent(device.address, tmpBleDeviceData) != null) {
            return
        }
        log(functionName, "$tmpBleDeviceData")

        updateListOrder()

        deviceNameReadActor.send(tmpBleDeviceData)
    }

    private fun updateListOrder() {
        val tmpList = scannedDeviceMap.values.sortedWith(BleDeviceData.comparator)
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

        scanning.postValue(true)
        // BLEデバイスのスキャン開始
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        val scanfilters = listOf<ScanFilter>()

        // デバイススキャンの結果を受け取るコルーチンを起動する
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            val scanFlow = deviceScanFlow(scanner, scanfilters, scanSettings)
            scanFlow.buffer().collect {
                addDevice(it.device)
            }
        }
    }

    fun stopDeviceScan() {
        Timber.v("stopDeviceScan")
        scanning.postValue(false)
        scanJob?.cancel()
    }


    suspend fun readDeviceName(bleDeviceData: BleDeviceData) {
        val functionName = "readDeviceName(${bleDeviceData.address})"

        val device = bluetoothAdapter.getRemoteDevice(bleDeviceData.address)
        if (device == null) {
            Timber.w("Device not found. Unable to scanServices.")
            return
        }

        // If ViewModel is destroyed during connection attempt, then `result` will contain
        // `ConnectGattResult.Canceled`.
        //val gattResult = device.connectGatt(getApplication<Application>(), autoConnect = false)
        log(functionName, "connecting")
        val gatt = device.connectGatt(getApplication(), autoConnect = false).let { result ->
            when (result) {
                is ConnectGattResult.Success -> {
                    result.gatt
                }
                is ConnectGattResult.Canceled -> {
                    return@readDeviceName
                }
                is ConnectGattResult.Failure -> {
                    Timber.w("Gatt Connect failed. result=$result")
                    return@readDeviceName
                }
                else -> {
                    throw IllegalStateException("unknown GattResult. result=$result")
                }
            }
        }

        log(functionName, "connected")

        gatt.use { gattHandler ->
            if (gattHandler.discoverServices() != BluetoothGatt.GATT_SUCCESS) {
                // discover services failed
                Timber.w("discover GATT services failed")
                return
            }

            val gattModel =
                GattDeviceModel(gattHandler.services)

            // デバイス名取得
            val deviceNameCharacteristicUUID =
                UUID.fromString(GattGenericAccessUUID.DeviceName.uuid)

            val deviceNameCharacteristic =
                gattModel.gattCharacteristicsMap[deviceNameCharacteristicUUID.toString()]

            log(functionName, "reading")
            val result = gattHandler.readCharacteristic(deviceNameCharacteristic!!)
            log(functionName, "read: result=${result.status}")
            if (result.status == BluetoothGatt.GATT_SUCCESS) {
                val data =
                    MyGattStringData(
                        deviceNameCharacteristicUUID.toString(),
                        String(result.value)
                    )
                gattModel.gattDeviceName = data.data
                bleDeviceData.gapDeviceName = data.data
                bleDeviceData.gapScanState = BleDeviceData.Companion.ScanState.SCAN_SUCCESS
                log(functionName, "GATT device Name = ${data.data}")
            } else {
                bleDeviceData.gapScanState = BleDeviceData.Companion.ScanState.SCAN_FAILED
            }

            gattHandler.disconnect()
        }
    }
}

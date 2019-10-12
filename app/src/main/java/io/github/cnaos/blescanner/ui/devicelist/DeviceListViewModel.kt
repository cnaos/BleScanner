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
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class DeviceListViewModel(application: Application) : AndroidViewModel(application) {
    // 定数
    companion object {
        private val SCAN_PERIOD: Long =
            TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS)// スキャン時間
    }

    private var deviceNameReadJob: Job? = null
    private var deviceNameReadActor: SendChannel<BleDeviceData>? = null
    private var deviceScanJob: Job? = null
    private val scannedDeviceMap = ConcurrentHashMap<String, BleDeviceData>()

    // 画面表示用のデバイスリスト
    val bleDeviceDataList = MutableLiveData<List<BleDeviceData>>(listOf<BleDeviceData>())

    // 画面にBLEデバイスのスキャンの状態を表示する
    val scanning = MutableLiveData<Boolean>(false)

    // 画面にデバイス名のスキャン状況を表示する
    val scanCountMap: LiveData<Map<String, Int>> = Transformations.map(bleDeviceDataList) {
        bleDeviceDataList.value?.groupingBy { it.gapScanState.name }?.eachCount()
    }

    lateinit var bluetoothAdapter: BluetoothAdapter

    val permissionLiveData = PermissionsLiveData()
    var isGrantedBLEPermission = false

    fun log(functionName: String, msg: String) =
        Timber.v("[${Thread.currentThread().name}] $functionName $msg")

    /**
     * BLEデバイスのスキャンを行うFlowを作る
     */
    fun deviceScanFlow(
        scanner: BluetoothLeScanner,
        scanfilters: List<ScanFilter>,
        scanSettings: ScanSettings
    ): Flow<ScanResult> = callbackFlow {
        val functionName = "deviceScanFlow()"
        val mLeScanCallback = object : ScanCallback() {
            // デバイスが検出されると呼ばれる
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
        // BLEデバイスのスキャン開始
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

    /**
     * BLEデバイス名を取得するためのActorを作る
     */
    private fun createActor(context: CoroutineContext): SendChannel<BleDeviceData> {
        return viewModelScope.actor<BleDeviceData>(
            capacity = 20,
            context = context + Dispatchers.IO
        ) {
            for (it in channel) {
                log("deviceNameReadActor()", "count=${scanCountMap.value}")
                log("deviceNameReadActor()", "actor ReadDeviceName: $it")
                readDeviceName(it)
                log("deviceNameReadActor()", "actor updateListOrder: $it")
                updateListOrder()
            }
            log("deviceNameReadActor()", "actor closed")
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
        log(functionName, "count=$tmpBleDeviceData")

        updateListOrder()

        deviceNameReadActor?.send(tmpBleDeviceData)
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
        // 前のデバイス名読み出しjobをキャンセルする
        if (deviceNameReadJob != null && deviceNameReadJob!!.isActive) {
            cancelNameReadJob()
        }

        // デバイス名読み出しjobを作り直し
        if (deviceNameReadJob == null || deviceNameReadJob!!.isCancelled) {
            deviceNameReadJob = Job()
        }

        if (deviceNameReadActor == null || deviceNameReadActor!!.isClosedForSend) {
            deviceNameReadActor = createActor(deviceNameReadJob!!)
        }

        // デバイススキャンの結果を受け取るコルーチンを起動する
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        val scanfilters = listOf<ScanFilter>()

        deviceScanJob = viewModelScope.launch(Dispatchers.IO) {
            val scanFlow = deviceScanFlow(scanner, scanfilters, scanSettings)
            scanFlow.buffer().collect {
                addDevice(it.device)
            }
        }

        // デバイス名の再取得
        viewModelScope.launch(Dispatchers.IO) {
            scannedDeviceMap.values.filter { it.gapScanState != BleDeviceData.Companion.ScanState.SCAN_SUCCESS }
                .forEach {
                    it.gapScanState = BleDeviceData.Companion.ScanState.NOT_YET
                    deviceNameReadActor?.send(it)
                }
        }

    }

    fun stopDeviceScan() {
        Timber.v("stopDeviceScan")
        scanning.postValue(false)
        cancelDeviceScanJob()
    }

    fun cancelDeviceScanJob() {
        Timber.v("cancel deviceScanJob")
        deviceScanJob?.cancel()
    }

    fun cancelNameReadJob() {
        Timber.v("cancel deivceNameReadJob")
        deviceNameReadJob?.cancel()
        deviceNameReadActor?.close()
    }


    /**
     * スキャンしたBLEデバイスのGATTプロファイルからデバイス名を取得する
     */
    suspend fun readDeviceName(bleDeviceData: BleDeviceData) {
        val functionName = "readDeviceName(${bleDeviceData.address})"
        log(functionName, "start count=${scanCountMap.value}")

        val device = bluetoothAdapter.getRemoteDevice(bleDeviceData.address)
        if (device == null) {
            Timber.w("Device not found. Unable to scanServices.")
            return
        }

        log(functionName, "connecting")
        val gattConnectResult = withTimeoutOrNull(10_000L) {
            device.connectGatt(getApplication(), autoConnect = false)
        }
        if (gattConnectResult == null) {
            log(functionName, "timeout")
            bleDeviceData.gapScanState = BleDeviceData.Companion.ScanState.SCAN_FAILED
            return
        }
        val gatt = gattConnectResult.let { result ->
            when (result) {
                is ConnectGattResult.Success -> {
                    result.gatt
                }
                is ConnectGattResult.Canceled -> {
                    Timber.w("Gatt Connect canceled. result=$result")
                    bleDeviceData.gapScanState = BleDeviceData.Companion.ScanState.SCAN_FAILED
                    return@readDeviceName
                }
                is ConnectGattResult.Failure -> {
                    Timber.w("Gatt Connect failed. result=$result")
                    bleDeviceData.gapScanState = BleDeviceData.Companion.ScanState.SCAN_FAILED
                    return@readDeviceName
                }
                else -> {
                    throw IllegalStateException("unknown GattResult. result=$result")
                }
            }
        }

        log(
            functionName,
            "connected ${scanCountMap.value}"
        )

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
            log(
                functionName,
                "finish ${scanCountMap.value}"
            )

            gattHandler.disconnect()
        }
    }
}

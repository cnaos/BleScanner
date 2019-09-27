package io.github.cnaos.blescanner.ui.devicedetail

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.juul.able.experimental.ConnectGattResult
import com.juul.able.experimental.Gatt
import com.juul.able.experimental.android.connectGatt
import io.github.cnaos.blescanner.gatt.generic.GattGenericAccessUUID
import io.github.cnaos.blescanner.gatt.generic.GattGenericUUIDConstants
import io.github.cnaos.blescanner.gattmodel.GattDeviceModel
import io.github.cnaos.blescanner.gattmodel.MyGattRawData
import io.github.cnaos.blescanner.gattmodel.MyGattStringData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.error
import org.jetbrains.anko.verbose
import org.jetbrains.anko.warn
import java.util.*
import kotlin.coroutines.CoroutineContext

class DeviceDetailViewModel(application: Application) : AndroidViewModel(application),
    CoroutineScope, AnkoLogger {

    lateinit var deviceName: String
    lateinit var deviceAddress: String
    lateinit var bluetoothAdapter: BluetoothAdapter

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        FAILED
    }

    /**
     * BLEデバイスとの接続状態
     */
    val connectionState: MutableLiveData<ConnectionState> =
        MutableLiveData(ConnectionState.DISCONNECTED)

    val bindGattModel: MutableLiveData<GattDeviceModel> = MutableLiveData(
        GattDeviceModel(listOf())
    )

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCleared() {
        job.cancel()
    }

    fun disconnect() {
        job.cancel()
        connectionState.value = ConnectionState.DISCONNECTED
    }

    fun scanServices(): Boolean {
        if (deviceAddress.isBlank()) {
            warn("unspecified ble device address.")
            return false
        }

        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        if (device == null) {
            warn("Device not found.  Unable to scanServices.")
            return false
        }

        launch {
            // If ViewModel is destroyed during connection attempt, then `result` will contain
            // `ConnectGattResult.Canceled`.
            connectionState.value = ConnectionState.CONNECTING
            //val gattResult = device.connectGatt(getApplication<Application>(), autoConnect = false)

            val gatt = device.connectGatt(getApplication(), autoConnect = false).let { result ->
                when (result) {
                    is ConnectGattResult.Success -> {
                        connectionState.value = ConnectionState.CONNECTED
                        result.gatt
                    }
                    is ConnectGattResult.Canceled -> {
                        connectionState.value = ConnectionState.DISCONNECTED
                        return@launch
                    }
                    is ConnectGattResult.Failure -> {
                        connectionState.value = ConnectionState.FAILED
                        return@launch
                    }
                    else -> {
                        throw IllegalStateException("unknown GattResult. result=$result")
                    }
                }
            }

            if (gatt.discoverServices() != BluetoothGatt.GATT_SUCCESS) {
                // discover services failed
                warn("discover GATT services failed")
                return@launch
            }

            val gattModel =
                GattDeviceModel(gatt.services)
            bindGattModel.value = gattModel


            // デバイス名取得
            val deviceNameCharacteristicUUID =
                UUID.fromString(GattGenericAccessUUID.DeviceName.uuid)

            val deviceNameCharacteristic =
                gattModel.gattCharacteristicsMap[deviceNameCharacteristicUUID.toString()]

            val result = gatt.readCharacteristic(deviceNameCharacteristic!!)
            if (result.status == BluetoothGatt.GATT_SUCCESS) {
                val data =
                    MyGattStringData(deviceNameCharacteristicUUID.toString(), String(result.value))
                gattModel.gattDeviceName = data.data
                verbose("GATT device Name = ${data.data}")
                bindGattModel.value = gattModel
            }

            // 全データの読み込み
            readGattCharacteristics(
                gatt, gattModel,
                gattModel.gattCharacteristicsMap.values.toList()
            )

            gatt.disconnect()
            connectionState.value = ConnectionState.DISCONNECTED
            gatt.close()
        }

        return true
    }

    private suspend fun readGattCharacteristics(
        gatt: Gatt,
        gattModel: GattDeviceModel,
        list: List<BluetoothGattCharacteristic>
    ) {
        list.forEach {
            // 読み込み可能なものだけ処理する
            if (it.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
                verbose("skip read characteristics(${it.uuid})")
                return@forEach
            }

            try {
                val readResult = gatt.readCharacteristic(it)
                val data =
                    if (GattGenericUUIDConstants.isStringCharacteristic(it.uuid.toString())) {
                        MyGattStringData(it.uuid.toString(), String(readResult.value))
                    } else {
                        MyGattRawData(it.uuid.toString(), readResult.value)
                    }

                gattModel.characteristicDataMap[it.uuid.toString()] = data
            } catch (e: Exception) {
                error("GATT read Characteristic error.", e)
            }
        }
    }

}

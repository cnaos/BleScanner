package io.github.cnaos.blescanner.ui.devicedetail

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.juul.able.experimental.ConnectGattResult
import com.juul.able.experimental.Gatt
import com.juul.able.experimental.android.connectGatt
import io.github.cnaos.blescanner.gatt.generic.GattDeviceInformationUUID
import io.github.cnaos.blescanner.gatt.generic.GattGenericAccessUUID
import io.github.cnaos.blescanner.gatt.generic.GattGenericUUIDConstants
import io.github.cnaos.blescanner.gattmodel.GattDeviceModel
import io.github.cnaos.blescanner.gattmodel.MyGattRawData
import io.github.cnaos.blescanner.gattmodel.MyGattStringData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class DeviceDetailViewModel(application: Application) : AndroidViewModel(application) {

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

    val errorMessage: MutableLiveData<String> = MutableLiveData("")

    private var mGatt: Gatt? = null


    fun disconnect() {
        val tmpGatt = mGatt ?: return

        if (tmpGatt.isActive) {
            viewModelScope.launch {
                tmpGatt.disconnect()
                connectionState.postValue(ConnectionState.DISCONNECTED)
            }
        }
    }

    fun log(msg: String) = Timber.v("[${Thread.currentThread().name}] $msg")

    fun scanServices(): Boolean {
        if (deviceAddress.isBlank()) {
            Timber.w("unspecified ble device address.")
            return false
        }

        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        if (device == null) {
            Timber.w("Device not found.  Unable to scanServices.")
            return false
        }

        viewModelScope.launch(Dispatchers.IO) {
            // If ViewModel is destroyed during connection attempt, then `result` will contain
            // `ConnectGattResult.Canceled`.
            connectionState.postValue(ConnectionState.CONNECTING)
            //val gattResult = device.connectGatt(getApplication<Application>(), autoConnect = false)
            log("connecting: $deviceAddress")
            val gatt = device.connectGatt(getApplication(), autoConnect = false).let { result ->
                when (result) {
                    is ConnectGattResult.Success -> {
                        connectionState.postValue(ConnectionState.CONNECTED)
                        result.gatt
                    }
                    is ConnectGattResult.Canceled -> {
                        connectionState.postValue(ConnectionState.DISCONNECTED)
                        return@launch
                    }
                    is ConnectGattResult.Failure -> {
                        connectionState.postValue(ConnectionState.FAILED)
                        val errorMsg = "Gatt Connect failed. deivce address=${deviceAddress}"
                        Timber.e(errorMsg)
                        errorMessage.postValue(errorMsg)
                        return@launch
                    }
                    else -> {
                        throw IllegalStateException("unknown GattResult. result=$result")
                    }
                }
            }
            log("connected: $deviceAddress")

            mGatt = gatt

            gatt.use { gattHandler ->
                if (gattHandler.discoverServices() != BluetoothGatt.GATT_SUCCESS) {
                    // discover services failed
                    Timber.w("discover GATT services failed")
                    return@launch
                }

                val gattModel =
                    GattDeviceModel(gattHandler.services)
                bindGattModel.postValue(gattModel)


                // デバイス名取得
                val deviceNameCharacteristicUUID =
                    UUID.fromString(GattGenericAccessUUID.DeviceName.uuid)

                val deviceNameCharacteristic =
                    gattModel.gattCharacteristicsMap[deviceNameCharacteristicUUID.toString()]

                val result = gattHandler.readCharacteristic(deviceNameCharacteristic!!)
                if (result.status == BluetoothGatt.GATT_SUCCESS) {
                    val data =
                        MyGattStringData(
                            deviceNameCharacteristicUUID.toString(),
                            String(result.value)
                        )
                    gattModel.gattDeviceName = data.data
                    log("GATT device Name = ${data.data}")
                    bindGattModel.postValue(gattModel)
                }

                // Gatt Generic AccessとDeviceInformationのみcharacteristicを取得する
                val list =
                    gattModel.serviceToCharacteristicsMap[GattGenericAccessUUID.uuid] ?: emptyList()

                val list2 =
                    gattModel.serviceToCharacteristicsMap[GattDeviceInformationUUID.uuid]
                        ?: emptyList()

                readGattCharacteristics(
                    gattHandler, gattModel,
                    list + list2
                )



                gattHandler.disconnect()
                connectionState.postValue(ConnectionState.DISCONNECTED)
            }
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
                Timber.v("skip read characteristics(${it.uuid})")
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
                Timber.e("GATT read Characteristic error.", e)
            }
        }
    }

}

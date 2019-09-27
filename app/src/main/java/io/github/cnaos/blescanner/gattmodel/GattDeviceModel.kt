package io.github.cnaos.blescanner.gattmodel

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import androidx.databinding.BaseObservable
import java.util.*
import kotlin.collections.ArrayList

class GattDeviceModel(
    val serviceList: List<BluetoothGattService>
) : BaseObservable() {

    var gattDeviceName = "Unknown"

    /**
     * ExpandableListViewAdapter用
     */
    val gattCharacteristicAdapterData: List<List<BluetoothGattCharacteristic>>

    /**
     * CharacteristicのUUIDからCharacteristicを引く
     */
    val gattCharacteristicsMap: TreeMap<String, BluetoothGattCharacteristic> = TreeMap()

    /**
     * CharacteristicのUUIDから格納されているデータを引く
     */
    val characteristicDataMap = TreeMap<String, AbstractGattData>()


    /**
     * serviceのUUIDから配下のCharacteristicを引く
     */
    val serviceToCharacteristicsMap: TreeMap<String, List<BluetoothGattCharacteristic>> = TreeMap()

    init {
        val adapterDataList = ArrayList<List<BluetoothGattCharacteristic>>()
        serviceList.forEach { service ->
            serviceToCharacteristicsMap[service.uuid.toString().toLowerCase(Locale.ROOT)] =
                service.characteristics
            gattCharacteristicsMap.putAll(service.characteristics.associateBy {
                it.uuid.toString().toLowerCase(Locale.ROOT)
            })
            val characteristicArray = service.characteristics.toList()
            adapterDataList.add(characteristicArray)
        }
        gattCharacteristicAdapterData = adapterDataList.toList()
    }

    fun dump(): String {
        val serviceMap = serviceToCharacteristicsMap.map {
            "${it.key} = ${it.value.map { cha -> cha.uuid }}"
        }

        val charaMap = gattCharacteristicsMap.map {
            "${it.key} = ${it.value.uuid}"
        }

        val map = mutableMapOf(
            "GATT device name" to gattDeviceName,
            "services" to serviceMap.toString(),
            "charmap" to charaMap.toString()
        )

        return map.map { entry -> "$entry.key=$entry.value" }.joinToString("\n")
    }

}
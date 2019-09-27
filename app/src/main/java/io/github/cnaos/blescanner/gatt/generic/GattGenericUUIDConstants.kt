package io.github.cnaos.blescanner.gatt.generic

import io.github.cnaos.blescanner.gatt.AbstractGattCharacteristicsUUID
import io.github.cnaos.blescanner.gatt.AbstractGattServiceUUID
import io.github.cnaos.blescanner.gatt.CharacteristicType
import java.util.*

object GattGenericUUIDConstants {
    private val services: Map<String, AbstractGattServiceUUID> = initialServicesData()

    private fun initialServicesData(): Map<String, AbstractGattServiceUUID> {
        return listOf(
            GattGenericAccessUUID,
            GattGenericAttributeUUID,
            GattDeviceInformationUUID
        ).associateBy { it.uuid }
    }


    private val characteristics: Map<String, AbstractGattCharacteristicsUUID> =
        initialCharacteristicsData()

    private fun initialCharacteristicsData(): Map<String, AbstractGattCharacteristicsUUID> {
        return listOf(
            GattGenericAccessUUID,
            GattGenericAttributeUUID,
            GattDeviceInformationUUID
        ).map { it.characteristicsSet }.flatten().associateBy { it.uuid }
    }

    fun lookupService(uuid: String): AbstractGattServiceUUID? {
        return services[toLowerCase((uuid))]
    }

    fun lookupCharacteristics(uuid: String): AbstractGattCharacteristicsUUID? {
        return characteristics[toLowerCase((uuid))]
    }

    fun isStringCharacteristic(uuid: String): Boolean {
        return characteristics[toLowerCase(uuid)]?.type == CharacteristicType.STRING
    }

    private fun toLowerCase(uuid: String) = uuid.toLowerCase(Locale.getDefault())
}
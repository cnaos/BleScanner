package io.github.cnaos.blescanner.gatt.generic

import io.github.cnaos.blescanner.gatt.AbstractGattCharacteristicsUUID
import io.github.cnaos.blescanner.gatt.AbstractGattServiceUUID


object GattGenericAttributeUUID : AbstractGattServiceUUID(
    GattPublicShortUUID("1801"),
    "Generic Attribute",
    setOf(ServiceChanged)
) {
    object ServiceChanged :
        AbstractGattCharacteristicsUUID(
            GattPublicShortUUID("2A05"),
            "Service Changed"
        )
}
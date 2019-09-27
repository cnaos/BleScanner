package io.github.cnaos.blescanner.gatt.generic

import io.github.cnaos.blescanner.gatt.AbstractGattCharacteristicsUUID
import io.github.cnaos.blescanner.gatt.AbstractGattServiceUUID
import io.github.cnaos.blescanner.gatt.CharacteristicType


object GattGenericAccessUUID : AbstractGattServiceUUID(
    GattPublicShortUUID("1800"),
    "Generic Access",
    setOf(
        DeviceName,
        Appearance,
        PeripheralPrivacyFlag,
        ReconnectionAddress,
        PeripheralPreferredConnectionParameters
    )
) {
    object DeviceName :
        AbstractGattCharacteristicsUUID(
            GattPublicShortUUID("2A00"),
            "Device Name",
            CharacteristicType.STRING
        )

    object Appearance :
        AbstractGattCharacteristicsUUID(
            GattPublicShortUUID("2A01"),
            "Appearance"
        )

    object PeripheralPrivacyFlag :
        AbstractGattCharacteristicsUUID(
            GattPublicShortUUID("2A02"),
            "Peripheral Privacy Flag"
        )

    object ReconnectionAddress :
        AbstractGattCharacteristicsUUID(
            GattPublicShortUUID("2A03"),
            "Reconnection Address"
        )

    object PeripheralPreferredConnectionParameters :
        AbstractGattCharacteristicsUUID(
            GattPublicShortUUID("2A04"),
            "Peripheral Preferred Connection Parameters"
        )

}
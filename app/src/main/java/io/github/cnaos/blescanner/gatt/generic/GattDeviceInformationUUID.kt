package io.github.cnaos.blescanner.gatt.generic

import io.github.cnaos.blescanner.gatt.AbstractGattCharacteristicsUUID
import io.github.cnaos.blescanner.gatt.AbstractGattServiceUUID
import io.github.cnaos.blescanner.gatt.CharacteristicType


object GattDeviceInformationUUID : AbstractGattServiceUUID(
    GattPublicShortUUID("180A"),
    "Device Information Service",
    setOf(
        SystemId,
        ModelNumber,
        SerialNumber,
        FirmwareRevision,
        HardwareRevision,
        SoftwareRevision,
        ManufacturerName
    )
) {
    object SystemId :
        AbstractGattCharacteristicsUUID(
            GattPublicShortUUID("2a23"),
            "System ID",
            CharacteristicType.STRING
        )

    object ModelNumber :
        AbstractGattCharacteristicsUUID(
            GattPublicShortUUID("2a24"),
            "ModelNumber String",
            CharacteristicType.STRING
        )

    object SerialNumber :
        AbstractGattCharacteristicsUUID(
            GattPublicShortUUID("2a25"),
            "Serial Number String",
            CharacteristicType.STRING
        )

    object FirmwareRevision :
        AbstractGattCharacteristicsUUID(
            GattPublicShortUUID("2a26"),
            "Firmware Revision String",
            CharacteristicType.STRING
        )

    object HardwareRevision :
        AbstractGattCharacteristicsUUID(
            GattPublicShortUUID("2a27"),
            "Hardware Revision String",
            CharacteristicType.STRING
        )

    object SoftwareRevision :
        AbstractGattCharacteristicsUUID(
            GattPublicShortUUID("2a28"),
            "Software Revision String",
            CharacteristicType.STRING
        )

    object ManufacturerName :
        AbstractGattCharacteristicsUUID(
            GattPublicShortUUID("2a29"),
            "Manufacturer Name String",
            CharacteristicType.STRING
        )

}
package io.github.cnaos.blescanner.gatt

/**
 * BLE GATTのサービスのuuidを扱うクラス
 */
abstract class AbstractGattServiceUUID(
    shortUUID: AbstractGattShortUUID,
    description: String,
    val characteristicsSet: Set<AbstractGattCharacteristicsUUID>
) : AbstractGattUUID(shortUUID, description)
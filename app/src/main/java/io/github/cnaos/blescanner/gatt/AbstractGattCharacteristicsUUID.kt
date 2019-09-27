package io.github.cnaos.blescanner.gatt

/**
 * BLE GATTのcharacteristicsのuuidを扱うクラス
 */
abstract class AbstractGattCharacteristicsUUID(
    uuid: AbstractGattShortUUID,
    description: String,
    val type: CharacteristicType = CharacteristicType.UNKNOWN
) : AbstractGattUUID(uuid, description)

enum class CharacteristicType {
    STRING,
    UNKNOWN,
}

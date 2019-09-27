package io.github.cnaos.blescanner.gatt

/**
 * BLE GATTのuuidを扱うクラス
 */
abstract class AbstractGattUUID(
    val shortUUID: AbstractGattShortUUID,
    val description: String
) {
    val uuid: String = shortUUID.fullUUID
}
package io.github.cnaos.blescanner.gatt.model

class MyGattRawData(uuid: String, val rawData: ByteArray) : AbstractGattData(uuid) {
    override fun dataString(): String {
        return rawData.joinToString(separator = " ") { String.format("%02X", it) }
    }

}

class MyGattStringData(uuid: String, val data: String) : AbstractGattData(uuid) {
    override fun dataString(): String {
        return data
    }
}
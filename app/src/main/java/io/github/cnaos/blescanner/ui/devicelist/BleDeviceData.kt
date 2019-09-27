package io.github.cnaos.blescanner.ui.devicelist

data class BleDeviceData(
    val name: String,
    val address: String
) {
    val isKnownDevice: Boolean = name != "Unknown"
    val tint: String = if (isKnownDevice) "#FF0000FF" else "#ffB0B0B0"
}
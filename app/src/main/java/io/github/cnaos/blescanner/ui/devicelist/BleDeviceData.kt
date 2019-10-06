package io.github.cnaos.blescanner.ui.devicelist

import androidx.recyclerview.widget.DiffUtil

data class BleDeviceData(
    val address: String,
    val name: String?,
    var gapDeviceName: String = "",
    var gapScanState: ScanState = ScanState.NOT_YET
) {
    val displayName: String
        get() {
            if (gapDeviceName.isNotBlank()) {
                return gapDeviceName
            }
            if (name != null) {
                return name
            }
            return "Unknown"
        }

    val isKnownDevice: Boolean
        get() {
            if (gapDeviceName.isNotBlank()) {
                return true
            }
            if (name != null) {
                return true
            }
            return false
        }
    val tint: String = if (isKnownDevice) "#FF0000FF" else "#ffB0B0B0"

    companion object {
        val ITEM_CALLBACK = object : DiffUtil.ItemCallback<BleDeviceData>() {
            override fun areItemsTheSame(oldItem: BleDeviceData, newItem: BleDeviceData): Boolean =
                oldItem.address == newItem.address

            override fun areContentsTheSame(
                oldItem: BleDeviceData,
                newItem: BleDeviceData
            ): Boolean = oldItem == newItem
        }

        enum class ScanState(val order: Int) {
            NOT_YET(1),
            SCAN_SUCCESS(0),
            SCAN_FAILED(0)
        }

        val comparator =
            compareByDescending<BleDeviceData> { it.isKnownDevice }
                .thenBy { it.displayName }
                .thenBy { it.address }
    }
}


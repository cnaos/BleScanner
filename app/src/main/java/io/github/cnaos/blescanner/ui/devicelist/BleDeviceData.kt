package io.github.cnaos.blescanner.ui.devicelist

import androidx.recyclerview.widget.DiffUtil

data class BleDeviceData(
    val name: String,
    val address: String
) {
    val isKnownDevice: Boolean = name != "Unknown"
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
    }
}


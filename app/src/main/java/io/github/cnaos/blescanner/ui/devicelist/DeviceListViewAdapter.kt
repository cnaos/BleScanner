package io.github.cnaos.blescanner.ui.devicelist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.cnaos.blescanner.databinding.RecyclerviewItemBinding

class DeviceListViewAdapter(
    context: Context,
    private val listener: ListRowClickListener
) :
    ListAdapter<BleDeviceData, DeviceListViewAdapter.BindingHolder>(BleDeviceData.ITEM_CALLBACK) {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    class BindingHolder(
        val binding: RecyclerviewItemBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder {
        val binding = RecyclerviewItemBinding.inflate(mInflater, parent, false)
        return BindingHolder(binding)
    }

    override fun onBindViewHolder(bindingHolder: BindingHolder, position: Int) {
        val current = getItem(position)
        bindingHolder.itemView.setOnClickListener {
            listener.onClickRow(it, current)
        }
        bindingHolder.binding.bleDevice = current
    }

    interface ListRowClickListener {
        fun onClickRow(tappedView: View, rowModel: BleDeviceData)
    }

}


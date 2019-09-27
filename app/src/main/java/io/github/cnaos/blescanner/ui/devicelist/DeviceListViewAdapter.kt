package io.github.cnaos.blescanner.ui.devicelist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.cnaos.blescanner.databinding.RecyclerviewItemBinding

class DeviceListViewAdapter(
    context: Context
) :
    ListAdapter<BleDeviceData, DeviceListViewAdapter.BindingHolder>(BleDeviceData.ITEM_CALLBACK) {

    private var listener: ((View, BleDeviceData) -> Unit)? = null
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
            this.listener?.invoke(it, current)
        }
        bindingHolder.binding.bleDevice = current
    }

    fun setOnItemClicked(listener: (tappedView: View, rowModel: BleDeviceData) -> Unit) {
        this.listener = listener
    }
}


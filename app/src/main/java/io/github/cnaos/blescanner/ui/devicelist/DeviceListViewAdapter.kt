package io.github.cnaos.blescanner.ui.devicelist

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.cnaos.blescanner.databinding.RecyclerviewItemBinding

class DeviceListViewAdapter(context: Context) :
    RecyclerView.Adapter<DeviceListViewAdapter.BindingHolder>() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mDeviceList: MutableList<BleDeviceData> = mutableListOf()

    class BindingHolder(
        val binding: RecyclerviewItemBinding
    ) : RecyclerView.ViewHolder(binding.root)

    fun updateDeviceList(deviceList: List<BleDeviceData>) {
        mDeviceList = deviceList.toMutableList()
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder {
        val binding = RecyclerviewItemBinding.inflate(mInflater, parent, false)
        return BindingHolder(binding)
    }

    override fun getItemCount(): Int {
        return mDeviceList.size
    }

    override fun onBindViewHolder(bindingHolder: BindingHolder, position: Int) {
        if (mDeviceList.isEmpty()) {
            return
        }

        val current = mDeviceList[position]
        bindingHolder.binding.bleDevice = current
    }


}


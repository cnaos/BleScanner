package io.github.cnaos.blescanner.ui.devicelist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.cnaos.blescanner.R

class DeviceListViewAdapter(context: Context) :
    RecyclerView.Adapter<DeviceListViewAdapter.DeviceDataViewHolder>() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mDeviceList: MutableList<BleDeviceData> = mutableListOf()

    class DeviceDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceAddress: TextView = itemView.findViewById(R.id.textview_deviceaddress)
        val deviceName: TextView = itemView.findViewById(R.id.textview_devicename)
    }

    fun updateDeviceList(deviceList: List<BleDeviceData>) {
        mDeviceList.clear()
        mDeviceList.addAll(deviceList)
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceDataViewHolder {
        val itemView = mInflater.inflate(R.layout.recyclerview_item, parent, false)
        return DeviceDataViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return mDeviceList.size
    }

    override fun onBindViewHolder(holder: DeviceDataViewHolder, position: Int) {
        if (mDeviceList.isEmpty()) {
            holder.deviceAddress.text = "no device"
        } else {
            val current = mDeviceList[position]
            holder.deviceAddress.text = current.address
            holder.deviceName.text = current.name
        }
    }


}


package io.github.cnaos.blescanner.ui.devicedetail

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import io.github.cnaos.blescanner.R
import io.github.cnaos.blescanner.gatt.generic.GattGenericUUIDConstants
import io.github.cnaos.blescanner.gattmodel.GattDeviceModel


class ExpandableListViewAdapter(private val inflater: LayoutInflater) :
    BaseExpandableListAdapter() {
    var model: GattDeviceModel = GattDeviceModel(emptyList())

    override fun getGroup(groupPosition: Int): Any {
        return model.serviceList[groupPosition]
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    override fun hasStableIds(): Boolean {
        // TODO modelが変更されたら応じて変える？
        // https://stackoverflow.com/questions/24385416/hasstableids-in-expandable-listview
        return true
    }

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val localConvertView =
            convertView ?: inflater.inflate(R.layout.expandable_list_group_item, parent, false)

        val service = getGroup(groupPosition) as BluetoothGattService
        val keyServiceUUID = service.uuid.toString()
        val gattServiceUUID = GattGenericUUIDConstants.lookupService(keyServiceUUID)

        val serviceText = gattServiceUUID?.description ?: "Unknown Service"
        val childCount = getChildrenCount(groupPosition)

        val uuidText = if (gattServiceUUID == null) {
            keyServiceUUID
        } else {
            "0x" + gattServiceUUID.shortUUID.shortUUID
        }

        localConvertView?.findViewById<TextView>(android.R.id.text1)?.text =
            "${serviceText}($childCount)"
        localConvertView?.findViewById<TextView>(android.R.id.text2)?.text = uuidText

        return localConvertView
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        val service = model.serviceList[groupPosition]
        return service.characteristics.size
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return model.gattCharacteristicAdapterData[groupPosition][childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return 0
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val localConvertView =
            convertView ?: inflater.inflate(R.layout.expandable_list_child_item, parent, false)

        val characteristic = getChild(groupPosition, childPosition) as BluetoothGattCharacteristic
        val keyCharaUUID = characteristic.uuid.toString()
        val charaUuid = GattGenericUUIDConstants.lookupCharacteristics(keyCharaUUID)

        val characteristicText = charaUuid?.description ?: "Unknown characteristics"

        val uuidText = if (charaUuid == null) {
            keyCharaUUID
        } else {
            "0x" + charaUuid.shortUUID.shortUUID
        }


        localConvertView?.findViewById<TextView>(R.id.text1)?.text = characteristicText
        localConvertView?.findViewById<TextView>(R.id.text2)?.text = uuidText

        // 読みだしたデータを表示用の文字列に変換
        val data = model.characteristicDataMap[keyCharaUUID]?.dataString()
        localConvertView?.findViewById<TextView>(R.id.text3)?.text = data

        return localConvertView
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return 0
    }

    override fun getGroupCount(): Int {
        return model.serviceList.size
    }

}
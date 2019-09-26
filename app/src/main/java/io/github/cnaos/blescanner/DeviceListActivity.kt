package io.github.cnaos.blescanner

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import io.github.cnaos.blescanner.ui.devicelist.DeviceListFragment
import io.github.cnaos.blescanner.ui.devicelist.DeviceListViewModel
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.verbose

class DeviceListActivity : AppCompatActivity(), AnkoLogger {
    // 定数
    companion object {
        private const val REQUEST_ENABLE_BLUETOOTH = 1 // Bluetooth機能の有効化要求時の識別コード
    }

    private val viewModel: DeviceListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_list_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, DeviceListFragment.newInstance())
                .commitNow()
        }

        // スキャン状態が変わったらメニューを更新する
        viewModel.scanning.observe(this, Observer {
            verbose("refresh menu")
            invalidateOptionsMenu()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.device_list, menu)
        if (menu == null) {
            return true
        }
        if (viewModel.scanning.value!!) {
            menu.findItem(R.id.menuitem_stop).isVisible = true
            menu.findItem(R.id.menuitem_scan).isVisible = false
            menu.findItem(R.id.menuitem_progress)
                .setActionView(R.layout.actionbar_indeterminate_progress)
        } else {
            menu.findItem(R.id.menuitem_stop).isVisible = false
            menu.findItem(R.id.menuitem_scan).isVisible = true
            menu.findItem(R.id.menuitem_progress).actionView = null
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menuitem_scan -> viewModel.startDeviceScan()    // スキャンの開始
            R.id.menuitem_stop -> viewModel.stopDeviceScan()    // スキャンの停止
        }
        return true
    }
}
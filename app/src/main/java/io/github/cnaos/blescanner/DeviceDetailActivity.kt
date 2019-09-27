package io.github.cnaos.blescanner

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import io.github.cnaos.blescanner.ui.devicedetail.DeviceDetailFragment
import io.github.cnaos.blescanner.ui.devicedetail.DeviceDetailViewModel
import io.github.cnaos.blescanner.ui.devicedetail.DeviceDetailViewModel.ConnectionState.*

class DeviceDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"
    }

    private val viewModel: DeviceDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_detail_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, DeviceDetailFragment.newInstance())
                .commitNow()
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            TODO("bluetoothAdapterが見つからなかった時の処理が未実装")
        }

        viewModel.deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME) ?: "Unknown"
        viewModel.deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)
        viewModel.bluetoothAdapter = bluetoothAdapter

        supportActionBar?.title = viewModel.deviceName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.connectionState.observe(this, Observer {
            invalidateOptionsMenu()
        })

        viewModel.scanServices()

    }

    override fun onPause() {
        super.onPause()
        viewModel.disconnect()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.device_detail, menu)
        when (viewModel.connectionState.value) {
            CONNECTED -> {
                menu.findItem(R.id.menu_connect).isVisible = false
                menu.findItem(R.id.menu_disconnect).isVisible = true
                menu.findItem(R.id.menu_refresh).actionView = null
            }
            CONNECTING -> {
                menu.findItem(R.id.menu_connect).isVisible = false
                menu.findItem(R.id.menu_disconnect).isVisible = true
                menu.findItem(R.id.menu_refresh)
                    .setActionView(R.layout.actionbar_indeterminate_progress)
            }
            DISCONNECTED,
            FAILED
            -> {
                menu.findItem(R.id.menu_connect).isVisible = true
                menu.findItem(R.id.menu_disconnect).isVisible = false
                menu.findItem(R.id.menu_refresh).actionView = null
            }
        }
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_connect -> {
                viewModel.scanServices()
                return true
            }
            R.id.menu_disconnect -> {
                viewModel.disconnect()
                return true
            }
            android.R.id.home -> {
                viewModel.disconnect()
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}

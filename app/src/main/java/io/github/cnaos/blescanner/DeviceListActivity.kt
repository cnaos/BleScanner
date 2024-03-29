package io.github.cnaos.blescanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.markodevcic.peko.PermissionResult
import io.github.cnaos.blescanner.ui.devicelist.DeviceListFragment
import io.github.cnaos.blescanner.ui.devicelist.DeviceListViewModel
import org.jetbrains.anko.alert
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.okButton
import org.jetbrains.anko.toast
import timber.log.Timber

class DeviceListActivity : AppCompatActivity() {
    // 定数
    companion object {
        private const val REQUEST_ENABLE_BLUETOOTH = 1 // Bluetooth機能の有効化要求時の識別コード
        private const val REQUEST_ENABLE_LOCATION = 2 // 位置情報の有効化要求時の識別コード
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

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            alert("Bluetooth Adapterが見つからないため終了します。") {
                okButton { finish() }
            }.show()
        } else {
            viewModel.bluetoothAdapter = bluetoothAdapter
        }

        // パーミッションの処理
        setupProcessPermissionResult()


        // スキャン状態が変わったらメニューを更新する
        viewModel.scanning.observe(this, Observer {
            Timber.v("refresh menu")
            invalidateOptionsMenu()
        })
    }

    private fun setupProcessPermissionResult() {
        viewModel.permissionLiveData.observe(this, Observer { result: PermissionResult ->
            Timber.v("process PermissionResult: $result")

            when (result) {
                is PermissionResult.Granted -> {
                    toast("permission granted")
                }
                is PermissionResult.Denied.JustDenied -> {
                    // at least one permission was denied
                    alert("this application need permission for BLE") {
                        okButton { }
                    }.show()
                }
                is PermissionResult.Denied.NeedsRationale -> {
                    // user clicked Deny, let's show a rationale
                    alert("アプリを利用するには 位置情報 の権限が必要です") {
                        okButton { }
                    }.show()
                }
                is PermissionResult.Denied.DeniedPermanently -> {
                    // Android System won't show Permission dialog anymore, let's tell the user we can't proceed
                    alert("アプリを利用するには、アプリの権限で 位置情報 を許可してください。") {
                        okButton { finish() }
                    }.show()
                }
            }
        })
    }


    fun isGrantedBlePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.isLocationEnabled.postValue(isLocationEnabled())
        requestEnableLocationFeature()
    }

    override fun onPause() {
        super.onPause()

        // スキャンの停止
        viewModel.stopDeviceScan()
        viewModel.cancelNameReadJob()
    }


    private fun requestEnableBluetoothFeature() {
        if (viewModel.bluetoothAdapter.isEnabled) {
            return
        }
        // デバイスのBluetooth機能が有効になっていないときは、有効化要求（ダイアログ表示）
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(
            enableBtIntent,
            REQUEST_ENABLE_BLUETOOTH
        )
    }

    private fun requestEnableLocationFeature() {
        if (isLocationEnabled()) {
            return
        }

        alert("Bluetoothデバイスのスキャンを行うには位置情報を有効にしてください。") {
            okButton {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent, REQUEST_ENABLE_LOCATION)
            }
            cancelButton { }
        }.show()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            val locationMode =
                Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE)
            locationMode != Settings.Secure.LOCATION_MODE_OFF
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_LOCATION -> {
                // 位置情報が有効になっているか確認
                viewModel.isLocationEnabled.postValue(isLocationEnabled())
            }
            else -> {
                // なにもしない
            }
        }
    }

    private fun startDeviceScan() {
        viewModel.isGrantedBLEPermission = isGrantedBlePermission()
        requestEnableBluetoothFeature()
        if (!isLocationEnabled()) {
            requestEnableLocationFeature()
            return
        }
        viewModel.startDeviceScan()
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
            R.id.menuitem_scan -> startDeviceScan()    // スキャンの開始
            R.id.menuitem_stop -> viewModel.stopDeviceScan()    // スキャンの停止
        }
        return true
    }

}
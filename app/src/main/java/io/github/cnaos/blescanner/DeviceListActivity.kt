package io.github.cnaos.blescanner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.github.cnaos.blescanner.ui.devicelist.DeviceListFragment

class DeviceListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_list_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, DeviceListFragment.newInstance())
                .commitNow()
        }
    }

}

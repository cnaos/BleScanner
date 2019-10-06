package io.github.cnaos.blescanner.ui.devicelist

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BleDeviceDataTest {
    @Test
    fun testComparator_beforeScan() {
        val data1 = BleDeviceData("2C", "OMVR-V190")
        val data2 = BleDeviceData("EE", "Env")
        val data3 = BleDeviceData("C8", "YamahaAV")
        val data4 = BleDeviceData("38", null)
        val list = listOf(
            data1,
            data2,
            data3,
            data4
        ).shuffled()

        val sorted = list.sortedWith(BleDeviceData.comparator)

        assertThat(sorted).containsExactly(
            data2,
            data1,
            data3,
            data4
        ).inOrder()
    }

    @Test
    fun testComparator_afterScan() {
        val data1 = BleDeviceData(
            "2C",
            "OMVR-V190",
            "OMVR-V190",
            BleDeviceData.Companion.ScanState.SCAN_SUCCESS
        )
        val data2 = BleDeviceData(
            "EE",
            "Env",
            "EnvSensor-BL01",
            BleDeviceData.Companion.ScanState.SCAN_SUCCESS
        )
        val data3 =
            BleDeviceData("C8", "YamahaAV", "", BleDeviceData.Companion.ScanState.SCAN_FAILED)
        val data4 =
            BleDeviceData("38", null, "caaaa", BleDeviceData.Companion.ScanState.SCAN_SUCCESS)
        val data5 =
            BleDeviceData("50", null, "iPhone", BleDeviceData.Companion.ScanState.SCAN_SUCCESS)
        val data6 =
            BleDeviceData("C1", null, "Ambient", BleDeviceData.Companion.ScanState.SCAN_SUCCESS)
        val list = listOf(
            data1,
            data2,
            data3,
            data4,
            data5,
            data6
        ).shuffled()

        val sorted = list.sortedWith(BleDeviceData.comparator)

        assertThat(sorted).containsExactly(
            data6,
            data2,
            data1,
            data3,
            data4,
            data5
        ).inOrder()
    }

    @Test
    fun displayName_nameNull() {
        val data = BleDeviceData("38", null)

        assertThat(data.displayName).isEqualTo("Unknown")
        assertThat(data.isKnownDevice).isEqualTo(false)
    }

    @Test
    fun displayName_nameEnv() {
        val data = BleDeviceData("EE", "Env")

        assertThat(data.displayName).isEqualTo("Env")
        assertThat(data.isKnownDevice).isEqualTo(true)
    }

    @Test
    fun displayName_nameNull_scanned() {
        val data = BleDeviceData("38", null)
        data.gapDeviceName = "GAP DEVICE NAME"

        assertThat(data.displayName).isEqualTo("GAP DEVICE NAME")
        assertThat(data.isKnownDevice).isEqualTo(true)
    }

    @Test
    fun displayName_nameEnv_scanned() {
        val data = BleDeviceData("EE", "Env")
        data.gapDeviceName = "GAP DEVICE NAME"

        assertThat(data.displayName).isEqualTo("GAP DEVICE NAME")
        assertThat(data.isKnownDevice).isEqualTo(true)
    }

}
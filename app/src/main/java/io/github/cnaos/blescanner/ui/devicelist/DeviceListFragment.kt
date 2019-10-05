package io.github.cnaos.blescanner.ui.devicelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.cnaos.blescanner.DeviceDetailActivity
import io.github.cnaos.blescanner.databinding.DeviceListFragmentBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.intentFor
import kotlin.coroutines.CoroutineContext


class DeviceListFragment : Fragment(), CoroutineScope, AnkoLogger {
    private var mJob = Job()
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main

    companion object {
        fun newInstance() = DeviceListFragment()
    }

    private val viewModel: DeviceListViewModel by activityViewModels()
    private lateinit var binding: DeviceListFragmentBinding
    private val uiScope = UiLifecycleScope()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DeviceListFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        lifecycle.addObserver(uiScope)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val adapter = DeviceListViewAdapter(context!!)
        adapter.setOnItemClicked { _, bleDeviceData ->
            viewModel.stopDeviceScan()

            activity?.startActivity(
                activity?.intentFor<DeviceDetailActivity>(
                    DeviceDetailActivity.EXTRAS_DEVICE_NAME to bleDeviceData.name,
                    DeviceDetailActivity.EXTRAS_DEVICE_ADDRESS to bleDeviceData.address
                )
            )
        }

        val recyclerView = binding.deviceListRecyclerView
        recyclerView.adapter = adapter

        recyclerView.layoutManager = LinearLayoutManager(context!!)
        val itemDecoration = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        viewModel.bleDeviceDataList.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })

    }

}

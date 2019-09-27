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
import io.github.cnaos.blescanner.databinding.DeviceListFragmentBinding



class DeviceListFragment : Fragment() {

    companion object {
        fun newInstance() = DeviceListFragment()
    }

    private val viewModel: DeviceListViewModel by activityViewModels()
    private lateinit var binding: DeviceListFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DeviceListFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val adapter = DeviceListViewAdapter(context!!)

        val recyclerView = binding.deviceListRecyclerView
        recyclerView.adapter = adapter

        recyclerView.layoutManager = LinearLayoutManager(context!!)
        val itemDecoration = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        viewModel.bleDeviceDataList.observe(viewLifecycleOwner, Observer {
            adapter.updateDeviceList(it)
        })
    }

}

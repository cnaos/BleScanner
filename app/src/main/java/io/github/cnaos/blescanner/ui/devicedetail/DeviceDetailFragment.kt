package io.github.cnaos.blescanner.ui.devicedetail

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import io.github.cnaos.blescanner.R
import io.github.cnaos.blescanner.databinding.DeviceDetailFragmentBinding

class DeviceDetailFragment : Fragment() {

    companion object {
        fun newInstance() = DeviceDetailFragment()
    }

    private val viewModel: DeviceDetailViewModel by activityViewModels()
    private lateinit var binding: DeviceDetailFragmentBinding

    private lateinit var mExpandableListViewAdapter: ExpandableListViewAdapter
    private lateinit var mExpandableListView: ExpandableListView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DeviceDetailFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel


        val stateIcon = binding.root.findViewById<ImageView>(R.id.state_icon)
        viewModel.connectionState.observe(viewLifecycleOwner, Observer {
            when (it) {
                DeviceDetailViewModel.ConnectionState.CONNECTED -> {
                    stateIcon.setImageResource(R.drawable.ic_bluetooth_connected_24px)
                    stateIcon.setColorFilter(Color.parseColor("#FF0000FF"))
                }
                DeviceDetailViewModel.ConnectionState.CONNECTING -> {
                    stateIcon.setImageResource(R.drawable.ic_bluetooth_searching_24px)
                    stateIcon.setColorFilter(Color.parseColor("#FF859AFF"))
                }
                DeviceDetailViewModel.ConnectionState.DISCONNECTED -> {
                    stateIcon.setImageResource(R.drawable.ic_bluetooth_disabled_24px)
                    stateIcon.setColorFilter(Color.parseColor("#FF777777"))
                }
            }
        })

        mExpandableListViewAdapter = ExpandableListViewAdapter(inflater)

        with(binding.root) {
            mExpandableListView = findViewById(R.id.gatt_services_list)
            mExpandableListView.setAdapter(mExpandableListViewAdapter)
        }

        viewModel.bindGattModel.observe(this, Observer {
            mExpandableListViewAdapter.model = it
            mExpandableListViewAdapter.notifyDataSetChanged()
        })


        return binding.root
    }


}

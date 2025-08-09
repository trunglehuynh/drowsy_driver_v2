package com.galaxylab.drowsydriver.UI

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.galaxylab.drowsydriver.R
import com.galaxylab.drowsydriver.Utility.PermissionController
import com.galaxylab.drowsydriver.databinding.FragmentPermissionBinding
import org.koin.android.ext.android.get

class PermissionFragment : Fragment() {

    private val permissionController: PermissionController = get()
    private var _binding: FragmentPermissionBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        permissionController.askAllNotGrantedPermissions(requireActivity())

        binding.permissionBtn.setOnClickListener {
            permissionController.openAppSetting(activity = requireActivity())
        }
        binding.startBtn.setOnClickListener {
            if (!permissionController.isAllPermissionGranted()) {
                Toast.makeText(context, R.string.permission_requires, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            (activity as MainActivity).showMainFragment()
        }
    }
}
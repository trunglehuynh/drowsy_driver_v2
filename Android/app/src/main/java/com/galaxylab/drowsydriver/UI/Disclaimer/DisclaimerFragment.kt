package com.galaxylab.drowsydriver.UI.Disclaimer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.galaxylab.drowsydriver.UI.MainActivity
import com.galaxylab.drowsydriver.UserInfo
import com.galaxylab.drowsydriver.databinding.DisclaimerFragmentBinding
import org.koin.android.ext.android.get

class DisclaimerFragment : Fragment() {
    private val userInfo: UserInfo = get()
    private var _binding: DisclaimerFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DisclaimerFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAgree.setOnClickListener {
            userInfo.userAgreedDisclaimer()
            (context as MainActivity).showNextFragment()
        }
        binding.btnQuit.setOnClickListener {
            activity?.finishAffinity()
        }
    }
}
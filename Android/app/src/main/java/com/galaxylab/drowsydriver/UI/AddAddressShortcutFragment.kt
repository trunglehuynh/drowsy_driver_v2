package com.galaxylab.drowsydriver.UI

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.galaxylab.drowsydriver.R
import com.galaxylab.drowsydriver.databinding.FragmentAddAdressShortcutBinding
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import org.koin.android.ext.android.get
import timber.log.Timber

class AddAddressShortcutFragment : Fragment() {
    companion object {
        const val SAVE_DESTINATION_ADDRESS_KEY = "SAVE_DESTINATION_ADDRESS_KEY"
    }

    private val sharedPreferences: SharedPreferences = get()

    private var _binding: FragmentAddAdressShortcutBinding? = null
    private val binding get() = _binding!!

    private lateinit var placesClient: PlacesClient
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddAdressShortcutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the SDK
        Places.initialize(requireActivity().applicationContext, getString(R.string.google_maps_key))
        // Create a new PlacesClient instance
        placesClient = Places.createClient(requireContext())

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line)
        sharedPreferences.getString(SAVE_DESTINATION_ADDRESS_KEY, null)?.let {
            binding.autocompleteAddress.setText(it) // = (it as  CharSequence)
        }

        binding.saveBtn.setOnClickListener {
            val address = binding.autocompleteAddress.text.toString().trim()
            sharedPreferences.edit().putString(SAVE_DESTINATION_ADDRESS_KEY, address).apply()
            Toast.makeText(
                requireContext(),
                "Save address $address as a shortcut",
                Toast.LENGTH_LONG
            ).show()
        }

        binding.removeBtn.setOnClickListener {
            val address = sharedPreferences.getString(SAVE_DESTINATION_ADDRESS_KEY, null)
                ?: return@setOnClickListener
            sharedPreferences.edit().putString(SAVE_DESTINATION_ADDRESS_KEY, null).apply()
            binding.autocompleteAddress.text = null
            Toast.makeText(requireContext(), "delete address $address", Toast.LENGTH_LONG).show()
        }

        binding.autocompleteAddress.setAdapter(adapter)

        val token = AutocompleteSessionToken.newInstance()

        binding.autocompleteAddress.addTextChangedListener {
            val query = it.toString()
            if (query.isNotEmpty()) {
                val request = FindAutocompletePredictionsRequest.builder()
                    .setSessionToken(token)
                    .setQuery(query)
                    .build()

                placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->

                    val suggestions = response.autocompletePredictions.map { autoPrediction ->
                        autoPrediction.getFullText(null).toString()
                    }
                    adapter.clear()
                    adapter.addAll(suggestions)
                    adapter.notifyDataSetChanged()
                    Timber.e("response $response")
                }.addOnFailureListener { exception ->
                    exception.printStackTrace()
                    Timber.e(exception)
                }
            }
        }
    }
}
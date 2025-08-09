package com.galaxylab.drowsydriver.UI.SoundPicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.galaxylab.drowsydriver.R
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class SoundListFragment : Fragment() {

    private val viewModel: SoundPickerViewModel by sharedViewModel()

    private lateinit var adapter: SoundItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_sound_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        val progress = view.findViewById<android.view.View>(R.id.progress)

        val source = requireArguments().getSerializable(ARG_SOURCE) as SoundSource

        adapter = SoundItemAdapter { item ->
            // forward selection + preview to parent fragment
            (parentFragment as? SoundPickerFragment)?.onItemSelectedFromChild(item)
        }
        val layoutManager = LinearLayoutManager(requireContext())
        recycler.layoutManager = layoutManager
        recycler.adapter = adapter
        val divider = DividerItemDecoration(requireContext(), layoutManager.orientation)
        ContextCompat.getDrawable(requireContext(), R.drawable.divider_sound)?.let { divider.setDrawable(it) }
        recycler.addItemDecoration(divider)

        viewModel.getLoading(source).observe(viewLifecycleOwner) { loading ->
            progress.visibility = if (loading) View.VISIBLE else View.GONE
            recycler.visibility = if (loading) View.INVISIBLE else View.VISIBLE
        }

        viewModel.getItems(source).observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        viewModel.selected.observe(viewLifecycleOwner) { item ->
            adapter.setSelectedUri(item?.uriString)
        }

        viewModel.ensureLoaded(source)
    }

    companion object {
        private const val ARG_SOURCE = "source"
        fun newInstance(source: SoundSource): SoundListFragment {
            val f = SoundListFragment()
            f.arguments = Bundle().apply {
                putSerializable(ARG_SOURCE, source)
            }
            return f
        }
    }
}

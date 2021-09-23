package dev.dokup.mediastoresample.ui

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import dev.dokup.mediastoresample.databinding.FragmentFirstBinding
import dev.dokup.mediastoresample.viewmodel.ImageGridViewModel

class ImageGridFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private val viewModel = ImageGridViewModel()
    private val adapter = GridAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        binding.gridRecycler.adapter = adapter
        binding.gridRecycler.layoutManager = GridLayoutManager(requireContext(), 3)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.imageList.observe(
            viewLifecycleOwner,
            Observer {
                adapter.submitList(it)
            }
        )

        binding.buttonLoadImages.setOnClickListener {
            viewModel.loadImages(requireContext())
        }

        adapter.onItemClickListener = { entity, _ ->
            navigateToImageCrop(entity.uri)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun navigateToImageCrop(uri: Uri) {
        val action = ImageGridFragmentDirections.actionGridToCrop(uri)
        findNavController().navigate(action)
    }
}

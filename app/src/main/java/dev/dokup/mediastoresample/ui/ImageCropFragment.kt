package dev.dokup.mediastoresample.ui

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dev.dokup.mediastoresample.R
import dev.dokup.mediastoresample.databinding.FragmentSecondBinding
import dev.dokup.mediastoresample.viewmodel.ImageCropViewModel

class ImageCropFragment : Fragment() {

    companion object {
        private const val ASPECT_WIDTH = 1
        private const val ASPECT_HEIGHT = 1
    }

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private val args: ImageCropFragmentArgs by navArgs()
    private val uri by lazy { args.uri }

    private val viewModel = ImageCropViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.bitmap.observe(
            viewLifecycleOwner,
            Observer { bitmap ->
                bitmap?.let {
                    binding.cropImageView.setImageBitmap(it)
                    binding.cropImageView.setAspectRatio(ASPECT_WIDTH, ASPECT_HEIGHT)
                }
            }
        )

        viewModel.croppedUri.observe(
            viewLifecycleOwner,
            Observer { uri ->
                if (uri == Uri.EMPTY) return@Observer
                findNavController().navigate(R.id.action_grid_pop)
            }
        )

        viewModel.isLoading.observe(
            viewLifecycleOwner,
            Observer { isLoading ->
                binding.isShowingProgress = isLoading
            }
        )

        viewModel.setCropSpec(requireContext(), uri)

        binding.cropButton.setOnClickListener {
            binding.cropImageView.croppedImage?.let { bitmap ->
                viewModel.saveCroppedBitmap(requireContext(), bitmap)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

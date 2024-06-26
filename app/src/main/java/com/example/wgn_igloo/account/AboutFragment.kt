package com.example.wgn_igloo.account

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.wgn_igloo.R
import com.example.wgn_igloo.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {

    // Binding to xml layout
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    // Toolbar initialization
    private lateinit var toolbarAbout: Toolbar
    private lateinit var toolbarAboutTitle: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup toolbar
        toolbarAbout = binding.toolbarAbout
        toolbarAboutTitle = binding.toolbarAboutTitle
        updateToolbar()
    }

    // Update toolbar with back button and title
    private fun updateToolbar() {
        toolbarAbout.navigationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.back_icon)
        toolbarAbout.setNavigationOnClickListener { activity?.onBackPressed() }
        toolbarAboutTitle.text = "About Us"

    }

    // Cleanup binding when view is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

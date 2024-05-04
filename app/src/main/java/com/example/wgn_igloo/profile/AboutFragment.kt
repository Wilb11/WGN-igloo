package com.example.wgn_igloo.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.wgn_igloo.R
import com.example.wgn_igloo.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!
    private lateinit var toolbarAbout: Toolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbarAbout = binding.toolbarAbout
        updateToolbar()
    }

    private fun updateToolbar() {
        toolbarAbout.navigationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.back_icon)
        toolbarAbout.setNavigationOnClickListener { activity?.onBackPressed() }

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

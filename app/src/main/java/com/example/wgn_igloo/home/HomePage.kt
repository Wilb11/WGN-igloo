package com.example.wgn_igloo.home

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.wgn_igloo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class HomePage : Fragment() {

    // RecyclerView and Carousel
    private lateinit var recyclerView: RecyclerView
    private lateinit var carouselAdapter: CarouselAdapter
    private lateinit var itemList: MutableList<CarouselAdapter.ItemData>
    private lateinit var carouselViewModel: CarouselViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        carouselViewModel = ViewModelProvider(requireActivity()).get(CarouselViewModel::class.java)
        setupCarousel(view)
        setupAddButton(view)
        updateDocumentIds()
    }
    private fun updateDocumentIds() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()
        val groceryItemsPath = firestore.collection("/users/$userId/groceryItems")

        groceryItemsPath.get().addOnSuccessListener { snapshot ->
            if (snapshot.documents.isEmpty()) {
                println("No grocery items found.")
                return@addOnSuccessListener
            }
            snapshot.documents.forEach { document ->
                val actualDocumentId = document.id
                val storedDocumentId = document.getString("documentId") ?: ""
                if (storedDocumentId != actualDocumentId) {
                    groceryItemsPath.document(actualDocumentId).update("documentId", actualDocumentId)
                        .addOnSuccessListener {
                            println("Document ID updated successfully for item: ${document.getString("name")}")
                        }
                        .addOnFailureListener { e ->
                            println("Error updating document ID: ${e.message}")
                        }
                }
            }
        }.addOnFailureListener { e ->
            println("Error retrieving grocery items: ${e.message}")
        }
    }

    interface OnCategorySelectedListener {
        fun onCategorySelected(category: String)
    }
    var categoryListener: OnCategorySelectedListener? = null


    // Setup carousel
    private fun setupCarousel(view: View) {
        // Set up RecyclerView, LinearLayoutManager, Adapter, and LinearSnapHelper as before
        recyclerView = view.findViewById(R.id.carousel)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Preset list of categories with their specified icons
        itemList = listOf(
            CarouselAdapter.ItemData(R.drawable.all, "All"),
            CarouselAdapter.ItemData(R.drawable.condiments, "Condiments"),
            CarouselAdapter.ItemData(R.drawable.dairy, "Dairy"),
            CarouselAdapter.ItemData(R.drawable.drinks, "Drinks"),
            CarouselAdapter.ItemData(R.drawable.freezer, "Freezer"),
            CarouselAdapter.ItemData(R.drawable.meat, "Meats"),
            CarouselAdapter.ItemData(R.drawable.produce, "Produce"),
            CarouselAdapter.ItemData(R.drawable.other, "Other")
        ).toMutableList()

        // Set initial position in carousel to "All" category
        val initialSelectedPosition = itemList.indexOfFirst { it.text == "All" }

        // Setup carousel adapter
        carouselAdapter = CarouselAdapter(itemList, requireContext(), object : CarouselAdapter.OnItemClickListener {
            override fun onItemClicked(position: Int, category: String) {
                recyclerView.smoothScrollToPosition(position)
                categoryListener?.onCategorySelected(category)
            }
        }, initialSelectedPosition)
        recyclerView.adapter = carouselAdapter

        // Attach a LinearSnapHelper to the RecyclerView to ensure items snap into place
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        // Adjust the RecyclerView scroll position so the selected item is centered
        recyclerView.post {
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(initialSelectedPosition, recyclerView.width / 2 - view.width / 2)
        }
    }

    // Setup add button
    private fun setupAddButton(view: View) {
        val addButton: Button = view.findViewById(R.id.add_button)
        addButton.bringToFront()
        addButton.setOnClickListener { v ->

            // Setup popup menu
            val newContext = ContextThemeWrapper(activity, R.style.CustomPopupMenu)
            val popup = PopupMenu(newContext, v)
            popup.menuInflater.inflate(R.menu.add_popup_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.add_manually -> {

                        // Inflate add new items form
                        val formFragment = NewItemsFormFragment()
                        requireActivity().supportFragmentManager.beginTransaction().replace(R.id.fragment_container, formFragment)
                            .addToBackStack(null)
                            .commit()
                        true
                    }
                    R.id.add_barcode -> {

                        // inflate barcode fragment
                        val barcodeFragment = BarcodeScannerFragment()
                        requireActivity().supportFragmentManager.beginTransaction().replace(R.id.fragment_container, barcodeFragment)
                            .addToBackStack(null)
                            .commit()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
}
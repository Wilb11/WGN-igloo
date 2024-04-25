package com.example.wgn_igloo.home

import android.R
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.wgn_igloo.database.FirestoreHelper
import com.example.wgn_igloo.grocery.GroceryItem
import com.example.wgn_igloo.databinding.FragmentNewItemsFormBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


private const val TAG = "NewItemsForm"
private const val API_KEY = "LLIyBbVQ7WJgTsWITh4TwWNHDBojLnJG2ypcXWAg"

class NewItemsFormFragment : Fragment() {

    private var _binding: FragmentNewItemsFormBinding? = null
    private val binding get() = _binding!!
    var message: String? = null
    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var auth: FirebaseAuth
//    private lateinit var firestore: FirebaseFirestore
    private val firestore by lazy { FirebaseFirestore.getInstance() }
//    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var submitButton: Button
    // input item for the form
    private lateinit var itemInput: EditText
    private lateinit var expirationDateInput: EditText
    private lateinit var quantityInput: EditText
    private lateinit var categoryInput: Spinner
    private lateinit var sharedWithInput: Spinner
    // Hard coded list
    private val categoryList = arrayOf("choose an option", "Meat", "Vegetable", "Dairy", "Fruits", "Carbohydrate")
//    private val sharedWithList = arrayOf("choose an option", "Wilbert", "Gary", "Nicole", "Rhett")

    // Default list with a placeholder for choosing an option
    private var sharedWithList = arrayOf("No one")

    companion object {
        private const val EXTRA_MESSAGE = "EXTRA_MESSAGE"

        fun newInstance(message: String): NewItemsFormFragment {
            val fragment = NewItemsFormFragment()
            val args = Bundle()
            args.putString(EXTRA_MESSAGE, message)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestoreHelper = FirestoreHelper(requireContext())
        arguments?.let {
            message = it.getString(EXTRA_MESSAGE)
            Log.d(TAG, "Testing to see if the data went through: $message")
        }
        fetchFriendsAndUpdateSpinner()
    }
    private fun fetchFriendsAndUpdateSpinner() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId).collection("friends")
                .get()
                .addOnSuccessListener { documents ->
                    val friendsUsernames = mutableListOf("choose an option")
                    for (document in documents) {
                        document.getString("username")?.let {
                            friendsUsernames.add(it)
                        }
                    }
                    sharedWithList = friendsUsernames.toTypedArray()
                    updateSharedWithSpinner()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error fetching friends", exception)
                }
        } else {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSharedWithSpinner() {
        val roommateAdapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_item,
            sharedWithList
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        sharedWithInput.adapter = roommateAdapter
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNewItemsFormBinding.inflate(inflater, container, false)
        val view = binding.root

        setupViews()
        setupDatePicker()
        setupSpinnerCategory()
        setupSpinnerRoomate()
//        fetchFriendsAndUpdateSpinner()


        submitButton.setOnClickListener {
            if (validateInputs()) {
                submitGroceryItem()
//                navigateBack()
            } else {
                Toast.makeText(context, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun setupViews() {
        submitButton = binding.submitButton
        itemInput = binding.itemInput
        categoryInput = binding.categoryInput
        expirationDateInput = binding.expirationInput
        quantityInput = binding.quantityInput
        sharedWithInput = binding.sharedWithInput
    }

    private fun validateInputs(): Boolean {
        return itemInput.text.isNotEmpty() &&
                quantityInput.text.isNotEmpty() &&
                expirationDateInput.text.isNotEmpty() &&
                categoryInput.selectedItemPosition != 0
    }

    private fun setupSpinnerRoomate() {
        val roommateAdapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_item,
            sharedWithList  // Correct list for roommates
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        sharedWithInput.adapter = roommateAdapter  // Use the correct Spinner
    }


    private fun setupSpinnerCategory() {
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_item,
            categoryList  // Use the correct list here
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        categoryInput.adapter = categoryAdapter  // Correct Spinner
    }



    private fun submitGroceryItem() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        if (userUid == null) {
            Toast.makeText(context, "User is not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val category = categoryInput.selectedItem.toString().takeIf { it != "choose an option" } ?: return
        val name = itemInput.text.toString()
        val sharedWithInput = sharedWithInput.selectedItem.toString().takeIf { it != "choose an option" } ?: return
        val quantity = quantityInput.text.toString().toIntOrNull() ?: return
        val expirationDate = convertStringToTimestamp(expirationDateInput.text.toString())
        // Collect other inputs similarly

        val groceryItem = GroceryItem(
            category = category,
            expirationDate = expirationDate,
            dateBought = Timestamp.now(), // Assuming the current timestamp as dateBought
            name = name,
            quantity = quantity,
            sharedWith = sharedWithInput,
            status = true // Assuming a new item is always active, adjust based on your logic
        )

        // Now push this groceryItem to Firestore
        addGroceryItemForUser(FirebaseAuth.getInstance().currentUser?.uid ?: return, groceryItem)
    }

    // You might already have this method in the InventoryDisplayFragment. You can move it to a common utility class or directly use it here.
    private fun addGroceryItemForUser(uid: String, groceryItem: GroceryItem) {
        firestoreHelper.addGroceryItem(uid, groceryItem, onSuccess = {
            // Navigate back to InventoryDisplayFragment upon success
            Toast.makeText(context, "Item added successfully", Toast.LENGTH_SHORT).show()
//            requireActivity().supportFragmentManager.popBackStack()
//            navigateBack()
        }, onFailure = { e ->
            Log.e("NewItemsFormFragment", "Failed to add item: ", e)
            Toast.makeText(context, "Failed to add item", Toast.LENGTH_SHORT).show()
        })
    }

//    private fun navigateBack() {
//        Log.d(TAG, "Back stack entry count: ${requireActivity().supportFragmentManager.backStackEntryCount}")
//        if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
//            requireActivity().supportFragmentManager.popBackStack()
//        } else {
//            Log.w(TAG, "No back stack entry to pop.")
//        }
//    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateLabel(calendar)
        }

        // Use an onFocusChangeListener to show the DatePicker when the EditText gains focus
        expirationDateInput.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                DatePickerDialog(
                    requireContext(), datePickerListener, calendar
                        .get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).also {
                    it.show()
                    view.clearFocus() // Optional: Clear focus after showing the dialog to prevent it from showing again on back press or navigation
                }
            }
        }

        // It might still be useful to keep the OnClickListener for users who might re-click the EditText after losing focus
        expirationDateInput.setOnClickListener {
            DatePickerDialog(
                requireContext(), datePickerListener, calendar
                    .get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateLabel(calendar: Calendar) {
        // Define your desired date format
        val dateFormat = "MM/dd/yyyy" // For example, "MM/dd/yyyy"
        val sdf = SimpleDateFormat(dateFormat, Locale.US)
        expirationDateInput.setText(sdf.format(calendar.time))
    }

    private fun convertStringToTimestamp(dateStr: String): Timestamp {
        val format = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        return try {
            val parsedDate = format.parse(dateStr) ?: Date()
            Timestamp(parsedDate)
        } catch (e: ParseException) {
            Log.e("NewItemsFormFragment", "Failed to parse date: ", e)
            Timestamp.now() // Return current time if parsing fails
        }
    }

}
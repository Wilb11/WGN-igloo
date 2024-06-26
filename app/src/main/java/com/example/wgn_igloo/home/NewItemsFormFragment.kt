package com.example.wgn_igloo.home

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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.wgn_igloo.database.FirestoreHelper
import com.example.wgn_igloo.databinding.FragmentNewItemsFormBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.wgn_igloo.R


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

    private val categoryList = arrayOf("Choose an option", "Condiments", "Dairy", "Drinks", "Freezer", "Meats", "Produce", "Other" )

    private var sharedWithList = arrayOf("No one")

    private lateinit var toolbarAddItem: Toolbar
    private lateinit var toolbarAddItemTitle: TextView
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

    // Fetches friends and updates the spinner for sharing with friends
    private fun fetchFriendsAndUpdateSpinner() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId).collection("friends")
                .get()
                .addOnSuccessListener { documents ->
                    val friendsUsernames = mutableListOf("Choose an option", "None")
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

    // Updates the spinner for choosing whom to share the item with
    private fun updateSharedWithSpinner() {
        val roommateAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            sharedWithList
        ).apply {
            setDropDownViewResource(R.layout.spinner_item)
        }
        sharedWithInput.adapter = roommateAdapter
    }

    // Setup the UI views, pickers, and submit button actions
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
        toolbarAddItem = binding.toolbarAddItem
        toolbarAddItemTitle = binding.toolbarAddItemTitle
        binding.itemInput.setText(message)
        updateToolbar()

        submitButton.setOnClickListener {
            if (validateInputs()) {
                submitGroceryItem()
            } else {
                Toast.makeText(context, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun updateToolbar() {
        toolbarAddItem.navigationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.back_icon)
        toolbarAddItem.setNavigationOnClickListener { activity?.onBackPressed() }
        toolbarAddItemTitle.text = "Add New Item"
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
            R.layout.spinner_item,
            sharedWithList
        ).apply {
            setDropDownViewResource(R.layout.spinner_item)
        }
        sharedWithInput.adapter = roommateAdapter
    }


    private fun setupSpinnerCategory() {
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            categoryList
        ).apply {
            setDropDownViewResource(R.layout.spinner_item)
        }
        categoryInput.adapter = categoryAdapter
    }

    private fun submitGroceryItem() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(context, "User is not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Ensure all fields are properly filled
        val name = itemInput.text.toString()
        val category = categoryInput.selectedItem.toString().takeIf { it != "Choose an option" } ?: return
        val sharedWithOption = sharedWithInput.selectedItem.toString()
        val quantity = quantityInput.text.toString().toIntOrNull() ?: return
        val expirationDate = convertStringToTimestamp(expirationDateInput.text.toString())

        // Create grocery item object
        val groceryItem = GroceryItem(
            category = category,
            expirationDate = expirationDate,
            dateBought = Timestamp.now(),
            name = name,
            quantity = quantity,
            sharedWith = if (sharedWithOption == "None" || sharedWithOption == "Choose an option") "" else sharedWithOption,
            status = true,
            isOwnedByUser = true
        )

        // Check if the item needs to be shared
        if (sharedWithOption == "None" || sharedWithOption == "Choose an option") {
            firestoreHelper.addGroceryItem(userUid, groceryItem, onSuccess = {
                Toast.makeText(context, "Item added successfully", Toast.LENGTH_SHORT).show()
            }, onFailure = { e ->
                Toast.makeText(context, "Failed to add item: ${e.message}", Toast.LENGTH_SHORT).show()
            })
        } else {
            // Find friend's UID based on the name selected in sharedWithInput spinner
            firestore.collection("users").whereEqualTo("username", sharedWithOption)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Toast.makeText(context, "Friend not found: $sharedWithOption", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    val friendUid = documents.documents.first().id
                    firestoreHelper.addGroceryItemToUserAndFriend(userUid, friendUid, groceryItem, onSuccess = {
                        Toast.makeText(context, "Item successfully added to both users", Toast.LENGTH_SHORT).show()
                    }, onFailure = { e ->
                        Toast.makeText(context, "Failed to add item for both users: ${e.message}", Toast.LENGTH_SHORT).show()
                    })
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to find friend: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


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
                    view.clearFocus()
                }
            }
        }

        expirationDateInput.setOnClickListener {
            DatePickerDialog(
                requireContext(), datePickerListener, calendar
                    .get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateLabel(calendar: Calendar) {
        val dateFormat = "MM/dd/yyyy"
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
            Timestamp.now()
        }
    }

}
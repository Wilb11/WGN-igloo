package com.example.wgn_igloo.recipe

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.wgn_igloo.R
import com.example.wgn_igloo.databinding.FragmentRecipeDetailsBinding
import java.util.ArrayList

private const val TAG = "RecipeDetailsFragment"
class RecipeDetailsFragment : Fragment() {
    private var _binding: FragmentRecipeDetailsBinding? = null
    private val binding get() = _binding!!
    private var parsed_instructions: MutableList<String> = mutableListOf()
    private var parsed_ingredients: MutableList<String> = mutableListOf()
    private var parsed_title: String = ""
    private var parsed_dish: String = ""
    private var parsed_cuisine: String = ""
    private var parsed_diet: String = ""
    private var parsed_time: String = ""
    private var parsed_servings: String = ""
    private var parsed_image: String = ""

    private lateinit var toolbarRecipeDetails: Toolbar

    companion object {
        private const val INSTRUCTIONS = "INSTRUCTIONS"
        private const val INGREDIENTS = "INGREDIENTS"
        private const val TITLE = "TITLE"
        private const val DISH = "DISH"
        private const val CUISINE = "CUISINE"
        private const val DIET = "DIET"
        private const val TIME = "TIME"
        private const val SERVINGS = "SERVINGS"
        private const val IMAGE = "IMAGE"

        fun newInstance(
            instructionsMessage: List<String>?, ingredientsMessage: List<String>?,
            titleMessage: String?, dishMessage: String?, cuisineMessage: String?,
            dietMessage: String?, timeMessage: String?, servingsMessage: String?,
            imageMessage: String?): RecipeDetailsFragment {
            val fragment = RecipeDetailsFragment()
            val args = Bundle()


            args.putStringArrayList(INSTRUCTIONS, instructionsMessage as ArrayList<String>?)
            args.putStringArrayList(INGREDIENTS, ingredientsMessage as ArrayList<String>?)
            args.putString(TITLE, titleMessage)
            args.putString(DISH, dishMessage)
            args.putString(CUISINE, cuisineMessage)
            args.putString(DIET, dietMessage)
            args.putString(TIME, timeMessage)
            args.putString(SERVINGS, servingsMessage)
            args.putString(IMAGE, imageMessage)

            // Set the Bundle as the fragment's arguments
            fragment.arguments = args

            return fragment
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecipeDetailsBinding.inflate(inflater, container, false)

        val instructions = arguments?.getStringArrayList(INSTRUCTIONS)!!
        parsed_ingredients = arguments?.getStringArrayList(INGREDIENTS)!!
        parsed_title = arguments?.getString(TITLE)!!
        parsed_dish = arguments?.getString(DISH)!!
        parsed_cuisine = arguments?.getString(CUISINE)!!
        parsed_diet = arguments?.getString(DIET)!!
        parsed_time = arguments?.getString(TIME)!!
        parsed_servings = arguments?.getString(SERVINGS)!!
        parsed_image = arguments?.getString(IMAGE)!!



        Log.d(TAG, "$parsed_ingredients")
        for (lines in instructions){
            val steps = lines.split(".").filter{it.isNotEmpty()}
            for (step in steps){
                parsed_instructions.add("${step.trim()}.")
                Log.d(TAG, step)
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup the adapters and layout managers
        setupRecyclerViews(
            parsed_instructions, parsed_ingredients,
            parsed_title, parsed_dish, parsed_cuisine,
            parsed_diet, parsed_time, parsed_servings, parsed_image)

        toolbarRecipeDetails = binding.toolbarRecipeDetails
        updateToolbar()
    }

    private fun updateToolbar() {
        toolbarRecipeDetails.navigationIcon = ContextCompat.getDrawable(requireContext(), com.example.wgn_igloo.R.drawable.back_icon)
        toolbarRecipeDetails.setNavigationOnClickListener { activity?.onBackPressed() }

    }

    private fun setupRecyclerViews(
        instructions: List<String>, ingredients: List<String>, title: String,
        dish: String, cuisine: String, diet: String, time: String, servings: String,
        imageURL: String
    ) {
        binding.recipePageName.text = title
        binding.recipeDishType.text = dish.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        if (cuisine == "") {
            binding.recipeCuisine.text = "N/A"
        }
        else {
            binding.recipeCuisine.text = cuisine
        }

        if (diet == "") {
            binding.recipeDiet.text = "N/A"
        }
        else {
            binding.recipeDiet.text = diet
        }

        binding.recipeTotalTimeInfo.text = time + " mins"
        binding.recipeServingSize.text = servings
        Log.d(TAG,"Image: $imageURL")
        Glide.with(this)
            .load(imageURL)
            .placeholder(R.drawable.salmon)  // Shows a placeholder image while loading.
            .error(R.drawable.salad)        // Shows an error image if the URL load fails.
            .into(binding.recipePageImage)

        // Setting up the Ingredients RecyclerView
        binding.ingredientsList.adapter = IngredientsAdapter(ingredients)
        binding.ingredientsList.layoutManager = LinearLayoutManager(context)

        // Setting up the Instructions RecyclerView
        binding.instructionsList.adapter = InstructionsAdapter(instructions)
        binding.instructionsList.layoutManager = LinearLayoutManager(context)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Clean up binding reference
    }
}
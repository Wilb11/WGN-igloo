package com.example.wgn_igloo

import com.example.wgn_igloo.GroceryItem
import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.DocumentReference


class FirestoreHelper(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "FirestoreHelper"
    }

    fun addUser(user: User) {
        db.collection("users").document(user.uid).set(user)
            .addOnSuccessListener { Log.d(TAG, "User added successfully") }
            .addOnFailureListener { e -> Log.w(TAG, "Error adding user", e) }
    }

    fun addGroceryItem(uid: String, item: GroceryItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(uid).collection("groceryItems").add(item)
            .addOnSuccessListener {
                Log.d(TAG, "Item added successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding item", e)
                onFailure(e)
            }
    }


    fun addSavedRecipe(uid: String, recipe: SavedRecipe) {
        db.collection("users").document(uid)
            .collection("savedRecipes").document(recipe.recipeName).set(recipe)
            .addOnSuccessListener {
                Log.d(TAG, "Saved recipe added successfully")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding saved recipe", e)
            }
    }

    fun addShoppingListItem(uid: String, item: ShoppingListItem) {
        db.collection("users").document(uid).collection("shoppingList").document(item.name).set(item)
            .addOnSuccessListener {
                Log.d(TAG, "Shopping list item added successfully")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding shopping list item", e)
            }
    }

    fun moveItemToInventory(uid: String, item: ShoppingListItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Remove from shopping list
        db.collection("users").document(uid).collection("shoppingList").document(item.name).delete()
            .addOnSuccessListener {
                // Add to grocery items with a unique name
                val groceryItem = mapShoppingListItemToGroceryItem(item)
//                db.collection("users").document(uid).collection("groceryItems").add(item)
                db.collection("users").document(uid).collection("groceryItems").add(groceryItem)
//                db.collection("users").document(uid).collection("groceryItems").document(groceryItem.name).set(groceryItem)
                    .addOnSuccessListener {
                        Log.d(TAG, "Item moved to grocery items successfully")
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error adding item to grocery items", e)
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error removing item from shopping list", e)
                onFailure(e)
            }
    }

    private fun mapShoppingListItemToGroceryItem(item: ShoppingListItem): GroceryItem {
        // Generate a unique identifier for the grocery item - not needed
        val uniqueName = "${item.name}_${System.currentTimeMillis()}"
        return GroceryItem(
            category = item.category,
            expirationDate = Timestamp.now(),  // Assuming current time as expiration date
            dateBought = item.lastPurchased,
            name = item.name,  // Use the generated unique name
            quantity = 1,  // Default quantity
            sharedWith = "",  // No shared user by default
            status = true  // Assuming the item is active/available
        )
    }

    fun shareGroceryItem(userId: String, itemId: String, friendUserId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val itemRef = db.collection("users").document(userId).collection("groceryItems").document(itemId)
        itemRef.update("sharedWith", friendUserId)
            .addOnSuccessListener {
                Log.d(TAG, "Grocery item shared successfully")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error sharing grocery item", exception)
                onFailure(exception)
            }
    }

    fun getUserByEmailOrUid(identifier: String, onSuccess: (User) -> Unit, onFailure: (Exception) -> Unit) {
        val query = if (identifier.contains("@")) {
            db.collection("users").whereEqualTo("email", identifier)
        } else {
            db.collection("users").whereEqualTo("uid", identifier)
        }

        query.get().addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                val user = documents.first().toObject(User::class.java)
                onSuccess(user)
            } else {
                onFailure(Exception("No user found with that identifier."))
            }
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }

    fun getUserByUid(uid: String, onSuccess: (User) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    onSuccess(documentSnapshot.toObject(User::class.java)!!)
                } else {
                    onFailure(Exception("No user found with UID: $uid"))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun addFriend(currentUserId: String, friendUserId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Prepare friend data for current user adding a friend
        val friendDataForCurrentUser = mapOf(
            "friendUid" to friendUserId,
            "friendSince" to Timestamp.now()  // Current time as timestamp
        )

        // Prepare friend data for the friend adding the current user
        val friendDataForFriendUser = mapOf(
            "friendUid" to currentUserId,
            "friendSince" to Timestamp.now()  // Current time as timestamp
        )

        // Reference to the current user's friends collection
        val currentUserFriendRef = db.collection("users").document(currentUserId).collection("friends").document(friendUserId)
        // Reference to the friend's friends collection
        val friendUserFriendRef = db.collection("users").document(friendUserId).collection("friends").document(currentUserId)

        // Start a batch to perform both operations together
        val batch = db.batch()
        batch.set(currentUserFriendRef, friendDataForCurrentUser)
        batch.set(friendUserFriendRef, friendDataForFriendUser)

        // Commit the batch
        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "Friendship established successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error establishing friendship", e)
                onFailure(e)
            }
    }


}

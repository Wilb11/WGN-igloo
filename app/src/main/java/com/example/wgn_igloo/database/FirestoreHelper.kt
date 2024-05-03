package com.example.wgn_igloo.database

import android.content.Context
import android.util.Log
import com.example.wgn_igloo.home.GroceryItem
import com.example.wgn_igloo.grocery.ShoppingListItem
import com.example.wgn_igloo.home.InventoryDisplayFragment
import com.example.wgn_igloo.recipe.SavedRecipe
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException


class FirestoreHelper(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "FirestoreHelper"
    }

    fun addUser(user: User) {
        val userWithUsername = if (user.username.isEmpty()) user.copy(username = user.uid) else user
        db.collection("users").document(user.uid).set(userWithUsername)
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

    fun getGroceryItems(userId: String, onSuccess: (List<GroceryItem>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(userId).collection("groceryItems")
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.toObjects(GroceryItem::class.java)
                onSuccess(items)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
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
            status = true,  // Assuming the item is active/available
            isOwnedByUser = true
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


    fun getUser(userId: String, onSuccess: (User) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                user?.let { onSuccess(it) }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting user", e)
                onFailure(e)
            }
    }

//    fun updateUsername(uid: String, newUsername: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
//        db.collection("users").document(uid).update("uid", newUsername)
//            .addOnSuccessListener {
//                Log.d(TAG, "Username updated successfully")
//                onSuccess()
//            }
//            .addOnFailureListener { e ->
//                Log.e(TAG, "Error updating username", e)
//                onFailure(e)
//            }
//    }

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

    fun addFriend(currentUserId: String, friendUsername: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").whereEqualTo("username", friendUsername).limit(1).get()
            .addOnSuccessListener { querySnapshot ->
                val friendUser = querySnapshot.documents.firstOrNull()?.toObject(User::class.java)
                if (friendUser != null) {
                    getCurrentUserEmail { currentUserEmail ->
                        getCurrentUsername { currentUsername ->
                            val friendDataForCurrentUser = mapOf(
                                "email" to friendUser.email,
                                "uid" to friendUser.uid,
                                "username" to friendUser.username,
                                "friendSince" to Timestamp.now()
                            )

                            val currentUserDataForFriend = mapOf(
                                "email" to currentUserEmail,
                                "uid" to currentUserId,
                                "username" to currentUsername,
                                "friendSince" to Timestamp.now()
                            )

                            val batch = db.batch()
                            batch.set(db.collection("users").document(currentUserId).collection("friends").document(friendUser.uid), friendDataForCurrentUser)
                            batch.set(db.collection("users").document(friendUser.uid).collection("friends").document(currentUserId), currentUserDataForFriend)

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
                    } else {
                        onFailure(Exception("Friend not found with username: $friendUsername"))
                    }
                }
                    .addOnFailureListener { e ->
                        onFailure(e)
                    }
            }
        fun getCurrentUserEmail(onResult: (String) -> Unit) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
        onResult(email)
    }

    fun getCurrentUsername(onResult: (String) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    val username = document.getString("username") ?: ""
                    onResult(username)
                }
                .addOnFailureListener {
                    Log.e(TAG, "Error fetching username")
                    onResult("")  // Handle the error case by returning an empty string
                }
        } else {
            onResult("")  // Return empty if uid is null
        }
    }

    fun updateUsername(uid: String, newUsername: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Step 1: Update the user's own document
        db.collection("users").document(uid).update("username", newUsername)
            .addOnSuccessListener {
                Log.d(TAG, "User's username updated successfully.")
                // Step 2: Update all references in friends' documents
                updateFriendsWithNewUsername(uid, newUsername, onSuccess, onFailure)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error updating user's username: ", exception)
                onFailure(exception)
            }
    }

    private fun updateFriendsWithNewUsername(userId: String, newUsername: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Log the operation to debug it
        Log.d(TAG, "Attempting to update friends with new username for user ID: $userId")

        db.collectionGroup("friends")
            .whereEqualTo("uid", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d(TAG, "No documents found to update.")
                    onSuccess()  // If no documents need updating, call onSuccess immediately
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                documents.forEach { documentSnapshot ->
                    val friendRef = documentSnapshot.reference
                    Log.d(TAG, "Updating username in document: ${friendRef.path}")
                    batch.update(friendRef, "username", newUsername)
                }

                batch.commit()
                    .addOnSuccessListener {
                        Log.d(TAG, "All friend references updated successfully.")
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error updating friend references: ", exception)
                        onFailure(exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to find friends for username update: ", exception)
                onFailure(exception)
            }
    }

//    fun updateUsername(uid: String, newUsername: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
//        db.collection("users").document(uid).update("username", newUsername)
//            .addOnSuccessListener {
//                Log.d(TAG, "User's username updated successfully.")
//                updateFriendsWithNewUsername(uid, newUsername, onSuccess, onFailure)
//            }
//            .addOnFailureListener { exception ->
//                Log.e(TAG, "Error updating user's username: ", exception)
//                if (exception is FirebaseFirestoreException && exception.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
//                    Log.e(TAG, "Missing index for query. Consider adding it via Firebase console.")
//                    // Provide the direct link to create the index if available or suggest checking Firestore documentation
//                }
//                onFailure(exception)
//            }
//    }
//
//    private fun updateFriendsWithNewUsername(userId: String, newUsername: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
//        db.collectionGroup("friends")
//            .whereEqualTo("uid", userId)
//            .get()
//            .addOnSuccessListener { documents ->
//                if (documents.isEmpty) {
//                    Log.d(TAG, "No documents found to update.")
//                    onSuccess()
//                    return@addOnSuccessListener
//                }
//
//                val batch = db.batch()
//                documents.forEach { documentSnapshot ->
//                    val friendRef = documentSnapshot.reference
//                    batch.update(friendRef, "username", newUsername)
//                }
//
//                batch.commit()
//                    .addOnSuccessListener {
//                        Log.d(TAG, "All friend references updated successfully.")
//                        onSuccess()
//                    }
//                    .addOnFailureListener { exception ->
//                        Log.e(TAG, "Error updating friend references: ", exception)
//                        onFailure(exception)
//                    }
//            }
//            .addOnFailureListener { exception ->
//                Log.e(TAG, "Failed to find friends for username update: ", exception)
//                onFailure(exception)
//            }
//    }
}

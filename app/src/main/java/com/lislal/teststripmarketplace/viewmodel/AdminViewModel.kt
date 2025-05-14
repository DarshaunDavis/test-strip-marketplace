package com.lislal.teststripmarketplace.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow

data class AppUser(
    val userId: String = "",
    val username: String = "",
    val role: String = "",
    val isSuspended: Boolean = false,
    val isBanned: Boolean = false
)

class AdminViewModel : ViewModel() {
    private val db = FirebaseDatabase.getInstance().getReference("users")

    /** Real-time stream of all users */
    fun usersFlow(): Flow<List<AppUser>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val userId = child.key ?: return@mapNotNull null
                    val username = child.child("username").getValue(String::class.java).orEmpty()
                    val role     = child.child("role").getValue(String::class.java).orEmpty()
                    val suspended = child.child("isSuspended").getValue(Boolean::class.java) ?: false
                    val banned    = child.child("isBannedSuspended").getValue(Boolean::class.java) ?: false
                    AppUser(userId, username, role, suspended, banned)
                }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        db.addValueEventListener(listener)
        awaitClose { db.removeEventListener(listener) }
    }

    /** Toggle a boolean field under /users/{userId}/{field} */
    fun updateUserFlag(userId: String, field: String, value: Boolean, onComplete: (Boolean) -> Unit) {
        db.child(userId)
            .child(field)
            .setValue(value)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}

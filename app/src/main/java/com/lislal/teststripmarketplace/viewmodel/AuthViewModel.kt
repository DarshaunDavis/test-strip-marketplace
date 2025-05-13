package com.lislal.teststripmarketplace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _userEmail = MutableStateFlow(auth.currentUser?.email ?: "")
    val userEmail: StateFlow<String> = _userEmail

    private val _userRole = MutableStateFlow("guest")
    val userRole: StateFlow<String> = _userRole

    init {
        fetchUserRole()
    }

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _isLoggedIn.value = true
                _userEmail.value = it.user?.email ?: ""
                fetchUserRole()
                onResult(true)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun logout() {
        auth.signOut()
        _isLoggedIn.value = false
        _userEmail.value = ""
        _userRole.value = "guest"
    }

    private fun fetchUserRole() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            database.getReference("users/$uid/role")
                .get()
                .addOnSuccessListener { snapshot ->
                    val role = snapshot.getValue(String::class.java)
                    _userRole.value = role ?: "user"
                }
                .addOnFailureListener {
                    _userRole.value = "user"
                }
        }
    }
}

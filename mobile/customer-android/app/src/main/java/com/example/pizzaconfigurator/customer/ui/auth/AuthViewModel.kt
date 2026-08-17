package com.example.pizzaconfigurator.customer.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pizzaconfigurator.customer.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val submitting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    val authState = authRepository.authState

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(submitting = true)
            _uiState.value = try {
                authRepository.login(email, password)
                AuthUiState(success = true)
            } catch (e: Exception) {
                AuthUiState(error = e.message)
            }
        }
    }

    fun register(name: String, email: String, phoneNumber: String?, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(submitting = true)
            _uiState.value = try {
                authRepository.register(name, email, phoneNumber, password)
                AuthUiState(success = true)
            } catch (e: Exception) {
                AuthUiState(error = e.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    companion object {
        fun factory(authRepository: AuthRepository) = viewModelFactory {
            initializer { AuthViewModel(authRepository) }
        }
    }
}

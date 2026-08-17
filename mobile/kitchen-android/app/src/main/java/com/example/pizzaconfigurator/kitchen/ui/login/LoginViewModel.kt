package com.example.pizzaconfigurator.kitchen.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pizzaconfigurator.kitchen.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(val submitting: Boolean = false, val error: String? = null)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    val authState = authRepository.authState

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(submitting = true)
            _uiState.value = try {
                authRepository.login(username, password)
                LoginUiState()
            } catch (e: Exception) {
                LoginUiState(error = e.message)
            }
        }
    }

    companion object {
        fun factory(authRepository: AuthRepository) = viewModelFactory {
            initializer { LoginViewModel(authRepository) }
        }
    }
}

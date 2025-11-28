package k.nutriguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import k.nutriguard.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    data class UiState(
        val email: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val successMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun onEmailChanged(v: String) {
        _uiState.update { it.copy(email = v.trim(), errorMessage = null, successMessage = null) }
    }

    fun sendReset() {
        val email = _uiState.value.email
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                authRepo.sendPasswordReset(email)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Check your email for reset instructions"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to send reset email"
                    )
                }
            }
        }
    }
}

package k.nutriguard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import k.nutriguard.repository.DBModule
import k.nutriguard.db.DBInterface
import k.nutriguard.domain.UserProfile
import k.nutriguard.repository.AuthRepository
import kotlinx.coroutines.launch

class SignInViewModel(
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    data class UiState(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val signedInUser: UserProfile? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun onEmailChanged(newEmail: String) {
        _uiState.update { it.copy(email = newEmail.trim(), errorMessage = null) }
    }

    fun onPasswordChanged(newPassword: String) {
        _uiState.update { it.copy(password = newPassword, errorMessage = null) }
    }

    fun signIn() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (email.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Email and password are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val user = authRepo.signIn(email, password)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        signedInUser = user,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to sign in, invalid email or password!"
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.value = UiState()
    }
}

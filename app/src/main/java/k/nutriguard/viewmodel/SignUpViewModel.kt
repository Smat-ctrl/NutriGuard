package k.nutriguard.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import k.nutriguard.domain.UserProfile
import k.nutriguard.repository.AuthRepository
import k.nutriguard.repository.UsernameAlreadyTakenException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    data class UiState(
        val email: String = "",
        val password: String = "",
        val username: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val createdUser: UserProfile? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun onEmailChanged(v: String)      = _uiState.update { it.copy(email = v.trim(), errorMessage = null) }
    fun onPasswordChanged(v: String)   = _uiState.update { it.copy(password = v, errorMessage = null) }
    fun onUsernameChanged(v: String)   = _uiState.update { it.copy(username = v.trim(), errorMessage = null) }

    fun signUp() {
        val s = _uiState.value
        if (s.email.isBlank() || s.password.isBlank() || s.username.isBlank()) {
            _uiState.update { it.copy(errorMessage = "All fields are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val user = authRepo.signUp(
                    email = s.email,
                    password = s.password,
                    username = s.username
                )
                _uiState.update { it.copy(isLoading = false, createdUser = user) }
            } catch (e: Exception) {
                Log.e("SignUpViewModel", "Sign-up failed", e)

                val raw = e.message ?: ""
                val userFacingMessage =
                    when {
                        e is UsernameAlreadyTakenException ->
                            "That username is already taken. Please choose a different one."

                        // Supabase invalid email format
                        raw.contains("Email address", ignoreCase = true) &&
                                raw.contains("invalid", ignoreCase = true) ->
                            "Please enter a valid email address."

                        // Supabase error when email already exists
                        raw.contains("User already registered", ignoreCase = true) ||
                                raw.contains("already registered", ignoreCase = true) ->
                            "An account with this email already exists. Try signing in instead."

                        // Username already exists
                        raw.contains("duplicate key value", ignoreCase = true) &&
                                raw.contains("UserProfile_pkey", ignoreCase = true) ->
                            "That username is already taken. Please choose a different one."

                        // Password too weak / not meeting requirements
                        raw.contains("password", ignoreCase = true) &&
                                raw.contains("at least", ignoreCase = true) ->
                            "Password does not meet requirements. Please choose a stronger password."

                        else ->
                            "Failed to create account. Please try again."
                    }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = userFacingMessage
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.value = UiState()
    }
}

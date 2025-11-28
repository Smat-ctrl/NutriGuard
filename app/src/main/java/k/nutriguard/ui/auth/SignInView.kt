package k.nutriguard.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import k.nutriguard.domain.UserProfile
import k.nutriguard.viewmodel.SignInViewModel

// Reuse the same theme colors as before
private val BgSurface   = Color(0xFF0E0E0E)
private val CardSurface = Color(0xFF1C1C1C)
private val Divider     = Color(0xFF2A2A2A)
private val NutriYellow = Color(0xFFFFD200)

@Composable
fun SignInView(
    viewModel: SignInViewModel,
    onSignedIn: (UserProfile) -> Unit,
    onNavigateToSignUp: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    // When sign-in succeeded -> notify parent with the FULL user
    LaunchedEffect(state.signedInUser) {
        val user = state.signedInUser
        if (user != null) {
            onSignedIn(user)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSurface),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "NutriGuard",
                    color = NutriYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )

                Text(
                    text = "Sign in to your account",
                    color = Color(0xFFDDDDDD),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                // Error message
                state.errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                // Email TextField
                TextField(
                    value = state.email,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    singleLine = true,
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        disabledContainerColor = CardSurface,

                        focusedIndicatorColor = NutriYellow,
                        unfocusedIndicatorColor = Divider,
                        disabledIndicatorColor = Divider,

                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color.Gray,

                        cursorColor = NutriYellow
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Password TextField
                TextField(
                    value = state.password,
                    onValueChange = { viewModel.onPasswordChanged(it) },
                    singleLine = true,
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        disabledContainerColor = CardSurface,

                        focusedIndicatorColor = NutriYellow,
                        unfocusedIndicatorColor = Divider,
                        disabledIndicatorColor = Divider,

                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color.Gray,

                        cursorColor = NutriYellow
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Login button
                Button(
                    onClick = { viewModel.signIn() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !state.isLoading &&
                            state.email.isNotBlank() &&
                            state.password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NutriYellow,
                        contentColor = Color.Black,
                        disabledContainerColor = NutriYellow.copy(alpha = 0.3f),
                        disabledContentColor = Color.Black.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Log in", fontWeight = FontWeight.Bold)
                    }
                }

                // Extra auth actions: Sign up / Forgot password
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onNavigateToSignUp) {
                        Text(
                            text = "Create account",
                            color = NutriYellow,
                            fontSize = 13.sp
                        )
                    }

                    TextButton(onClick = onNavigateToForgotPassword) {
                        Text(
                            text = "Forgot password?",
                            color = Color(0xFFCCCCCC),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

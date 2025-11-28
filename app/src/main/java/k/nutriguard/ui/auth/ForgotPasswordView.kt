package k.nutriguard.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import k.nutriguard.viewmodel.ForgotPasswordViewModel

// Same palette as auth screens
private val BgSurface   = Color(0xFF0E0E0E)
private val CardSurface = Color(0xFF1C1C1C)
private val Divider     = Color(0xFF2A2A2A)
private val NutriYellow = Color(0xFFFFD200)

@Composable
fun ForgotPasswordView(
    viewModel: ForgotPasswordViewModel,
    onBackToSignIn: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

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
                    text = "Reset Password",
                    color = NutriYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Enter the email associated with your account and we'll send you a reset link.",
                    color = Color(0xFFDDDDDD),
                    fontSize = 13.sp,
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

                // Success message
                state.successMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = Color(0xFF4CAF50),
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

                // Send reset link button
                Button(
                    onClick = { viewModel.sendReset() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !state.isLoading && state.email.isNotBlank(),
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
                        Text("Send reset link", fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(onClick = onBackToSignIn) {
                    Text(
                        text = "Back to sign in",
                        color = Color(0xFFCCCCCC),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

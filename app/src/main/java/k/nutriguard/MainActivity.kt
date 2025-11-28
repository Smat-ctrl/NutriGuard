package k.nutriguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import k.nutriguard.domain.UserProfile
import k.nutriguard.ui.auth.ForgotPasswordView
import k.nutriguard.ui.NutriAppRoot
import k.nutriguard.ui.auth.SignUpView
import k.nutriguard.ui.auth.SignInView
import k.nutriguard.viewmodel.ForgotPasswordViewModel
import k.nutriguard.viewmodel.GroupViewModel
import k.nutriguard.viewmodel.InventoryViewModel
import k.nutriguard.viewmodel.ProfileViewModel
import k.nutriguard.viewmodel.SignInViewModel
import k.nutriguard.viewmodel.SignUpViewModel
import k.nutriguard.ui.auth.AuthScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {

                val profileVm: ProfileViewModel = viewModel()
                val groupVm: GroupViewModel = viewModel()
                val inventoryVm: InventoryViewModel = viewModel()

                val signInVm: SignInViewModel = viewModel()
                val signUpVm: SignUpViewModel = viewModel()
                val forgotVm: ForgotPasswordViewModel = viewModel()

                var loggedInUser by remember {
                    mutableStateOf<UserProfile?>(null)
                }

                var authScreen by remember {
                    mutableStateOf(AuthScreen.SignIn)
                }

                if (loggedInUser == null) {
                    when (authScreen) {
                        AuthScreen.SignIn -> SignInView(
                            viewModel = signInVm,
                            onSignedIn = { user ->
                                loggedInUser = user
                            },
                            onNavigateToSignUp = {
                                authScreen = AuthScreen.SignUp
                                signUpVm.reset()
                            },
                            onNavigateToForgotPassword = {
                                authScreen = AuthScreen.ForgotPassword
                                forgotVm // optional: reset, etc.
                            }
                        )

                        AuthScreen.SignUp -> SignUpView(
                            viewModel = signUpVm,
                            onSignedUp = { user ->
                                loggedInUser = user
                            },
                            onNavigateToSignIn = {
                                authScreen = AuthScreen.SignIn
                                signInVm.reset()
                            }
                        )

                        AuthScreen.ForgotPassword -> ForgotPasswordView(
                            viewModel = forgotVm,
                            onBackToSignIn = {
                                authScreen = AuthScreen.SignIn
                            }
                        )
                    }
                } else {
                    NutriAppRoot(
                        profileVm = profileVm,
                        groupVm = groupVm,
                        inventoryVm = inventoryVm,
                        user = loggedInUser!!,
                        onLogout = {
                            loggedInUser = null
                            signInVm.reset()
                            authScreen = AuthScreen.SignIn
                        }
                    )
                }
            }
        }
    }
}

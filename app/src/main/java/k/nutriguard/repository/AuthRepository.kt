package k.nutriguard.repository

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.gotrue.providers.builtin.Email
import k.nutriguard.db.DBInterface
import k.nutriguard.domain.CartModel
import k.nutriguard.domain.UserProfile
import java.util.UUID

class UsernameAlreadyTakenException : Exception("USERNAME_TAKEN")

class AuthRepository(
    private val db: DBInterface = DBModule.db
) {

    private val client = SupabaseProvider.client

    suspend fun signUp(
        email: String,
        password: String,
        username: String
    ): UserProfile {
        val existingProfile = db.getUserBasic(username) //using basic since we dont need populated obj
        if (existingProfile != null) {
            throw UsernameAlreadyTakenException()
        }
        //only create account if the username is not taken

        // 1) Create auth user (email/password)
        val userInfo: UserInfo? = client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }

        // userInfo comes directly from signUpWith
        val authIdStr = userInfo?.id
            ?: throw IllegalStateException("Auth user not found after sign up")

        val authId = UUID.fromString(authIdStr)


        // 2) Create personal cart
        val newCart = CartModel(
            id = UUID.randomUUID(),
            items = mutableListOf()
        )
        val insertedCart = db.createCart(newCart)
            ?: throw IllegalStateException("Failed to create cart")

        // 3) Create userprofile row linked to auth_id
        val newUser = UserProfile(
            id = UUID.randomUUID(),
            authId = authId,
            username = username,
            personalCartId = insertedCart.id
        )

        val created = db.createUser(newUser)
            ?: throw IllegalStateException("Failed to create user profile")

        return created
    }

    suspend fun signIn(
        email: String,
        password: String
    ): UserProfile {
        // 1) GoTrue email/password sign in
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }

        val userInfo = client.auth.currentUserOrNull()
            ?: throw IllegalStateException("Failed to get auth user")

        val authId = UUID.fromString(userInfo.id)

        // 2) Look up userprofile by auth_id
        val user = db.getUserByAuthId(authId)
            ?: throw IllegalStateException("No profile found for this account")

        return user
    }

    suspend fun sendPasswordReset(email: String) {
        client.auth.resetPasswordForEmail(email)
    }

    suspend fun signOut() {
        client.auth.signOut()
    }
}

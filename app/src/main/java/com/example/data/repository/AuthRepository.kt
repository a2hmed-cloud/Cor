package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.core.security.OperationType
import com.example.core.security.handleFirestoreError
import com.example.data.preferences.UserPrefsSnapshot
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "Auth"

class AuthRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager
    ): Boolean {
        if (auth.currentUser != null) return true
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            return false
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        return try {
            val result = credentialManager.getCredential(context, request)
            val cred = result.credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                true
            } else {
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    suspend fun signInInteractive(
        activityContext: Context,
        credentialManager: CredentialManager,
        prefs: UserPrefsSnapshot
    ): SignInOutcome {
        val clientId = try {
            activityContext.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            return SignInOutcome.Error("Google Sign-In configuration missing.")
        }
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
        return try {
            val hostContext = activityContext as? Activity ?: activityContext
            val cred = credentialManager.getCredential(hostContext, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val idTokenCred = GoogleIdTokenCredential.createFrom(cred.data)
                auth.signInWithCredential(GoogleAuthProvider.getCredential(idTokenCred.idToken, null)).await()
                val signedInUser = auth.currentUser
                if (signedInUser != null) {
                    ensureUserProfiles(signedInUser, prefs)
                }
                SignInOutcome.Success
            } else {
                SignInOutcome.Error("Unexpected credential type returned.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            SignInOutcome.Cancelled
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            SignInOutcome.Error(e.localizedMessage ?: "Google Sign-In failed.")
        }
    }

    suspend fun ensureUserProfiles(user: FirebaseUser, prefs: UserPrefsSnapshot): Result<Unit> {
        val uid = user.uid
        return try {
            val privateRef = db.collection("users").document(uid)
            val privateSnap = privateRef.get().await()
            if (!privateSnap.exists()) {
                val privatePayload = buildMap<String, Any> {
                    put("userId", uid)
                    user.email?.takeIf { it.isNotBlank() }?.let { put("email", it.take(200)) }
                    put("preferredLanguage", prefs.language.code)
                    put("themeMode", prefs.themeMode.value)
                    put("aiModel", prefs.aiModel.modelId)
                    put("accountStatus", "active")
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                privateRef.set(privatePayload).await()
            }

            val publicRef = db.collection("public_profiles").document(uid)
            val publicSnap = publicRef.get().await()
            if (!publicSnap.exists()) {
                val rawName = user.displayName?.takeIf { it.isNotBlank() } ?: "Creator"
                val baseUsername = rawName.lowercase()
                    .replace(Regex("[^a-z0-9_]"), "_")
                    .trim('_')
                    .ifBlank { "creator" }
                    .take(18) + "_" + uid.take(4).lowercase()
                val publicPayload = buildMap<String, Any> {
                    put("userId", uid)
                    put("displayName", rawName.take(60))
                    put("username", baseUsername.take(32))
                    put("bio", "")
                    user.photoUrl?.toString()?.takeIf { it.isNotBlank() && it.length <= 500 }?.let {
                        put("avatarUrl", it)
                    }
                    put("specialty", "Creator")
                    put("publicProfile", true)
                    put("followersCount", 0L)
                    put("followingCount", 0L)
                    put("postsCount", 0L)
                    put("resourcesCount", 0L)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                publicRef.set(publicPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun signOut(credentialManager: CredentialManager) {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                auth.signOut()
            }
        }
    }

    suspend fun deleteCurrentAccount(credentialManager: CredentialManager): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in."))
        val uid = user.uid
        return try {
            // Delete user's saved items, private account doc, and public profile before auth deletion
            val savedSnaps = db.collection("saved_items").whereEqualTo("userId", uid).get().await()
            for (doc in savedSnaps.documents) {
                doc.reference.delete().await()
            }
            db.collection("users").document(uid).delete().await()
            db.collection("public_profiles").document(uid).delete().await()
            user.delete().await()
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (_: Exception) {
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }
}

sealed interface SignInOutcome {
    data object Success : SignInOutcome
    data object Cancelled : SignInOutcome
    data class Error(val message: String) : SignInOutcome
}

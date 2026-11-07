package com.taurotechnologies.godseye.nativeapp.auth

import android.app.Activity
import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.taurotechnologies.godseye.nativeapp.BuildConfig
import java.security.MessageDigest
import java.security.SecureRandom

/** Credential Manager bridge for the explicit Google sign-in button. */
class GoogleSignIn(context: Context) {
    private val credentialManager = CredentialManager.create(context.applicationContext)

    suspend fun signIn(activity: Activity): GoogleSignInResult {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            return GoogleSignInResult.Failure("Google sign-in is not configured for this build.")
        }

        val nonce = createNonce()
        val option = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(sha256Hex(nonce))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleSignInResult.Failure("Google did not return a supported sign-in credential. Try again.")
            } else {
                try {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    GoogleSignInResult.Success(googleCredential.idToken, nonce)
                } catch (_: GoogleIdTokenParsingException) {
                    GoogleSignInResult.Failure("Google returned an invalid sign-in response. Try again.")
                }
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (exception: GetCredentialException) {
            GoogleSignInResult.Failure(friendlyCredentialError(exception))
        }
    }

    private fun friendlyCredentialError(exception: GetCredentialException): String {
        if (exception is NoCredentialException) {
            return "No Google account is available on this device. Add an account or sign in with email."
        }
        val description = exception.message.orEmpty().lowercase()
        return when {
            "network" in description || "connect" in description ->
                "Google sign-in could not connect. Check your internet connection and try again."
            "developer" in description || "configuration" in description || "client" in description ->
                "Google sign-in is not configured for this app build."
            else -> "Google sign-in couldn't be completed. Check that Google Play services is available, then try again."
        }
    }

    private fun createNonce(): String {
        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun sha256Hex(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}

sealed interface GoogleSignInResult {
    data class Success(val idToken: String, val nonce: String) : GoogleSignInResult
    data class Failure(val message: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
}

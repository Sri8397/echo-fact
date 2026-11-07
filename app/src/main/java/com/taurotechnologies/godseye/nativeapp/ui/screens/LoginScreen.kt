package com.taurotechnologies.godseye.nativeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.safeDrawingPadding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taurotechnologies.godseye.nativeapp.R
import com.taurotechnologies.godseye.nativeapp.data.isValidEmail
import com.taurotechnologies.godseye.nativeapp.ui.theme.LocalSuvidhaColors

@Composable
fun LoginScreen(
    isLoading: Boolean,
    error: String?,
    info: String?,
    onSubmit: (email: String, password: String, createAccount: Boolean) -> Unit,
    onContinueWithGoogle: () -> Unit,
    onContinueAsGuest: () -> Unit,
) {
    val colors = LocalSuvidhaColors.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createAccount by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    val emailError = if (submitted && !isValidEmail(email)) {
        if (email.isBlank()) "Enter your email address." else "Enter a valid email address."
    } else null
    val passwordError = when {
        !submitted -> null
        password.isBlank() -> "Enter your password."
        createAccount && password.length < 6 -> "Use at least 6 characters for your password."
        else -> null
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.canvas).safeDrawingPadding().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.godseye_logo),
            contentDescription = "GodsEye logo",
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text("GodsEye World Radar", color = colors.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("by Suvidha", color = colors.muted, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.cardSurface,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 2.dp,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (createAccount) "Create your account" else "Welcome back",
                    color = colors.ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (createAccount) "Use Google or your email address."
                    else "Sign in with Google or email.",
                    color = colors.muted,
                    fontSize = 14.sp,
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; submitted = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    singleLine = true,
                    enabled = !isLoading,
                    isError = emailError != null,
                    supportingText = emailError?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; submitted = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !isLoading,
                    isError = passwordError != null,
                    supportingText = passwordError?.let { message -> { Text(message) } },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !isLoading) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                )
                if (error != null) Text(error, color = colors.danger, fontSize = 14.sp)
                if (info != null) Text(info, color = colors.confirm, fontSize = 14.sp)

                Button(
                    onClick = {
                        submitted = true
                        if (emailError == null && passwordError == null && isValidEmail(email) && password.isNotBlank()) {
                            onSubmit(email.trim(), password, createAccount)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.green, contentColor = colors.onGreen),
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = colors.onGreen, strokeWidth = 2.dp)
                    else Text(if (createAccount) "Create account" else "Sign in", fontWeight = FontWeight.SemiBold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f), color = colors.borderLight)
                    Text("  or  ", color = colors.muted, fontSize = 13.sp)
                    HorizontalDivider(Modifier.weight(1f), color = colors.borderLight)
                }
                OutlinedButton(
                    onClick = onContinueWithGoogle,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Continue with Google", color = colors.ink, fontWeight = FontWeight.Medium) }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { createAccount = !createAccount; submitted = false },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (createAccount) "Sign in" else "Create account", color = colors.green)
                    }
                    TextButton(onClick = onContinueAsGuest, enabled = !isLoading, modifier = Modifier.weight(1f)) {
                        Text("Continue as guest", color = colors.muted)
                    }
                }
                Text("Guest access: public radar only.", color = colors.muted, fontSize = 12.sp)
            }
        }
    }
}

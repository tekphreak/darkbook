package com.tekphreak.darkbook.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.tekphreak.darkbook.R
import com.tekphreak.darkbook.data.PinManager
import java.util.concurrent.Executor

@Composable
fun LockScreen(activity: FragmentActivity, onUnlocked: () -> Unit) {
    val hasPin = remember { PinManager.hasPin(activity) }
    var settingUpPin by remember { mutableStateOf(!hasPin) }
    var pinInput by remember { mutableStateOf("") }
    var confirmInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val biometricAvailable = remember {
        BiometricManager.from(activity)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt() {
        val executor: Executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.lock_title))
            .setNegativeButtonText(activity.getString(R.string.lock_use_pin))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(Unit) {
        if (biometricAvailable && hasPin) showBiometricPrompt()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringRes(activity, R.string.lock_title),
            style = MaterialTheme.typography.titleLarge
        )

        if (settingUpPin) {
            OutlinedTextField(
                value = pinInput,
                onValueChange = { if (it.length <= 6) pinInput = it },
                label = { Text(stringRes(activity, R.string.lock_set_pin)) },
                visualTransformation = PasswordVisualTransformation()
            )
            OutlinedTextField(
                value = confirmInput,
                onValueChange = { if (it.length <= 6) confirmInput = it },
                label = { Text(stringRes(activity, R.string.lock_confirm_pin)) },
                visualTransformation = PasswordVisualTransformation()
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text(stringRes(activity, R.string.lock_no_recovery), style = MaterialTheme.typography.bodySmall)
            Button(onClick = {
                if (pinInput.length < 4) {
                    error = null
                } else if (pinInput != confirmInput) {
                    error = activity.getString(R.string.lock_pin_mismatch)
                } else {
                    PinManager.setPin(activity, pinInput)
                    onUnlocked()
                }
            }) { Text(stringRes(activity, R.string.lock_pin_submit)) }
        } else {
            if (biometricAvailable) {
                Button(onClick = { showBiometricPrompt() }) {
                    Text(stringRes(activity, R.string.lock_unlock_biometric))
                }
            }
            OutlinedTextField(
                value = pinInput,
                onValueChange = { if (it.length <= 6) pinInput = it },
                label = { Text(stringRes(activity, R.string.lock_enter_pin)) },
                visualTransformation = PasswordVisualTransformation()
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                if (PinManager.verifyPin(activity, pinInput)) {
                    onUnlocked()
                } else {
                    error = activity.getString(R.string.lock_pin_wrong)
                    pinInput = ""
                }
            }) { Text(stringRes(activity, R.string.lock_pin_submit)) }
        }
    }
}

private fun stringRes(activity: FragmentActivity, id: Int) = activity.getString(id)

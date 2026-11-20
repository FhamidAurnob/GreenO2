package com.example.greeno2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.greeno2.ui.UiState
import com.example.greeno2.ui.components.GradientButton
import com.example.greeno2.ui.components.OutlineButton
import com.example.greeno2.ui.components.Wordmark
import com.example.greeno2.ui.theme.Olive

/** Branded launch screen: solid olive with the serif wordmark, as in the case study. */
@Composable
fun SplashScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Olive)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Wordmark(color = Color.White)
        Text(
            "© 2026 GreenO2",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
fun AuthScreen(
    ui: UiState,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (name: String, email: String, password: String) -> Unit,
    onClearError: () -> Unit,
) {
    var registering by rememberSaveable { mutableStateOf(true) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val scheme = MaterialTheme.colorScheme
    val fieldShape = RoundedCornerShape(10.dp)

    Column(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Wordmark()
        Text(
            "Travel smarter, travel greener! We help you cut your carbon footprint on every " +
                "commute and trip. Your journey matters. Make every trip a step toward a greener future!",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (registering) "Create your account" else "Welcome back",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
        )

        if (registering) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; onClearError() },
                label = { Text("Name") },
                singleLine = true,
                shape = fieldShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; onClearError() },
            label = { Text("Email") },
            singleLine = true,
            shape = fieldShape,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; onClearError() },
            label = { Text("Password") },
            singleLine = true,
            shape = fieldShape,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            supportingText = { if (registering) Text("At least 6 characters") },
            modifier = Modifier.fillMaxWidth(),
        )
        if (ui.authError != null) {
            Surface(color = scheme.errorContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(ui.authError, color = scheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
            }
        }

        if (ui.authBusy) {
            Box(Modifier.fillMaxWidth().height(54.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            }
        } else {
            GradientButton(
                if (registering) "Sign up" else "Sign in",
                onClick = { if (registering) onRegister(name, email, password) else onLogin(email, password) },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(scheme.outline))
            Text("Or", color = scheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp))
            Box(Modifier.weight(1f).height(1.dp).background(scheme.outline))
        }
        OutlineButton(
            if (registering) "I already have an account" else "Create a new account",
            onClick = { registering = !registering; onClearError() },
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Terms of Use · Privacy Policy\n© 2026 GreenO2",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

package com.example.pizzaconfigurator.kitchen.ui.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pizzaconfigurator.kitchen.ui.rememberAppContainer

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: LoginViewModel = viewModel(factory = LoginViewModel.factory(container.authRepository))
    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState(initial = null)

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(authState?.isAuthenticated) {
        if (authState?.isAuthenticated == true) onLoggedIn()
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Pizza Kitchen", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth().padding(top = 24.dp))
            OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Button(
                onClick = { viewModel.login(username, password) },
                enabled = !uiState.submitting,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) { Text(if (uiState.submitting) "Signing in..." else "Log in") }
            uiState.error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}

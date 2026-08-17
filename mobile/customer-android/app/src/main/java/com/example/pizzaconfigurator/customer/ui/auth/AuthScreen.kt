package com.example.pizzaconfigurator.customer.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pizzaconfigurator.customer.ui.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(container.authRepository))
    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState(initial = null)

    var isRegisterMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Account") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (authState?.token != null) {
                Text("Signed in as ${authState?.name} (${authState?.email})")
                Button(onClick = { viewModel.logout() }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Log out (continue as guest)")
                }
                return@Scaffold
            }

            Text("Guest checkout works too — sign in is optional.")

            if (isRegisterMode) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            }
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))

            Button(
                onClick = {
                    if (isRegisterMode) viewModel.register(name, email, null, password) else viewModel.login(email, password)
                },
                enabled = !uiState.submitting,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) { Text(if (isRegisterMode) "Register" else "Log in") }

            TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                Text(if (isRegisterMode) "Already have an account? Log in" else "New here? Register")
            }

            uiState.error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
        }
    }
}

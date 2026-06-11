package com.jinsupark.helpumta.ui.screen.auth

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jinsupark.helpumta.ui.theme.HelpumtaTheme

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                LoginContract.SideEffect.NavigateToHome -> onNavigateToHome()
                is LoginContract.SideEffect.ShowToast -> { /* TODO */ }
            }
        }
    }

    LoginContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onNavigateToSignUp = onNavigateToSignUp
    )
}

@Composable
private fun LoginContent(
    uiState: LoginContract.UiState,
    onIntent: (LoginContract.Intent) -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 앱 타이틀
            Text(
                text = "헬품타",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "오늘도 같이 운동하자 💪",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 이메일 입력
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { onIntent(LoginContract.Intent.UpdateEmail(it)) },
                label = { Text("이메일") },
                singleLine = true,
                enabled = !uiState.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            // 비밀번호 입력
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { onIntent(LoginContract.Intent.UpdatePassword(it)) },
                label = { Text("비밀번호") },
                singleLine = true,
                enabled = !uiState.isLoading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            // 에러 메시지
            uiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 로그인 버튼 (로딩 중이면 enabled = false, 안에 스피너)
            Button(
                onClick = { onIntent(LoginContract.Intent.SubmitLogin) },
                enabled = uiState.isLoginEnabled && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("로그인")
                }
            }

            // 구글 로그인 버튼
            OutlinedButton(
                onClick = {
                    onIntent(LoginContract.Intent.GoogleLogin(activity))  // context 전달
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Google로 로그인")
            }

            TextButton(
                onClick = onNavigateToSignUp,
                enabled = !uiState.isLoading
            ) {
                Text("계정이 없으신가요? 회원가입")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginContentPreview() {
    HelpumtaTheme {
        LoginContent(
            uiState = LoginContract.UiState(),
            onIntent = {},
            onNavigateToSignUp = {}
        )
    }
}
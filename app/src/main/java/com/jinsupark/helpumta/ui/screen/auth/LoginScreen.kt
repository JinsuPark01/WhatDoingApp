package com.jinsupark.helpumta.ui.screen.auth

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jinsupark.helpumta.R
import com.jinsupark.helpumta.data.auth.GoogleAuthClient
import com.jinsupark.helpumta.ui.screen.auth.components.AuthTextField
import com.jinsupark.helpumta.ui.theme.HelpumtaTheme
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    googleAuthClient: GoogleAuthClient,
    onNavigateToHome: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()

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
        onNavigateToSignUp = onNavigateToSignUp,
        onGoogleLoginClick = {
            // Credential Manager 팝업은 Activity가 필요 → Screen에서 처리
            viewModel.onGoogleLoginStarted()
            scope.launch {
                googleAuthClient.getIdToken(activity).fold(
                    onSuccess = { idToken ->
                        viewModel.handleIntent(LoginContract.Intent.GoogleLogin(idToken))
                    },
                    onFailure = {
                        viewModel.handleIntent(LoginContract.Intent.GoogleLoginCancelled)
                    }
                )
            }
        }
    )
}

@Composable
private fun LoginContent(
    uiState: LoginContract.UiState,
    onIntent: (LoginContract.Intent) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onGoogleLoginClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                // 어댑티브 아이콘 전경 레이어는 자체 여백이 커서
                // Box와 같은 크기를 줘야 덤벨이 적당한 비율로 보인다
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(76.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "헬품타",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "오늘도 같이 운동하자",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(40.dp))

            AuthTextField(
                label = "이메일",
                value = uiState.email,
                onValueChange = { onIntent(LoginContract.Intent.UpdateEmail(it)) },
                keyboardType = KeyboardType.Email,
                enabled = !uiState.isLoading
            )

            Spacer(Modifier.height(12.dp))

            AuthTextField(
                label = "비밀번호",
                value = uiState.password,
                onValueChange = { onIntent(LoginContract.Intent.UpdatePassword(it)) },
                isPassword = true,
                enabled = !uiState.isLoading,
                errorMessage = uiState.errorMessage
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onIntent(LoginContract.Intent.SubmitLogin) },
                enabled = uiState.isLoginEnabled && !uiState.isLoading,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("로그인", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(10.dp))

            // OutlinedButton 기본 contentColor가 primary라 글씨가 주황으로 나온다
            OutlinedButton(
                onClick = onGoogleLoginClick,
                enabled = !uiState.isLoading,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Google로 로그인", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.weight(1f))

            TextButton(
                onClick = onNavigateToSignUp,
                enabled = !uiState.isLoading
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append("계정이 없으신가요?  ")
                        }
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("회원가입")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
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
            onNavigateToSignUp = {},
            onGoogleLoginClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginContentErrorPreview() {
    HelpumtaTheme {
        LoginContent(
            uiState = LoginContract.UiState(
                email = "test@example.com",
                password = "wrongpw",
                errorMessage = "이메일 또는 비밀번호가 올바르지 않아요"
            ),
            onIntent = {},
            onNavigateToSignUp = {},
            onGoogleLoginClick = {}
        )
    }
}
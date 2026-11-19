@file:OptIn(ExperimentalMaterial3Api::class)

package com.jinsupark.helpumta.ui.screen.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jinsupark.helpumta.ui.screen.auth.components.AuthTextField
import com.jinsupark.helpumta.ui.theme.HelpumtaTheme

@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                SignUpContract.SideEffect.NavigateToHome -> onNavigateToHome()
                is SignUpContract.SideEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    SignUpContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun SignUpContent(
    uiState: SignUpContract.UiState,
    onIntent: (SignUpContract.Intent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text("회원가입", style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                // 칸이 네 개라 키보드가 올라오면 아래가 잘린다
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AuthTextField(
                label = "이메일",
                value = uiState.email,
                onValueChange = { onIntent(SignUpContract.Intent.UpdateEmail(it)) },
                keyboardType = KeyboardType.Email,
                enabled = !uiState.isLoading,
                // 비어 있을 때는 아직 틀렸다고 말하지 않는다
                errorMessage = if (uiState.email.isNotBlank() && !uiState.isValidEmail) {
                    "올바른 이메일 형식이 아니에요"
                } else null
            )

            AuthTextField(
                label = "닉네임",
                value = uiState.nickname,
                onValueChange = { onIntent(SignUpContract.Intent.UpdateNickname(it)) },
                enabled = !uiState.isLoading
            )

            AuthTextField(
                label = "비밀번호",
                value = uiState.password,
                onValueChange = { onIntent(SignUpContract.Intent.UpdatePassword(it)) },
                isPassword = true,
                enabled = !uiState.isLoading,
                errorMessage = if (uiState.password.isNotBlank() && uiState.password.length < 6) {
                    "6자 이상 입력해주세요"
                } else null
            )

            AuthTextField(
                label = "비밀번호 확인",
                value = uiState.passwordConfirm,
                onValueChange = { onIntent(SignUpContract.Intent.UpdatePasswordConfirm(it)) },
                isPassword = true,
                enabled = !uiState.isLoading,
                errorMessage = if (uiState.passwordConfirm.isNotBlank() &&
                    uiState.password != uiState.passwordConfirm
                ) {
                    "비밀번호가 일치하지 않아요"
                } else null
            )

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { onIntent(SignUpContract.Intent.SubmitSignUp) },
                enabled = uiState.isSignUpEnabled && !uiState.isLoading,
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
                    Text("가입하기", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignUpContentPreview() {
    HelpumtaTheme {
        SignUpContent(
            uiState = SignUpContract.UiState(),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignUpContentErrorPreview() {
    HelpumtaTheme {
        SignUpContent(
            uiState = SignUpContract.UiState(
                email = "notanemail",
                nickname = "진수",
                password = "123",
                passwordConfirm = "456"
            ),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}
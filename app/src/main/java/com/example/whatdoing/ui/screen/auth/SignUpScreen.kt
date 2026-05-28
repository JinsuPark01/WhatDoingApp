@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.whatdoing.ui.screen.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.whatdoing.ui.theme.WhatDoingTheme

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
        topBar = {
            TopAppBar(
                title = { Text("회원가입") },
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
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 이메일
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { onIntent(SignUpContract.Intent.UpdateEmail(it)) },
                label = { Text("이메일") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = uiState.email.isNotBlank() && !uiState.isValidEmail,  // 추가
                modifier = Modifier.fillMaxWidth()
            )

            // 이메일 형식 오류 안내
            if (uiState.email.isNotBlank() && !uiState.isValidEmail) {
                Text(
                    text = "올바른 이메일 형식이 아닙니다",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // 닉네임
            OutlinedTextField(
                value = uiState.nickname,
                onValueChange = { onIntent(SignUpContract.Intent.UpdateNickname(it)) },
                label = { Text("닉네임") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 비밀번호
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { onIntent(SignUpContract.Intent.UpdatePassword(it)) },
                label = { Text("비밀번호 (6자 이상)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            // 비밀번호 확인
            OutlinedTextField(
                value = uiState.passwordConfirm,
                onValueChange = { onIntent(SignUpContract.Intent.UpdatePasswordConfirm(it)) },
                label = { Text("비밀번호 확인") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = uiState.password.isNotBlank() &&  // 추가
                        uiState.passwordConfirm.isNotBlank() &&
                        uiState.password != uiState.passwordConfirm,
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.password.isNotBlank() &&  // 추가
                uiState.passwordConfirm.isNotBlank() &&
                uiState.password != uiState.passwordConfirm) {
                Text(
                    text = "비밀번호가 일치하지 않습니다",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // 에러 메시지
            uiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 가입 버튼
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                Button(
                    onClick = { onIntent(SignUpContract.Intent.SubmitSignUp) },
                    enabled = uiState.isSignUpEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("가입하기")
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignUpContentPreview() {
    WhatDoingTheme {
        SignUpContent(
            uiState = SignUpContract.UiState(),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}
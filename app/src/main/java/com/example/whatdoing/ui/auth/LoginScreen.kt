package com.example.whatdoing.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatdoing.ui.theme.WhatDoingTheme

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 앱 로고
        Text(
            text = "지금뭐해?",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isSignUpMode) "회원가입" else "로그인",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 이메일
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("이메일") },
            placeholder = { Text("example@email.com") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 비밀번호
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("비밀번호") },
            placeholder = { Text("6자 이상") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        )

        // 회원가입 모드일 때 비밀번호 확인
        if (isSignUpMode) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = null
                },
                label = { Text("비밀번호 확인") },
                placeholder = { Text("비밀번호를 다시 입력하세요") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            )
        }

        // 에러 메시지
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 로그인/회원가입 버튼
        Button(
            onClick = {
                if (isSignUpMode) {
                    // 회원가입 유효성 검사
                    when {
                        email.isBlank() -> errorMessage = "이메일을 입력해주세요"
                        password.length < 6 -> errorMessage = "비밀번호는 6자 이상이어야 합니다"
                        password != confirmPassword -> errorMessage = "비밀번호가 일치하지 않습니다"
                        else -> {
                            // TODO: Firebase 회원가입
                            isLoading = true
                        }
                    }
                } else {
                    // 로그인 유효성 검사
                    when {
                        email.isBlank() -> errorMessage = "이메일을 입력해주세요"
                        password.isBlank() -> errorMessage = "비밀번호를 입력해주세요"
                        else -> {
                            // TODO: Firebase 로그인
                            isLoading = true
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = if (isSignUpMode) "회원가입" else "로그인",
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 모드 전환 버튼
        TextButton(
            onClick = {
                isSignUpMode = !isSignUpMode
                confirmPassword = ""
                errorMessage = null
            },
            enabled = !isLoading
        ) {
            Text(
                text = if (isSignUpMode) "이미 계정이 있으신가요? 로그인" else "계정이 없으신가요? 회원가입",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    WhatDoingTheme {
        LoginScreen(onLoginSuccess = {})
    }
}
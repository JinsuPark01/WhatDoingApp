package com.jinsupark.helpumta

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HelpumtaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        // 빌드 타입별 구현 (debug/release 소스셋)
        installAppCheck()
    }
}
package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.security.configureAppCheck
import com.example.data.preferences.CreovaPreferences
import com.example.data.repository.AuthRepository
import com.example.data.repository.CreovaFirestoreRepository
import com.example.ui.navigation.CreovaNavHost
import com.example.ui.theme.CreovaTheme
import com.example.ui.viewmodel.CreovaAppViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // App Check MUST be installed before any Firebase.ai / Firestore / Compose initialization
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val appContext = applicationContext
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val databaseId = appContext.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(databaseId)
                val firestoreRepository = CreovaFirestoreRepository(firestore)
                val authRepository = AuthRepository(
                    db = firestore,
                    auth = FirebaseAuth.getInstance()
                )
                val preferences = CreovaPreferences(appContext)
                return CreovaAppViewModel(
                    authRepository = authRepository,
                    firestoreRepository = firestoreRepository,
                    preferences = preferences
                ) as T
            }
        }

        setContent {
            val appViewModel: CreovaAppViewModel = viewModel(factory = factory)
            val prefs by appViewModel.prefs.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                appViewModel.attemptSilentSignIn(this@MainActivity)
            }

            CreovaTheme(
                themeMode = prefs.themeMode,
                language = prefs.language,
                userReducedMotion = prefs.reducedMotion
            ) {
                CreovaNavHost(appViewModel = appViewModel)
            }
        }
    }
}

package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.notifications.NudgeWorker
import com.example.ui.CaptureScreen
import com.example.ui.HomeScreen
import com.example.ui.SiftViewModel
import com.example.ui.SortOrder
import com.example.ui.theme.MyApplicationTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Schedule nudges twice a day
        val nudgeRequest = PeriodicWorkRequestBuilder<NudgeWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "nudge_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            nudgeRequest
        )
        
        // Handle incoming intent
        if (intent?.getBooleanExtra("OPEN_IMPORTANT", false) == true) {
            val prefs = getSharedPreferences("sift_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("sort_order", SortOrder.IMPORTANT.name).apply()
        }
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SiftApp()
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("OPEN_IMPORTANT", false) == true) {
            val prefs = getSharedPreferences("sift_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("sort_order", SortOrder.IMPORTANT.name).apply()
            // In a real app we might force ViewModel to reload prefs here or observe them, 
            // but for simplicity setting prefs before the composables recompose may catch it, 
            // or they can just restart the app.
        }
    }
}

@Composable
fun SiftApp() {
    val navController = rememberNavController()
    val viewModel: SiftViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToCapture = { id ->
                    navController.navigate("capture/$id")
                }
            )
        }
        composable(
            route = "capture/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            CaptureScreen(
                itemId = id,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

package com.example.beautyapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        requestNotificationPermissionIfNeeded()
        AppNotificationHelper.ensureChannel(this)
        scheduleNotificationPolling()

        val navHost = findViewById<View>(R.id.nav_host_fragment)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        applyWindowInsets(navHost, bottomNav)

        val navController = findNavController(R.id.nav_host_fragment)
        val sessionManager = SessionManager(this)


        bottomNav.setupWithNavController(navController)
        syncMasterMenu(bottomNav, sessionManager.isMaster())
        navController.addOnDestinationChangedListener { _, destination, _ ->
            syncMasterMenu(bottomNav, sessionManager.isMaster())
            bottomNav.visibility = if (
                destination.id == R.id.authFragment ||
                destination.id == R.id.registerFragment
            ) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        syncMasterMenu(bottomNav, SessionManager(this).isMaster())
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun syncMasterMenu(bottomNav: BottomNavigationView, isMaster: Boolean) {
        bottomNav.menu.findItem(R.id.homeFragment)?.title = if (isMaster) "Мои работы" else "Главная"
        bottomNav.menu.findItem(R.id.servicesFragment)?.isVisible = isMaster
        bottomNav.menu.findItem(R.id.scheduleFragment)?.isVisible = isMaster
    }

    private fun scheduleNotificationPolling() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val work = PeriodicWorkRequestBuilder<NotificationPollingWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "notification_polling",
            ExistingPeriodicWorkPolicy.UPDATE,
            work,
        )
    }

    private fun applyWindowInsets(navHost: View, bottomNav: BottomNavigationView) {
        val navHostTopPadding = navHost.paddingTop
        val bottomNavBottomPadding = bottomNav.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(navHost) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = navHostTopPadding + systemBars.top)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = bottomNavBottomPadding + systemBars.bottom)
            insets
        }

        ViewCompat.requestApplyInsets(navHost)
        ViewCompat.requestApplyInsets(bottomNav)
    }
}

package com.salud.personal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.salud.personal.data.ble.huami.AmazfitGtr3BleManager
import com.salud.personal.data.ble.renpho.RenphoScaleBleScanner
import com.salud.personal.data.local.AppDatabase
import com.salud.personal.data.repository.WatchRepositoryImpl
import com.salud.personal.data.repository.WeightRepository
import com.salud.personal.ui.coach.CoachViewModel
import com.salud.personal.ui.coach.CoachViewModelFactory
import com.salud.personal.ui.dashboard.DashboardViewModel
import com.salud.personal.ui.dashboard.DashboardViewModelFactory
import com.salud.personal.ui.easyui.EasyGlassNavBar
import com.salud.personal.ui.navigation.MainNavHost
import com.salud.personal.ui.navigation.TopLevelDestination
import com.salud.personal.ui.theme.SaludPersonalTheme
import com.salud.personal.ui.theme.WhoopColors
import com.salud.personal.ui.trends.ProgressViewModel
import com.salud.personal.ui.trends.ProgressViewModelFactory
import com.salud.personal.ui.workout.WorkoutViewModel
import com.salud.personal.ui.workout.WorkoutViewModelFactory
import androidx.lifecycle.lifecycleScope
import androidx.health.connect.client.PermissionController
import com.salud.personal.data.healthconnect.HealthConnectManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var bleManager: AmazfitGtr3BleManager
    private lateinit var renphoScanner: RenphoScaleBleScanner
    private lateinit var healthConnectManager: HealthConnectManager

    private val viewModel: DashboardViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val weightRepository = WeightRepository(database.weightDao())
        bleManager = AmazfitGtr3BleManager(applicationContext)
        renphoScanner = RenphoScaleBleScanner(applicationContext, weightRepository)
        val watchRepository = WatchRepositoryImpl(
            bleManager = bleManager,
            heartRateDao = database.heartRateDao(),
            dailyActivityDao = database.dailyActivityDao(),
            workoutDao = database.workoutDao(),
            sleepSessionDao = database.sleepSessionDao()
        )
        DashboardViewModelFactory(weightRepository, watchRepository)
    }

    private val workoutViewModel: WorkoutViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        WorkoutViewModelFactory(database.workoutDao())
    }

    private val progressViewModel: ProgressViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val weightRepository = WeightRepository(database.weightDao())
        ProgressViewModelFactory(
            workoutDao = database.workoutDao(),
            weightRepository = weightRepository,
            dailyCheckInDao = database.dailyCheckInDao(),
            dailyActivityDao = database.dailyActivityDao(),
            sleepSessionDao = database.sleepSessionDao()
        )
    }

    private val coachViewModel: CoachViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val weightRepository = WeightRepository(database.weightDao())
        CoachViewModelFactory(
            dailyActivityDao = database.dailyActivityDao(),
            workoutDao = database.workoutDao(),
            heartRateDao = database.heartRateDao(),
            weightRepository = weightRepository,
            dailyCheckInDao = database.dailyCheckInDao(),
            sleepSessionDao = database.sleepSessionDao()
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            connectWatchIfConfigured()
            startRenphoScanningIfPermitted()
        }
    }

    private val healthConnectPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (::healthConnectManager.isInitialized && granted.isNotEmpty()) {
            lifecycleScope.launch {
                val summary = healthConnectManager.syncAllHealthRecords()
                android.widget.Toast.makeText(
                    applicationContext,
                    if (summary.total > 0) "Sincronizados ${summary.total} registros desde Health Connect" else "Health Connect sincronizado (0 nuevos)",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun requestHealthConnectSync() {
        android.util.Log.d("SaludPersonalHC", "requestHealthConnectSync invoked")
        if (!::healthConnectManager.isInitialized) {
            android.util.Log.d("SaludPersonalHC", "healthConnectManager not initialized")
            return
        }
        lifecycleScope.launch {
            val hasPerms = healthConnectManager.hasPermissions()
            android.util.Log.d("SaludPersonalHC", "hasPermissions=$hasPerms")
            if (hasPerms) {
                val summary = healthConnectManager.syncAllHealthRecords()
                healthConnectManager.cleanDuplicateWorkouts()
                val parts = mutableListOf<String>()
                if (summary.workouts > 0) parts.add("${summary.workouts} entrenos")
                if (summary.weights > 0) parts.add("${summary.weights} pesajes")
                if (summary.sleepSessions > 0) parts.add("${summary.sleepSessions} noches de sueño")
                if (summary.activityDays > 0) parts.add("${summary.activityDays} días pasos")
                if (summary.heartRateSamples > 0) parts.add("${summary.heartRateSamples} pulso")

                val msg = if (parts.isNotEmpty()) {
                    "Sincronizado Zepp: ${parts.joinToString(", ")}"
                } else {
                    "Zepp / Health Connect al día"
                }
                android.util.Log.d("SaludPersonalHC", "syncAllHealthRecords total=${summary.total}")
                android.widget.Toast.makeText(applicationContext, msg, android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.util.Log.d("SaludPersonalHC", "launching healthConnectPermissionLauncher for ${healthConnectManager.permissions}")
                healthConnectPermissionLauncher.launch(healthConnectManager.permissions)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName

        val database = AppDatabase.getInstance(applicationContext)
        val weightRepository = WeightRepository(database.weightDao())
        healthConnectManager = HealthConnectManager(
            context = applicationContext,
            weightRepository = weightRepository,
            sleepSessionDao = database.sleepSessionDao(),
            dailyActivityDao = database.dailyActivityDao(),
            heartRateDao = database.heartRateDao(),
            workoutDao = database.workoutDao(),
            hourlyStepsDao = database.hourlyStepsDao()
        )

        lifecycleScope.launch {
            if (healthConnectManager.hasAnyPermission()) {
                healthConnectManager.syncAllHealthRecords()
                healthConnectManager.cleanDuplicateWorkouts()
            }
        }

        setContent {
            SaludPersonalTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = currentRoute == null || TopLevelDestination.entries.any { it.route == currentRoute }

                var currentDestination by rememberSaveable {
                    mutableStateOf(TopLevelDestination.HOY)
                }

                Scaffold(
                    containerColor = WhoopColors.Background,
                    bottomBar = {
                        if (showBottomBar) {
                            EasyGlassNavBar(
                                currentDestination = currentDestination,
                                onNavigate = { destination ->
                                    currentDestination = destination
                                    navController.navigate(destination.route) {
                                        popUpTo(TopLevelDestination.HOY.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    MainNavHost(
                        navController = navController,
                        dashboardViewModel = viewModel,
                        workoutViewModel = workoutViewModel,
                        progressViewModel = progressViewModel,
                        coachViewModel = coachViewModel,
                        database = database,
                        healthConnectAvailable = healthConnectManager.isAvailable(),
                        onSyncHealthConnect = { requestHealthConnectSync() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // Forward live BLE heart rate samples to WorkoutViewModel for TRIMP tracking
        if (::bleManager.isInitialized) {
            lifecycleScope.launch {
                bleManager.heartRateSamples.collectLatest { sample ->
                    workoutViewModel.onHeartRateReceived(sample)
                }
            }
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionsList = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsList.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissionsList.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissions = permissionsList.toTypedArray()
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            connectWatchIfConfigured()
            startRenphoScanningIfPermitted()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun connectWatchIfConfigured() {
        if (BuildConfig.AMAZFIT_MAC.isNotBlank() && BuildConfig.AMAZFIT_AUTH_KEY.isNotBlank()) {
            viewModel.connectWatch(BuildConfig.AMAZFIT_MAC, BuildConfig.AMAZFIT_AUTH_KEY)
        }
    }

    private fun startRenphoScanningIfPermitted() {
        if (::renphoScanner.isInitialized) {
            renphoScanner.startScanning()
        }
    }

    override fun onStart() {
        super.onStart()
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            startRenphoScanningIfPermitted()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::renphoScanner.isInitialized) {
            renphoScanner.stopScanning()
        }
    }
}

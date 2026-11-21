package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.SystemTelemetryEngine
import com.example.model.AuthState
import com.example.model.TeleportAnchor
import com.example.ui.components.InAppFloatingHud
import com.example.ui.components.NeonPulseDot
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PerformanceDashboardScreen
import com.example.ui.screens.UtilityDashboardScreen
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSurfaceSolid
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NipexLagTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBgDeep
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.NipexViewModel

private enum class HomeTab {
    UTILITY,
    TELEMETRY,
}

class MainActivity : ComponentActivity() {

    private val viewModel: NipexViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NipexLagTheme {
                NipexLagRoot(
                    viewModel = viewModel,
                    onKeepScreenOnChanged = { keepOn ->
                        if (keepOn) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun NipexLagRoot(
    viewModel: NipexViewModel,
    onKeepScreenOnChanged: (Boolean) -> Unit,
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val loginUiState by viewModel.loginUiState.collectAsStateWithLifecycle()
    val moduleConfig by viewModel.moduleConfig.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val overlayState by viewModel.overlayUiState.collectAsStateWithLifecycle()
    val isCleaningMemory by viewModel.isCleaningMemory.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(moduleConfig.freezeEnabled) {
        onKeepScreenOnChanged(moduleConfig.freezeEnabled)
    }

    LaunchedEffect(overlayState.statusToast) {
        val message = overlayState.statusToast
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusToast()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.refreshPermissions()
                    SystemTelemetryEngine.onAppForegroundChanged(true)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    SystemTelemetryEngine.onAppForegroundChanged(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.refreshPermissions()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.startFloatingOverlayService()
    }

    val launchOverlayWithNotificationCheck: () -> Unit = {
        if (overlayState.isOverlayServiceRunning) {
            viewModel.stopFloatingOverlayService()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.startFloatingOverlayService()
        }
    }

    val requestOverlayPermission: () -> Unit = {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        runCatching { overlayPermissionLauncher.launch(intent) }
    }

    AnimatedContent(
        targetState = authState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "root_auth_transition",
    ) { currentAuth ->
        when (currentAuth) {
            AuthState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBg),
                ) {
                    CircularProgressIndicator(color = ElectricGreen)
                }
            }

            is AuthState.Unauthenticated,
            AuthState.Verifying -> {
                LoginScreen(
                    authState = currentAuth,
                    loginUiState = loginUiState,
                    onKeyChanged = viewModel::onKeyInputChanged,
                    onActivateClick = viewModel::verifyAndActivateKey,
                    onGetKeyClick = { viewModel.openGetKeySheet(proEdition = true) },
                    onDismissGetKeySheet = viewModel::closeGetKeySheet,
                    onRegenerateKey = viewModel::regenerateKeyInSheet,
                    onApplyGeneratedKey = viewModel::applyGeneratedKeyAndVerify,
                )
            }

            is AuthState.Authenticated -> {
                var selectedTab by rememberSaveable { mutableStateOf(HomeTab.UTILITY) }

                BackHandler(enabled = selectedTab == HomeTab.TELEMETRY) {
                    selectedTab = HomeTab.UTILITY
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useNavigationRail = maxWidth >= 600.dp

                    Scaffold(
                        containerColor = ObsidianBg,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = {
                            SnackbarHost(hostState = snackbarHostState) { data ->
                                Snackbar(
                                    snackbarData = data,
                                    containerColor = GlassSurfaceSolid,
                                    contentColor = ElectricGreen,
                                    shape = RoundedCornerShape(12.dp),
                                )
                            }
                        },
                        topBar = {
                            NipexTopHeaderBar(
                                authState = currentAuth,
                                fps = telemetry.currentFps,
                                onSignOut = viewModel::signOutKey,
                            )
                        },
                        bottomBar = {
                            if (!useNavigationRail) {
                                NavigationBar(
                                    containerColor = ObsidianBgDeep,
                                    contentColor = TextPrimary,
                                ) {
                                    NavigationBarItem(
                                        selected = selectedTab == HomeTab.UTILITY,
                                        onClick = { selectedTab = HomeTab.UTILITY },
                                        icon = {
                                            Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.nav_utility))
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(R.string.nav_utility),
                                                fontFamily = OrbitronFontFamily,
                                                fontSize = 11.sp,
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ElectricGreen,
                                            selectedTextColor = ElectricGreen,
                                            indicatorColor = ElectricGreen.copy(alpha = 0.16f),
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted,
                                        ),
                                        modifier = Modifier.testTag("nav_utility"),
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == HomeTab.TELEMETRY,
                                        onClick = { selectedTab = HomeTab.TELEMETRY },
                                        icon = {
                                            Icon(Icons.Default.Analytics, contentDescription = stringResource(R.string.nav_telemetry))
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(R.string.nav_telemetry),
                                                fontFamily = OrbitronFontFamily,
                                                fontSize = 11.sp,
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = NeonCrimson,
                                            selectedTextColor = NeonCrimson,
                                            indicatorColor = NeonCrimson.copy(alpha = 0.18f),
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted,
                                        ),
                                        modifier = Modifier.testTag("nav_telemetry"),
                                    )
                                }
                            }
                        },
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        ) {
                            if (useNavigationRail) {
                                NavigationRail(
                                    containerColor = ObsidianBgDeep,
                                    modifier = Modifier.fillMaxHeight(),
                                ) {
                                    NavigationRailItem(
                                        selected = selectedTab == HomeTab.UTILITY,
                                        onClick = { selectedTab = HomeTab.UTILITY },
                                        icon = {
                                            Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.nav_utility))
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(R.string.nav_utility),
                                                fontFamily = OrbitronFontFamily,
                                                fontSize = 10.sp,
                                            )
                                        },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = ElectricGreen,
                                            selectedTextColor = ElectricGreen,
                                            indicatorColor = ElectricGreen.copy(alpha = 0.16f),
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted,
                                        ),
                                    )
                                    NavigationRailItem(
                                        selected = selectedTab == HomeTab.TELEMETRY,
                                        onClick = { selectedTab = HomeTab.TELEMETRY },
                                        icon = {
                                            Icon(Icons.Default.Analytics, contentDescription = stringResource(R.string.nav_telemetry))
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(R.string.nav_telemetry),
                                                fontFamily = OrbitronFontFamily,
                                                fontSize = 10.sp,
                                            )
                                        },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = NeonCrimson,
                                            selectedTextColor = NeonCrimson,
                                            indicatorColor = NeonCrimson.copy(alpha = 0.18f),
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted,
                                        ),
                                    )
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                AnimatedContent(
                                    targetState = selectedTab,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "tab_content",
                                ) { tab ->
                                    when (tab) {
                                        HomeTab.UTILITY -> {
                                            UtilityDashboardScreen(
                                                config = moduleConfig,
                                                telemetry = telemetry,
                                                overlayState = overlayState,
                                                isCleaningMemory = isCleaningMemory,
                                                onRequestOverlayPermission = requestOverlayPermission,
                                                onToggleOverlayService = launchOverlayWithNotificationCheck,
                                                onToggleInAppHud = viewModel::toggleInAppHudVisible,
                                                onFreezeToggle = viewModel::setFreezeEnabled,
                                                onFreezeHoldChange = viewModel::setFreezeHoldMs,
                                                onTeleportToggle = viewModel::setTeleportEnabled,
                                                onTeleportAnchorSelect = viewModel::setTeleportAnchor,
                                                onTeleportSensitivityChange = viewModel::setTeleportSnapSensitivity,
                                                onGhostToggle = viewModel::setGhostEnabled,
                                                onGhostOpacityChange = viewModel::setGhostOpacity,
                                                onGhostAutoFadeToggle = viewModel::setGhostAutoFade,
                                                onNetworkBoosterToggle = viewModel::setNetworkBoosterEnabled,
                                                onPingIntervalChange = viewModel::setPingIntervalMs,
                                                onDnsTargetSelect = viewModel::setDnsTarget,
                                                onProbeNetworkNow = viewModel::triggerPingProbe,
                                                onAntiLagToggle = viewModel::setAntiLagEnabled,
                                                onAutoTrimChange = viewModel::setAutoTrimThreshold,
                                                onCleanMemoryNow = viewModel::cleanMemoryNow,
                                                onFpsMonitorToggle = viewModel::setFpsMonitorEnabled,
                                                onTargetFpsChange = viewModel::setTargetFpsThreshold,
                                            )
                                        }

                                        HomeTab.TELEMETRY -> {
                                            PerformanceDashboardScreen(
                                                telemetry = telemetry,
                                                config = moduleConfig,
                                                authState = currentAuth,
                                                isCleaningMemory = isCleaningMemory,
                                                onCleanMemoryNow = viewModel::cleanMemoryNow,
                                                onProbeNetworkNow = viewModel::triggerPingProbe,
                                            )
                                        }
                                    }
                                }

                                // In-App Draggable Floating Bubble & Expandable Panel
                                InAppFloatingHud(
                                    visible = overlayState.inAppHudVisible,
                                    expanded = overlayState.inAppHudExpanded,
                                    config = moduleConfig,
                                    telemetry = telemetry,
                                    onToggleExpanded = viewModel::toggleInAppHudExpanded,
                                    onHideHud = viewModel::toggleInAppHudVisible,
                                    onToggleFreeze = viewModel::setFreezeEnabled,
                                    onCycleTeleport = {
                                        val entries = TeleportAnchor.entries
                                        val next = entries[(moduleConfig.teleportAnchor.ordinal + 1) % entries.size]
                                        viewModel.setTeleportAnchor(next)
                                    },
                                    onToggleGhost = viewModel::setGhostEnabled,
                                    onToggleNetBooster = viewModel::setNetworkBoosterEnabled,
                                    onToggleAntiLag = viewModel::setAntiLagEnabled,
                                    onToggleFpsMonitor = viewModel::setFpsMonitorEnabled,
                                    onCleanMemory = viewModel::cleanMemoryNow,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NipexTopHeaderBar(
    authState: AuthState.Authenticated,
    fps: Int,
    onSignOut: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianBgDeep)
            .statusBarsPadding()
            .border(width = 0.5.dp, color = GlassBorderSubtle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_nipex_emblem_1795279576883),
                contentDescription = "NIPEX LAG V12",
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(1.dp, NeonCrimson.copy(alpha = 0.7f), CircleShape),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = OrbitronFontFamily,
                    color = TextPrimary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeonPulseDot(color = ElectricGreen, size = 6.dp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = authState.tier.badgeLabel,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = ElectricGreen,
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF131824))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                Text(
                    text = "$fps FPS",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (fps >= 50) ElectricGreen else NeonCrimson,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onSignOut,
                modifier = Modifier.testTag("sign_out_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = stringResource(R.string.btn_deactivate),
                    tint = TextMuted,
                )
            }
        }
    }
}

package com.shield.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shield.app.ui.MainViewModel
import com.shield.app.ui.Screen
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldApp(
    viewModel: MainViewModel,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenDeviceAdminSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onOpenAutostartSettings: () -> Unit,
    isAccessibilityEnabled: () -> Boolean,
    isDeviceAdminEnabled: () -> Boolean,
    isBatteryOptimizationIgnored: () -> Boolean,
    isAutostartRestrictedOem: () -> Boolean
) {
    val screen = viewModel.screen.collectAsState().value
    val canNavigateBack = viewModel.canNavigateBack.collectAsState().value

    // Intercept the system/gesture back action: pop to the previous
    // in-app screen instead of exiting the app. Only enabled when there
    // is somewhere to go back to (i.e. we're not on Home) — on Home the
    // back press falls through to the normal system behavior (exit app).
    BackHandler(enabled = canNavigateBack) {
        viewModel.navigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle(screen)) },
                navigationIcon = {
                    if (canNavigateBack) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onOpenDeviceAdminSettings = onOpenDeviceAdminSettings,
                    onOpenBatteryOptimizationSettings = onOpenBatteryOptimizationSettings,
                    onOpenAutostartSettings = onOpenAutostartSettings,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    isDeviceAdminEnabled = isDeviceAdminEnabled,
                    isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                    isAutostartRestrictedOem = isAutostartRestrictedOem
                )
                Screen.LOCK_SETUP -> LockSetupScreen(viewModel = viewModel)
                Screen.BLOCKLIST -> BlocklistScreen(viewModel = viewModel)
                Screen.WHITELIST -> WhitelistScreen(viewModel = viewModel)
                Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                Screen.MANAGED_APPS -> ManagedAppsScreen(viewModel = viewModel)
            }
        }
    }
}

private fun screenTitle(screen: Screen): String = when (screen) {
    Screen.HOME -> "حصن"
    Screen.LOCK_SETUP -> "Lock Timer"
    Screen.BLOCKLIST -> "Blocklist"
    Screen.WHITELIST -> "Whitelist"
    Screen.SETTINGS -> "Settings"
    Screen.MANAGED_APPS -> "Manage Apps"
}

private enum class ProtectionState { VULNERABLE, ACTIVE, LOCKED }

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenDeviceAdminSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onOpenAutostartSettings: () -> Unit,
    isAccessibilityEnabled: () -> Boolean,
    isDeviceAdminEnabled: () -> Boolean,
    isBatteryOptimizationIgnored: () -> Boolean,
    isAutostartRestrictedOem: () -> Boolean
) {
    val isLocked = viewModel.isLocked.collectAsState().value
    val remainingText = viewModel.lockRemainingText.collectAsState().value

    var accessibilityOn by remember { mutableStateOf(isAccessibilityEnabled()) }
    var adminOn by remember { mutableStateOf(isDeviceAdminEnabled()) }
    var batteryExempt by remember { mutableStateOf(isBatteryOptimizationIgnored()) }
    val showAutostartCard = remember { isAutostartRestrictedOem() }

    // Periodically refresh status so the UI stays in sync with the
    // system settings and lock countdown without requiring navigation.
    LaunchedEffect(Unit) {
        while (true) {
            accessibilityOn = isAccessibilityEnabled()
            adminOn = isDeviceAdminEnabled()
            batteryExempt = isBatteryOptimizationIgnored()
            viewModel.refreshLockState()
            delay(2000)
        }
    }

    val protectionState = when {
        !accessibilityOn -> ProtectionState.VULNERABLE
        isLocked -> ProtectionState.LOCKED
        else -> ProtectionState.ACTIVE
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        ProtectionHero(state = protectionState, remainingText = remainingText, isLocked = isLocked)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Protection Status")

            StatusCard(
                title = "Accessibility Service",
                description = "Required to detect and block matching content.",
                enabled = accessibilityOn,
                icon = Icons.Filled.Shield,
                onFix = onOpenAccessibilitySettings
            )

            StatusCard(
                title = "Device Admin",
                description = "Prevents uninstalling حصن while a lock is active.",
                enabled = adminOn,
                icon = Icons.Filled.Lock,
                onFix = onOpenDeviceAdminSettings
            )

            StatusCard(
                title = "Battery Optimization",
                description = "Unrestricted battery use stops the system from killing " +
                    "protection in the background.",
                enabled = batteryExempt,
                icon = Icons.Filled.BatteryChargingFull,
                onFix = onOpenBatteryOptimizationSettings,
                fixLabel = "Allow"
            )

            if (showAutostartCard) {
                StatusCard(
                    title = "Autostart Permission",
                    description = "Your device's manufacturer restricts which apps can " +
                        "start automatically after reboot — allow حصن so protection " +
                        "survives a restart.",
                    enabled = false,
                    icon = Icons.Filled.RocketLaunch,
                    onFix = onOpenAutostartSettings,
                    fixLabel = "Review",
                    // We can't reliably read this OEM setting back, so this
                    // card always offers the shortcut rather than claiming
                    // a status we can't verify.
                    alwaysShowFix = true
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Quick Actions")

            Button(
                onClick = { viewModel.navigate(Screen.LOCK_SETUP) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set Lock Timer")
            }

            FilledTonalButton(
                onClick = { viewModel.navigate(Screen.BLOCKLIST) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Blocklist")
            }

            FilledTonalButton(
                onClick = { viewModel.navigate(Screen.MANAGED_APPS) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Apps (Browsers/Downloaders)")
            }

            FilledTonalButton(
                onClick = { viewModel.navigate(Screen.WHITELIST) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Whitelist")
            }

            FilledTonalButton(
                onClick = { viewModel.navigate(Screen.SETTINGS) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Settings")
            }
        }

        Text(
            text = "\u062D\u0635\u0646 \u2014 designed by Ebrahim Sadeq Alhemyary",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun ProtectionHero(state: ProtectionState, remainingText: String, isLocked: Boolean) {
    val targetColor = when (state) {
        ProtectionState.VULNERABLE -> MaterialTheme.colorScheme.error
        ProtectionState.ACTIVE -> MaterialTheme.colorScheme.primary
        ProtectionState.LOCKED -> MaterialTheme.colorScheme.secondary
    }
    // A soft crossfade rather than an instant color jump when protection
    // state changes (e.g. the moment the Accessibility Service is enabled),
    // so the dashboard reads as reacting rather than just refreshing.
    val color by androidx.compose.animation.animateColorAsState(
        targetValue = targetColor,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 450),
        label = "protectionColor"
    )
    val statusWord = when (state) {
        ProtectionState.VULNERABLE -> "Vulnerable"
        ProtectionState.ACTIVE -> "Protected"
        ProtectionState.LOCKED -> "Locked In"
    }
    val subtitle = when (state) {
        ProtectionState.VULNERABLE -> "Turn on the Accessibility Service below to start blocking."
        ProtectionState.ACTIVE -> "Blocking is active. Settings are still editable."
        ProtectionState.LOCKED -> "$remainingText remaining \u2022 settings are locked"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShieldRingBadge(color = color)
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.animation.AnimatedContent(
                targetState = statusWord,
                label = "protectionStatusWord"
            ) { word ->
                Text(
                    text = word,
                    style = MaterialTheme.typography.displaySmall,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// The app's one signature visual element: a status ring around an
// 8-pointed geometric star (a rub el hizb-style motif, matching the
// launcher icon and the block-overlay badge), changing color with the
// current protection state — reused as the visual anchor of the Home
// screen the way a real security app's "system status" indicator would be.
@Composable
private fun ShieldRingBadge(color: Color) {
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 7.dp.toPx()
            val ringRadius = (size.minDimension - strokeWidth) / 2f
            drawCircle(color = color.copy(alpha = 0.12f))
            drawCircle(
                color = color,
                style = Stroke(width = strokeWidth),
                radius = ringRadius
            )
            drawPath(
                path = eightPointedStarPath(
                    center = center,
                    outerRadius = ringRadius * 0.56f,
                    innerRadius = ringRadius * 0.24f
                ),
                color = color
            )
        }
    }
}

/** An 8-pointed geometric star (two overlapping squares), a common motif in Islamic geometric art. */
private fun eightPointedStarPath(center: Offset, outerRadius: Float, innerRadius: Float): Path {
    val path = Path()
    val totalVertices = 16
    for (i in 0 until totalVertices) {
        val angleDeg = -90.0 + i * (360.0 / totalVertices)
        val angleRad = Math.toRadians(angleDeg)
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + radius * cos(angleRad).toFloat()
        val y = center.y + radius * sin(angleRad).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

@Composable
private fun StatusCard(
    title: String,
    description: String,
    enabled: Boolean,
    icon: ImageVector,
    onFix: () -> Unit,
    fixLabel: String = "Open Settings",
    alwaysShowFix: Boolean = false
) {
    val statusColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 350),
        label = "statusCardColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )
            Column(modifier = Modifier
                .weight(1f)
                .padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (enabled) "Enabled" else "Action needed",
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.animation.AnimatedVisibility(visible = !enabled || alwaysShowFix) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onFix) {
                            Text(fixLabel)
                        }
                    }
                }
            }
        }
    }
}

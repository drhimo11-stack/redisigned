package com.shield.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.core.app.ActivityCompat
import com.shield.app.accessibility.ShieldAccessibilityService
import com.shield.app.accessibility.ShieldKeepAliveService
import com.shield.app.admin.ShieldDeviceAdminReceiver
import com.shield.app.power.DeviceOptimizationHelper
import com.shield.app.ui.MainViewModel
import com.shield.app.ui.screens.ShieldApp
import com.shield.app.ui.theme.ShieldTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS
            )
        }

        ShieldKeepAliveService.start(this)

        setContent {
            ShieldTheme {
                ShieldApp(
                    viewModel = viewModel,
                    onOpenAccessibilitySettings = { openAccessibilitySettings() },
                    onOpenDeviceAdminSettings = { openDeviceAdminSettings() },
                    onOpenBatteryOptimizationSettings = {
                        DeviceOptimizationHelper.requestIgnoreBatteryOptimizations(this)
                    },
                    onOpenAutostartSettings = { openAutostartSettings() },
                    isAccessibilityEnabled = { isAccessibilityServiceEnabled() },
                    isDeviceAdminEnabled = { isDeviceAdminActive() },
                    isBatteryOptimizationIgnored = {
                        DeviceOptimizationHelper.isIgnoringBatteryOptimizations(this)
                    },
                    isAutostartRestrictedOem = { DeviceOptimizationHelper.isKnownAutostartRestrictedOem() }
                )
            }
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openAutostartSettings() {
        val intent = DeviceOptimizationHelper.autostartSettingsIntent(this)
        if (intent != null) {
            startActivity(intent)
        } else {
            // No known/resolvable OEM screen — send them to the app's own
            // settings page as the best generic fallback.
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun openDeviceAdminSettings() {
        val componentName = ComponentName(this, ShieldDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.device_admin_explanation)
            )
        }
        startActivity(intent)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == packageName &&
                it.resolveInfo.serviceInfo.name == ShieldAccessibilityService::class.java.name
        }
    }

    private fun isDeviceAdminActive(): Boolean {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, ShieldDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(componentName)
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 100
    }
}

package com.shield.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shield.app.accessibility.InstalledAppInfo
import com.shield.app.accessibility.InstalledAppScanner
import com.shield.app.blocklist.BlocklistItem
import com.shield.app.blocklist.BlocklistManager
import com.shield.app.blocklist.ManagedAppItem
import com.shield.app.blocklist.ManagedAppManager
import com.shield.app.blocklist.WhitelistItem
import com.shield.app.lock.LockManager
import com.shield.app.lock.SettingsRepository
import com.shield.app.lock.SettingsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen {
    HOME, LOCK_SETUP, BLOCKLIST, WHITELIST, SETTINGS, MANAGED_APPS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val blocklistManager = BlocklistManager.get(application)
    private val settingsRepository = SettingsRepository.get(application)
    private val lockManager = LockManager.get(application)
    private val managedAppManager = ManagedAppManager.get(application)

    // Real navigation back stack (instead of a single current-screen value)
    // so the system back button can pop to the previous in-app screen
    // instead of always exiting the app.
    private val _screenStack = MutableStateFlow(listOf(Screen.HOME))

    val screen: StateFlow<Screen> = _screenStack
        .map { it.last() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Screen.HOME)

    val canNavigateBack: StateFlow<Boolean> = _screenStack
        .map { it.size > 1 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _blocklistItems = MutableStateFlow<List<BlocklistItem>>(emptyList())
    val blocklistItems: StateFlow<List<BlocklistItem>> = _blocklistItems.asStateFlow()

    private val _whitelistItems = MutableStateFlow<List<WhitelistItem>>(emptyList())
    val whitelistItems: StateFlow<List<WhitelistItem>> = _whitelistItems.asStateFlow()

    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    private val _lockRemainingText = MutableStateFlow("0m")
    val lockRemainingText: StateFlow<String> = _lockRemainingText.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _managedApps = MutableStateFlow<List<ManagedAppItem>>(emptyList())
    val managedApps: StateFlow<List<ManagedAppItem>> = _managedApps.asStateFlow()

    init {
        viewModelScope.launch {
            blocklistManager.getRepository().observeBlocklist().collect {
                _blocklistItems.value = it
            }
        }
        viewModelScope.launch {
            blocklistManager.getRepository().observeWhitelist().collect {
                _whitelistItems.value = it
            }
        }
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect {
                _settings.value = it
            }
        }
        viewModelScope.launch {
            managedAppManager.repository.observeAll().collect {
                _managedApps.value = it
            }
        }
        managedAppManager.initialLoad()
        refreshLockState()
    }

    /** Push a new screen onto the back stack. */
    fun navigate(target: Screen) {
        _screenStack.value = _screenStack.value + target
    }

    /**
     * Pop the back stack to the previous screen. Returns true if it popped
     * (there was somewhere to go back to within the app), or false if
     * already at the root (Home) — callers should let the system handle
     * the back press (i.e. exit the app) in that case.
     */
    fun navigateBack(): Boolean {
        val stack = _screenStack.value
        if (stack.size <= 1) return false
        _screenStack.value = stack.dropLast(1)
        return true
    }

    fun refreshLockState() {
        _isLocked.value = lockManager.isLocked()
        _lockRemainingText.value = lockManager.remainingHumanReadable()
    }

    fun startLock(days: Int) {
        lockManager.startLock(days)
        refreshLockState()
    }

    fun addBlockPattern(pattern: String) {
        viewModelScope.launch { blocklistManager.addUserBlock(pattern) }
    }

    fun removeBlockPattern(pattern: String) {
        viewModelScope.launch { blocklistManager.removeUserBlock(pattern) }
    }

    fun addWhitelistPattern(pattern: String) {
        viewModelScope.launch { blocklistManager.addWhitelist(pattern) }
    }

    fun removeWhitelistPattern(pattern: String) {
        viewModelScope.launch { blocklistManager.removeWhitelist(pattern) }
    }

    fun setCustomMessage(message: String) {
        viewModelScope.launch { settingsRepository.setCustomMessage(message) }
    }

    fun setCountdown(seconds: Int) {
        viewModelScope.launch { settingsRepository.setCountdown(seconds) }
    }

    fun setRedirectUrl(url: String) {
        viewModelScope.launch { settingsRepository.setRedirectUrl(url) }
    }

    fun setBlockUnsupportedBrowsers(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBlockUnsupportedBrowsers(enabled) }
    }

    fun setScheduleEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setScheduleEnabled(enabled) }
    }

    fun setScheduleWindow(startMinute: Int, endMinute: Int) {
        viewModelScope.launch { settingsRepository.setScheduleWindow(startMinute, endMinute) }
    }

    fun setScheduleDaysMask(mask: Int) {
        viewModelScope.launch { settingsRepository.setScheduleDaysMask(mask) }
    }

    // --- Managed apps (Manage Apps screen) — usable regardless of lock
    // state, since managing which apps get scanned is a Shield-side
    // configuration action, not something the lock is meant to restrict. ---

    fun addManagedApp(packageName: String, appLabel: String) {
        viewModelScope.launch { managedAppManager.add(packageName, appLabel, autoDetected = false) }
    }

    fun setManagedAppBlocked(packageName: String, blocked: Boolean) {
        viewModelScope.launch { managedAppManager.setBlocked(packageName, blocked) }
    }

    fun removeManagedApp(packageName: String) {
        viewModelScope.launch { managedAppManager.remove(packageName) }
    }

    /** Candidate apps for the add-picker, off the main thread. */
    suspend fun listInstallableApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        InstalledAppScanner.listLaunchableApps(getApplication())
    }
}

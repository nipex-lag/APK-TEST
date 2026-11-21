package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KeyValidator
import com.example.data.PreferencesRepository
import com.example.engine.SystemTelemetryEngine
import com.example.model.AuthState
import com.example.model.DnsTarget
import com.example.model.KeyTier
import com.example.model.ModuleConfig
import com.example.model.OverlayUiState
import com.example.model.TelemetrySnapshot
import com.example.model.TeleportAnchor
import com.example.service.FloatingOverlayService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val keyInput: String = "",
    val generatedKeyPreview: String? = null,
    val generatedKeyIsPro: Boolean = false,
    val isGetKeySheetOpen: Boolean = false,
)

class NipexViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _isCleaningMemory = MutableStateFlow(false)
    val isCleaningMemory: StateFlow<Boolean> = _isCleaningMemory.asStateFlow()

    val moduleConfig: StateFlow<ModuleConfig> = SystemTelemetryEngine.moduleConfig
    val telemetry: StateFlow<TelemetrySnapshot> = SystemTelemetryEngine.telemetry
    val overlayUiState: StateFlow<OverlayUiState> = SystemTelemetryEngine.overlayUiState

    init {
        SystemTelemetryEngine.initialize(application)
        viewModelScope.launch {
            val session = prefsRepo.sessionFlow.first()
            SystemTelemetryEngine.updateModuleConfig(session.moduleConfig)
            val savedKey = session.activatedKey
            if (!savedKey.isNullOrBlank()) {
                val check = KeyValidator.validate(savedKey)
                if (check.isValid && check.tier != null) {
                    _authState.value = AuthState.Authenticated(
                        keyCode = check.normalizedKey,
                        tier = check.tier,
                    )
                } else {
                    _authState.value = AuthState.Unauthenticated()
                }
            } else {
                _authState.value = AuthState.Unauthenticated()
            }
        }
    }

    fun onKeyInputChanged(value: String) {
        _loginUiState.update { it.copy(keyInput = value.uppercase()) }
        if (_authState.value is AuthState.Unauthenticated) {
            _authState.value = AuthState.Unauthenticated(errorMessage = null)
        }
    }

    fun openGetKeySheet(proEdition: Boolean = true) {
        val key = KeyValidator.generateInstantKey(proEdition = proEdition)
        _loginUiState.update {
            it.copy(
                generatedKeyPreview = key,
                generatedKeyIsPro = proEdition,
                isGetKeySheetOpen = true,
            )
        }
    }

    fun regenerateKeyInSheet(proEdition: Boolean) {
        val key = KeyValidator.generateInstantKey(proEdition = proEdition)
        _loginUiState.update {
            it.copy(
                generatedKeyPreview = key,
                generatedKeyIsPro = proEdition,
            )
        }
    }

    fun closeGetKeySheet() {
        _loginUiState.update { it.copy(isGetKeySheetOpen = false) }
    }

    fun applyGeneratedKeyAndVerify() {
        val generated = _loginUiState.value.generatedKeyPreview ?: return
        _loginUiState.update {
            it.copy(
                keyInput = generated,
                isGetKeySheetOpen = false,
            )
        }
        verifyAndActivateKey()
    }

    fun verifyAndActivateKey() {
        val raw = _loginUiState.value.keyInput
        viewModelScope.launch {
            _authState.value = AuthState.Verifying
            delay(650L) // Smooth cyber calibration animation
            val result = KeyValidator.validate(raw)
            if (result.isValid && result.tier != null) {
                prefsRepo.saveActivation(result.normalizedKey, result.tier)
                _authState.value = AuthState.Authenticated(
                    keyCode = result.normalizedKey,
                    tier = result.tier,
                )
            } else {
                _authState.value = AuthState.Unauthenticated(errorMessage = result.message)
            }
        }
    }

    fun signOutKey() {
        stopFloatingOverlayService()
        viewModelScope.launch {
            prefsRepo.clearActivation()
            _loginUiState.update { it.copy(keyInput = "") }
            _authState.value = AuthState.Unauthenticated()
        }
    }

    fun refreshPermissions() {
        SystemTelemetryEngine.refreshOverlayPermission(getApplication())
    }

    fun startFloatingOverlayService() {
        val app = getApplication<Application>()
        if (!Settings.canDrawOverlays(app)) {
            SystemTelemetryEngine.refreshOverlayPermission(app)
            SystemTelemetryEngine.setStatusToast("Grant Display Over Other Apps permission first.")
            return
        }
        val intent = Intent(app, FloatingOverlayService::class.java).apply {
            action = FloatingOverlayService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }
    }

    fun stopFloatingOverlayService() {
        val app = getApplication<Application>()
        val intent = Intent(app, FloatingOverlayService::class.java).apply {
            action = FloatingOverlayService.ACTION_STOP
        }
        runCatching { app.stopService(intent) }
        SystemTelemetryEngine.setOverlayServiceRunning(false)
    }

    fun toggleInAppHudExpanded() {
        val current = SystemTelemetryEngine.overlayUiState.value.inAppHudExpanded
        SystemTelemetryEngine.setInAppHudExpanded(!current)
    }

    fun toggleInAppHudVisible() {
        val current = SystemTelemetryEngine.overlayUiState.value.inAppHudVisible
        SystemTelemetryEngine.setInAppHudVisible(!current)
    }

    fun clearStatusToast() {
        SystemTelemetryEngine.setStatusToast(null)
    }

    fun triggerPingProbe() {
        SystemTelemetryEngine.triggerImmediatePingProbe()
        SystemTelemetryEngine.setStatusToast("Probing ${moduleConfig.value.dnsTarget.host} latency...")
    }

    fun cleanMemoryNow() {
        if (_isCleaningMemory.value) return
        viewModelScope.launch {
            _isCleaningMemory.value = true
            val freedMb = SystemTelemetryEngine.cleanSystemMemory()
            _isCleaningMemory.value = false
            SystemTelemetryEngine.setStatusToast("Memory sweep complete: ${freedMb} MB reclaimed")
        }
    }

    // Module config mutators
    fun setFreezeEnabled(enabled: Boolean) = updateConfig { copy(freezeEnabled = enabled) }
    fun setFreezeHoldMs(ms: Int) = updateConfig { copy(freezeHoldMs = ms) }

    fun setTeleportEnabled(enabled: Boolean) = updateConfig { copy(teleportEnabled = enabled) }
    fun setTeleportAnchor(anchor: TeleportAnchor) = updateConfig {
        copy(teleportAnchor = anchor, teleportEnabled = true)
    }
    fun setTeleportSnapSensitivity(value: Float) = updateConfig { copy(teleportSnapSensitivity = value) }

    fun setGhostEnabled(enabled: Boolean) = updateConfig { copy(ghostEnabled = enabled) }
    fun setGhostOpacity(opacity: Float) = updateConfig { copy(ghostOpacity = opacity.coerceIn(0.2f, 1.0f)) }
    fun setGhostAutoFade(autoFade: Boolean) = updateConfig { copy(ghostAutoFade = autoFade) }

    fun setNetworkBoosterEnabled(enabled: Boolean) = updateConfig { copy(networkBoosterEnabled = enabled) }
    fun setPingIntervalMs(ms: Int) = updateConfig { copy(pingIntervalMs = ms) }
    fun setDnsTarget(target: DnsTarget) = updateConfig { copy(dnsTarget = target) }

    fun setAntiLagEnabled(enabled: Boolean) = updateConfig { copy(antiLagEnabled = enabled) }
    fun setAutoTrimThreshold(percent: Int) = updateConfig { copy(autoTrimThresholdPercent = percent) }

    fun setFpsMonitorEnabled(enabled: Boolean) = updateConfig { copy(fpsMonitorEnabled = enabled) }
    fun setTargetFpsThreshold(fps: Int) = updateConfig { copy(targetFpsThreshold = fps) }

    private fun updateConfig(transform: ModuleConfig.() -> ModuleConfig) {
        val updated = SystemTelemetryEngine.moduleConfig.value.transform()
        SystemTelemetryEngine.updateModuleConfig(updated)
        viewModelScope.launch {
            prefsRepo.saveModuleConfig(updated)
        }
    }
}

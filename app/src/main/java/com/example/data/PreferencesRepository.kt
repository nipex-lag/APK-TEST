package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.DnsTarget
import com.example.model.KeyTier
import com.example.model.ModuleConfig
import com.example.model.TeleportAnchor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nipexDataStore: DataStore<Preferences> by preferencesDataStore(name = "nipex_lag_v12_prefs")

data class StoredSession(
    val activatedKey: String? = null,
    val tier: KeyTier = KeyTier.TRIAL_24H,
    val moduleConfig: ModuleConfig = ModuleConfig(),
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val ACTIVATED_KEY = stringPreferencesKey("activated_key")
        val KEY_TIER = stringPreferencesKey("key_tier")

        val FREEZE_ENABLED = booleanPreferencesKey("freeze_enabled")
        val FREEZE_HOLD_MS = intPreferencesKey("freeze_hold_ms")

        val TELEPORT_ENABLED = booleanPreferencesKey("teleport_enabled")
        val TELEPORT_ANCHOR = stringPreferencesKey("teleport_anchor")
        val TELEPORT_SNAP = floatPreferencesKey("teleport_snap")

        val GHOST_ENABLED = booleanPreferencesKey("ghost_enabled")
        val GHOST_OPACITY = floatPreferencesKey("ghost_opacity")
        val GHOST_AUTO_FADE = booleanPreferencesKey("ghost_auto_fade")

        val NET_BOOSTER_ENABLED = booleanPreferencesKey("net_booster_enabled")
        val PING_INTERVAL_MS = intPreferencesKey("ping_interval_ms")
        val DNS_TARGET = stringPreferencesKey("dns_target")

        val ANTI_LAG_ENABLED = booleanPreferencesKey("anti_lag_enabled")
        val AUTO_TRIM_THRESHOLD = intPreferencesKey("auto_trim_threshold")

        val FPS_MONITOR_ENABLED = booleanPreferencesKey("fps_monitor_enabled")
        val TARGET_FPS_THRESHOLD = intPreferencesKey("target_fps_threshold")
    }

    val sessionFlow: Flow<StoredSession> = context.nipexDataStore.data.map { prefs ->
        val tierName = prefs[Keys.KEY_TIER] ?: KeyTier.TRIAL_24H.name
        val tier = runCatching { KeyTier.valueOf(tierName) }.getOrDefault(KeyTier.TRIAL_24H)

        val anchorName = prefs[Keys.TELEPORT_ANCHOR] ?: TeleportAnchor.TOP_RIGHT.name
        val anchor = runCatching { TeleportAnchor.valueOf(anchorName) }.getOrDefault(TeleportAnchor.TOP_RIGHT)

        val dnsName = prefs[Keys.DNS_TARGET] ?: DnsTarget.CLOUDFLARE.name
        val dns = runCatching { DnsTarget.valueOf(dnsName) }.getOrDefault(DnsTarget.CLOUDFLARE)

        StoredSession(
            activatedKey = prefs[Keys.ACTIVATED_KEY],
            tier = tier,
            moduleConfig = ModuleConfig(
                freezeEnabled = prefs[Keys.FREEZE_ENABLED] ?: false,
                freezeHoldMs = prefs[Keys.FREEZE_HOLD_MS] ?: 2000,
                teleportEnabled = prefs[Keys.TELEPORT_ENABLED] ?: false,
                teleportAnchor = anchor,
                teleportSnapSensitivity = prefs[Keys.TELEPORT_SNAP] ?: 0.85f,
                ghostEnabled = prefs[Keys.GHOST_ENABLED] ?: false,
                ghostOpacity = prefs[Keys.GHOST_OPACITY] ?: 0.48f,
                ghostAutoFade = prefs[Keys.GHOST_AUTO_FADE] ?: true,
                networkBoosterEnabled = prefs[Keys.NET_BOOSTER_ENABLED] ?: true,
                pingIntervalMs = prefs[Keys.PING_INTERVAL_MS] ?: 1500,
                dnsTarget = dns,
                antiLagEnabled = prefs[Keys.ANTI_LAG_ENABLED] ?: true,
                autoTrimThresholdPercent = prefs[Keys.AUTO_TRIM_THRESHOLD] ?: 80,
                fpsMonitorEnabled = prefs[Keys.FPS_MONITOR_ENABLED] ?: true,
                targetFpsThreshold = prefs[Keys.TARGET_FPS_THRESHOLD] ?: 60,
            ),
        )
    }

    suspend fun saveActivation(key: String, tier: KeyTier) {
        context.nipexDataStore.edit { prefs ->
            prefs[Keys.ACTIVATED_KEY] = key
            prefs[Keys.KEY_TIER] = tier.name
        }
    }

    suspend fun clearActivation() {
        context.nipexDataStore.edit { prefs ->
            prefs.remove(Keys.ACTIVATED_KEY)
            prefs.remove(Keys.KEY_TIER)
        }
    }

    suspend fun saveModuleConfig(config: ModuleConfig) {
        context.nipexDataStore.edit { prefs ->
            prefs[Keys.FREEZE_ENABLED] = config.freezeEnabled
            prefs[Keys.FREEZE_HOLD_MS] = config.freezeHoldMs
            prefs[Keys.TELEPORT_ENABLED] = config.teleportEnabled
            prefs[Keys.TELEPORT_ANCHOR] = config.teleportAnchor.name
            prefs[Keys.TELEPORT_SNAP] = config.teleportSnapSensitivity
            prefs[Keys.GHOST_ENABLED] = config.ghostEnabled
            prefs[Keys.GHOST_OPACITY] = config.ghostOpacity
            prefs[Keys.GHOST_AUTO_FADE] = config.ghostAutoFade
            prefs[Keys.NET_BOOSTER_ENABLED] = config.networkBoosterEnabled
            prefs[Keys.PING_INTERVAL_MS] = config.pingIntervalMs
            prefs[Keys.DNS_TARGET] = config.dnsTarget.name
            prefs[Keys.ANTI_LAG_ENABLED] = config.antiLagEnabled
            prefs[Keys.AUTO_TRIM_THRESHOLD] = config.autoTrimThresholdPercent
            prefs[Keys.FPS_MONITOR_ENABLED] = config.fpsMonitorEnabled
            prefs[Keys.TARGET_FPS_THRESHOLD] = config.targetFpsThreshold
        }
    }
}

package com.example.data

import com.example.model.KeyTier
import kotlin.random.Random

object KeyValidator {

    private val masterKeys = mapOf(
        "NIPEX-V12-PRO-2026" to KeyTier.VIP_PRO,
        "NIPEX-VIP-9941" to KeyTier.VIP_PRO,
        "NIPEX-ELITE-V12" to KeyTier.ELITE_V12,
        "NIPEX-2026-VIP" to KeyTier.VIP_PRO,
    )

    data class ValidationResult(
        val isValid: Boolean,
        val normalizedKey: String,
        val tier: KeyTier?,
        val message: String,
    )

    fun validate(rawInput: String): ValidationResult {
        val cleaned = rawInput.trim().uppercase()
        if (cleaned.isEmpty()) {
            return ValidationResult(
                isValid = false,
                normalizedKey = cleaned,
                tier = null,
                message = "Enter an activation key or tap GET KEY.",
            )
        }

        masterKeys[cleaned]?.let { tier ->
            return ValidationResult(
                isValid = true,
                normalizedKey = cleaned,
                tier = tier,
                message = "License verified: ${tier.badgeLabel}",
            )
        }

        // Dynamic checksum format: NIPEX-V12-XXXX-CC
        val regex = Regex("^NIPEX-V12-([A-Z0-9]{4})-([0-9A-F]{2})$")
        val match = regex.matchEntire(cleaned)
        if (match != null) {
            val payload = match.groupValues[1]
            val checksum = match.groupValues[2]
            if (computeChecksum(payload) == checksum) {
                val tier = if (payload.startsWith("P")) KeyTier.VIP_PRO else KeyTier.TRIAL_24H
                return ValidationResult(
                    isValid = true,
                    normalizedKey = cleaned,
                    tier = tier,
                    message = "License verified: ${tier.badgeLabel}",
                )
            }
        }

        return ValidationResult(
            isValid = false,
            normalizedKey = cleaned,
            tier = null,
            message = "Invalid license signature. Tap GET KEY for an instant key.",
        )
    }

    fun generateInstantKey(proEdition: Boolean = false): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val prefixChar = if (proEdition) 'P' else 'T'
        val suffix = buildString {
            append(prefixChar)
            repeat(3) {
                append(alphabet[Random.nextInt(alphabet.length)])
            }
        }
        val checksum = computeChecksum(suffix)
        return "NIPEX-V12-$suffix-$checksum"
    }

    private fun computeChecksum(payload: String): String {
        var acc = 0x5A
        payload.forEachIndexed { index, ch ->
            acc = (acc + ch.code * (index + 3)) and 0xFF
        }
        return acc.toString(16).uppercase().padStart(2, '0')
    }
}

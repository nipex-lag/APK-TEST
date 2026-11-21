package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.KeyValidator
import com.example.model.KeyTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NIPEX LAG V12", appName)
    }

    @Test
    fun `generated VIP key passes instant validation`() {
        val proKey = KeyValidator.generateInstantKey(proEdition = true)
        val result = KeyValidator.validate(proKey)
        assertTrue(result.isValid)
        assertEquals(KeyTier.VIP_PRO, result.tier)

        val trialKey = KeyValidator.generateInstantKey(proEdition = false)
        val trialResult = KeyValidator.validate(trialKey)
        assertTrue(trialResult.isValid)
        assertEquals(KeyTier.TRIAL_24H, trialResult.tier)
    }

    @Test
    fun `invalid key fails validation cleanly`() {
        val invalid = KeyValidator.validate("BAD-KEY-0000")
        assertFalse(invalid.isValid)
    }
}

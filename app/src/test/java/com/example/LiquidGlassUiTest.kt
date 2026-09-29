package com.example

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.camera.CameraHaptics
import com.example.ui.components.LiquidEnvironment
import com.example.ui.components.LiquidGlassButtonVariant
import com.example.ui.components.PerformanceMode
import com.example.ui.theme.AmberStudio
import com.example.ui.theme.DarkBg
import com.example.ui.theme.GlassBorderTop
import com.example.ui.theme.GlassSurfaceLight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiquidGlassUiTest {

    // TEST A : Palette et Thème sombre cinématique
    @Test
    fun testLiquidGlassColorPalette() {
        assertNotNull(DarkBg)
        assertNotNull(AmberStudio)
        assertNotNull(GlassBorderTop)
        assertNotNull(GlassSurfaceLight)

        // DarkBg must be deep obsidian / midnight tone
        assertEquals(Color(0xFF06080C), DarkBg)
    }

    // TEST B : Variantes de boutons Liquid Glass
    @Test
    fun testLiquidGlassButtonVariants() {
        val variants = LiquidGlassButtonVariant.entries
        assertTrue(variants.contains(LiquidGlassButtonVariant.PRIMARY))
        assertTrue(variants.contains(LiquidGlassButtonVariant.SECONDARY))
        assertTrue(variants.contains(LiquidGlassButtonVariant.SUBTLE))
        assertTrue(variants.contains(LiquidGlassButtonVariant.ACCENT))
    }

    // TEST C : Environnements et Modes de performance Liquid Background
    @Test
    fun testLiquidEnvironmentsAndPerformanceModes() {
        val envs = LiquidEnvironment.entries
        assertEquals(6, envs.size)
        assertTrue(envs.any { it.name == "OBSIDIAN" })
        assertTrue(envs.any { it.name == "MIDNIGHT" })
        assertTrue(envs.any { it.name == "DEEP_OCEAN" })

        val perfModes = PerformanceMode.entries
        assertEquals(3, perfModes.size)
        assertTrue(perfModes.contains(PerformanceMode.STANDARD))
        assertTrue(perfModes.contains(PerformanceMode.ECONOMY))
        assertTrue(perfModes.contains(PerformanceMode.DISABLED))
    }

    // TEST D : Haptiques sécurisés et non bloquants
    @Test
    fun testCameraHapticsExecution() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Ensure calling haptics does not crash or throw on Robolectric
        CameraHaptics.playLightTick(context)
        CameraHaptics.playShutterTick(context)
        CameraHaptics.playLevelLock(context)
    }
}

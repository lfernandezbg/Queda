package com.luisete.queda.feature.inventory

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraLifecycleTest {
    @Test
    fun coordinatorLifecycle() {
        val coordinator = CameraResourceCoordinator { }

        // Initial state
        assertNotNull(coordinator.imageAnalysis)
        assertNotNull(coordinator.executor)
        assertNotNull(coordinator.analyzer)
        assertFalse(coordinator.executor.isShutdown)

        // Dispose
        coordinator.close()
        assertTrue(coordinator.executor.isShutdown)

        // Idempotent dispose
        coordinator.close()
    }
}

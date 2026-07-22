package com.luisete.queda.feature.inventory

import org.junit.Assert.assertEquals
import org.junit.Test

class BarcodeScannerPermissionLogicTest {
    @Test
    fun `resolvePermissionState - granted`() {
        val state =
            resolvePermissionState(
                isGranted = true,
                shouldShowRationale = false,
                hadPreviousCompletedRequest = false,
            )
        assertEquals(PermissionState.GRANTED, state)
    }

    @Test
    fun `resolvePermissionState - first rejection with rationale`() {
        val state =
            resolvePermissionState(
                isGranted = false,
                shouldShowRationale = true,
                hadPreviousCompletedRequest = false,
            )
        assertEquals(PermissionState.DENIED, state)
    }

    @Test
    fun `resolvePermissionState - first rejection without rationale`() {
        val state =
            resolvePermissionState(
                isGranted = false,
                shouldShowRationale = false,
                hadPreviousCompletedRequest = false,
            )
        assertEquals(PermissionState.DENIED, state)
    }

    @Test
    fun `resolvePermissionState - later rejection with rationale`() {
        val state =
            resolvePermissionState(
                isGranted = false,
                shouldShowRationale = true,
                hadPreviousCompletedRequest = true,
            )
        assertEquals(PermissionState.DENIED, state)
    }

    @Test
    fun `resolvePermissionState - later rejection without rationale`() {
        val state =
            resolvePermissionState(
                isGranted = false,
                shouldShowRationale = false,
                hadPreviousCompletedRequest = true,
            )
        assertEquals(PermissionState.PERMANENTLY_DENIED, state)
    }
}

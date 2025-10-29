package com.sap.codelab

import android.content.Context
import androidx.core.content.ContextCompat
import com.sap.codelab.repository.Repository
import com.sap.codelab.service.LocationService
import com.sap.codelab.service.LocationServiceStartingProcessor
import com.sap.codelab.utils.permissions.isAllPermissionsGranted
import com.sap.codelab.utils.permissions.isPostNotificationsGranted
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class LocationServiceStartingProcessorTest {

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(ContextCompat::class)
        mockkStatic("com.sap.codelab.utils.permissions.PermissionsUtilsKt")
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        unmockkStatic("com.sap.codelab.utils.permissions.PermissionsUtilsKt")
        unmockkObject(Repository)
    }

    @Test
    fun `startService is called only when permissions granted`() = runTest {
        LocationService.isRunning.set(false)

        every { context.isAllPermissionsGranted() } returns true
        every { context.isPostNotificationsGranted() } returns true

        every { context.applicationContext } returns context

        every { ContextCompat.startForegroundService(any(), any()) } returns mockk()

        mockkObject(Repository)
        coEvery { Repository.hasOpenWithLocationFlow() } returns flowOf(true)

        LocationServiceStartingProcessor.observeAndManageLocationService(context)

        verify(exactly = 1) { ContextCompat.startForegroundService(eq(context), any()) }
    }
}
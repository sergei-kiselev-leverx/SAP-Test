package com.sap.codelab

import android.location.Location
import com.sap.codelab.model.Memo
import com.sap.codelab.repository.Repository
import com.sap.codelab.service.LocationNotificationHelper
import com.sap.codelab.service.LocationUpdateProcessor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [com.sap.codelab.service.LocationUpdateProcessor].
 * The test ensures that when a location update occurs,
 * the processor correctly queries nearby memos and triggers notifications.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class LocationUpdateProcessorTest {

    private val repo = mockk<Repository>()
    private val notificationHelper = mockk<LocationNotificationHelper>(relaxed = true)
    private val processor = LocationUpdateProcessor(repo, notificationHelper)

    // Fake user location for testing
    private val fakeLocation = Location("mock").apply {
        latitude = 41.0
        longitude = 41.0
    }

    @Test
    fun `notifies for each nearby memo`() = runTest {
        // Given: repository returns two memos near the location
        val memos = listOf(
            Memo(1, "Title1", "Description1", 0, false),
            Memo(2, "Title2", "Description2", 0, false)
        )

        coEvery {
            repo.findNearMemoByFlatDistance(
                latitude = 41.0,
                longitude = 41.0,
                radiusMeters = 200.0
            )
        } returns memos

        // When: a location update occurs
        processor.onLocationUpdate(fakeLocation)

        // Then: repository is queried exactly once with correct parameters
        coVerify(exactly = 1) {
            repo.findNearMemoByFlatDistance(41.0, 41.0, 200.0)
        }

        // Then: a notification is shown for each memo found
        verify(exactly = memos.size) {
            notificationHelper.showMemoLocatedNotification(match { it in memos })
        }
    }
}
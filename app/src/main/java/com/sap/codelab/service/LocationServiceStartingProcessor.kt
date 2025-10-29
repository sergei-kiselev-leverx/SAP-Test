package com.sap.codelab.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.sap.codelab.repository.Repository
import com.sap.codelab.utils.permissions.isAllPermissionsGranted
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * A processor responsible for observing opened memos and starting the [LocationService]
 * when any of them have an associated location. This ensures that the location service is
 * running only when it is actually needed.
 */
object LocationServiceStartingProcessor {
    suspend fun observeAndManageLocationService(context: Context) {
        Repository.hasOpenWithLocationFlow()
            .distinctUntilChanged()
            .collect { hasMemosWithLocation ->
                startOrStopIfNeed(hasMemosWithLocation, context)
            }
    }

    suspend fun startLocationServiceIfNeed(context: Context) {
        val hasMemosWithLocation = Repository.hasOpenWithLocation()

        startOrStopIfNeed(hasMemosWithLocation, context)
    }

    private fun startOrStopIfNeed(hasMemosWithLocation: Boolean, context: Context) {
        if (hasMemosWithLocation) {
            startService(context.applicationContext)
        } else {
            stopService(context.applicationContext)
        }
    }

    private fun startService(context: Context) {
        if (!context.isAllPermissionsGranted() || LocationService.isRunning.get()) {
            return
        }

        ContextCompat.startForegroundService(
            context, Intent(context, LocationService::class.java)
        )
    }

    private fun stopService(context: Context) {
        if (!LocationService.isRunning.get()) return

        context.stopService(Intent(context, LocationService::class.java))
    }
}
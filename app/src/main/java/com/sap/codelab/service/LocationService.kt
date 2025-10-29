package com.sap.codelab.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sap.codelab.repository.Repository
import com.sap.codelab.utils.coroutines.ScopeProvider
import com.sap.codelab.utils.permissions.isAllPermissionsGranted
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class LocationService : Service() {

    companion object {
        private const val TAG = "LocationService"
        private const val LOCATION_REQUEST_INTERVAL_MS = 10_000L
        private const val LOCATION_REQUEST_DEBOUNCE_MS = 5_000L
        private const val LOCATION_REQUEST_MIN_UPDATES_METERS = 10.0f

        val isRunning = AtomicBoolean(false)
    }

    private var fusedLocationClient: FusedLocationProviderClient? = null

    private val notificationHelper by lazy { LocationNotificationHelper(applicationContext) }
    private val coroutineScope = ScopeProvider.supervisorScope()
    private val locationProcessor: LocationUpdateProcessor by lazy {
        LocationUpdateProcessor(
            repository = Repository,
            notificationHelper = notificationHelper,
        )
    }
    private val locationCallback by lazy {
        object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                val lastLocation = p0.lastLocation ?: return

                coroutineScope.launch {
                    try {
                        locationProcessor.onLocationUpdate(lastLocation)
                    } catch (e: Throwable) {
                        coroutineScope.ensureActive()
                        Log.e(TAG, "Failed to process location update", e)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning.set(true)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isAllPermissionsGranted()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = notificationHelper.createLocationServiceNotification()
        startForeground(
            LocationNotificationHelper.NOTIFICATION_LOCATION_SERVICE_ID,
            notification
        )

        requestLocationUpdates()

        return START_STICKY
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION])
    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest
            .Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_REQUEST_INTERVAL_MS)
            .setMinUpdateIntervalMillis(LOCATION_REQUEST_DEBOUNCE_MS)
            .setMinUpdateDistanceMeters(LOCATION_REQUEST_MIN_UPDATES_METERS)
            .setWaitForAccurateLocation(true)
            .build()

        fusedLocationClient?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    override fun onDestroy() {
        fusedLocationClient?.removeLocationUpdates(locationCallback)
        fusedLocationClient = null
        coroutineScope.cancel()

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
            NotificationManagerCompat.from(this)
                .cancel(LocationNotificationHelper.NOTIFICATION_LOCATION_SERVICE_ID)
        } catch (_: Throwable) {
        }

        super.onDestroy()
        isRunning.set(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
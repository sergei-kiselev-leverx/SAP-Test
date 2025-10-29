package com.sap.codelab.view.location

import android.annotation.SuppressLint
import android.app.Activity
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.sap.codelab.model.MemoLocation
import com.sap.codelab.utils.permissions.isFineLocationGranted
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

internal class ChooseLocationViewModel : ViewModel() {
    companion object {
        private val BATUMI_LOCATION = MemoLocation(41.6413, 41.6359)
        private val DEFAULT_LOCATION = BATUMI_LOCATION
    }

    private val _data = MutableStateFlow(DEFAULT_LOCATION)
    val data = _data.asStateFlow()
    private var initialUserLocationLoad: Job? = null

    fun updateLocation(latitude: Double, longitude: Double) {
        _data.value = MemoLocation(latitude = latitude, longitude = longitude)
        initialUserLocationLoad?.cancel()
    }

    fun initArgs(args: ChooseLocationArgs, activity: Activity) {
        if (args.location != null) {
            _data.value = args.location
        } else {
            updateLocationByCurrent(activity)
        }
    }

    private fun updateLocationByCurrent(activity: Activity) {
        if (!activity.isFineLocationGranted()) {
            return
        }
        initialUserLocationLoad = viewModelScope.launch {
            val location = getCurrentLocation(activity)
            if (location != null) {
                updateLocation(location.latitude, location.longitude)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(activity: Activity): Location? {
        return coroutineScope {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)

            val lastKnownLocation = async {
                runCatching { fusedLocationClient.lastLocation.await() }.getOrNull()
            }
            val highAccuracyLocation = async {
                runCatching {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        CancellationTokenSource().token
                    ).await()
                }.getOrNull()
            }

            lastKnownLocation.await() ?: highAccuracyLocation.await()
        }
    }
}
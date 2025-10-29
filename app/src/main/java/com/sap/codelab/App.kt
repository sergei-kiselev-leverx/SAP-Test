package com.sap.codelab

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import com.sap.codelab.repository.Repository
import com.sap.codelab.service.LocationServiceStartingProcessor
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

/**
 * Extension of the Android Application class.
 */
internal class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Repository.initialize(this)
        initOsmDroid()
        initLocationService()
    }

    private fun initLocationService() {
        ProcessLifecycleOwner.Companion.get().lifecycleScope.launch {
            ProcessLifecycleOwner.Companion.get().repeatOnLifecycle(Lifecycle.State.STARTED) {
                LocationServiceStartingProcessor.observeAndManageLocationService(applicationContext)
            }
        }
    }

    private fun initOsmDroid() {
        with(Configuration.getInstance()) {
            load(
                applicationContext,
                PreferenceManager.getDefaultSharedPreferences(applicationContext)
            )
            userAgentValue = packageName
        }
    }
}
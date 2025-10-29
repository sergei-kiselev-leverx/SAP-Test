package com.sap.codelab.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sap.codelab.repository.Repository
import com.sap.codelab.utils.coroutines.ScopeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receiver that restarts LocationService after device reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Repository.initialize(context.applicationContext)

            ScopeProvider.newScope(Dispatchers.IO).launch {
                LocationServiceStartingProcessor.startLocationServiceIfNeed(context.applicationContext)
            }
        }
    }
}

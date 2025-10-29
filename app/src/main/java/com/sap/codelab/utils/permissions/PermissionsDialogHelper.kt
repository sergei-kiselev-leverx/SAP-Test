package com.sap.codelab.utils.permissions

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import com.sap.codelab.R

/**
 * Helper for showing permission-related dialogs and opening system settings.
 */
object PermissionDialogHelper {

    /**
     * Returns all permissions required by the app.
     */
    fun getRequiredPermissions(): Array<String> {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        return permissions.toTypedArray()
    }

    /**
     * Shows a non-cancelable dialog that requires the user
     * to grant permissions or exit the app.
     */
    fun showBlockingPermissionDialog(
        context: Context,
        launcher: ActivityResultLauncher<Array<String>>,
        onExitClick: () -> Unit,
    ): Dialog {
        val dialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.permission_dialog_title))
            .setMessage(context.getString(R.string.permission_dialog_message))
            .setCancelable(false)
            .setNegativeButton(context.getString(R.string.permission_dialog_exit), null)
            .setPositiveButton(context.getString(R.string.permission_dialog_grant), null)
            .create()

        dialog.setOnShowListener {
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setOnClickListener {
                onExitClick()
            }

            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                launcher.launch(getRequiredPermissions())
            }
        }

        dialog.show()

        return dialog
    }

    /**
     * Shows a cancelable dialog for optional permission requests.
     */
    fun showPermissionDialog(
        context: Context,
        launcher: ActivityResultLauncher<Array<String>>
    ) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.permission_dialog_title))
            .setMessage(context.getString(R.string.permission_dialog_message))
            .setCancelable(true)
            .setNegativeButton(context.getString(R.string.permission_dialog_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(context.getString(R.string.permission_dialog_grant)) { dialog, _ ->
                launcher.launch(getRequiredPermissions())
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Opens the system app settings for manual permission management.
     */
    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        context.startActivity(intent)
    }
}
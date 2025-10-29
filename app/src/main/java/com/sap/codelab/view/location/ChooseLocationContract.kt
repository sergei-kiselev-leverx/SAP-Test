package com.sap.codelab.view.location

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.IntentCompat

class ChooseLocationContract : ActivityResultContract<ChooseLocationArgs, ChooseLocationResult>() {
    companion object {
        private const val EXTRA = "location"

        fun getArgs(intent: Intent?): ChooseLocationArgs {
            return intent?.let {
                IntentCompat.getParcelableExtra(
                    it,
                    EXTRA,
                    ChooseLocationArgs::class.java
                )
            } ?: ChooseLocationArgs(location = null)
        }

        fun createResult(output: ChooseLocationResult): Intent {
            return Intent().apply {
                putExtra(EXTRA, output)
            }
        }
    }

    override fun createIntent(context: Context, input: ChooseLocationArgs): Intent {
        return Intent(context, ChooseLocationActivity::class.java).apply {
            putExtra(EXTRA, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ChooseLocationResult {
        return intent?.let {
            IntentCompat.getParcelableExtra(
                it,
                EXTRA,
                ChooseLocationResult::class.java
            )
        }
            ?: ChooseLocationResult(location = null)
    }
}
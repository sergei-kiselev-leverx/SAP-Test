package com.sap.codelab.view.detail

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sap.codelab.R
import com.sap.codelab.databinding.ActivityViewMemoBinding
import com.sap.codelab.databinding.ContentCreateMemoBinding
import com.sap.codelab.model.Memo
import com.sap.codelab.model.MemoLocation
import com.sap.codelab.utils.extensions.setupEdgeToEdge
import com.sap.codelab.view.location.ChooseLocationArgs
import com.sap.codelab.view.location.ChooseLocationContract
import kotlinx.coroutines.launch

internal const val BUNDLE_MEMO_ID: String = "memoId"

/**
 * Activity that allows a user to see the details of a memo.
 */
internal class ViewMemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewMemoBinding
    private val locationLauncher = registerForActivityResult(ChooseLocationContract()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewMemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupEdgeToEdge(binding.root)
        // Initialize views with the passed memo id
        val model = ViewModelProvider(this)[ViewMemoViewModel::class.java]
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.memo.collect { value ->
                    value?.let { memo ->
                        // Update the UI whenever the memo changes
                        updateUI(memo)
                    }
                }
            }
        }
        val id = getIdFromDeeplink() ?: getIdFromParams() ?: -1
        model.loadMemo(id)

        initUi()
    }

    private fun getIdFromDeeplink(): Long? {
        return intent?.data?.getQueryParameter("id")?.toLongOrNull()
    }

    private fun getIdFromParams(): Long? {
        return intent?.extras?.getLong(BUNDLE_MEMO_ID)
    }

    private fun initUi() {
        with(binding.contentCreateMemo) {
            memoTitle.isEnabled = false
            memoDescription.isEnabled = false
            memoLocationContainer.isEnabled = false
        }
    }

    /**
     * Updates the UI with the given memo details.
     *
     * @param memo - the memo whose details are to be displayed.
     */
    private fun updateUI(memo: Memo) {
        binding.contentCreateMemo.run {
            memoTitle.setText(memo.title)
            memoDescription.setText(memo.description)
            updateLocationUi(memo.location)
        }
    }

    private fun ContentCreateMemoBinding.updateLocationUi(location: MemoLocation?) {
        val text = location
            ?.let { getString(R.string.location_data, it.latitude, it.longitude) }
            .orEmpty()

        memoLocation.setText(text)
        memoLocationSeeOnMapButton.setOnClickListener {
            locationLauncher.launch(
                ChooseLocationArgs(
                    location = location,
                    canChooseLocation = false,
                )
            )
        }
        memoLocationSeeOnMapButton.isVisible = true
    }
}

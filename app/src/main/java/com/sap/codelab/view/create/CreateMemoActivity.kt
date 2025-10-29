package com.sap.codelab.view.create

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.sap.codelab.R
import com.sap.codelab.databinding.ActivityCreateMemoBinding
import com.sap.codelab.utils.extensions.empty
import com.sap.codelab.utils.extensions.setupEdgeToEdge
import com.sap.codelab.utils.permissions.PermissionDialogHelper
import com.sap.codelab.utils.permissions.isAllPermissionsGranted
import com.sap.codelab.view.location.ChooseLocationArgs
import com.sap.codelab.view.location.ChooseLocationContract

/**
 * Activity that allows a user to create a new Memo.
 */
internal class CreateMemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateMemoBinding
    private lateinit var model: CreateMemoViewModel

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                openChooseLocation()
            } else {
                PermissionDialogHelper.openSettings(this)
            }
        }

    private val chooseLocationLauncher =
        registerForActivityResult(ChooseLocationContract()) { result ->
            model.updateMemoLocation(result.location)
            updateLocationInfo()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateMemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupEdgeToEdge(binding.root)
        model = ViewModelProvider(this)[CreateMemoViewModel::class.java]

        setupClickListeners()
        initData()
    }

    private fun initData() {
        updateLocationInfo()
    }

    private fun updateLocationInfo() {
        with(binding.contentCreateMemo) {
            memoLocation.setText(model.getMemoLocation()?.let { "${it.latitude}, ${it.longitude}" })
        }
    }

    private fun setupClickListeners() {
        with(binding.contentCreateMemo) {
            memoLocation.setOnClickListener {
                if (isAllPermissionsGranted()) {
                    openChooseLocation()
                } else {
                    PermissionDialogHelper.showPermissionDialog(
                        this@CreateMemoActivity,
                        permissionsLauncher
                    )
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_create_memo, menu)
        return true
    }

    /**
     * Handles actionbar interactions.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveMemo()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Saves the memo if the input is valid; otherwise shows the corresponding error messages.
     */
    private fun saveMemo() {
        binding.contentCreateMemo.run {
            model.updateMemo(memoTitle.text.toString(), memoDescription.text.toString())
            if (model.isMemoValid()) {
                model.saveMemo()
                setResult(RESULT_OK)
                finish()
            } else {
                memoTitleContainer.error =
                    getErrorMessage(model.hasTitleError(), R.string.memo_title_empty_error)
                memoDescriptionContainer.error =
                    getErrorMessage(model.hasTextError(), R.string.memo_text_empty_error)
                memoLocationContainer.error =
                    getErrorMessage(model.hasLocationError(), R.string.memo_location_empty_error)
            }
        }
    }

    /**
     * Returns the error message if there is an error, or an empty string otherwise.
     *
     * @param hasError          - whether there is an error.
     * @param errorMessageResId - the resource id of the error message to show.
     * @return the error message if there is an error, or an empty string otherwise.
     */
    private fun getErrorMessage(hasError: Boolean, @StringRes errorMessageResId: Int): String {
        return if (hasError) {
            getString(errorMessageResId)
        } else {
            String.empty()
        }
    }

    private fun openChooseLocation() {
        chooseLocationLauncher.launch(
            ChooseLocationArgs(location = model.getMemoLocation())
        )
    }
}

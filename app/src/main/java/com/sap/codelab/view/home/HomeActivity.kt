package com.sap.codelab.view.home

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.sap.codelab.R
import com.sap.codelab.databinding.ActivityHomeBinding
import com.sap.codelab.model.Memo
import com.sap.codelab.service.LocationService
import com.sap.codelab.utils.extensions.setupEdgeToEdge
import com.sap.codelab.utils.permissions.PermissionDialogHelper
import com.sap.codelab.view.create.CreateMemoActivity
import com.sap.codelab.view.detail.BUNDLE_MEMO_ID
import com.sap.codelab.view.detail.ViewMemoActivity
import kotlinx.coroutines.launch

/**
 * The main activity of the app. Shows a list of recorded memos and lets the user add new memos.
 */
internal class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var viewModel: HomeViewModel
    private var permissionDialog: Dialog? = null

    private val locationPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                startLocationService()
            } else {
                PermissionDialogHelper.openSettings(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupEdgeToEdge(binding.root)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // Setup the adapter and the recycler view
        setupRecyclerView(initializeAdapter())

        binding.fab.setOnClickListener {
            // Handles clicks on the FAB button > creates a new Memo
            startActivity(Intent(this, CreateMemoActivity::class.java))
        }

        collectLocationServiceStartState()
    }

    /**
     * Observes the state that decides whether the location service should start.
     *
     * The app checks location permissions at startup only if there are existing memos
     * that include a location. In this case, the background service must run to monitor
     * proximity and trigger reminders.
     *
     * If no memos with location exist, the app neither starts the service
     * nor requests any permissions — avoiding unnecessary prompts.
     *
     * This approach ensures the service runs only when it is actually needed
     * while still recovering automatically if permissions were revoked.
     */
    private fun collectLocationServiceStartState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.locationServiceState.collect {
                    when (it) {
                        is LocationServiceStartState.ShouldStart -> {
                            dismissPermissionDialog()
                            startLocationService()
                        }

                        is LocationServiceStartState.RequestPermissions -> {
                            dismissPermissionDialog()
                            permissionDialog = PermissionDialogHelper.showBlockingPermissionDialog(
                                context = this@HomeActivity,
                                launcher = locationPermissionsLauncher,
                                onExitClick = { finishAffinity() },
                            )
                        }

                        is LocationServiceStartState.NoReasonToStart -> {
                            dismissPermissionDialog()
                        }
                    }
                }
            }
        }
    }

    private fun dismissPermissionDialog() {
        permissionDialog?.dismiss()
        permissionDialog = null
    }

    private fun startLocationService() {
        Intent(applicationContext, LocationService::class.java).apply {
            startService(this)
        }
    }

    /**
     * We request location permissions at app startup instead of when the map is opened
     * because the location service must start as soon as the app launches.
     * This service handles background updates and reminders, and it needs location access
     * even before the map screen is shown. Requesting permissions early ensures that:
     * 1) The service can be (re)started immediately if it wasn't running previously.
     * 2) If the permissions were revoked by the user, the app can detect it early
     *    and reinitialize the service once permissions are granted again.
     * 3) The map and other location-dependent features are ready to use without delays.
     *
     */
    override fun onResume() {
        super.onResume()
        viewModel.onResume(this)
    }

    /**
     * Initializes the adapter and sets the needed callbacks.
     */
    private fun initializeAdapter(): MemoAdapter {
        val adapter = MemoAdapter(mutableListOf(), { view ->
            // Implementation for when the user selects a row to show the detail view
            showMemo((view.tag as Memo).id)
        }, { checkbox, isChecked ->
            // Implementation for when the user marks a memo as completed
            viewModel.onUpdateMemo(checkbox.tag as Memo, isChecked)
        })
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.memos.collect { memos ->
                    adapter.setItems(memos)
                }
            }
        }
        return adapter
    }

    /**
     * Opens the Memo detail view for the given memoId.
     *
     * @param memoId    - the id of the memo to be shown.
     */
    private fun showMemo(memoId: Long) {
        val intent = Intent(this@HomeActivity, ViewMemoActivity::class.java)
        intent.putExtra(BUNDLE_MEMO_ID, memoId)
        startActivity(intent)
    }

    /**
     * Initializes the recycler view to display the list of memos.
     */
    private fun setupRecyclerView(adapter: MemoAdapter) {
        binding.contentHome.recyclerView.apply {
            layoutManager =
                LinearLayoutManager(this@HomeActivity, LinearLayoutManager.VERTICAL, false)
            this.adapter = adapter
            addItemDecoration(
                DividerItemDecoration(
                    this@HomeActivity,
                    (layoutManager as LinearLayoutManager).orientation
                )
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        val menuItemShowAll = menu.findItem(R.id.action_show_all)
        val menuItemShowOpen = menu.findItem(R.id.action_show_open)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.showAll.collect {
                    if (it) {
                        menuItemShowAll.isVisible = false
                        menuItemShowOpen.isVisible = true
                    } else {
                        menuItemShowOpen.isVisible = false
                        menuItemShowAll.isVisible = true
                    }
                }
            }
        }
        return true
    }

    /**
     * Handles actionbar interactions.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_show_all -> {
                viewModel.onChangeMemosScope(true)
                true
            }

            R.id.action_show_open -> {
                viewModel.onChangeMemosScope(false)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

}

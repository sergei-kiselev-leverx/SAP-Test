package com.sap.codelab.view.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sap.codelab.model.Memo
import com.sap.codelab.repository.Repository
import com.sap.codelab.utils.coroutines.ScopeProvider
import com.sap.codelab.utils.permissions.isAllPermissionsGranted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home Activity.
 */
internal class HomeViewModel : ViewModel() {

    private val _showAll = MutableStateFlow(false)
    val showAll: StateFlow<Boolean> = _showAll

    private val _locationServiceState: MutableStateFlow<LocationServiceStartState> =
        MutableStateFlow(LocationServiceStartState.NoReasonToStart)
    val locationServiceState: StateFlow<LocationServiceStartState> = _locationServiceState

    @OptIn(ExperimentalCoroutinesApi::class)
    val memos: StateFlow<List<Memo>> = showAll
        .flatMapLatest { all -> if (all) Repository.getAll() else Repository.getOpen() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun onChangeMemosScope(shouldShowAll: Boolean) {
        _showAll.value = shouldShowAll
    }

    /**
     * Updates the given memo, marking it as done if isChecked is true.
     *
     * @param memo      - the memo to update.
     * @param isChecked - whether the memo has been checked (marked as done).
     */
    fun onUpdateMemo(memo: Memo, isChecked: Boolean) {
        ScopeProvider.application.launch(Dispatchers.IO) {
            // We'll only forward the update if the memo has been checked, since we don't offer to uncheck memos right now
            if (isChecked) {
                Repository.saveMemo(memo.copy(isDone = true))
            }
        }
    }

    fun onResume(context: Context) {
        showResuestPermissionsIfNeed(context)
    }

    private fun showResuestPermissionsIfNeed(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val shouldStartLocationService = Repository.hasOpenWithLocation()
            val isPermissionsGranted = context.isAllPermissionsGranted()

            _locationServiceState.value = if (shouldStartLocationService && isPermissionsGranted) {
                LocationServiceStartState.ShouldStart
            } else if (shouldStartLocationService) {
                LocationServiceStartState.RequestPermissions
            } else {
                LocationServiceStartState.NoReasonToStart
            }
        }
    }
}
package com.sap.codelab.view.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sap.codelab.model.Memo
import com.sap.codelab.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for matching ViewMemo view.
 */
internal class ViewMemoViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _memo: MutableStateFlow<Memo?> = MutableStateFlow(null)
    val memo: StateFlow<Memo?> = _memo

    /**
     * Loads the memo whose id matches the given memoId from the database.
     */
    fun loadMemo(memoId: Long) {
        val saved = savedStateHandle.get<Long>(BUNDLE_MEMO_ID)
        val id = saved ?: memoId
        if (saved == null) savedStateHandle[BUNDLE_MEMO_ID] = id

        viewModelScope.launch(Dispatchers.Default) {
            _memo.value = Repository.getMemoById(id)
        }
    }
}
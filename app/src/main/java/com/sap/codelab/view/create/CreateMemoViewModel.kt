package com.sap.codelab.view.create

import androidx.lifecycle.ViewModel
import com.sap.codelab.model.Memo
import com.sap.codelab.model.MemoLocation
import com.sap.codelab.repository.Repository
import com.sap.codelab.utils.coroutines.ScopeProvider
import com.sap.codelab.utils.extensions.empty
import kotlinx.coroutines.launch

/**
 * ViewModel for matching CreateMemo view. Handles user interactions.
 */
internal class CreateMemoViewModel : ViewModel() {

    private var memo = Memo(
        id = 0,
        title = String.empty(),
        description = String.empty(),
        reminderDate = 0,
        isDone = false,
    )

    /**
     * Saves the memo in it's current state.
     */
    fun saveMemo() {
        ScopeProvider.application.launch {
            Repository.saveMemo(memo)
        }
    }

    /**
     * Call this method to update the memo. This is usually needed when the user changed his input.
     */
    fun updateMemo(title: String, description: String) {
        memo = Memo(
            title = title,
            description = description,
            id = 0,
            reminderDate = 0,
            isDone = false,
            location = memo.location?.copy(),
        )
    }

    fun updateMemoLocation(location: MemoLocation?) {
        memo = memo.copy(
            location = location,
        )
    }

    fun getMemoLocation(): MemoLocation? {
        return memo.location
    }

    /**
     * @return true if the title and content are not blank; false otherwise.
     */
    fun isMemoValid(): Boolean =
        memo.title.isNotBlank() && memo.description.isNotBlank() && memo.location != null

    /**
     * @return true if the memo text is blank, false otherwise.
     */
    fun hasTextError() = memo.description.isBlank()

    /**
     * @return true if the memo title is blank, false otherwise.
     */
    fun hasTitleError() = memo.title.isBlank()

    /**
     * @return true if the memo location is null, false otherwise.
     */
    fun hasLocationError() = memo.location == null
}
package com.sap.codelab

import com.sap.codelab.model.MemoLocation
import com.sap.codelab.view.create.CreateMemoViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateMemoViewModelTest {

    private val viewModel = CreateMemoViewModel()

    @Test
    fun `memo is invalid when empty`() {
        assertFalse(viewModel.isMemoValid())
    }

    @Test
    fun `memo becomes valid when title desc and location are set`() {
        viewModel.updateMemo("Title", "Desc")
        viewModel.updateMemoLocation(MemoLocation(1.0, 2.0))
        assertTrue(viewModel.isMemoValid())
    }

    @Test
    fun `hasTitleError returns true if title blank`() {
        viewModel.updateMemo("", "Desc")
        assertTrue(viewModel.hasTitleError())
    }

    @Test
    fun `hasLocationError returns true if no location`() {
        viewModel.updateMemo("T", "D")
        assertTrue(viewModel.hasLocationError())
    }
}

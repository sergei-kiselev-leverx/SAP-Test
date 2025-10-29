package com.sap.codelab.view.location

import android.os.Parcelable
import com.sap.codelab.model.MemoLocation
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChooseLocationArgs(
    val location: MemoLocation?,
    val canChooseLocation: Boolean = true,
) : Parcelable
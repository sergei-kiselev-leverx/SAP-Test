package com.sap.codelab.view.location

import android.os.Parcelable
import com.sap.codelab.model.MemoLocation
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChooseLocationResult(
    val location: MemoLocation?,
) : Parcelable
package com.sap.codelab.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

/**
 * Represents a memo location.
 *
 * @param latitude The latitude of the location.
 * @param longitude The longitude of the location.
 */
@Parcelize
data class MemoLocation(
    @ColumnInfo(name = "reminderLatitude")
    val latitude: Double,
    @ColumnInfo(name = "reminderLongitude")
    val longitude: Double,
) : Parcelable
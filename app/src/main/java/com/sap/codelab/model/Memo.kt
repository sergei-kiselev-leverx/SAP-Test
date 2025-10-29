package com.sap.codelab.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a memo.
 */
@Entity(
    tableName = "memo",
    indices = [
        Index(value = ["isDone"], name = "idx_memo_isDone"),
        Index(
            value = ["isDone", "reminderLatitude", "reminderLongitude"],
            name = "idx_memo_isDone_lat_lon"
        )
    ],
)
internal data class Memo(
    @ColumnInfo(name = "id")
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    @ColumnInfo(name = "title")
    var title: String,
    @ColumnInfo(name = "description")
    var description: String,
    @ColumnInfo(name = "reminderDate")
    var reminderDate: Long,
    @ColumnInfo(name = "isDone")
    var isDone: Boolean = false,
    @Embedded
    val location: MemoLocation? = null,
)

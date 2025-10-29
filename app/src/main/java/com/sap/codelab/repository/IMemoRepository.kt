package com.sap.codelab.repository

import com.sap.codelab.model.Memo
import kotlinx.coroutines.flow.Flow

/**
 * Interface for a repository offering memo related CRUD operations.
 */
internal interface IMemoRepository {

    /**
     * Saves the given memo to the database.
     */
    fun saveMemo(memo: Memo)

    /**
     * @return all memos currently in the database.
     */
    fun getAll(): Flow<List<Memo>>

    /**
     * @return all memos currently in the database, except those that have been marked as "done".
     */
    fun getOpen(): Flow<List<Memo>>

    /**
     * @return memos from the database that have been marked as "done" and not null coordinates.
     */
    fun hasOpenWithLocationFlow(): Flow<Boolean>

    /**
     * @return memos from the database that have been marked as "done" and not null coordinates.
     */
    suspend fun hasOpenWithLocation(): Boolean

    /**
     * @return the memo whose id matches the given id.
     */
    fun getMemoById(id: Long): Memo

    /**
     * @param latitude The latitude of the location.
     * @param longitude The longitude of the location.
     * @param radiusMeters The radius of search in meters.
     *
     * @return all nearby memos.
     */
    suspend fun findNearMemoByFlatDistance(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double
    ): List<Memo>
}
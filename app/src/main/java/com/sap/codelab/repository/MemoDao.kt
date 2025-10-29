package com.sap.codelab.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sap.codelab.model.Memo
import kotlinx.coroutines.flow.Flow

/**
 * The Dao representation of a Memo.
 */
@Dao
internal interface MemoDao {

    /**
     * @return all memos that are currently in the database.
     */
    @Query("SELECT * FROM memo")
    fun getAll(): Flow<List<Memo>>

    /**
     * @return all memos that are currently in the database and have not yet been marked as "done".
     */
    @Query("SELECT * FROM memo WHERE isDone = 0")
    fun getOpen(): Flow<List<Memo>>

    /**
     * @return memos that are currently in the database and have not yet been marked as "done" and have a location.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM memo
            WHERE isDone = 0
              AND reminderLatitude  IS NOT NULL
              AND reminderLongitude IS NOT NULL
        )
    """
    )
    fun hasOpenWithLocation(): Flow<Boolean>

    /**
     * Inserts the given Memo into the database. We currently do not support updating of memos.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(memo: Memo)

    /**
     * @return the memo whose id matches the given id.
     */
    @Query("SELECT * FROM memo WHERE id = :memoId")
    fun getMemoById(memoId: Long): Memo

    /**
     * Calculates the squared flat-Earth distance between two latitude/longitude points.
     *
     * Each degree of latitude is approximately 111,320 meters everywhere on Earth,
     * while each degree of longitude equals 111,320 * cos(latitude) meters.
     *
     * We multiply coordinate differences by those scale factors to convert them to meters,
     * then apply the Pythagorean theorem (Δx² + Δy²). The query keeps only points
     * whose squared distance is less than or equal to (radiusMeters)².
     *
     * This approach assumes a locally flat Earth surface, which is accurate enough
     * for short distances (e.g. ≤ 1–2 km) and avoids expensive trigonometric functions.
     * *
     * @param lat The latitude of the location.
     * @param lon The longitude of the location.
     * @param latCos The cosine of the latitude.
     * @param degPerLat The degrees per latitude.
     * @param degPerLon The degrees per longitude.
     * @param radiusMeters The radius of search in meters.
     *
     * @return all memos that are currently in the database and have not yet been marked as "done".
     *
     */
    @Query(
        """
                SELECT *
                FROM memo
                WHERE isDone = 0
                  AND reminderLatitude BETWEEN (:lat - :degPerLat) AND (:lat + :degPerLat)
                  AND reminderLongitude BETWEEN (:lon - :degPerLon) AND (:lon + :degPerLon)
                  AND (
                      ((reminderLatitude - :lat) * 111320.0) * ((reminderLatitude - :lat) * 111320.0) +
                      ((reminderLongitude - :lon) * 111320.0 * :latCos) * ((reminderLongitude - :lon) * 111320.0 * :latCos)
                  ) <= (:radiusMeters * :radiusMeters)
        """
    )
    fun findNearOpenedMemoByFlatDistance(
        lat: Double,
        lon: Double,
        latCos: Double,
        degPerLat: Double,
        degPerLon: Double,
        radiusMeters: Double
    ): List<Memo>
}
package com.sap.codelab.repository

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.room.Room
import com.sap.codelab.model.Memo
import com.sap.codelab.repository.migrations.MIGRATION_1_2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

private const val DATABASE_NAME: String = "codelab"

/**
 * The repository is used to retrieve data from a data source.
 */
internal object Repository : IMemoRepository {

    lateinit var database: Database

    fun initialize(applicationContext: Context) {
        if (::database.isInitialized) return

        database = Room
            .databaseBuilder(applicationContext, Database::class.java, DATABASE_NAME)
            .addMigrations(
                MIGRATION_1_2,
            )
            .build()
    }

    @WorkerThread
    override fun saveMemo(memo: Memo) {
        database.getMemoDao().insert(memo)
    }

    @WorkerThread
    override fun hasOpenWithLocationFlow(): Flow<Boolean> =
        database.getMemoDao().hasOpenWithLocation()

    @WorkerThread
    override suspend fun hasOpenWithLocation(): Boolean =
        database.getMemoDao().hasOpenWithLocation().firstOrNull() == true

    @WorkerThread
    override fun getOpen(): Flow<List<Memo>> = database.getMemoDao().getOpen()

    @WorkerThread
    override fun getAll(): Flow<List<Memo>> = database.getMemoDao().getAll()

    @WorkerThread
    override fun getMemoById(id: Long): Memo = database.getMemoDao().getMemoById(id)

    @WorkerThread
    override suspend fun findNearMemoByFlatDistance(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double
    ): List<Memo> {
        val latCos = kotlin.math.cos(Math.toRadians(latitude))
        val degPerLat = radiusMeters / 111_320.0
        val degPerLon = radiusMeters / (111_320.0 * latCos)

        return database.getMemoDao().findNearOpenedMemoByFlatDistance(
            lat = latitude,
            lon = longitude,
            latCos = latCos,
            degPerLat = degPerLat,
            degPerLon = degPerLon,
            radiusMeters = radiusMeters
        )
    }
}
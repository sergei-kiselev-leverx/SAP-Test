package com.sap.codelab

import android.location.Location
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sap.codelab.model.Memo
import com.sap.codelab.model.MemoLocation
import com.sap.codelab.repository.Database
import com.sap.codelab.repository.IMemoRepository
import com.sap.codelab.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Repository-level tests for findNearMemoByFlatDistance(radius=200m).
 *
 * What we verify here:
 *  - Repository computes params and delegates to DAO correctly.
 *  - Open memos (<200 m, and ~199 m "boundary") are included.
 *  - Closed memos (isDone=1) are excluded even if within radius.
 *  - Empty DB returns empty result.
 *  - DB is cleaned between tests to avoid cross-test coupling.
 *
 * NOTE: All coordinates are hardcoded; no math in tests.
 */
@RunWith(AndroidJUnit4::class)
class FindNearMemoFlatDistanceTest {

    private lateinit var db: Database
    private lateinit var repository: IMemoRepository

    // Batumi center.
    private val baseLatitude = 41.64635
    private val baseLongitude = 41.6247383
    private val radiusMeters = 200.0

    private data class Seed(
        val title: String,
        val latitude: Double,
        val longitude: Double,
        val isDone: Boolean
    ) {
        fun toMemo(): Memo = Memo(
            title = title,
            isDone = isDone,
            location = MemoLocation(
                latitude = latitude,
                longitude = longitude,
            ),
            reminderDate = 0,
            id = 0,
            description = "",
        )
    }

    private val defaultSeeds = listOf(
        // < 200 m — should be INCLUDED
        Seed("inside_50m_north", 41.64680, 41.62474, false),
        Seed("inside_100m_east", 41.64635, 41.62580, false),
        Seed("inside_150m_southwest", 41.64500, 41.62370, false),
        Seed("inside_180m_northwest", 41.64760, 41.62380, false),

        // ≈ 200 m — should be INCLUDED (boundary)
        Seed("boundary_199m_north", 41.64813, 41.62474, false),
        Seed("boundary_199m_east", 41.64635, 41.62712, false),

        // > 200 m — should be EXCLUDED
        Seed("outside_250m_east", 41.64635, 41.62774, false),
        Seed("outside_300m_south", 41.64365, 41.62474, false),
        Seed("outside_350m_west", 41.64635, 41.62053, false),

        // < 200 m but isDone=1 — should be EXCLUDED
        Seed("done_inside_100m", 41.64560, 41.62490, true)
    )


    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            Database::class.java
        )
            .allowMainThreadQueries()
            .build()

        repository = Repository.also {
            it.database = db
        }

    }

    @After
    fun tearDown() {
        db.clearAllTables()
        db.close()
    }

    @Test
    fun within200m_areReturned_outside200m_areExcluded_and_doneAreExcluded(): Unit = runBlocking {
        insertSeeds(defaultSeeds)

        val result = repository.findNearMemoByFlatDistance(
            latitude = baseLatitude,
            longitude = baseLongitude,
            radiusMeters = radiusMeters
        )

        val expectedIds = defaultSeeds
            .filter { !it.isDone }
            .filter {
                getRealDistance(it.latitude, it.longitude) <= radiusMeters
            }
            .map { it.title }
            .toSet()

        val actualIds = result.map { it.title }.toSet()

        // THEN: only inside + boundary memos (isDone=0) are included
        val expectedIncluded = setOf(
            "inside_50m_north",
            "inside_100m_east",
            "inside_150m_southwest",
            "inside_180m_northwest",
            "boundary_199m_north",
            "boundary_199m_east"
        )

        assertEquals(expectedIds, actualIds)
        assertEquals(expectedIncluded, actualIds)
    }

    @Test
    fun repository_returns_empty_on_empty_db() = runBlocking {
        // No inserts here.

        val result = repository.findNearMemoByFlatDistance(
            latitude = baseLatitude,
            longitude = baseLongitude,
            radiusMeters = radiusMeters
        )

        assert(result.isEmpty())
    }

    @Test
    fun repository_returns_empty_when_all_inside_are_done(): Unit = runBlocking {
        val doneInsideSeeds = listOf(
            // < 200 m but closed
            Seed("done_1", 41.64680, 41.62474, true),
            // < 200 m but closed
            Seed("done_2", 41.64635, 41.62580, true),
            // ~199 m but closed
            Seed("done_3", 41.64813, 41.62474, true),
        )
        insertSeeds(doneInsideSeeds)

        val result = repository.findNearMemoByFlatDistance(
            latitude = baseLatitude,
            longitude = baseLongitude,
            radiusMeters = radiusMeters
        )

        assert(result.isEmpty())
    }

    private fun insertSeeds(seeds: List<Seed>) {
        seeds.forEach { repository.saveMemo(it.toMemo()) }
    }

    private fun getRealDistance(latitude: Double, longitude: Double): Float {
        val location1 = Location("test1").apply {
            this.latitude = baseLatitude
            this.longitude = baseLongitude
        }
        val location2 = Location("test2").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
        return location1.distanceTo(location2)
    }
}
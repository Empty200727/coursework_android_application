package ru.kinopolka.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.kinopolka.core.model.MediaType
import ru.kinopolka.core.model.WatchStatus

/**
 * Base migration test (docs/PLAN.md, section 6): the exported schema of version 1 matches the
 * entities, and a database created from it keeps the library when the app opens it. Every new
 * schema version adds `helper.runMigrationsAndValidate(DB_NAME, N, true, MIGRATION_X_N)` here.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KinopolkaDatabase::class.java,
    )

    @Test
    fun `schema 1 is valid and the library survives opening`() = runTest {
        helper.createDatabase(DB_NAME, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO media (media_type, tmdb_id, title, original_title, overview, is_overview_fallback,
                    poster_path, backdrop_path, release_date, last_air_date, vote_average, vote_count, popularity,
                    runtime, number_of_seasons, in_production, cached_at, details_cached_at)
                VALUES ('movie', 550, 'Бойцовский клуб', 'Fight Club', NULL, 0, '/p.jpg', NULL, '1999-10-15',
                    NULL, 8.4, 29870, 73.4, 139, NULL, NULL, 1000, 1000)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO library_entry (media_type, tmdb_id, watch_status, is_favorite, added_at, watched_at,
                    user_rating, local_poster_path)
                VALUES ('movie', 550, 'WATCHED', 1, 2000, 3000, NULL, NULL)
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 1, true).close()

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KinopolkaDatabase::class.java,
            DB_NAME,
        ).allowMainThreadQueries().build()
        try {
            val entry = database.libraryDao().observeEntry(MediaType.MOVIE, 550).first()
            assertEquals(WatchStatus.WATCHED, entry?.watchStatus)
            assertEquals("Бойцовский клуб", database.mediaDao().get(MediaType.MOVIE, 550)?.title)
        } finally {
            database.close()
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}

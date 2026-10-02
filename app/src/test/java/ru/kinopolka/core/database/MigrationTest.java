package ru.kinopolka.core.database;

import static org.junit.Assert.assertEquals;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.WatchStatus;

/**
 * Base migration test (docs/PLAN.md, section 6): the exported schema of version 1 matches the
 * entities, and a database created from it keeps the library when the app opens it. Every new
 * schema version adds {@code helper.runMigrationsAndValidate(DB_NAME, N, true, MIGRATION_X_N)} here.
 */
@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    private static final String DB_NAME = "migration-test.db";

    @Rule
    public final MigrationTestHelper helper =
            new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), KinopolkaDatabase.class);

    @Test
    public void schema1IsValidAndTheLibrarySurvivesOpening() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(DB_NAME, 1)) {
            db.execSQL("INSERT INTO media (media_type, tmdb_id, title, original_title, overview, is_overview_fallback,"
                    + " poster_path, backdrop_path, release_date, last_air_date, vote_average, vote_count, popularity,"
                    + " runtime, number_of_seasons, in_production, cached_at, details_cached_at)"
                    + " VALUES ('movie', 550, 'Бойцовский клуб', 'Fight Club', NULL, 0, '/p.jpg', NULL, '1999-10-15',"
                    + " NULL, 8.4, 29870, 73.4, 139, NULL, NULL, 1000, 1000)");
            db.execSQL("INSERT INTO library_entry (media_type, tmdb_id, watch_status, is_favorite, added_at,"
                    + " watched_at, user_rating, local_poster_path)"
                    + " VALUES ('movie', 550, 'WATCHED', 1, 2000, 3000, NULL, NULL)");
        }

        helper.runMigrationsAndValidate(DB_NAME, 1, true).close();

        KinopolkaDatabase database = Room.databaseBuilder(ApplicationProvider.getApplicationContext(),
                KinopolkaDatabase.class, DB_NAME).allowMainThreadQueries().build();
        try {
            assertEquals(WatchStatus.WATCHED, database.libraryDao().get(MediaType.MOVIE, 550).watchStatus);
            assertEquals("Бойцовский клуб", database.mediaDao().get(MediaType.MOVIE, 550).title);
        } finally {
            database.close();
        }
    }
}

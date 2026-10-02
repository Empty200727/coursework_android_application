package ru.kinopolka.core.database;

import androidx.annotation.Nullable;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import ru.kinopolka.core.database.entity.MediaEntity;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.testing.TestDatabase;

/** Base class for DAO tests: a fresh in-memory database per test. */
public abstract class DatabaseTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutor = new InstantTaskExecutorRule();

    protected KinopolkaDatabase database;

    @Before
    public void createDatabase() {
        database = TestDatabase.create();
    }

    @After
    public void closeDatabase() {
        database.close();
    }

    protected static MediaEntity media(int id) {
        return media(id, MediaType.MOVIE, "Фильм " + id, 1_000L, null, null);
    }

    protected static MediaEntity media(int id, MediaType type) {
        return media(id, type, "Фильм " + id, 1_000L, null, null);
    }

    protected static MediaEntity media(int id, long cachedAt) {
        return media(id, MediaType.MOVIE, "Фильм " + id, cachedAt, null, null);
    }

    protected static MediaEntity media(int id, MediaType type, String title, long cachedAt, @Nullable Integer runtime,
            @Nullable Long detailsCachedAt) {
        return new MediaEntity(type, id, title, null, "Описание " + id, false, "/" + id + ".jpg", null,
                "2020-01-01", null, 7.5, 1000, 10.0, runtime, null, null, cachedAt, detailsCachedAt);
    }
}

package ru.kinopolka.testing;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import ru.kinopolka.core.database.KinopolkaDatabase;

/** In-memory Room database; queries and LiveData run on the calling thread. */
public final class TestDatabase {

    private TestDatabase() {
    }

    public static KinopolkaDatabase create() {
        Context context = ApplicationProvider.getApplicationContext();
        return Room.inMemoryDatabaseBuilder(context, KinopolkaDatabase.class)
                .allowMainThreadQueries()
                .setQueryExecutor(Runnable::run)
                .setTransactionExecutor(Runnable::run)
                .build();
    }
}

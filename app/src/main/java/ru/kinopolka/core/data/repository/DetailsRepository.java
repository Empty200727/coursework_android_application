package ru.kinopolka.core.data.repository;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import ru.kinopolka.core.data.RefreshResult;
import ru.kinopolka.core.model.MediaDetails;
import ru.kinopolka.core.model.MediaKey;

/** Title card (F-08…F-10): read from Room, loaded from TMDB with one request and cached for 24 hours. */
public interface DetailsRepository {

    /** Emits {@code null} while nothing is known about the title. */
    LiveData<MediaDetails> observeDetails(MediaKey key);

    @WorkerThread
    RefreshResult refreshDetails(MediaKey key, boolean force);
}

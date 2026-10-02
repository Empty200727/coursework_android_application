package ru.kinopolka.testing;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.kinopolka.core.data.RefreshResult;
import ru.kinopolka.core.data.repository.DetailsRepository;
import ru.kinopolka.core.model.MediaDetails;
import ru.kinopolka.core.model.MediaKey;

public final class FakeDetailsRepository implements DetailsRepository {

    private final Map<MediaKey, MutableLiveData<MediaDetails>> details = new HashMap<>();
    public volatile RefreshResult refreshResult = RefreshResult.UPDATED;
    public final List<MediaKey> refreshed = Collections.synchronizedList(new ArrayList<>());

    private synchronized MutableLiveData<MediaDetails> data(MediaKey key) {
        return details.computeIfAbsent(key, ignored -> new MutableLiveData<>(null));
    }

    public void setDetails(MediaDetails value) {
        data(value.media().key()).postValue(value);
    }

    @Override
    public LiveData<MediaDetails> observeDetails(MediaKey key) {
        return data(key);
    }

    @Override
    public RefreshResult refreshDetails(MediaKey key, boolean force) {
        refreshed.add(key);
        return refreshResult;
    }
}

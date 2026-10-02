package ru.kinopolka.testing;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import ru.kinopolka.core.data.util.NetworkMonitor;

public final class FakeNetworkMonitor implements NetworkMonitor {

    public final MutableLiveData<Boolean> online;

    public FakeNetworkMonitor() {
        this(true);
    }

    public FakeNetworkMonitor(boolean isOnline) {
        online = new MutableLiveData<>(isOnline);
    }

    @Override
    public LiveData<Boolean> isOnline() {
        return online;
    }
}

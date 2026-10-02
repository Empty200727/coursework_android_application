package ru.kinopolka.core.data.util;

import androidx.lifecycle.LiveData;

/** Connection state for the «Нет подключения» banner and automatic refresh (N-02). */
public interface NetworkMonitor {

    LiveData<Boolean> isOnline();
}

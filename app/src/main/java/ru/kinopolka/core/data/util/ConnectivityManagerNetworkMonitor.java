package ru.kinopolka.core.data.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class ConnectivityManagerNetworkMonitor implements NetworkMonitor {

    private final LiveData<Boolean> online;

    @Inject
    public ConnectivityManagerNetworkMonitor(@ApplicationContext Context context) {
        online = Transformations.distinctUntilChanged(new ConnectionLiveData(context));
    }

    @Override
    public LiveData<Boolean> isOnline() {
        return online;
    }

    /** Listens to the system only while somebody observes the state. */
    private static final class ConnectionLiveData extends LiveData<Boolean> {

        private final ConnectivityManager connectivityManager;
        private final Set<Network> networks = new HashSet<>();
        private final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                synchronized (networks) {
                    networks.add(network);
                }
                postValue(true);
            }

            @Override
            public void onLost(@NonNull Network network) {
                boolean any;
                synchronized (networks) {
                    networks.remove(network);
                    any = !networks.isEmpty();
                }
                postValue(any);
            }
        };

        ConnectionLiveData(Context context) {
            connectivityManager = context.getSystemService(ConnectivityManager.class);
        }

        @Override
        protected void onActive() {
            if (connectivityManager == null) {
                setValue(false);
                return;
            }
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            connectivityManager.registerNetworkCallback(request, callback);
            setValue(isCurrentlyConnected());
        }

        @Override
        protected void onInactive() {
            if (connectivityManager != null) {
                connectivityManager.unregisterNetworkCallback(callback);
                synchronized (networks) {
                    networks.clear();
                }
            }
        }

        private boolean isCurrentlyConnected() {
            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
    }
}

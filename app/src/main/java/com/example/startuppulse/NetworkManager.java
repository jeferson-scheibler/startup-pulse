package com.example.startuppulse;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Uma classe Singleton que monitoriza o estado da rede em tempo real para toda a aplicação.
 * Usa o moderno NetworkCallback para uma deteção robusta.
 */
public class NetworkManager {

    private static volatile NetworkManager instance;
    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> isNetworkAvailable = new MutableLiveData<>();

    private NetworkManager(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        registerNetworkCallback();
        // Verifica o estado inicial da rede
        updateNetworkStatus();
    }

    public static synchronized NetworkManager getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkManager(context.getApplicationContext());
        }
        return instance;
    }

    public LiveData<Boolean> isNetworkAvailable() {
        return isNetworkAvailable;
    }

    private void registerNetworkCallback() {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                isNetworkAvailable.postValue(true);
            }

            @Override
            public void onLost(@NonNull Network network) {
                isNetworkAvailable.postValue(false);
            }

            @Override
            public void onUnavailable() {
                isNetworkAvailable.postValue(false);
            }
        });
    }

    private void updateNetworkStatus() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            isNetworkAvailable.postValue(false);
            return;
        }
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) {
            isNetworkAvailable.postValue(false);
            return;
        }
        boolean isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        isNetworkAvailable.postValue(isConnected);
    }
}

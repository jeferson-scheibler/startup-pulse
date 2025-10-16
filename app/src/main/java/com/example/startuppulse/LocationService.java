package com.example.startuppulse;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

public class LocationService {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private final FusedLocationProviderClient fusedLocationClient;
    private final Activity activity;

    public interface LocationCallback {
        void onLocationResult(Location location);
        void onLocationError(String error);
    }

    public LocationService(Activity activity) {
        this.activity = activity;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
    }

    public void getCurrentLocation(LocationCallback callback) {
        if (hasLocationPermission()) {
            fetchLastLocation(callback);
        } else {
            requestLocationPermission();
            // Informa o usuário que a permissão é necessária e a lógica continuará no onRequestPermissionsResult
            callback.onLocationError("Permissão de localização necessária. Por favor, conceda a permissão.");
        }
    }

    private void fetchLastLocation(LocationCallback callback) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onLocationError("Permissão não concedida para buscar localização.");
            return;
        }

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(activity, location -> {
                    if (location != null) {
                        callback.onLocationResult(location);
                    } else {
                        callback.onLocationError("Não foi possível obter a localização atual. Tente novamente.");
                    }
                })
                .addOnFailureListener(activity, e -> callback.onLocationError("Erro ao obter localização: " + e.getMessage()));
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    public void handlePermissionResult(int requestCode, int[] grantResults, LocationCallback callback) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLastLocation(callback);
            } else {
                callback.onLocationError("Permissão de localização foi negada.");
            }
        }
    }
}
package com.example.startuppulse.data.repositories;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.startuppulse.data.ResultCallback;

public interface IStorageRepository {
    void uploadFile(@NonNull String folder, @NonNull String fileName, @NonNull Uri fileUri, @NonNull ResultCallback<String> callback);
}

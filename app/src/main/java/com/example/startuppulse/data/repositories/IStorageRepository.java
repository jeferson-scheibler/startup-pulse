package com.example.startuppulse.data.repositories;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.startuppulse.common.ResultCallback;

public interface IStorageRepository {
    void uploadFile(@NonNull String folder, @NonNull String fileName, @NonNull Uri fileUri, @NonNull ResultCallback<String> callback);
    void uploadImage(@NonNull Uri fileUri, @NonNull String folderPath, @NonNull String fileName, @NonNull ResultCallback<String> callback);

    void uploadInvestorProfileImage(String investorId, Uri imageUri, ResultCallback<String> callback);
}

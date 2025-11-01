package com.example.startuppulse.data.repositories;

import android.net.Uri;
import androidx.annotation.NonNull;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.common.ResultCallback;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import javax.inject.Inject;

public class StorageRepository implements IStorageRepository {

    private final FirebaseStorage storage;

    @Inject
    public StorageRepository(FirebaseStorage storage) {
        this.storage = storage;
    }

    @Override
    public void uploadFile(@NonNull String folder, @NonNull String fileName, @NonNull Uri fileUri, @NonNull ResultCallback<String> callback) {
        StorageReference fileRef = storage.getReference()
                .child(folder)
                .child(fileName);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot ->
                        fileRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> callback.onResult(new Result.Success<>(uri.toString())))
                                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e))))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    @Override
    public void uploadImage(@NonNull Uri fileUri, @NonNull String folderPath, @NonNull String fileName, @NonNull ResultCallback<String> callback) {
        StorageReference storageRef = storage.getReference().child(folderPath).child(fileName);

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Após o sucesso do upload, obtemos a URL de download
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                callback.onResult(new Result.Success<>(uri.toString()));
                            })
                            .addOnFailureListener(e -> {
                                callback.onResult(new Result.Error<>(new Exception("Falha ao obter a URL de download.", e)));
                            });
                })
                .addOnFailureListener(e -> {
                    callback.onResult(new Result.Error<>(new Exception("Falha no upload da imagem.", e)));
                });
    }
}

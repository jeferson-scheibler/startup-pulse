package com.example.startuppulse.data;

public interface ResultCallback<T> {
    void onSuccess(T data);
    void onError(Exception e);
}
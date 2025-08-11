package com.example.startuppulse.common;

import androidx.annotation.Nullable;

public final class Result<T> {
    @Nullable public final T data;
    @Nullable public final Throwable error;

    private Result(@Nullable T data, @Nullable Throwable error) {
        this.data = data; this.error = error;
    }
    public static <T> Result<T> ok(T data) { return new Result<>(data, null); }
    public static <T> Result<T> err(Throwable error) { return new Result<>(null, error); }
    public boolean isOk() { return error == null; }
}
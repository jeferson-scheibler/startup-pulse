package com.example.startuppulse;

public class ReadinessTask {
    private final String description;
    private final boolean isCompleted;

    public ReadinessTask(String description, boolean isCompleted) {
        this.description = description;
        this.isCompleted = isCompleted;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return isCompleted;
    }
}
package com.canineai.backend.entity;

public enum JobState {
    QUEUED,
    CLAIMED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED,
    TIMEOUT
}

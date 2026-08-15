package dev.shahirjalal.backend.enums;

public enum Status {
    RUNNING,
    STOPPED,
    /** Default state for an application that hasn't been health-checked yet. */
    UNKNOWN
}

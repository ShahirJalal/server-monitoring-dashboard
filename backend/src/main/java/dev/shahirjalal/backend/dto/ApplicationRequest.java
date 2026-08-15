package dev.shahirjalal.backend.dto;

import dev.shahirjalal.backend.enums.Status;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Validated request body for creating/updating an application. Kept separate from
 * {@link dev.shahirjalal.backend.entity.ApplicationEntity} so persistence-only fields
 * (id, lastCheckedAt, lastStatusChangeAt) can never be set directly by a client.
 */
@Getter
@Setter
public class ApplicationRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Port is required")
    @Min(value = 1, message = "Port must be between 1 and 65535")
    @Max(value = 65535, message = "Port must be between 1 and 65535")
    private Integer port;

    /**
     * Optional on create — new applications start as UNKNOWN until the next health
     * check runs. If provided, it's treated as a manual override until the next check.
     */
    private Status status;
}

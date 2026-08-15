package dev.shahirjalal.backend.entity;

import dev.shahirjalal.backend.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * One recorded status transition for an application, written by HealthCheckService
 * every time a check observes a change. Denormalizes the application name so
 * history survives even if the application is later deleted.
 */
@Entity
@Table(name = "status_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private String applicationName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status newStatus;

    @Column(nullable = false)
    private Instant occurredAt;
}

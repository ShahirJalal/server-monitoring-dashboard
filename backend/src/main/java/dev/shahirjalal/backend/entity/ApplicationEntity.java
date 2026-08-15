package dev.shahirjalal.backend.entity;

import dev.shahirjalal.backend.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Integer port;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.UNKNOWN;

    /** Last time the automated health check ran against this application. */
    private Instant lastCheckedAt;

    /** Last time the health check observed the status actually change. */
    private Instant lastStatusChangeAt;
}

package dev.shahirjalal.backend.service;

import dev.shahirjalal.backend.entity.ApplicationEntity;
import dev.shahirjalal.backend.entity.StatusEvent;
import dev.shahirjalal.backend.enums.Status;
import dev.shahirjalal.backend.repository.ApplicationRepository;
import dev.shahirjalal.backend.repository.StatusEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Periodically probes every tracked application and keeps its status in sync with
 * reality, instead of relying on whatever status was last typed in by hand.
 *
 * All tracked applications are assumed to run on the same host as this backend
 * (a single home server) -- each check is a plain TCP connect to
 * {@code localhost:<port>}, which is enough to tell "something is listening" apart
 * from "nothing is listening" without needing an app-specific health endpoint.
 *
 * Every observed transition is recorded to {@link StatusEventRepository} for history,
 * and pushed through {@link AlertService} -- except the very first check on a given
 * application (UNKNOWN -> something), which is "discovery", not "news".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final ApplicationRepository repository;
    private final StatusEventRepository statusEventRepository;
    private final AlertService alertService;
    private final PortProbe portProbe;

    @Value("${app.health-check.host:localhost}")
    private String healthCheckHost;

    @Value("${app.health-check.timeout-ms:2000}")
    private int timeoutMs;

    @Scheduled(fixedDelayString = "${app.health-check.interval-ms:30000}")
    public void checkAll() {

        List<ApplicationEntity> applications = repository.findAll();

        for (ApplicationEntity application : applications) {
            checkOne(application);
        }

        if (!applications.isEmpty()) {
            repository.saveAll(applications);
        }
    }

    private void checkOne(ApplicationEntity application) {

        Status observed = portProbe.isOpen(healthCheckHost, application.getPort(), timeoutMs)
                ? Status.RUNNING : Status.STOPPED;
        Instant now = Instant.now();
        Status previous = application.getStatus();

        if (previous != observed) {

            application.setStatus(observed);
            application.setLastStatusChangeAt(now);
            log.info("Application '{}' (port {}) changed status to {}",
                    application.getName(), application.getPort(), observed);

            statusEventRepository.save(StatusEvent.builder()
                    .applicationId(application.getId())
                    .applicationName(application.getName())
                    .oldStatus(previous)
                    .newStatus(observed)
                    .occurredAt(now)
                    .build());

            if (previous != Status.UNKNOWN) {
                alertService.send(String.format(
                        "%s is now %s (was %s)", application.getName(), observed, previous));
            }
        }

        application.setLastCheckedAt(now);
    }
}

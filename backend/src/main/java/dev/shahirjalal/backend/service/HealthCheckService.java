package dev.shahirjalal.backend.service;

import dev.shahirjalal.backend.entity.ApplicationEntity;
import dev.shahirjalal.backend.enums.Status;
import dev.shahirjalal.backend.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.util.List;

/**
 * Periodically probes every tracked application and keeps its status in sync with
 * reality, instead of relying on whatever status was last typed in by hand.
 *
 * All tracked applications are assumed to run on the same host as this backend
 * (a single home server) — each check is a plain TCP connect to
 * {@code localhost:<port>}, which is enough to tell "something is listening" apart
 * from "nothing is listening" without needing an app-specific health endpoint.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final ApplicationRepository repository;

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

        Status observed = isPortOpen(application.getPort()) ? Status.RUNNING : Status.STOPPED;
        Instant now = Instant.now();

        if (application.getStatus() != observed) {
            application.setStatus(observed);
            application.setLastStatusChangeAt(now);
            log.info("Application '{}' (port {}) changed status to {}",
                    application.getName(), application.getPort(), observed);
        }

        application.setLastCheckedAt(now);
    }

    private boolean isPortOpen(int port) {

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(healthCheckHost, port), timeoutMs);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}

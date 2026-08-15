package dev.shahirjalal.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Pushes a plain-text notification to ntfy.sh when a tracked application's status
 * changes. No SDK needed -- ntfy is just an HTTP POST with the message as the body.
 * Silently does nothing if no topic is configured, so alerting is opt-in.
 */
@Slf4j
@Service
public class AlertService {

    @Value("${app.alerts.ntfy-topic:}")
    private String ntfyTopic;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(5))
            .build();

    public void send(String message) {

        if (ntfyTopic == null || ntfyTopic.isBlank()) {
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ntfy.sh/" + ntfyTopic))
                .header("Title", "Server Monitoring Dashboard")
                .POST(HttpRequest.BodyPublishers.ofString(message, StandardCharsets.UTF_8))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .exceptionally(ex -> {
                    log.warn("Failed to send ntfy alert: {}", ex.getMessage());
                    return null;
                });
    }
}

package dev.shahirjalal.backend.service;

import dev.shahirjalal.backend.dto.HostMetricsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HostMetricsServiceTest {

    @TempDir
    Path tempDir;

    private HostMetricsService service;

    @BeforeEach
    void setUp() throws IOException {

        Path proc = Files.createDirectory(tempDir.resolve("proc"));
        Files.writeString(proc.resolve("meminfo"), "MemTotal:       16000000 kB\nMemAvailable:    4000000 kB\n");
        Files.writeString(proc.resolve("uptime"), "12345.67 98765.43\n");
        Files.writeString(proc.resolve("stat"), "cpu  100 0 100 800 0 0 0 0 0 0\n");

        service = new HostMetricsService(proc.toString(), tempDir.toString());
    }

    @Test
    void readsMemoryUptimeAndDisk() {

        HostMetricsResponse result = service.computeMetrics();

        assertThat(result.memoryTotalBytes()).isEqualTo(16_000_000L * 1024);
        assertThat(result.memoryUsedBytes()).isEqualTo((16_000_000L - 4_000_000L) * 1024);
        assertThat(result.uptimeSeconds()).isEqualTo(12345L);
        assertThat(result.diskTotalBytes()).isNotNull();
    }

    @Test
    void cpuPercentIsNullUntilSecondSample() throws IOException {

        HostMetricsResponse first = service.computeMetrics();
        assertThat(first.cpuUsagePercent()).isNull();

        // Simulate jiffies having advanced since the first read -- a static fixture
        // would report a zero delta and stay null forever, which isn't the real case.
        Files.writeString(tempDir.resolve("proc").resolve("stat"), "cpu  200 0 100 900 0 0 0 0 0 0\n");

        HostMetricsResponse second = service.computeMetrics();
        assertThat(second.cpuUsagePercent()).isNotNull();
    }

    @Test
    void missingProcFiles_returnsNullsInsteadOfThrowing() {

        HostMetricsService brokenService = new HostMetricsService("/nonexistent", "/nonexistent");
        HostMetricsResponse result = brokenService.computeMetrics();

        assertThat(result.memoryTotalBytes()).isNull();
        assertThat(result.diskTotalBytes()).isNull();
        assertThat(result.uptimeSeconds()).isNull();
        assertThat(result.cpuUsagePercent()).isNull();
    }
}

package dev.shahirjalal.backend.service;

import dev.shahirjalal.backend.dto.HostMetricsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads host-level CPU/memory/disk/uptime straight from /proc and a bind-mounted
 * host root, rather than pulling in a native-library metrics dependency (OSHI etc.)
 * for what's just a few well-documented text files.
 *
 * Since the backend runs in a container, this only works if the host's /proc and /
 * are bind-mounted in read-only (see docker-compose.yml) -- when they're not (e.g.
 * local dev), every reader below fails closed to null instead of throwing.
 */
@Slf4j
@Service
public class HostMetricsService {

    private final String procPath;
    private final String rootPath;

    private volatile CpuSnapshot previousCpuSnapshot;
    private volatile HostMetricsResponse cached = HostMetricsResponse.unavailable();

    public HostMetricsService(
            @Value("${app.host-metrics.proc-path:/host/proc}") String procPath,
            @Value("${app.host-metrics.root-path:/host/root}") String rootPath) {
        this.procPath = procPath;
        this.rootPath = rootPath;
    }

    @Scheduled(fixedDelayString = "${app.host-metrics.interval-ms:15000}")
    public void refresh() {
        cached = computeMetrics();
    }

    public HostMetricsResponse getCurrent() {
        return cached;
    }

    HostMetricsResponse computeMetrics() {

        MemoryInfo memory = readMemory();
        DiskInfo disk = readDisk();

        return new HostMetricsResponse(
                readCpuPercent(),
                memory == null ? null : memory.usedBytes(),
                memory == null ? null : memory.totalBytes(),
                disk == null ? null : disk.usedBytes(),
                disk == null ? null : disk.totalBytes(),
                readUptimeSeconds());
    }

    private Double readCpuPercent() {

        try {
            String statLine = Files.readAllLines(Path.of(procPath, "stat")).get(0);
            long[] fields = parseCpuFields(statLine);

            long idle = fields[3] + fields[4]; // idle + iowait
            long total = 0;
            for (long field : fields) {
                total += field;
            }

            CpuSnapshot previous = this.previousCpuSnapshot;
            this.previousCpuSnapshot = new CpuSnapshot(idle, total);

            if (previous == null) {
                return null; // no baseline yet -- next tick will have one
            }

            long idleDelta = idle - previous.idle();
            long totalDelta = total - previous.total();

            if (totalDelta <= 0) {
                return null;
            }

            return 100.0 * (1.0 - ((double) idleDelta / totalDelta));

        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private long[] parseCpuFields(String cpuLine) {

        String[] parts = cpuLine.trim().split("\\s+");
        long[] fields = new long[parts.length - 1];

        for (int i = 1; i < parts.length; i++) {
            fields[i - 1] = Long.parseLong(parts[i]);
        }

        return fields;
    }

    private MemoryInfo readMemory() {

        try {
            List<String> lines = Files.readAllLines(Path.of(procPath, "meminfo"));
            long totalKb = 0;
            long availableKb = 0;

            for (String line : lines) {
                if (line.startsWith("MemTotal:")) {
                    totalKb = extractKb(line);
                } else if (line.startsWith("MemAvailable:")) {
                    availableKb = extractKb(line);
                }
            }

            if (totalKb == 0) {
                return null;
            }

            return new MemoryInfo((totalKb - availableKb) * 1024, totalKb * 1024);

        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private long extractKb(String line) {
        String digits = line.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Long.parseLong(digits);
    }

    private DiskInfo readDisk() {

        try {
            FileStore store = Files.getFileStore(Path.of(rootPath));
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            return new DiskInfo(total - usable, total);

        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private Long readUptimeSeconds() {

        try {
            String content = Files.readString(Path.of(procPath, "uptime")).trim();
            return (long) Double.parseDouble(content.split("\\s+")[0]);

        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private record CpuSnapshot(long idle, long total) { }

    private record MemoryInfo(long usedBytes, long totalBytes) { }

    private record DiskInfo(long usedBytes, long totalBytes) { }
}

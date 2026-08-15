package dev.shahirjalal.backend.dto;

/**
 * All fields nullable -- any metric this host doesn't expose (e.g. /proc not
 * mounted in) comes back null rather than failing the whole response.
 */
public record HostMetricsResponse(
        Double cpuUsagePercent,
        Long memoryUsedBytes,
        Long memoryTotalBytes,
        Long diskUsedBytes,
        Long diskTotalBytes,
        Long uptimeSeconds
) {

    public static HostMetricsResponse unavailable() {
        return new HostMetricsResponse(null, null, null, null, null, null);
    }
}

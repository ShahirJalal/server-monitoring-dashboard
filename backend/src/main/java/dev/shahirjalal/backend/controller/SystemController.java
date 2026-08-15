package dev.shahirjalal.backend.controller;

import dev.shahirjalal.backend.dto.DockerContainerInfo;
import dev.shahirjalal.backend.dto.HostMetricsResponse;
import dev.shahirjalal.backend.service.DockerClientService;
import dev.shahirjalal.backend.service.HostMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Host-level info: not covered by the applications' public GET rule, so these stay
 * behind login (SecurityConfig's default-deny) -- CPU/memory/disk/uptime and a list
 * of every container on the host are more sensitive than "is app X reachable".
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final HostMetricsService hostMetricsService;
    private final DockerClientService dockerClientService;

    @GetMapping("/metrics")
    public HostMetricsResponse metrics() {
        return hostMetricsService.getCurrent();
    }

    @GetMapping("/containers")
    public List<DockerContainerInfo> containers() {
        return dockerClientService.listContainers();
    }
}

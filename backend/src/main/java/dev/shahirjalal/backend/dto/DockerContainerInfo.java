package dev.shahirjalal.backend.dto;

public record DockerContainerInfo(
        String id,
        String name,
        String image,
        String state,
        String status
) {
}

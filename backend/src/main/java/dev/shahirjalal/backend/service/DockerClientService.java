package dev.shahirjalal.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.shahirjalal.backend.dto.DockerContainerInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lists every container on the host via the Docker Engine API over its Unix socket
 * -- hand-rolled rather than pulling in a Docker client library, since this only
 * needs one read-only GET endpoint and a raw HTTP request over a socket is a few
 * dozen lines with zero new dependencies (JDK 16+ supports AF_UNIX natively).
 *
 * Requires /var/run/docker.sock bind-mounted into this container (read-only) --
 * see docker-compose.yml. Fails closed to an empty list if it's not there, so this
 * only degrades the container view instead of breaking the app.
 */
@Slf4j
@Service
public class DockerClientService {

    private final String socketPath;
    private final ObjectMapper objectMapper;

    public DockerClientService(
            @Value("${app.docker.socket-path:/var/run/docker.sock}") String socketPath,
            ObjectMapper objectMapper) {
        this.socketPath = socketPath;
        this.objectMapper = objectMapper;
    }

    public List<DockerContainerInfo> listContainers() {

        Path socket = Path.of(socketPath);
        if (!Files.exists(socket)) {
            return List.of();
        }

        try (SocketChannel channel = SocketChannel.open(UnixDomainSocketAddress.of(socket))) {

            String request = "GET /containers/json?all=true HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Accept: application/json\r\n"
                    + "Connection: close\r\n\r\n";
            channel.write(ByteBuffer.wrap(request.getBytes(StandardCharsets.UTF_8)));

            String body = extractBody(readFullResponse(channel));
            return parseContainers(body);

        } catch (IOException ex) {
            log.warn("Could not reach Docker socket at {}: {}", socketPath, ex.getMessage());
            return List.of();
        }
    }

    private List<DockerContainerInfo> parseContainers(String body) {

        List<DockerContainerInfo> containers = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(body);
            if (!root.isArray()) {
                return containers;
            }

            for (JsonNode node : root) {
                String id = node.path("Id").asText("");
                containers.add(new DockerContainerInfo(
                        id.length() > 12 ? id.substring(0, 12) : id,
                        firstName(node.path("Names")),
                        node.path("Image").asText("unknown"),
                        node.path("State").asText("unknown"),
                        node.path("Status").asText("")));
            }
        } catch (IOException ex) {
            log.warn("Could not parse Docker API response: {}", ex.getMessage());
        }

        return containers;
    }

    private String firstName(JsonNode namesNode) {

        if (namesNode.isArray() && !namesNode.isEmpty()) {
            String name = namesNode.get(0).asText();
            return name.startsWith("/") ? name.substring(1) : name;
        }

        return "unknown";
    }

    private String readFullResponse(SocketChannel channel) throws IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ByteBuffer chunk = ByteBuffer.allocate(8192);

        int read;
        while ((read = channel.read(chunk)) != -1) {
            chunk.flip();
            buffer.write(chunk.array(), 0, chunk.limit());
            chunk.clear();
        }

        return buffer.toString(StandardCharsets.UTF_8);
    }

    private String extractBody(String response) {

        int headerEnd = response.indexOf("\r\n\r\n");
        if (headerEnd == -1) {
            return "[]";
        }

        String headers = response.substring(0, headerEnd);
        String rawBody = response.substring(headerEnd + 4);

        if (headers.toLowerCase(Locale.ROOT).contains("transfer-encoding: chunked")) {
            return dechunk(rawBody);
        }

        return rawBody;
    }

    /** Minimal HTTP chunked-transfer decoder -- Docker's API chunks responses by default. */
    private String dechunk(String chunkedBody) {

        StringBuilder result = new StringBuilder();
        int pos = 0;

        while (pos < chunkedBody.length()) {

            int lineEnd = chunkedBody.indexOf("\r\n", pos);
            if (lineEnd == -1) {
                break;
            }

            int size;
            try {
                size = Integer.parseInt(chunkedBody.substring(pos, lineEnd).trim(), 16);
            } catch (NumberFormatException ex) {
                break;
            }

            if (size == 0) {
                break;
            }

            int chunkStart = lineEnd + 2;
            int chunkEnd = Math.min(chunkStart + size, chunkedBody.length());
            result.append(chunkedBody, chunkStart, chunkEnd);
            pos = chunkEnd + 2;
        }

        return result.toString();
    }
}

package dev.shahirjalal.backend.service;

/**
 * Abstraction over "is something listening on this port", so HealthCheckService can
 * be unit tested without opening real sockets.
 */
public interface PortProbe {

    boolean isOpen(String host, int port, int timeoutMs);
}

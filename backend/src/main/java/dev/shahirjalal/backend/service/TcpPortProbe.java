package dev.shahirjalal.backend.service;

import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;

@Component
public class TcpPortProbe implements PortProbe {

    @Override
    public boolean isOpen(String host, int port, int timeoutMs) {

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}

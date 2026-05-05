package com.trafficparrot.demo.sftpfix.server;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.session.ServerSessionAware;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class SftpServer {
    private final Path root;
    private final int requestedPort;
    private final String username;
    private final String password;
    private final SshServer server;
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private final AtomicLong sessionSeq = new AtomicLong();

    public SftpServer(Path root, int port, String username, String password) {
        this.root = root;
        this.requestedPort = port;
        this.username = username;
        this.password = password;
        this.server = SshServer.setUpDefaultServer();
    }

    public void start() throws IOException {
        server.setPort(requestedPort);
        server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        server.setPasswordAuthenticator((u, p, s) -> {
            boolean ok = username.equals(u) && password.equals(p);
            if (ok) {
                String id = "session-" + sessionSeq.incrementAndGet();
                sessions.put(id, new SessionInfo(id, u, s.getClientAddress() == null ? "?" : s.getClientAddress().toString(), System.currentTimeMillis()));
                s.setAttribute(SESSION_ID, id);
            }
            return ok;
        });
        server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        server.setFileSystemFactory(new VirtualFileSystemFactory(root));
        server.addSessionListener(new org.apache.sshd.common.session.SessionListener() {
            @Override public void sessionClosed(org.apache.sshd.common.session.Session s) {
                Object id = s.getAttribute(SESSION_ID);
                if (id != null) sessions.remove(id.toString());
            }
        });
        server.start();
    }

    public void stop() throws IOException {
        server.stop();
    }

    public int port() { return server.getPort(); }

    public Map<String, SessionInfo> sessions() {
        return new java.util.HashMap<>(sessions);
    }

    public static final org.apache.sshd.common.AttributeRepository.AttributeKey<String> SESSION_ID = new org.apache.sshd.common.AttributeRepository.AttributeKey<>();

    public record SessionInfo(String id, String username, String clientAddress, long connectedAtMillis) {}
}

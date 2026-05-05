package com.trafficparrot.demo.sftpfix.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Polls the inbound directory for NewOrderSingle files, generates an ExecutionReport for
 * each, and writes it to the outbound directory. Processed inbound files are deleted.
 */
public final class BusinessLoop {

    private static final Logger LOG = LoggerFactory.getLogger(BusinessLoop.class);
    private static final long POLL_INTERVAL_MILLIS = 250;

    private final Path inboundDir;
    private final Path outboundDir;
    private final Thread thread;
    private final AtomicLong execSeq = new AtomicLong();
    private volatile boolean running;

    public BusinessLoop(Path inboundDir, Path outboundDir) {
        this.inboundDir = inboundDir;
        this.outboundDir = outboundDir;
        this.thread = new Thread(this::loop, "fix-server-business-loop");
        this.thread.setDaemon(true);
    }

    public void start() {
        running = true;
        thread.start();
    }

    public void joinForever() throws InterruptedException {
        thread.join();
    }

    private void loop() {
        while (running) {
            try {
                processOnce();
            } catch (Exception e) {
                LOG.warn("Polling cycle failed", e);
            }
            try { Thread.sleep(POLL_INTERVAL_MILLIS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
    }

    private void processOnce() throws IOException {
        if (!Files.isDirectory(inboundDir)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inboundDir, "*.fix")) {
            for (Path inbound : stream) {
                handleOrder(inbound);
            }
        }
    }

    private void handleOrder(Path inbound) {
        try {
            byte[] bytes = Files.readAllBytes(inbound);
            String body = new String(bytes, StandardCharsets.US_ASCII);
            FixMessage parsed = FixMessage.parse(body);
            if (!"D".equals(parsed.field("35"))) {
                LOG.info("Skipping non-NewOrderSingle file {} (msgType={})", inbound.getFileName(), parsed.field("35"));
                Files.deleteIfExists(inbound);
                return;
            }
            String execId = "EXEC-" + execSeq.incrementAndGet();
            String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String executionReport = FixMessage.buildExecutionReport(parsed, execId, orderId);

            Path outboundFile = outboundDir.resolve("exec-" + parsed.fieldOr("11", execId) + ".fix");
            Path tmp = outboundDir.resolve(outboundFile.getFileName() + ".tmp");
            Files.write(tmp, executionReport.getBytes(StandardCharsets.US_ASCII));
            Files.move(tmp, outboundFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(inbound);
            LOG.info("Generated ExecutionReport {} from order {} (clOrdId={}, symbol={}, qty={})",
                    outboundFile.getFileName(), inbound.getFileName(),
                    parsed.field("11"), parsed.field("55"), parsed.field("38"));
        } catch (Exception e) {
            LOG.error("Failed to handle inbound order {}", inbound, e);
        }
    }
}

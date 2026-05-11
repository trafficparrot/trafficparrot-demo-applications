package com.trafficparrot.demo.sftpfix.client;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public final class SftpUploader {

    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public SftpUploader(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public Session openSession() throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("PreferredAuthentications", "password");
        session.setTimeout(15_000);
        session.connect(15_000);
        return session;
    }

    public ChannelSftp openSftp(Session session) throws Exception {
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect(15_000);
        return sftp;
    }

    public void upload(ChannelSftp sftp, String remoteDir, String fileName, String content) throws SftpException {
        ensureDir(sftp, remoteDir);
        String tmpName = fileName + ".tmp";
        try (InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.US_ASCII))) {
            sftp.put(in, remoteDir + "/" + tmpName);
        } catch (Exception e) {
            throw new SftpException(ChannelSftp.SSH_FX_FAILURE, "upload tmp failed: " + e.getMessage());
        }
        try {
            sftp.rm(remoteDir + "/" + fileName);
        } catch (SftpException ignored) {}
        sftp.rename(remoteDir + "/" + tmpName, remoteDir + "/" + fileName);
    }

    public String tryRead(ChannelSftp sftp, String remoteDir, String fileName) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            sftp.get(remoteDir + "/" + fileName, out);
            return out.toString(StandardCharsets.US_ASCII);
        } catch (SftpException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> listNames(ChannelSftp sftp, String remoteDir) {
        List<String> out = new ArrayList<>();
        try {
            Vector<ChannelSftp.LsEntry> entries = sftp.ls(remoteDir);
            for (ChannelSftp.LsEntry e : entries) {
                String n = e.getFilename();
                if (".".equals(n) || "..".equals(n)) continue;
                if (!e.getAttrs().isReg()) continue;
                out.add(n);
            }
        } catch (SftpException ignored) {}
        return out;
    }

    public void delete(ChannelSftp sftp, String remoteDir, String fileName) {
        try {
            sftp.rm(remoteDir + "/" + fileName);
        } catch (SftpException ignored) {}
    }

    private void ensureDir(ChannelSftp sftp, String dir) throws SftpException {
        try {
            sftp.cd(dir);
            sftp.cd("/");
        } catch (SftpException e) {
            sftp.mkdir(dir);
        }
    }
}

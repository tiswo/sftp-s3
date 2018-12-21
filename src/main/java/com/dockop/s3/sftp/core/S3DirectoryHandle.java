package com.dockop.s3.sftp.core;

import com.dockop.s3.sftp.core.file.S3FileSystem;
import org.apache.sshd.server.subsystem.sftp.DirectoryHandle;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystem;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * 类名称: S3DirectoryHandle <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 18/2/9 上午11:16
 */
public class S3DirectoryHandle extends DirectoryHandle implements Iterator<Path>{

    private boolean done;
    private boolean sendDotDot = true;
    private boolean sendDot = true;

    private DirectoryStream<Path> ds;

    private Iterator<Path> fileList;

    public S3DirectoryHandle(SftpSubsystem subsystem, Path dir, String handle) throws IOException {
        super(subsystem,dir,handle);
        S3FileSystem fileSystem = (S3FileSystem) dir.getFileSystem();
        fileList = fileSystem.getClient().list(dir).iterator();
        sendDotDot=false;
    }

    public boolean isDone() {
        return done;
    }

    public void markDone() {
        this.done = true;
        // allow the garbage collector to do the job
        this.fileList = null;
    }

    public boolean isSendDot() {
        return sendDot;
    }

    public void markDotSent() {
        sendDot = false;
    }

    public boolean isSendDotDot() {
        return sendDotDot;
    }

    public void markDotDotSent() {
        sendDotDot = false;
    }

    @Override
    public boolean isOpen() {
        return super.isOpen();
    }

    @Override
    protected void signalHandleOpening(SftpSubsystem subsystem) throws IOException {
        super.signalHandleOpening(subsystem);
    }

    @Override
    protected void signalHandleOpen(SftpSubsystem subsystem) throws IOException {
        super.signalHandleOpen(subsystem);
    }

    @Override
    public Path getFile() {
        return super.getFile();
    }

    @Override
    public String getFileHandle() {
        return super.getFileHandle();
    }

    @Override
    public boolean hasNext() {
        return fileList.hasNext();
    }

    @Override
    public Path next() {
        return fileList.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not allowed to remove " + toString());
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}

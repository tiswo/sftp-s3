package com.dockop.s3.sftp.core;

import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.subsystem.sftp.*;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * 类名称: SFTPSubsystemFactory <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 18/2/8 下午2:37
 */
public class SFTPSubsystemFactory extends SftpSubsystemFactory{

    public SFTPSubsystemFactory() {
        super();
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public ExecutorService getExecutorService() {
        return super.getExecutorService();
    }

    /**
     * @param service The {@link ExecutorService} to be used by the {@link SftpSubsystem}
     *                command when starting execution. If {@code null} then a single-threaded ad-hoc service is used.
     */
    @Override
    public void setExecutorService(ExecutorService service) {
        super.setExecutorService(service);
    }

    @Override
    public boolean isShutdownOnExit() {
        return super.isShutdownOnExit();
    }

    /**
     * @param shutdownOnExit If {@code true} the {@link ExecutorService#shutdownNow()}
     *                       will be called when subsystem terminates - unless it is the ad-hoc service, which
     *                       will be shutdown regardless
     */
    @Override
    public void setShutdownOnExit(boolean shutdownOnExit) {
        super.setShutdownOnExit(shutdownOnExit);
    }

    @Override
    public UnsupportedAttributePolicy getUnsupportedAttributePolicy() {
        return super.getUnsupportedAttributePolicy();
    }

    /**
     * @param p The {@link UnsupportedAttributePolicy} to use if failed to access
     *          some local file attributes - never {@code null}
     */
    @Override
    public void setUnsupportedAttributePolicy(UnsupportedAttributePolicy p) {
        super.setUnsupportedAttributePolicy(p);
    }

    @Override
    public SftpFileSystemAccessor getFileSystemAccessor() {
        return super.getFileSystemAccessor();
    }

    @Override
    public void setFileSystemAccessor(SftpFileSystemAccessor accessor) {
        super.setFileSystemAccessor(accessor);
    }

    @Override
    public SftpErrorStatusDataHandler getErrorStatusDataHandler() {
        return super.getErrorStatusDataHandler();
    }

    @Override
    public void setErrorStatusDataHandler(SftpErrorStatusDataHandler handler) {
        super.setErrorStatusDataHandler(handler);
    }

    @Override
    public Command create() {
        final SFTPSubsystem subsystem =
                new SFTPSubsystem(getExecutorService(), isShutdownOnExit(),
                        getUnsupportedAttributePolicy(), getFileSystemAccessor(),
                        getErrorStatusDataHandler());
        GenericUtils.forEach(getRegisteredListeners(), new Consumer<SftpEventListener>() {
            @Override
            public void accept(SftpEventListener sftpEventListener) {
                subsystem.addSftpEventListener(sftpEventListener);
            }
        });
        return subsystem;
    }

    @Override
    public Collection<SftpEventListener> getRegisteredListeners() {
        return super.getRegisteredListeners();
    }

    @Override
    public SftpEventListener getSftpEventListenerProxy() {
        return super.getSftpEventListenerProxy();
    }

    @Override
    public boolean addSftpEventListener(SftpEventListener listener) {
        return super.addSftpEventListener(listener);
    }

    @Override
    public boolean removeSftpEventListener(SftpEventListener listener) {
        return super.removeSftpEventListener(listener);
    }
}

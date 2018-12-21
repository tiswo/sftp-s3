package com.dockop.s3.sftp.core;

import com.dockop.s3.sftp.common.Constants;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.DirectoryHandle;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystem;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 类名称: RouterDirectoryHandle <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 18/3/16 上午10:29
 */
public class RouterDirectoryHandle extends DirectoryHandle{

    private DirectoryHandle proxy;

    public RouterDirectoryHandle(SftpSubsystem subsystem, Path dir, String handle) throws IOException {
        super(subsystem, dir, handle);
        ServerSession session = subsystem.getServerSession();
        Boolean s3 = session.getAttribute(Constants.USE_S3);
        if(s3){
            proxy = new S3DirectoryHandle(subsystem,dir,handle);
        }else {
            proxy = new DirectoryHandle(subsystem,dir,handle);
        }
    }

    @Override
    public boolean isDone() {
        return proxy.isDone();
    }

    @Override
    public void markDone() {
        proxy.markDone();
    }

    @Override
    public boolean isSendDot() {
        return proxy.isSendDot();
    }

    @Override
    public void markDotSent() {
        proxy.markDotSent();
    }

    @Override
    public boolean isSendDotDot() {
        return proxy.isSendDotDot();
    }

    @Override
    public void markDotDotSent() {
        proxy.markDotDotSent();
    }

    @Override
    public boolean hasNext() {
        return proxy.hasNext();
    }

    @Override
    public Path next() {
        return proxy.next();
    }

    @Override
    public void remove() {
        proxy.remove();
    }

    @Override
    public void close() throws IOException {
        proxy.close();
    }
}

package com.dockop.s3.sftp.core.file;

import io.github.mentegy.s3.channels.impl.S3AppendableObjectChannel;
import org.apache.sshd.client.subsystem.sftp.SftpFileSystemProvider;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.io.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 类名称: S3FileSystemChannel <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 18/2/26 下午5:37
 */
public class S3FileSystemChannel extends FileChannel{

    protected final Logger log = LoggerFactory.getLogger(S3FileSystemChannel.class);

    private final AtomicLong posTracker = new AtomicLong(0L);

    private InputStream is;

    private S3AppendableObjectChannel objectChannel;

    private S3Path path;

    private Set<PosixFilePermission> permissions;

    public static final Set<PosixFilePermission> WRITE_PERMISSIONS =
            Collections.unmodifiableSet(
                    EnumSet.of(PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_WRITE, PosixFilePermission.OTHERS_WRITE));

    public S3FileSystemChannel(S3Path p) throws IOException {
        Objects.requireNonNull(p, "No target path").toString();
        this.path=p;
        this.permissions = SftpFileSystemProvider.permissionsToAttributes(path.getAttributes().getPermissions());
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return (int) doRead(Collections.singletonList(dst), -1);
    }

    protected long doRead(List<ByteBuffer> buffers, long position) throws IOException {
        boolean eof = false;
        long curPos = (position >= 0L) ? position : posTracker.get();
        if(is == null){
            is = path.createInputStream(0L);
        }
        if(is.available()==0){
            return -1;
        }
        try {
            long totalRead = 0;
            byte[] bytes= new byte[1024];
            loop:
            for (ByteBuffer buffer : buffers) {
                while (buffer.hasRemaining()) {
                    ByteBuffer wrap = buffer;
                    if (!buffer.hasArray()) {
                        wrap = ByteBuffer.allocate(Math.min(IoUtils.DEFAULT_COPY_SIZE, buffer.remaining()));
                    }
                    int readMax= 1024<wrap.remaining()?1024:wrap.remaining();
                    int read=is.read(bytes,0,readMax);
                    if (read > 0) {
                        buffer.put(bytes,0,read);
                        curPos += read;
                        totalRead += read;
                    } else {
                        eof = read == -1;
                        break loop;
                    }
                }
            }
            if (totalRead > 0) {
                return totalRead;
            }

            if (eof) {
                return -1;
            } else {
                return 0;
            }
        } finally {
            if (position < 0L) {
                posTracker.set(curPos);
            }
        }
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        return 0;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        ensureOpen(WRITE_PERMISSIONS);
        if(objectChannel == null){
            objectChannel = path.getChannel();
        }
        return objectChannel.write(src);
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        return 0;
    }

    @Override
    public long position() throws IOException {
        return 0;
    }


    @Override
    public FileChannel position(long newPosition) throws IOException {
        if (newPosition < 0L) {
            throw new IllegalArgumentException("position(" + ") illegal file channel position: " + newPosition);
        }

        ensureOpen(Collections.emptySet());
        posTracker.set(newPosition);
        return this;
    }

    private void ensureOpen(Collection<PosixFilePermission> permission) throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        if (GenericUtils.size(permission) > 0) {
            for (PosixFilePermission p : permission) {
                if (this.permissions.contains(p)) {
                    return;
                }
            }

            throw new IOException("ensureOpen(" + path.toString() + ") current channel modes (" + this.permissions + ") do contain any of the required: " + permission);
        }
    }

    @Override
    public long size() throws IOException {
        return path.getAttributes().getSize();
    }


    @Override
    public FileChannel truncate(long size) throws IOException {
        return null;
    }

    @Override
    public void force(boolean metaData) throws IOException {

    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        return 0;
    }


    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        return 0;
    }


    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        return (int) doRead(Collections.singletonList(dst),position);
    }


    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        return 0;
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        return null;
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        return null;
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return null;
    }

    @Override
    protected void implCloseChannel() throws IOException {
        if(is != null){
            is.close();
        }
        if(objectChannel != null){
            objectChannel.close();
        }
    }
}

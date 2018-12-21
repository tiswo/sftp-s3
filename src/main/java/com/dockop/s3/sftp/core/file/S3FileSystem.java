package com.dockop.s3.sftp.core.file;

import com.dockop.s3.sftp.common.UserClient;
import org.apache.sshd.client.subsystem.sftp.SftpClient;
import org.apache.sshd.client.subsystem.sftp.SftpVersionSelector;
import org.apache.sshd.common.file.util.BaseFileSystem;
import org.apache.sshd.common.util.GenericUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

/**
 * 类名称: S3FileSystem <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 18/3/9 上午11:23
 */
public class S3FileSystem extends BaseFileSystem<S3Path>{

    public static final String POOL_SIZE_PROP = "sftp-fs-pool-size";

    public static final int DEFAULT_POOL_SIZE = 8;

    public static final Set<String> UNIVERSAL_SUPPORTED_VIEWS =
            Collections.unmodifiableSet(
                    GenericUtils.asSortedSet(String.CASE_INSENSITIVE_ORDER,
                            "posix"));
    private final String id;
    private final Set<String> supportedViews;
    private final SftpVersionSelector selector;

    private final UserClient client;

    int MIN_BUFFER_SIZE = Byte.MAX_VALUE;

    private int readBufferSize = 32 * 1024;

    private int writeBufferSize = 32 * 1024;

    public S3FileSystem(FileSystemProvider fileSystemProvider, UserClient client, String id, SftpVersionSelector selector) {
        super(fileSystemProvider);
        Set<String> views = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        views.addAll(UNIVERSAL_SUPPORTED_VIEWS);
//        views.add("acl");
        supportedViews = Collections.unmodifiableSet(views);
        this.id = id;
        this.selector = selector;
        this.client = client;
    }

    public UserClient getClient() {
        return client;
    }

    @Override
    public S3Path getDefaultDir() {
        return super.getDefaultDir();
    }

    @Override
    public boolean isReadOnly() {
        return super.isReadOnly();
    }

    @Override
    public S3FileSystemProvider provider() {
        return (S3FileSystemProvider) super.provider();
    }

    @Override
    public String getSeparator() {
        return super.getSeparator();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return super.getRootDirectories();
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return super.getFileStores();
    }

    @Override
    public S3Path getPath(String first, String... more) {
        return super.getPath(first, more);
    }

    @Override
    protected void appendDedupSep(StringBuilder sb, CharSequence s) {
        super.appendDedupSep(sb, s);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        return super.getPathMatcher(syntaxAndPattern);
    }

    @Override
    protected String globToRegex(String pattern) {
        return super.globToRegex(pattern);
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return super.newWatchService();
    }

    @Override
    protected S3Path create(String root, String... names) {
        return super.create(root, names);
    }

    @Override
    protected S3Path create(String root, Collection names) {
        return super.create(root, names);
    }

    @Override
    protected S3Path create(String root, List names) {
        return new S3Path(this,root,names);
    }

    public String getId() {
        return id;
    }

    @Override
    public void close() throws IOException {
        if (isOpen()) {
            if (client != null) {
                client.shutdown();
            }
            S3FileSystemProvider provider = provider();
            String fsId = getId();
            S3FileSystem fs = provider.removeFileSystem(fsId);
            if ((fs != null) && (fs != this)) {
                throw new FileSystemException(fsId, fsId, "Mismatched FS instance for id=" + fsId);
            }
        }
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return supportedViews;
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return null;
    }


    public int getReadBufferSize() {
        return readBufferSize;
    }

    public void setReadBufferSize(int size) {
        if (size < MIN_BUFFER_SIZE) {
            throw new IllegalArgumentException("Insufficient read buffer size: " + size + ", min.=" + SftpClient.MIN_READ_BUFFER_SIZE);
        }

        readBufferSize = size;
    }

    public int getWriteBufferSize() {
        return writeBufferSize;
    }

    public void setWriteBufferSize(int size) {
        if (size < MIN_BUFFER_SIZE) {
            throw new IllegalArgumentException("Insufficient write buffer size: " + size + ", min.=" + SftpClient.MIN_WRITE_BUFFER_SIZE);
        }

        writeBufferSize = size;
    }

    public final SftpVersionSelector getSftpVersionSelector() {
        return selector;
    }

}


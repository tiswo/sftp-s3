package com.dockop.s3.sftp.core;

import com.dockop.s3.sftp.common.Attributes;
import com.dockop.s3.sftp.core.file.S3FileSystem;
import com.dockop.s3.sftp.core.file.S3FileSystemProvider;
import com.dockop.s3.sftp.core.file.S3Path;
import org.apache.sshd.common.util.logging.AbstractLoggingBean;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;
import java.util.Objects;

/**
 * 类名称: S3AclFileAttributeView <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 18/2/24 下午4:05
 */
public class S3AclFileAttributeView extends AbstractLoggingBean implements AclFileAttributeView {

    protected final S3FileSystemProvider provider;
    protected final Path path;
    protected final LinkOption[] options;

    public S3AclFileAttributeView(S3FileSystemProvider provider, Path path, LinkOption... options) {
        this.provider = Objects.requireNonNull(provider, "No file system provider instance");
        this.path = Objects.requireNonNull(path, "No path");
        this.options = options;
    }

    public final S3FileSystemProvider provider() {
        return provider;
    }

    /**
     * @return The referenced view {@link Path}
     */
    public final Path getPath() {
        return path;
    }

    protected Attributes readRemoteAttributes() throws IOException {
        return provider.readRemoteAttributes(provider.toSftpPath(path), options);
    }

    protected void writeRemoteAttributes(Attributes attrs) throws IOException {
        S3Path p = provider.toSftpPath(path);
        S3FileSystem fs = p.getFileSystem();
    }

    @Override
    public UserPrincipal getOwner() throws IOException {
        PosixFileAttributes v = provider.readAttributes(path, PosixFileAttributes.class, options);
        return v.owner();
    }

    @Override
    public void setOwner(UserPrincipal owner) throws IOException {
        provider.setAttribute(path, "posix", "owner", owner, options);
    }

    @Override
    public String name() {
        return "acl";
    }

    @Override
    public List<AclEntry> getAcl() throws IOException {
        return readRemoteAttributes().getAcl();
    }

    @Override
    public void setAcl(List<AclEntry> acl) throws IOException {
        writeRemoteAttributes(new Attributes().acl(acl));
    }
}

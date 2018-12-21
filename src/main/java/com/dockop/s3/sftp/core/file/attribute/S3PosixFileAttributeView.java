package com.dockop.s3.sftp.core.file.attribute;

import com.dockop.s3.sftp.common.Attributes;
import com.dockop.s3.sftp.core.file.S3FileSystem;
import com.dockop.s3.sftp.core.file.S3FileSystemProvider;
import com.dockop.s3.sftp.core.file.S3Path;
import org.apache.sshd.client.subsystem.sftp.SftpClient;
import org.apache.sshd.common.util.GenericUtils;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.attribute.*;
import java.util.Objects;
import java.util.Set;

/**
 * 类名称: S3PosixFileAttributeView <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 18/2/24 下午4:16
 */
public class S3PosixFileAttributeView implements PosixFileAttributeView{

    protected final S3FileSystemProvider provider;
    protected final S3Path path;
    protected final LinkOption[] options;

    public S3PosixFileAttributeView(S3FileSystemProvider provider, S3Path path, LinkOption... options) {
        this.provider = Objects.requireNonNull(provider, "No file system provider instance");
        this.path = Objects.requireNonNull(path, "No path");
        this.options = options;
    }

    @Override
    public String name() {
        return "posix";
    }

    @Override
    public PosixFileAttributes readAttributes() throws IOException {
        return new S3PosixFileAttributes(path);
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        SftpClient.Attributes attrs = new SftpClient.Attributes();
        if (lastModifiedTime != null) {
            attrs.modifyTime(lastModifiedTime);
        }
        if (lastAccessTime != null) {
            attrs.accessTime(lastAccessTime);
        }
        if (createTime != null) {
            attrs.createTime(createTime);
        }

        if (GenericUtils.isEmpty(attrs.getFlags())) {
//            if (log.isDebugEnabled()) {
//                log.debug("setTimes({}) no changes", path);
//            }
        } else {
            writeRemoteAttributes(attrs);
        }
    }

    @Override
    public void setPermissions(Set<PosixFilePermission> perms) throws IOException {
        provider.setAttribute(path, "permissions", perms, options);
    }

    @Override
    public void setGroup(GroupPrincipal group) throws IOException {
        provider.setAttribute(path, "group", group, options);
    }

    @Override
    public UserPrincipal getOwner() throws IOException {
        return readAttributes().owner();
    }

    @Override
    public void setOwner(UserPrincipal owner) throws IOException {
        provider.setAttribute(path, "owner", owner, options);
    }

    protected Attributes readRemoteAttributes() throws IOException {
        return provider.readRemoteAttributes(provider.toSftpPath(path), options);
    }

    protected void writeRemoteAttributes(SftpClient.Attributes attrs) throws IOException {
        S3Path p = provider.toSftpPath(path);
        S3FileSystem fs = p.getFileSystem();
//        try (SftpClient client = fs.getClient()) {
//            try {
////                if (log.isDebugEnabled()) {
////                    log.debug("writeRemoteAttributes({})[{}]: {}", fs, p, attrs);
////                }
//                client.setStat(p.toString(), attrs);
//            } catch (SftpException e) {
//                if (e.getStatus() == SftpConstants.SSH_FX_NO_SUCH_FILE) {
//                    throw new NoSuchFileException(p.toString());
//                }
//                throw e;
//            }
//        }
    }
}

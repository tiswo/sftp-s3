package com.dockop.s3.sftp.core.file.attribute;

import com.dockop.s3.sftp.common.Attributes;
import com.dockop.s3.sftp.core.file.S3Path;
import org.apache.sshd.client.subsystem.sftp.SftpFileSystem;
import org.apache.sshd.client.subsystem.sftp.SftpFileSystemProvider;
import org.apache.sshd.common.util.GenericUtils;

import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.util.Set;

/**
 * 类名称: S3PosixFileAttributes <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 18/2/26 下午3:26
 */
public class S3PosixFileAttributes implements PosixFileAttributes{
    private final S3Path path;
    private final Attributes attributes;

    public S3PosixFileAttributes(S3Path path) {
        this.path = path;
        this.attributes = path.getAttributes();
    }

    /**
     * @return The referenced attributes file {@link Path}
     */
    public final Path getPath() {
        return path;
    }

    @Override
    public UserPrincipal owner() {
        String owner = attributes.getOwner();
        return GenericUtils.isEmpty(owner) ? null : new SftpFileSystem.DefaultUserPrincipal(owner);
    }

    @Override
    public GroupPrincipal group() {
        String group = attributes.getOwner();
        return GenericUtils.isEmpty(group) ? null : new SftpFileSystem.DefaultGroupPrincipal(group);
    }

    @Override
    public Set<PosixFilePermission> permissions() {
        Set<PosixFilePermission> permissions= SftpFileSystemProvider.permissionsToAttributes(attributes.getPermissions());
        permissions.add(PosixFilePermission.OWNER_READ);//默认添加读权限;
        return permissions;
    }

    @Override
    public FileTime lastModifiedTime() {
        return attributes.getModifyTime();
    }

    @Override
    public FileTime lastAccessTime() {
        return attributes.getAccessTime();
    }

    @Override
    public FileTime creationTime() {
        return attributes.getCreateTime();
    }

    @Override
    public boolean isRegularFile() {
        return attributes.getFileType()== Attributes.Type.File;
    }

    @Override
    public boolean isDirectory() {
        return attributes.getFileType() == Attributes.Type.Directory;
    }

    @Override
    public boolean isSymbolicLink() {
        return attributes.isSymbolicLink();
    }

    @Override
    public boolean isOther() {
        return !isRegularFile() && !isDirectory() && !isSymbolicLink();
    }

    @Override
    public long size() {
        return attributes.getSize();
    }

    @Override
    public Object fileKey() {
        // TODO consider implementing this
        return path.getFileName();
    }
}

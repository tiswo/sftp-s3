package com.dockop.s3.sftp.common;

import org.apache.sshd.common.subsystem.sftp.SftpConstants;
import org.apache.sshd.common.subsystem.sftp.SftpHelper;
import org.apache.sshd.common.subsystem.sftp.SftpUniversalOwnerAndGroup;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.ValidateUtils;

import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 类名称: Attributes <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 18/3/2 下午7:59
 */
public class Attributes {
    private Set<Attribute> flags = EnumSet.noneOf(Attribute.class);
    private int type = SftpConstants.SSH_FILEXFER_TYPE_UNKNOWN;
    private int perms;
    private int uid;
    private int gid;
    private String owner;
    private String group;
    private long size;
    private FileTime accessTime;
    private FileTime createTime;
    private FileTime modifyTime;
    private List<AclEntry> acl;
    private Map<String, byte[]> extensions = Collections.emptyMap();
    private Type fileType;

    public enum Type {
        File,
        Directory,
        Other,
        NoExist
    }

    public Attributes() {
        super();
    }

    public Set<Attribute> getFlags() {
        return flags;
    }

    public Attributes addFlag(Attribute flag) {
        flags.add(flag);
        return this;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public Attributes size(long size) {
        setSize(size);
        return this;
    }

    public void setSize(long size) {
        this.size = size;
        addFlag(Attribute.Size);
    }

    public String getOwner() {
        return owner;
    }

    public Attributes owner(String owner) {
        setOwner(owner);
        return this;
    }

    public Type getFileType() {
        return fileType;
    }

    public void setFileType(Type fileType) {
        this.fileType = fileType;
    }

    public void setOwner(String owner) {
        this.owner = ValidateUtils.checkNotNullAndNotEmpty(owner, "No owner");
        addFlag(Attribute.OwnerGroup);
        if (GenericUtils.isEmpty(getGroup())) {
            setGroup(SftpUniversalOwnerAndGroup.Group.getName());
        }
    }

    public String getGroup() {
        return group;
    }

    public Attributes group(String group) {
        setGroup(group);
        return this;
    }

    public void setGroup(String group) {
        this.group = ValidateUtils.checkNotNullAndNotEmpty(group, "No group");
        addFlag(Attribute.OwnerGroup);
        if (GenericUtils.isEmpty(getOwner())) {
            setOwner(SftpUniversalOwnerAndGroup.Owner.getName());
        }
    }

    public int getUserId() {
        return uid;
    }

    public int getGroupId() {
        return gid;
    }

    public Attributes owner(int uid, int gid) {
        this.uid = uid;
        this.gid = gid;
        addFlag(Attribute.UidGid);
        return this;
    }

    public int getPermissions() {
        return perms;
    }

    public Attributes perms(int perms) {
        setPermissions(perms);
        return this;
    }

    public void setPermissions(int perms) {
        this.perms = perms;
        addFlag(Attribute.Perms);
    }

    public FileTime getAccessTime() {
        return accessTime;
    }

    public Attributes accessTime(long atime) {
        return accessTime(atime, TimeUnit.SECONDS);
    }

    public Attributes accessTime(long atime, TimeUnit unit) {
        return accessTime(FileTime.from(atime, unit));
    }

    public Attributes accessTime(FileTime atime) {
        setAccessTime(atime);
        return this;
    }

    public void setAccessTime(FileTime atime) {
        accessTime = Objects.requireNonNull(atime, "No access time");
        addFlag(Attribute.AccessTime);
    }

    public FileTime getCreateTime() {
        return createTime;
    }

    public Attributes createTime(long ctime) {
        return createTime(ctime, TimeUnit.SECONDS);
    }

    public Attributes createTime(long ctime, TimeUnit unit) {
        return createTime(FileTime.from(ctime, unit));
    }

    public Attributes createTime(FileTime ctime) {
        setCreateTime(ctime);
        return this;
    }

    public void setCreateTime(FileTime ctime) {
        createTime = Objects.requireNonNull(ctime, "No create time");
        addFlag(Attribute.CreateTime);
    }

    public FileTime getModifyTime() {
        return modifyTime;
    }

    public Attributes modifyTime(long mtime) {
        return modifyTime(mtime, TimeUnit.SECONDS);
    }

    public Attributes modifyTime(long mtime, TimeUnit unit) {
        return modifyTime(FileTime.from(mtime, unit));
    }

    public Attributes modifyTime(FileTime mtime) {
        setModifyTime(mtime);
        return this;
    }

    public void setModifyTime(FileTime mtime) {
        modifyTime = Objects.requireNonNull(mtime, "No modify time");
        addFlag(Attribute.ModifyTime);
    }

    public List<AclEntry> getAcl() {
        return acl;
    }

    public Attributes acl(List<AclEntry> acl) {
        setAcl(acl);
        return this;
    }

    public void setAcl(List<AclEntry> acl) {
        this.acl = Objects.requireNonNull(acl, "No ACLs");
        addFlag(Attribute.Acl);
    }

    public Map<String, byte[]> getExtensions() {
        return extensions;
    }

    public Attributes extensions(Map<String, byte[]> extensions) {
        setExtensions(extensions);
        return this;
    }

    public void setStringExtensions(Map<String, String> extensions) {
        setExtensions(SftpHelper.toBinaryExtensions(extensions));
    }

    public void setExtensions(Map<String, byte[]> extensions) {
        this.extensions = Objects.requireNonNull(extensions, "No extensions");
        addFlag(Attribute.Extensions);
    }

    public boolean isRegularFile() {
        return (getPermissions() & SftpConstants.S_IFMT) == SftpConstants.S_IFREG;
    }

    public boolean isDirectory() {
        return (getPermissions() & SftpConstants.S_IFMT) == SftpConstants.S_IFDIR;
    }

    public boolean isSymbolicLink() {
        return (getPermissions() & SftpConstants.S_IFMT) == SftpConstants.S_IFLNK;
    }

    public boolean isOther() {
        return !isRegularFile() && !isDirectory() && !isSymbolicLink();
    }

    @Override
    public String toString() {
        return "type=" + getType()
                + ";size=" + getSize()
                + ";uid=" + getUserId()
                + ";gid=" + getGroupId()
                + ";perms=0x" + Integer.toHexString(getPermissions())
                + ";flags=" + getFlags()
                + ";owner=" + getOwner()
                + ";group=" + getGroup()
                + ";aTime=" + getAccessTime()
                + ";cTime=" + getCreateTime()
                + ";mTime=" + getModifyTime()
                + ";extensions=" + getExtensions().keySet();
    }

    enum Attribute {
        Size,
        UidGid,
        Perms,
        OwnerGroup,
        AccessTime,
        ModifyTime,
        CreateTime,
        Acl,
        Extensions
    }
}

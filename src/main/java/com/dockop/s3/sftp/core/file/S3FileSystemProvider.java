package com.dockop.s3.sftp.core.file;


import com.dockop.s3.sftp.common.Attributes;
import com.dockop.s3.sftp.common.UserClient;
import com.dockop.s3.sftp.core.S3AclFileAttributeView;
import com.dockop.s3.sftp.core.file.attribute.S3PosixFileAttributeView;
import org.apache.sshd.client.subsystem.sftp.SftpClient;
import org.apache.sshd.client.subsystem.sftp.SftpFileSystem;
import org.apache.sshd.client.subsystem.sftp.SftpVersionSelector;
import org.apache.sshd.common.PropertyResolver;
import org.apache.sshd.common.PropertyResolverUtils;
import org.apache.sshd.common.config.SshConfigFileReader;
import org.apache.sshd.common.subsystem.sftp.SftpConstants;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.NumberUtils;
import org.apache.sshd.common.util.ValidateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类名称: S3FileSystemProvider <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 18/2/9 下午5:27
 */
public class S3FileSystemProvider extends FileSystemProvider {
    public static final String READ_BUFFER_PROP_NAME = "sftp-fs-read-buffer-size";
    public static final int DEFAULT_READ_BUFFER_SIZE = SftpClient.DEFAULT_READ_BUFFER_SIZE;
    public static final String WRITE_BUFFER_PROP_NAME = "sftp-fs-write-buffer-size";
    public static final int DEFAULT_WRITE_BUFFER_SIZE = SftpClient.DEFAULT_WRITE_BUFFER_SIZE;
    public static final String CONNECT_TIME_PROP_NAME = "sftp-fs-connect-time";
    public static final long DEFAULT_CONNECT_TIME = SftpClient.DEFAULT_WAIT_TIMEOUT;
    public static final String AUTH_TIME_PROP_NAME = "sftp-fs-auth-time";
    public static final long DEFAULT_AUTH_TIME = SftpClient.DEFAULT_WAIT_TIMEOUT;
    public static final String NAME_DECORDER_CHARSET_PROP_NAME = "sftp-fs-name-decoder-charset";
    public static final Charset DEFAULT_NAME_DECODER_CHARSET = SftpClient.DEFAULT_NAME_DECODING_CHARSET;

    public static final String VERSION_PARAM = "version";

    public static final Set<Class<? extends FileAttributeView>> UNIVERSAL_SUPPORTED_VIEWS =
            Collections.unmodifiableSet(GenericUtils.asSet(
                    PosixFileAttributeView.class,
                    FileOwnerAttributeView.class,
                    BasicFileAttributeView.class
            ));

    protected static final Logger log = LoggerFactory.getLogger(S3FileSystemProvider.class);

    private final UserClient client;

    private final SftpVersionSelector selector;

    private final NavigableMap<String, S3FileSystem> fileSystems = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public static AtomicInteger instanceSize=new AtomicInteger(0);

    public S3FileSystemProvider(UserClient client) {
        super();
        // TODO: make this configurable using system properties
        this.client=client;
        selector=SftpVersionSelector.CURRENT;
    }

    @Override
    public S3FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
        String id = path.toString();
        Map<String, Object> params = resolveFileSystemParameters(env,null);
        PropertyResolver resolver = PropertyResolverUtils.toPropertyResolver(params);
        SftpVersionSelector selector = resolveSftpVersionSelector(getSftpVersionSelector(), resolver);
        S3FileSystem fileSystem;
        synchronized (fileSystems) {
            if (fileSystems.containsKey(id)) {
                throw new FileSystemAlreadyExistsException(id);
            }
            // TODO try and find a way to avoid doing this while locking the file systems cache
            try {
                fileSystem = new S3FileSystem(this, client, id, selector);
                fileSystems.put(id, fileSystem);
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw (IOException) e;
                } else if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new IOException(e);
                }
            }
        }

        fileSystem.setReadBufferSize(resolver.getIntProperty(READ_BUFFER_PROP_NAME, DEFAULT_READ_BUFFER_SIZE));
        fileSystem.setWriteBufferSize(resolver.getIntProperty(WRITE_BUFFER_PROP_NAME, DEFAULT_WRITE_BUFFER_SIZE));
        if (log.isDebugEnabled()) {
            log.debug("newFileSystem({}): {}", path.toString(), fileSystem);
        }
        return fileSystem;
    }

    @Override
    public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        return super.newInputStream(path, options);
    }

    @Override
    public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        return super.newOutputStream(path, options);
    }

    @Override
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return new S3FileSystemChannel(toSftpPath(path));
    }

    @Override
    public AsynchronousFileChannel newAsynchronousFileChannel(Path path, Set<? extends OpenOption> options, ExecutorService executor, FileAttribute<?>... attrs) throws IOException {
        return super.newAsynchronousFileChannel(path, options, executor, attrs);
    }

    @Override
    public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs) throws IOException {
        super.createSymbolicLink(link, target, attrs);
    }

    @Override
    public void createLink(Path link, Path existing) throws IOException {
        super.createLink(link, existing);
    }

    @Override
    public boolean deleteIfExists(Path path) throws IOException {
        return super.deleteIfExists(path);
    }

    @Override
    public Path readSymbolicLink(Path link) throws IOException {
        return super.readSymbolicLink(link);
    }

    @Override
    public String getScheme() {
        return SftpConstants.SFTP_SUBSYSTEM_NAME;
    }

    @Override
    public S3FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        String host = ValidateUtils.checkNotNullAndNotEmpty(uri.getHost(), "Host not provided");
        int port = uri.getPort();
        if (port <= 0) {
            port = SshConfigFileReader.DEFAULT_PORT;
        }

        String userInfo = ValidateUtils.checkNotNullAndNotEmpty(uri.getUserInfo(), "UserInfo not provided");
        String[] ui = GenericUtils.split(userInfo, ':');
        ValidateUtils.checkTrue(GenericUtils.length(ui) == 2, "Invalid user info: %s", userInfo);

        String username = ui[0];
        String password = ui[1];
        String id = getFileSystemIdentifier(host, port, username);
        Map<String, Object> params = resolveFileSystemParameters(env, parseURIParameters(uri));
        PropertyResolver resolver = PropertyResolverUtils.toPropertyResolver(params);
        SftpVersionSelector selector = resolveSftpVersionSelector(getSftpVersionSelector(), resolver);
        Charset decodingCharset =
                PropertyResolverUtils.getCharset(resolver, NAME_DECORDER_CHARSET_PROP_NAME, DEFAULT_NAME_DECODER_CHARSET);
        long maxConnectTime = resolver.getLongProperty(CONNECT_TIME_PROP_NAME, DEFAULT_CONNECT_TIME);
        long maxAuthTime = resolver.getLongProperty(AUTH_TIME_PROP_NAME, DEFAULT_AUTH_TIME);

        S3FileSystem fileSystem;
        synchronized (fileSystems) {
//            if (fileSystems.containsKey(id)) {
//                throw new FileSystemAlreadyExistsException(id);
//            }
            // TODO try and find a way to avoid doing this while locking the file systems cache
//            ClientSession session = null;
            try {
//                session = client.connect(username, host, port)
//                        .verify(maxConnectTime)
//                        .getSession();
//                if (GenericUtils.size(params) > 0) {
//                    // Cannot use forEach because the session is not effectively final
//                    for (Map.Entry<String, ?> pe : params.entrySet()) {
//                        String key = pe.getKey();
//                        Object value = pe.getValue();
//                        if (VERSION_PARAM.equalsIgnoreCase(key)) {
//                            continue;
//                        }
//
//                        PropertyResolverUtils.updateProperty(session, key, value);
//                    }
//
//                    PropertyResolverUtils.updateProperty(session, SftpClient.NAME_DECODING_CHARSET, decodingCharset);
//                }
//
//                session.addPasswordIdentity(password);
//                session.auth().verify(maxAuthTime);

                fileSystem = new S3FileSystem(this, client, id, selector);
                fileSystems.put(id, fileSystem);
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw (IOException) e;
                } else if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new IOException(e);
                }
            }
        }

        fileSystem.setReadBufferSize(resolver.getIntProperty(READ_BUFFER_PROP_NAME, DEFAULT_READ_BUFFER_SIZE));
        fileSystem.setWriteBufferSize(resolver.getIntProperty(WRITE_BUFFER_PROP_NAME, DEFAULT_WRITE_BUFFER_SIZE));
        if (log.isDebugEnabled()) {
            log.debug("newFileSystem({}): {}", uri.toASCIIString(), fileSystem);
        }
        return fileSystem;
    }

    public static String getFileSystemIdentifier(URI uri) {
        String userInfo = ValidateUtils.checkNotNullAndNotEmpty(uri.getUserInfo(), "UserInfo not provided");
        String[] ui = GenericUtils.split(userInfo, ':');
        ValidateUtils.checkTrue(GenericUtils.length(ui) == 2, "Invalid user info: %s", userInfo);
        return getFileSystemIdentifier(uri.getHost(), uri.getPort(), ui[0]);
    }

    public static String getFileSystemIdentifier(String host, int port, String username) {
        return GenericUtils.trimToEmpty(host) + ':'
                + ((port <= 0) ? SshConfigFileReader.DEFAULT_PORT : port) + ':'
                + GenericUtils.trimToEmpty(username);
    }


    protected SftpVersionSelector resolveSftpVersionSelector(SftpVersionSelector defaultSelector, PropertyResolver resolver) {
        String preference = resolver.getString(VERSION_PARAM);
        if (GenericUtils.isEmpty(preference)) {
            return defaultSelector;
        }

        if (log.isDebugEnabled()) {
            log.debug("resolveSftpVersionSelector() preference={}", preference);
        }

        if ("max".equalsIgnoreCase(preference)) {
            return SftpVersionSelector.MAXIMUM;
        } else if ("min".equalsIgnoreCase(preference)) {
            return SftpVersionSelector.MINIMUM;
        } else if ("current".equalsIgnoreCase(preference)) {
            return SftpVersionSelector.CURRENT;
        }

        String[] values = GenericUtils.split(preference, ',');
        if (values.length == 1) {
            return SftpVersionSelector.fixedVersionSelector(Integer.parseInt(values[0]));
        }

        int[] preferred = new int[values.length];
        for (int index = 0; index < values.length; index++) {
            preferred[index] = Integer.parseInt(values[index]);
        }

        return SftpVersionSelector.preferredVersionSelector(preferred);
    }

    // NOTE: URI parameters override environment ones
    public static Map<String, Object> resolveFileSystemParameters(Map<String, ?> env, Map<String, Object> uriParams) {
        if (GenericUtils.isEmpty(env)) {
            return GenericUtils.isEmpty(uriParams) ? Collections.emptyMap() : uriParams;
        } else if (GenericUtils.isEmpty(uriParams)) {
            return Collections.unmodifiableMap(env);
        }

        Map<String, Object> resolved = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        resolved.putAll(env);
        resolved.putAll(uriParams);
        return resolved;
    }


    public static Map<String, Object> parseURIParameters(URI uri) {
        return parseURIParameters((uri == null) ? "" : uri.getQuery());
    }

    public static Map<String, Object> parseURIParameters(String params) {
        if (GenericUtils.isEmpty(params)) {
            return Collections.emptyMap();
        }

        if (params.charAt(0) == '?') {
            if (params.length() == 1) {
                return Collections.emptyMap();
            }
            params = params.substring(1);
        }

        String[] pairs = GenericUtils.split(params, '&');
        Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String p : pairs) {
            int pos = p.indexOf('=');
            if (pos < 0) {
                map.put(p, Boolean.TRUE);
                continue;
            }

            String key = p.substring(0, pos);
            String value = p.substring(pos + 1);
            if (NumberUtils.isIntegerNumber(value)) {
                map.put(key, Long.parseLong(value));
            } else {
                map.put(key, value);
            }
        }

        return map;
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        String id = getFileSystemIdentifier(uri);
        S3FileSystem fs = getFileSystem(id);
        if (fs == null) {
            throw new FileSystemNotFoundException(id);
        }
        return fs;
    }


    /**
     * @param id File system identifier - ignored if {@code null}/empty
     * @return The cached {@link SftpFileSystem} - {@code null} if no match
     */
    public S3FileSystem getFileSystem(String id) {
        if (GenericUtils.isEmpty(id)) {
            return null;
        }
        synchronized (fileSystems) {
            return fileSystems.get(id);
        }
    }

    @Override
    public Path getPath(URI uri) {
        return null;
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return newFileChannel(path, options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return new S3DirectoryStream(toSftpPath(dir));
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        client.mkdir(dir);
    }

    @Override
    public void delete(Path path) throws IOException {

    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {

    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {

    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return false;
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return null;
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        S3Path sftpCephPath=toSftpPath(path);
        if(sftpCephPath.getAttributes().getFileType() == Attributes.Type.NoExist){
           throw new NoSuchFileException("No such File");
        }
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        if (isSupportedFileAttributeView(path, type)) {
            if (AclFileAttributeView.class.isAssignableFrom(type)) {
                return type.cast(new S3AclFileAttributeView(this, path, options));
            } else if (BasicFileAttributeView.class.isAssignableFrom(type)) {
                return type.cast(new S3PosixFileAttributeView(this, (S3Path) path, options));
            }
        }
        throw new UnsupportedOperationException("getFileAttributeView(" + path + ") view not supported: " + type.getSimpleName());
    }

    public boolean isSupportedFileAttributeView(Path path, Class<? extends FileAttributeView> type) {
        S3Path paths= toSftpPath(path);
//        if(!paths.exist()){
//            throw new RuntimeException("No such File");
//        }
        return isSupportedFileAttributeView(toSftpPath(path).getFileSystem(), type);
    }

    public boolean isSupportedFileAttributeView(S3FileSystem fs, Class<? extends FileAttributeView> type) {
        Collection<String> views = fs.supportedFileAttributeViews();
        if ((type == null) || GenericUtils.isEmpty(views)) {
            return false;
        } else if (PosixFileAttributeView.class.isAssignableFrom(type)) {
            return views.contains("posix");
        } else if (AclFileAttributeView.class.isAssignableFrom(type)) {
            return views.contains("acl");   // must come before owner view
        } else if (FileOwnerAttributeView.class.isAssignableFrom(type)) {
            return views.contains("owner");
        } else if (BasicFileAttributeView.class.isAssignableFrom(type)) {
            return views.contains("basic"); // must be last
        } else {
            return false;
        }
    }

    public S3Path toSftpPath(Path path) {
        Objects.requireNonNull(path, "No path provided");
        if (!(path instanceof S3Path)) {
            throw new ProviderMismatchException("Path is not SFTP: " + path);
        }
        return (S3Path) path;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        checkAccess(path);
        if (type.isAssignableFrom(PosixFileAttributes.class)) {
            return type.cast(getFileAttributeView(path, PosixFileAttributeView.class, options).readAttributes());
        }

        throw new UnsupportedOperationException("readAttributes(" + path + ")[" + type.getSimpleName() + "] N/A");    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        String view;
        String attrs;
        int i = attributes.indexOf(':');
        if (i == -1) {
            view = "basic";
            attrs = attributes;
        } else {
            view = attributes.substring(0, i++);
            attrs = attributes.substring(i);
        }

        return readAttributes(path, view, attrs, options);
    }

    public Map<String, Object> readAttributes(Path path, String view, String attrs, LinkOption... options) throws IOException {
        if (!(path instanceof S3Path)) {
            throw new ProviderMismatchException("Path is not SFTP: " + path);
        }
        S3Path cephPath= (S3Path) path;
        S3FileSystem fs = cephPath.getFileSystem();
        Collection<String> views = fs.supportedFileAttributeViews();
        if (GenericUtils.isEmpty(views) || (!views.contains(view))) {
            throw new UnsupportedOperationException("readAttributes(" + path + ")[" + view + ":" + attrs + "] view not supported: " + views);
        }

        if ("basic".equalsIgnoreCase(view) || "posix".equalsIgnoreCase(view) || "owner".equalsIgnoreCase(view)) {
            return readPosixViewAttributes(cephPath, view, attrs, options);
        } else if ("acl".equalsIgnoreCase(view)) {
            return readAclViewAttributes(cephPath, view, attrs, options);
        } else {
            return readCustomViewAttributes(cephPath, view, attrs, options);
        }
    }

    protected Map<String, Object> readPosixViewAttributes(S3Path path, String view, String attrs, LinkOption... options) throws IOException {
        PosixFileAttributes v = readAttributes(path, PosixFileAttributes.class, options);
        if ("*".equals(attrs)) {
            attrs = "lastModifiedTime,lastAccessTime,creationTime,size,isRegularFile,isDirectory,isSymbolicLink,isOther,fileKey,owner,permissions,group";
        }

        Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String attr : attrs.split(",")) {
            switch (attr) {
                case "lastModifiedTime":
                    map.put(attr, v.lastModifiedTime());
                    break;
                case "lastAccessTime":
                    map.put(attr, v.lastAccessTime());
                    break;
                case "creationTime":
                    map.put(attr, v.creationTime());
                    break;
                case "size":
                    map.put(attr, v.size());
                    break;
                case "isRegularFile":
                    map.put(attr, v.isRegularFile());
                    break;
                case "isDirectory":
                    map.put(attr, v.isDirectory());
                    break;
                case "isSymbolicLink":
                    map.put(attr, v.isSymbolicLink());
                    break;
                case "isOther":
                    map.put(attr, v.isOther());
                    break;
                case "fileKey":
                    map.put(attr, v.fileKey());
                    break;
                case "owner":
                    map.put(attr, v.owner());
                    break;
                case "permissions":
                    map.put(attr, v.permissions());
                    break;
                case "group":
                    map.put(attr, v.group());
                    break;
                default:
//                    if (log.isTraceEnabled()) {
//                        log.trace("readPosixViewAttributes({})[{}:{}] ignored for {}", path, view, attr, attrs);
//                    }
            }
        }
        return map;
    }

    protected Map<String, Object> readCustomViewAttributes(S3Path path, String view, String attrs, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("readCustomViewAttributes(" + path + ")[" + view + ":" + attrs + "] view not supported");
    }

    protected Map<String, Object> readAclViewAttributes(S3Path path, String view, String attrs, LinkOption... options) throws IOException {
        if ("*".equals(attrs)) {
            attrs = "acl,owner";
        }

        S3FileSystem fs = path.getFileSystem();
//        SftpClient.Attributes attributes;
//        try (SftpClient client = fs.getClient()) {
//            attributes = readRemoteAttributes(path, options);
//        }
//
        Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
//        for (String attr : attrs.split(",")) {
//            switch (attr) {
//                case "acl":
//                    List<AclEntry> acl = attributes.getAcl();
//                    if (acl != null) {
//                        map.put(attr, acl);
//                    }
//                    break;
//                case "owner":
//                    String owner = attributes.getOwner();
//                    if (GenericUtils.length(owner) > 0) {
//                        map.put(attr, new SftpFileSystem.DefaultUserPrincipal(owner));
//                    }
//                    break;
//                default:
////                    if (log.isTraceEnabled()) {
////                        log.trace("readAclViewAttributes({})[{}] unknown attribute: {}", fs, attrs, attr);
////                    }
//            }
//        }

        return map;
    }

    public Attributes readRemoteAttributes(S3Path path, LinkOption... options) throws IOException {
        S3FileSystem fs = path.getFileSystem();
        return new Attributes();
//        try (SftpClient client = fs.getClient()) {
//            try {
//                SftpClient.Attributes attrs;
//                if (IoUtils.followLinks(options)) {
//                    attrs = client.stat(path.toString());
//                } else {
//                    attrs = client.lstat(path.toString());
//                }
//                if (log.isTraceEnabled()) {
////                    log.trace("readRemoteAttributes({})[{}]: {}", fs, path, attrs);
//                }
//                return attrs;
//            } catch (SftpException e) {
//                if (e.getStatus() == SftpConstants.SSH_FX_NO_SUCH_FILE) {
//                    throw new NoSuchFileException(path.toString());
//                }
//                throw e;
//            }
//        }
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        String view;
        String attr;
        int i = attribute.indexOf(':');
        if (i == -1) {
            view = "basic";
            attr = attribute;
        } else {
            view = attribute.substring(0, i++);
            attr = attribute.substring(i);
        }

        setAttribute(path, view, attr, value, options);
    }

    public void setAttribute(Path path, String view, String attr, Object value, LinkOption... options) throws IOException {
        S3Path p = toSftpPath(path);
        S3FileSystem fs = p.getFileSystem();
        Collection<String> views = fs.supportedFileAttributeViews();
        if (GenericUtils.isEmpty(views) || (!views.contains(view))) {
            throw new UnsupportedOperationException("setAttribute(" + path + ")[" + view + ":" + attr + "=" + value + "] view " + view + " not supported: " + views);
        }

        SftpClient.Attributes attributes = new SftpClient.Attributes();
        switch (attr) {
            case "lastModifiedTime":
                attributes.modifyTime((int) ((FileTime) value).to(TimeUnit.SECONDS));
                break;
            case "lastAccessTime":
                attributes.accessTime((int) ((FileTime) value).to(TimeUnit.SECONDS));
                break;
            case "creationTime":
                attributes.createTime((int) ((FileTime) value).to(TimeUnit.SECONDS));
                break;
            case "size":
                attributes.size(((Number) value).longValue());
                break;
            case "permissions":
                @SuppressWarnings("unchecked")
                Set<PosixFilePermission> attrSet = (Set<PosixFilePermission>) value;
                attributes.perms(attributesToPermissions(path, attrSet));
                break;
            case "owner":
                attributes.owner(((UserPrincipal) value).getName());
                break;
            case "group":
                attributes.group(((GroupPrincipal) value).getName());
                break;
            case "acl":
                ValidateUtils.checkTrue("acl".equalsIgnoreCase(view), "ACL cannot be set via view=%s", view);
                @SuppressWarnings("unchecked")
                List<AclEntry> acl = (List<AclEntry>) value;
                attributes.acl(acl);
                break;
            case "isRegularFile":
            case "isDirectory":
            case "isSymbolicLink":
            case "isOther":
            case "fileKey":
                throw new UnsupportedOperationException("setAttribute(" + path + ")[" + view + ":" + attr + "=" + value + "] modification N/A");
            default:
                if (log.isTraceEnabled()) {
//                    log.trace("setAttribute({})[{}] ignore {}:{}={}", fs, path, view, attr, value);
                }
        }

        if (log.isDebugEnabled()) {
//            log.debug("setAttribute({}) {}: {}", fs, path, attributes);
        }

//        try (SftpClient client = fs.getClient()) {
//            client.setStat(p.toString(), attributes);
//        }
    }

    protected int attributesToPermissions(Path path, Collection<PosixFilePermission> perms) {
        if (GenericUtils.isEmpty(perms)) {
            return 0;
        }

        int pf = 0;
        for (PosixFilePermission p : perms) {
            switch (p) {
                case OWNER_READ:
                    pf |= SftpConstants.S_IRUSR;
                    break;
                case OWNER_WRITE:
                    pf |= SftpConstants.S_IWUSR;
                    break;
                case OWNER_EXECUTE:
                    pf |= SftpConstants.S_IXUSR;
                    break;
                case GROUP_READ:
                    pf |= SftpConstants.S_IRGRP;
                    break;
                case GROUP_WRITE:
                    pf |= SftpConstants.S_IWGRP;
                    break;
                case GROUP_EXECUTE:
                    pf |= SftpConstants.S_IXGRP;
                    break;
                case OTHERS_READ:
                    pf |= SftpConstants.S_IROTH;
                    break;
                case OTHERS_WRITE:
                    pf |= SftpConstants.S_IWOTH;
                    break;
                case OTHERS_EXECUTE:
                    pf |= SftpConstants.S_IXOTH;
                    break;
                default:
                    if (log.isTraceEnabled()) {
                        log.trace("attributesToPermissions(" + path + ") ignored " + p);
                    }
            }
        }

        return pf;
    }

    public final SftpVersionSelector getSftpVersionSelector() {
        return selector;
    }

    public S3FileSystem removeFileSystem(String id) {
        if (GenericUtils.isEmpty(id)) {
            return null;
        }
        S3FileSystem removed;
        synchronized (fileSystems) {
            removed = fileSystems.remove(id);
        }

        if (log.isDebugEnabled()) {
            log.debug("removeFileSystem({}): {}", id, removed);
        }
        return removed;
    }
}

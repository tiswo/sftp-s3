package com.dockop.s3.sftp.core.file;

import com.dockop.s3.sftp.common.Attributes;
import com.dockop.s3.sftp.common.UserClient;
import io.github.mentegy.s3.channels.impl.S3AppendableObjectChannel;
import org.apache.sshd.common.file.util.BasePath;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 类名称: CephFtpFile <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 18/1/24 下午12:19
 */
public class S3Path extends BasePath<S3Path, S3FileSystem> {

    private UserClient client;//连接分布式存储的client;

    private AtomicBoolean marked = new AtomicBoolean(false);

    private Attributes attributes = new Attributes();

    public S3Path(S3FileSystem fileSystem, String root, List names) {
        this(fileSystem,root,names,null);
    }

    public S3Path(S3FileSystem fileSystem, String root, List names, Attributes attributes) {
        super(fileSystem, root, names);
        this.client = fileSystem.getClient();
        if(attributes != null){
            this.attributes = attributes;
            marked();
        }
    }

    @Override
    protected S3Path asT() {
        return super.asT();
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
        return super.create(root, names);
    }

    @Override
    public S3FileSystem getFileSystem() {
        return super.getFileSystem();
    }

    @Override
    public boolean isAbsolute() {
        return super.isAbsolute();
    }

    @Override
    public S3Path getRoot() {
        return super.getRoot();
    }

    @Override
    public S3Path getFileName() {
        return this;
    }

    @Override
    public S3Path getParent() {
        return super.getParent();
    }

    @Override
    public int getNameCount() {
        return super.getNameCount();
    }

    @Override
    public S3Path getName(int index) {
        return super.getName(index);
    }

    @Override
    public S3Path subpath(int beginIndex, int endIndex) {
        return super.subpath(beginIndex, endIndex);
    }

    @Override
    protected boolean startsWith(List list, List other) {
        return super.startsWith(list, other);
    }

    @Override
    public boolean startsWith(Path other) {
        return super.startsWith(other);
    }

    @Override
    public boolean startsWith(String other) {
        return super.startsWith(other);
    }

    @Override
    protected boolean endsWith(List list, List other) {
        return super.endsWith(list, other);
    }

    @Override
    public boolean endsWith(Path other) {
        return super.endsWith(other);
    }

    @Override
    public boolean endsWith(String other) {
        return super.endsWith(other);
    }

    @Override
    protected boolean isNormal() {
        return super.isNormal();
    }

    @Override
    public S3Path normalize() {
        return super.normalize();
    }

    @Override
    public S3Path resolve(Path other) {
        return super.resolve(other);
    }

    @Override
    public S3Path resolve(String other) {
        return super.resolve(other);
    }

    @Override
    public Path resolveSibling(Path other) {
        return super.resolveSibling(other);
    }

    @Override
    public Path resolveSibling(String other) {
        return super.resolveSibling(other);
    }

    @Override
    public S3Path relativize(Path other) {
        return super.relativize(other);
    }

    @Override
    public S3Path toAbsolutePath() {
        return super.toAbsolutePath();
    }

    @Override
    public URI toUri() {
        return super.toUri();
    }

    @Override
    public File toFile() {
        return super.toFile();
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind[] events) throws IOException {
        return super.register(watcher, events);
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind[] events, WatchEvent.Modifier... modifiers) throws IOException {
        return super.register(watcher, events, modifiers);
    }

    @Override
    public Iterator<Path> iterator() {
        return super.iterator();
    }

    @Override
    public int compareTo(Path paramPath) {
        return super.compareTo(paramPath);
    }

    @Override
    protected int compare(String s1, String s2) {
        return super.compare(s1, s2);
    }

    @Override
    protected S3Path checkPath(Path paramPath) {
        return super.checkPath(paramPath);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    protected int calculatedHashCode() {
        return super.calculatedHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    protected String asString() {
        return super.asString();
    }

    @Override
    public S3Path toRealPath(LinkOption... options) throws IOException {
        S3Path absolute = toAbsolutePath();
        FileSystem fs = getFileSystem();
        FileSystemProvider provider = fs.provider();
        provider.checkAccess(absolute);
        return absolute;
    }

    public void setAttributes(Attributes attributes) {
        synchronized (this){
            if (isMarked()) {
                return;
            }
            this.attributes = attributes;
            marked();
        }
    }

    public Attributes getAttributes() {
        if(isMarked()){
            return attributes;
        }else{
            client.markPath(this);
        }
        return attributes;
    }

    public InputStream createInputStream(long offset) throws IOException {
        return client.getInputStream(this);
    }

    public S3AppendableObjectChannel getChannel() throws IOException {
        return client.getChannel(this);
    }

    public void marked(){
        this.marked.set(true);
    }

    public boolean isMarked() {
        return marked.get();
    }
}

package com.dockop.s3.sftp.common;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.dockop.s3.sftp.auth.User;
import com.dockop.s3.sftp.core.file.S3FileSystem;
import com.dockop.s3.sftp.core.file.S3Path;
import io.github.mentegy.s3.channels.S3WritableObjectChannel;
import io.github.mentegy.s3.channels.impl.S3AppendableObjectChannel;
import org.apache.sshd.common.util.logging.AbstractLoggingBean;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.dockop.s3.sftp.common.Constants.PATH_SEPARATOR;

/**
 * 类名称: UserClient <br>
 * 类描述: 连接ceph的客户端<br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 18/3/1 下午7:37
 */
public class UserClient extends AbstractLoggingBean {

    private AmazonS3 client;

    private String userName;

    private String bucketName;

    private String defaultPath="/";

    private User user;

    private ConfigurationContext context;

    public UserClient(User user, AmazonS3 client, String bucketName) {
        this.user = user;
        this.userName = user.getName();
        this.bucketName = bucketName;
        this.client = client;
    }

    public UserClient(String userName, AmazonS3 client, String bucketName) {
        this.userName = userName;
        this.client = client;
        this.bucketName = bucketName;
    }

    public UserClient(AmazonS3 client, String bucketName, String userName, String defaultPath) {
        this.client = client;
        this.bucketName = bucketName;
        this.userName = userName;
        this.defaultPath = defaultPath;
    }

    public List<Path> list(Path path){
        if (path instanceof S3Path){
            S3Path s3Path = (S3Path) path;
            S3FileSystem fileSystem = s3Path.getFileSystem();
            String prefix = normalizeDirectory(s3Path);
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
            listObjectsRequest.setBucketName(bucketName);
            listObjectsRequest.setPrefix(prefix);
            listObjectsRequest.setDelimiter(PATH_SEPARATOR);
            ObjectListing objects = client.listObjects(listObjectsRequest);
            List<Path> list = new ArrayList<Path>();
            List<String> names = Collections.emptyList();
            int perm = user.isWritepermission() ? 0000200 : 0000400;
            while (true) {
                for (String commonPrefixes : objects.getCommonPrefixes()) {
                    Attributes attributes = new Attributes();
                    attributes.setSize(0);
                    attributes.setOwner(user.getName());
                    attributes.setGroup(user.getName());
                    attributes.setFileType(Attributes.Type.Directory);
                    attributes.setAccessTime(FileTime.from(new Date().getTime(), TimeUnit.MILLISECONDS));
                    attributes.setCreateTime(FileTime.from(new Date().getTime(), TimeUnit.MILLISECONDS));
                    attributes.setModifyTime(FileTime.from(new Date().getTime(), TimeUnit.MILLISECONDS));
                    attributes.setPermissions(perm);
                    String dirName = commonPrefixes;
                    if (dirName.length() > 0 && dirName.charAt(dirName.length() - 1) == '/') {
                        dirName = dirName.substring(0, dirName.length() - 1);
                    }
                    S3Path sftpCephPath = new S3Path(fileSystem, dirName.replaceFirst(prefix, ""), names, attributes);
                    list.add(sftpCephPath);
                }
                for (S3ObjectSummary s3ObjectSummary : objects.getObjectSummaries()) {
                    String objectName = s3ObjectSummary.getKey().replaceFirst(prefix, Constants.EMPTY_STRING);
                    if (!StringUtils.isEmpty(objectName)) {
                        Attributes attributes = new Attributes();
                        attributes.setFileType(Attributes.Type.File);
                        attributes.setOwner(user.getName());
                        attributes.setGroup(user.getName());
                        attributes.setSize(s3ObjectSummary.getSize());
                        attributes.setAccessTime(FileTime.from(s3ObjectSummary.getLastModified().getTime(), TimeUnit.MILLISECONDS));
                        attributes.setCreateTime(FileTime.from(s3ObjectSummary.getLastModified().getTime(), TimeUnit.MILLISECONDS));
                        attributes.setModifyTime(FileTime.from(s3ObjectSummary.getLastModified().getTime(), TimeUnit.MILLISECONDS));
                        attributes.setPermissions(perm);
                        S3Path sftpCephPath = new S3Path(fileSystem, objectName, names, attributes);
                        list.add(sftpCephPath);
                    }
                }
                if (list.size() > 1000) {//超过1千不显示
                    break;
                }
                if (objects.isTruncated()) {
                    objects = client.listNextBatchOfObjects(objects);
                } else {
                    break;
                }
            }
            return list;
        }else {
            return Collections.emptyList();
        }
    }

    public AmazonS3 getS3Client() {
        return client;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getDefaultPath() {
        return defaultPath;
    }

    public String getUserName() {
        return userName;
    }

    public void setContext(ConfigurationContext context) {
        this.context = context;
    }

    public void markPath(S3Path path){
        if(path.isMarked()){
            return;
        }
        boolean isDirectory = false;
        boolean isFile = false;
        int perm = user.isWritepermission() ? 0000200 : 0000400;
        if (PATH_SEPARATOR.equals(path.toString())) {
            isDirectory = true;
        }else{
            String dir = normalizeDirectory(path);
            if (client.doesObjectExist(bucketName, dir)) {
                isDirectory = true;
            }else{
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
                listObjectsRequest.setBucketName(bucketName);
                listObjectsRequest.setPrefix(dir);
                listObjectsRequest.setDelimiter(PATH_SEPARATOR);
                ObjectListing objects = client.listObjects(listObjectsRequest);
                while (true) {
                    if (objects.getCommonPrefixes().size() > 0 || objects.getObjectSummaries().size() > 0) {
                        isDirectory = true;
                    }
                    if (objects.isTruncated()) {
                        objects = client.listNextBatchOfObjects(objects);
                    } else {
                        break;
                    }
                }
            }
        }
        Attributes attributes = new Attributes();
        attributes.setFileType(Attributes.Type.Other);
        attributes.setPermissions(perm);
        if(isDirectory){
            attributes.setSize(0);
            attributes.setModifyTime(FileTime.from(new Date().getTime(), TimeUnit.MILLISECONDS));
            attributes.setFileType(Attributes.Type.Directory);
        }else{
            isFile = client.doesObjectExist(bucketName, normalize(path));
            if (isFile) {
                ObjectMetadata objectMetadata = client.getObjectMetadata(bucketName, normalize(path));
                attributes.setSize(objectMetadata.getContentLength());
                attributes.setModifyTime(FileTime.from(objectMetadata.getLastModified().getTime(), TimeUnit.MILLISECONDS));
                attributes.setFileType(Attributes.Type.File);
            }
        }
        if(!isDirectory && !isFile){
            attributes.setFileType(Attributes.Type.NoExist);
        }
        path.setAttributes(attributes);
    }

    public void mkdir(Path path){
        if(path instanceof S3Path){
            String objectName = normalizeDirectory((S3Path) path);
            try{
                client.putObject(bucketName, objectName, "sftp create");
            }catch (Exception e){
                log.error("create object fail",e);
                throw e;
            }
            log.info("mkdir {} success", objectName);
        }else {
            log.warn("path:{} not a SFTPCephPath",path.getClass().getName());
        }
    }

    private String normalize(S3Path s3Path){
        String path = s3Path.getFileSystem().getId() + s3Path.toString();
        path = path.replaceAll("//","/");
        if(path.charAt(0) == '/'){
            return path.substring(1);
        }
        return path;
    }

    private String normalizeDirectory(S3Path s3Path) {
        String dir=normalize(s3Path);
        if (dir.length() > 0 && dir.charAt(dir.length() - 1) != '/') {
            dir += '/';
        }
        return dir;
    }

    public InputStream getInputStream(S3Path path){
        try{
            S3Object object = client.getObject(bucketName, normalize(path));
            return object.getObjectContent();
        }catch (Exception e){
           log.warn(e.getMessage());
        }
        return null;
    }

    public S3AppendableObjectChannel getChannel(S3Path path){
        InitiateMultipartUploadResult upload = null;
        try {
            String key = normalize(path);
            upload = client.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucketName, key));
            log.info("sftp upload,upload bucket:{}, key:{}", bucketName, key);
            S3AppendableObjectChannel s3channel = (S3AppendableObjectChannel) S3WritableObjectChannel.builder()
                    .amazonS3(client)
                    .defaultCachedThreadPoolExecutor()
                    .bucket(bucketName)
                    .key(key)
                    .uploadId(upload.getUploadId()).build();
            return s3channel;
        } catch (Throwable e) {
            if (upload != null) {
                client.abortMultipartUpload(new AbortMultipartUploadRequest(upload.getBucketName(), upload.getKey(), upload.getUploadId()));
            }
            throw e;
        }
    }

    public void shutdown(){
        if(client != null){
            synchronized (client){
                if (client instanceof AmazonS3Client) {
                    ((AmazonS3Client) client).shutdown();
                }
            }
        }
    }
}

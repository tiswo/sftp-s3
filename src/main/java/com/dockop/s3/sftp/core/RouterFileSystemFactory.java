package com.dockop.s3.sftp.core;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.dockop.s3.sftp.auth.User;
import com.dockop.s3.sftp.common.ConfigurationContext;
import com.dockop.s3.sftp.common.Constants;
import com.dockop.s3.sftp.common.UserClient;
import com.dockop.s3.sftp.core.file.S3FileSystemProvider;
import com.dockop.s3.sftp.core.file.S3RootPath;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.root.RootedFileSystemProvider;
import org.apache.sshd.common.session.Session;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;

/**
 * 类名称: RouterFileSystemFactory <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 18/3/9 下午6:00
 */
public class RouterFileSystemFactory implements FileSystemFactory{

    private ConfigurationContext context;

    public RouterFileSystemFactory(ConfigurationContext context) {
        this.context = context;
    }

    @Override
    public FileSystem createFileSystem(Session session) throws IOException {
        User user = session.getAttribute(Constants.USER_SESSION_KEY);
        if (user.isUseS3()) {//use s3 server as sftp fileSystem
            AWSCredentials credentials = new BasicAWSCredentials(user.getAccessKey(), user.getSecretKey());
            ClientConfiguration clientConfig = new ClientConfiguration();
            clientConfig.setProtocol(context.getS3().getProtocol());
            AmazonS3 client = new AmazonS3Client(credentials, clientConfig);
            client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build());
            client.setEndpoint(context.getS3().getEndPoint());
            UserClient userClient = new UserClient(user, client, user.getBucket());
            userClient.setContext(context);
            return new S3FileSystemProvider(userClient).newFileSystem(new S3RootPath(user.getHomeDirectory()), Collections.emptyMap());
        } else {
            //user local filesystem
            Path dir = new File(user.getHomeDirectory()).toPath();
            if (dir == null) {
                throw new InvalidPathException(user.getName(), "Cannot resolve home directory");
            }
            return new RootedFileSystemProvider().newFileSystem(dir, Collections.emptyMap());
        }
    }
}

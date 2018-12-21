package com.dockop.s3.sftp.core.file;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * 类名称: S3DirectoryStream <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 18/2/26 下午3:46
 */
public class S3DirectoryStream implements DirectoryStream{

    private final S3Path p;

    public S3DirectoryStream(S3Path path) throws IOException {
        S3FileSystem fs = path.getFileSystem();
        p = path;
    }

    @Override
    public Iterator<Path> iterator() {
        return new ArrayList<Path>().iterator();
    }

    @Override
    public void close() throws IOException {

    }
}

package com.dockop.s3.sftp.common;

/**
 * 类名称: SFTPException <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 2018/12/22 上午1:12
 */
public class SFTPException extends RuntimeException{

    public SFTPException(String message) {
        super(message);
    }

    public SFTPException(String message, Throwable cause) {
        super(message, cause);
    }
}

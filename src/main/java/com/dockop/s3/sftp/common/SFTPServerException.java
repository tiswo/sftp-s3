package com.dockop.s3.sftp.common;

/**
 * 类名称: SFTPServerException <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 2018/12/22 上午1:12
 */
public class SFTPServerException extends RuntimeException{

    public SFTPServerException(String message) {
        super(message);
    }

    public SFTPServerException(String message, Throwable cause) {
        super(message, cause);
    }
}

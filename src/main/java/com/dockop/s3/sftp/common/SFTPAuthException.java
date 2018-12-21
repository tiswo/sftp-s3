package com.dockop.s3.sftp.common;

/**
 * 类名称: SFTPAuthException <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 2018/12/22 上午1:12
 */
public class SFTPAuthException extends RuntimeException{

    public SFTPAuthException(String message) {
        super(message);
    }

    public SFTPAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.dockop.s3.sftp.common;

import com.dockop.s3.sftp.auth.User;
import org.apache.sshd.common.AttributeStore;

/**
 * 类名称: Constants <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 18/3/2 下午8:19
 */
public class Constants {
    public final static String PATH_SEPARATOR ="/";

    public final static String EMPTY_STRING = "";

    public final static AttributeStore.AttributeKey<User> USER_SESSION_KEY = new AttributeStore.AttributeKey<User>();

    public final static AttributeStore.AttributeKey<Boolean> USE_S3 = new AttributeStore.AttributeKey<Boolean>();

    public final static String DEFAULT_AUTHORIZED_KEYS_PATH = System.getProperty("user.home") + "/.ssh/authorized_keys";

    public final static String AUTHORIZED_KEYS_PATH = System.getProperty("authorized.keys.path", DEFAULT_AUTHORIZED_KEYS_PATH);
}

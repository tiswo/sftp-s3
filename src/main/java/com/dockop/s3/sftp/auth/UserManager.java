package com.dockop.s3.sftp.auth;

/**
 * 类名称: UserManager <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 2018/12/21 下午11:58
 */
public interface UserManager {

    public static final String ATTR_LOGIN = "username";

    public static final String ATTR_PASSWORD = "password";

    public static final String ATTR_HOME = "homedirectory";

    public static final String ATTR_ACCESS_KEY = "accesskey";

    public static final String ATTR_SECRET_KEY = "secretkey";

    public static final String ATTR_USE_S3 = "s3";

    public static final String ATTR_BUCKET = "bucket";

    public static final String ATTR_WRITE_PERM = "writepermission";

    public static final String ATTR_ENABLE = "enableflag";

    public static final String ATTR_MAX_IDLE_TIME = "idletime";

    public static final String ATTR_MAX_UPLOAD_RATE = "uploadrate";

    public static final String ATTR_MAX_DOWNLOAD_RATE = "downloadrate";

    public static final String ATTR_MAX_LOGIN_NUMBER = "maxloginnumber";

    public static final String ATTR_MAX_LOGIN_PER_IP = "maxloginperip";

    User getUserByName(String username);

    String[] getAllUserNames();

    void delete(String username);

    void save(User user);

    boolean doesExist(String username);

    User authenticate(Authentication authentication);

    public PasswordEncryptor getPasswordEncryptor();
}
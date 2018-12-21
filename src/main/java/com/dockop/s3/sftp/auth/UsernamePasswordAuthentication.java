package com.dockop.s3.sftp.auth;

/**
 * 类名称: UsernamePasswordAuthentication <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 2018/12/21 下午11:58
 */
public class UsernamePasswordAuthentication implements Authentication {

    private String username;

    private String password;

    public UsernamePasswordAuthentication(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getPassword() {
        return this.password;
    }

    public String getUsername() {
        return this.username;
    }

}

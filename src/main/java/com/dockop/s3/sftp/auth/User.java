package com.dockop.s3.sftp.auth;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * 类名称: User <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 2018/12/21 下午11:56
 */
@Entity
public class User {

    @Id
    private Long id;

    private String name = null;

    private String password = null;

    private String homeDirectory = null;

    private String accessKey;

    private String secretKey;

    private boolean useS3 = false;

    private String bucket;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isUseS3() {
        return useS3;
    }

    public void setUseS3(boolean useS3) {
        this.useS3 = useS3;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setHomeDirectory(String homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    public String getName(){
        return name;
    }

    public String getPassword(){
        return password;
    }

    public String getHomeDirectory(){
        return homeDirectory;
    }

    public boolean isWritepermission(){//TODO 设置读写权限
        return true;
    }

}

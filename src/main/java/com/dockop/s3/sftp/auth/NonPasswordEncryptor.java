package com.dockop.s3.sftp.auth;


/**
 * 类名称: Authentication <br>
 * 类描述: do noting<br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 2018/12/22 上午01:56
 */
public class NonPasswordEncryptor implements PasswordEncryptor{


    public String encrypt(String password){
        return password;
    }


    public boolean matches(String passwordToCheck, String storedPassword){
        if(passwordToCheck == null|| storedPassword == null){
            return false;
        }
        return passwordToCheck.equals(storedPassword);
    }
    
}

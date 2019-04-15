package com.dockop.s3.sftp.auth;

import com.dockop.s3.sftp.common.Constants;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类名称: UserPasswordAuthenticator <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 2019/3/20 下午1:02
 */
public class UserPasswordAuthenticator implements PasswordAuthenticator{

    private static Logger logger = LoggerFactory.getLogger(UserPasswordAuthenticator.class);

    private UserManager userManager;

    public UserPasswordAuthenticator(UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public boolean authenticate(String username, String password, ServerSession session) throws PasswordChangeRequiredException {
        Authentication authentication = new UsernamePasswordAuthentication(username, password);
        try {
            User user = userManager.authenticate(authentication);
            session.setAttribute(Constants.USE_S3, user.isUseS3());
            session.setAttribute(Constants.USER_SESSION_KEY, user);
            return true;
        } catch (Exception e) {
            logger.error("user authentication fail", e);
            return false;
        }
    }
}

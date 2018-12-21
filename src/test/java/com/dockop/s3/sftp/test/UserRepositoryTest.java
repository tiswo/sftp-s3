package com.dockop.s3.sftp.test;

import com.dockop.s3.sftp.auth.User;
import com.dockop.s3.sftp.auth.UserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 类名称: UserRepositoryTest <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 2018/12/22 上午12:56
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void addUser() {
        User user = new User();
        user.setId(1l);
        user.setName("张三");
        user.setUseS3(false);
        user.setHomeDirectory("/apps");
        user.setPassword("123456");
        userRepository.save(user);
    }
}
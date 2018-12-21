package com.dockop.s3.sftp.auth;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 类名称: UserRepository <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 2018/12/21 下午11:56
 */
public interface UserRepository extends JpaRepository<User,Integer> {

}
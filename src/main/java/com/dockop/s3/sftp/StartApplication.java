package com.dockop.s3.sftp;

import com.dockop.s3.sftp.auth.*;
import com.dockop.s3.sftp.common.ConfigurationContext;
import com.dockop.s3.sftp.common.Constants;
import com.dockop.s3.sftp.core.RouterFileSystemFactory;
import com.dockop.s3.sftp.core.SFTPSubsystemFactory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuth;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.auth.pubkey.UserAuthPublicKeyFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationPidFileWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class StartApplication implements CommandLineRunner {

    private static Logger logger = LoggerFactory.getLogger(StartApplication.class);

    @Autowired
    private ConfigurationContext configurationContext;

    @Autowired
    private UserManager userManager;

    public static void main(String[] args){
        SpringApplication app = new SpringApplication(StartApplication.class);
        app.setWebEnvironment(false);
        app.addListeners(new ApplicationPidFileWriter());
        app.run(args);
    }

    @Override
    public void run(String... strings) throws Exception {
        logger.info(configurationContext.toString());
        final SshServer sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(configurationContext.getPort());
        sshServer.setFileSystemFactory(new RouterFileSystemFactory(configurationContext));
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File(configurationContext.getHostKeyProvider())));
        sshServer.setCommandFactory(new ScpCommandFactory());
        List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>();
        userAuthFactories.add(new UserAuthPasswordFactory());
        userAuthFactories.add(new UserAuthPublicKeyFactory());
        sshServer.setUserAuthFactories(userAuthFactories);

        //支持公钥免密登陆和密码登陆两种模式
        sshServer.setPublickeyAuthenticator(new UserAuthorizedKeysAuthenticator(new File(Constants.AUTHORIZED_KEYS_PATH), userManager));
        sshServer.setPasswordAuthenticator(new UserPasswordAuthenticator(userManager));
        List<NamedFactory<Command>> namedFactoryList = new ArrayList<NamedFactory<Command>>();
        namedFactoryList.add(new SFTPSubsystemFactory());
        sshServer.setSubsystemFactories(namedFactoryList);
        try {
            sshServer.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Thread.sleep(Long.MAX_VALUE);
    }


    @Bean
    public UserManager userManager() throws IOException {
        ClassPathResource resource = new ClassPathResource(configurationContext.getUserProperties());
        PropertiesUserManager userManager = new PropertiesUserManager(new NonPasswordEncryptor(), resource.getInputStream());
        return userManager;
    }

}
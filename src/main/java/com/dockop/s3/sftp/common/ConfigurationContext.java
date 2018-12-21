package com.dockop.s3.sftp.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * 类名称: ConfigurationContext <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 18/3/5 下午12:46
 */
@ConfigurationProperties(prefix = "sftp")
@Configuration
@Component
public class ConfigurationContext {

    private int port;

    private String hostKeyProvider;

    private String userProperties;

    private S3Config s3;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHostKeyProvider() {
        return hostKeyProvider;
    }

    public void setHostKeyProvider(String hostKeyProvider) {
        this.hostKeyProvider = hostKeyProvider;
    }

    public String getUserProperties() {
        return userProperties;
    }

    public void setUserProperties(String userProperties) {
        this.userProperties = userProperties;
    }

    public S3Config getS3() {
        return s3;
    }

    public void setS3(S3Config s3) {
        this.s3 = s3;
    }

    @Override
    public String toString() {
        return "ConfigurationContext" +
                "\n{" +
                "\n\tport=" + port +
                ", \n\thostKeyProvider='" + hostKeyProvider + '\'' +
                ", \n\tendPoint='" + s3.getEndPoint() + '\'' +
                ", \n\tprotocol=" + s3.getProtocol() +
                "\n}";
    }
}

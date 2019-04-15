# SFTP代理
提供SFTP协议代理访问支持s3协议的ceph等分布式存储
## 启动方式
执行mvn package -Dmaven.test.skip=true

进入sftp-s3-server/bin目录，执行 ./start.sh start

参数说明：
`useS3`=true 表示该用户启用 s3作为底层存储。（反之，以本地系统作为存储）

# 安装 Jenkins

```shell
wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo

rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io.key

yum install jenkins
```

安装成功后查询Jenkins安装路径：

```shell
rpm -ql jenkins
```

创建启动脚本：

```shell
cd /usr/lib/jenkins/

vi jenkins.sh
```

```bash
#!/bin/bash
###主要目的用于开机启动服务,不然 启动jenkins.war包没有java -jar的权限
JAVA_HOME=/usr/local/java
pid=`ps -ef | grep jenkins.war | grep -v 'grep'| awk '{print $2}'| wc -l`
  if [ "$1" = "start" ];then
  if [ $pid -gt 0 ];then
  echo 'jenkins is running...'
else
  ### java启动服务 配置java安装根路径,和启动war包存的根路径
  nohup $JAVA_HOME/bin/java -jar /usr/lib/jenkins/jenkins.war --httpPort=8080  2>&1 &
  fi
  elif [ "$1" = "stop" ];then
  exec ps -ef | grep jenkins | grep -v grep | awk '{print $2}'| xargs kill -9
  echo 'jenkins is stop..'
else
  echo "Please input like this:"./jenkins.sh start" or "./jenkins stop""
  fi
```

```shell
#授执行权限
chmod +x /usr/lib/jenkins/jenkins.sh

#服务注册
vi /lib/systemd/system/jenkins.service
```

```conf
[Unit]
Description=Jenkins
After=network.target
 
[Service]
Type=forking
ExecStart=/usr/lib/jenkins/jenkins.sh start
ExecReload=
ExecStop=/usr/lib/jenkins/jenkins.sh stop
PrivateTmp=true
 
[Install]
WantedBy=multi-user.target
```

```shell
systemctl daemon-reload

systemctl start jenkins.service
systemctl status jenkins.service

#开机启动
systemctl enable jenkins.service
```

```shell
#重启
reboot
```

- Jenkins默认端口：8080
- 启动后访问http://ServerIP:8080进行访问
- 初始密码在/root/.jenkins/secrets/initialAdminPassword。

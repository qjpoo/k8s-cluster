# 创建Jenkins的Slave服务器

## 准备工作

[安装Jenkins](https://github.com/sunweisheng/Jenkins/blob/master/Install-Jenkins.md)

[安装一台Windows服务器](https://github.com/sunweisheng/Kvm/blob/master/Create-win2008R2.md)

[安装一台Linux服务器](https://github.com/sunweisheng/Kvm/blob/master/Create-Bridge-Cluster.md)

## Linux-Slave

在Slave服务器上创建jenkins用户

```shell
adduser jenkins
passwd jenkins
usermod -aG wheel jenkins

# 创建jenkins工作目录
cd /home/jenkins/
mkdir JenkinsHome
chown jenkins:jenkins JenkinsHome
```

在Jenkins Master服务器上用SSH登录Slave服务器

```shell
ssh jenkins@centos7.slave.ops.bluersw.com
```

在Jenkins-Slave服务器上安装Java

```shell
scp jdk-8u211-linux-x64.tar root@centos7.slave.ops.bluersw.com:/opt

ssh root@centos7.slave.ops.bluersw.com

cd /opt/

tar -xvf jdk-8u211-linux-x64.tar

chown -R root:root jdk1.8.0_211/

ln -s /opt/jdk1.8.0_211/ /usr/local/java

vi /etc/profile
```

```conf
JAVA_HOME=/usr/local/java
JRE_HOME=/usr/local/java/jre
PATH=$PATH:$JAVA_HOME/bin:$JRE_HOME/bin
CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar:$JRE_HOME/lib
export JAVA_HOME JRE_HOME PATH CLASSPATH
```

```shell
#重新加载
source /etc/profile

#检查结果
java -version
```

在Jenkins系统创建slave服务器的jenkins凭证

![Alt text](http://static.bluersw.com/images/Jenkins/jenkins-slave-01.png)

在“系统管理”-“节点管理”中新建节点

![Alt text](http://static.bluersw.com/images/Jenkins/jenkins-slave-02.png)
![Alt text](http://static.bluersw.com/images/Jenkins/jenkins-slave-03.png)
点击保存后查看代理连接日志
![Alt text](http://static.bluersw.com/images/Jenkins/jenkins-slave-04.png)
![Alt text](http://static.bluersw.com/images/Jenkins/jenkins-slave-05.png)

## Windows-Slave

[下载并安装Java](https://www.oracle.com/technetwork/java/javase/downloads/index.html)

添加：

系统变量：JAVA_HOME

变量值：C:\Program Files\Java\jdk1.8.0_251

系统变量：CLASSPATH

变量值：.;%Java_Home%\bin;%Java_Home%\lib\dt.jar;%Java_Home%\lib\tools.jar

修改：

系统变量：Path

变量值：%Java_Home%\bin;%Java_Home%\jre\bin;（在最前面添加）

```shell
# 测试
java -version
```

安装 .NET Framework (2.0和4.0)

[.NET 4.0下载](https://www.microsoft.com/zh-cn/download/details.aspx?id=17718)

![Alt text](http://static.bluersw.com/images/Jenkins/jenkins-slave-09.png)


在Master配置节点，在Win-Slave上进入Jenkins系统启动并安装服务

![Alt text](http://static.bluersw.com/images/Jenkins/jenkins-slave-06.png)
在Win-Slave上进入Jenkins系统启动并安装服务
![Alt text](http://static.bluersw.com/images/Jenkins/jenkins-slave-07.png)

将Slave的Agent程序安装为服务

![Alt text](http://static.bluersw.com/images/Jenkins/jenkins-slave-08.png)

安装成功

![Alt text](http://static.bluersw.com/images/Jenkins/jenkins-slave-10.png)

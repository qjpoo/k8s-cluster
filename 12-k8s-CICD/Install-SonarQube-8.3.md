# 安装SonarQube 8.3版本

[官方文档](https://docs.sonarqube.org/latest/setup/install-server/)

[下载地址](https://www.sonarqube.org)

## 准备工作

* 准备一台CentOS 7服务器
* SonarQube 8.3版本只支持Java 11 ([下载Java 11](https://www.oracle.com/java/technologies/javase-downloads.html))
* [安装pgAdmin](https://www.pgadmin.org/download/)

## 安装PostgreSQL 12.0

```shell
yum install https://download.postgresql.org/pub/repos/yum/reporpms/EL-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm

yum install postgresql12-server

/usr/pgsql-12/bin/postgresql-12-setup initdb
systemctl enable postgresql-12
systemctl start postgresql-12

# 安装后的数据库data目录
cd /var/lib/pgsql/12/data

# 修改配置
vi pg_hba.conf
host    all             all             0.0.0.0/0            md5

vi postgresql.conf
listen_addresses = '*'

systemctl restart postgresql-12

# 客户端程序目录
cd /usr/pgsql-12/bin

# 安装的时候会自动创建postgres用户密码为空
su postgres
bash-4.2$ psql
psql (12.3)
输入 "help" 来获取帮助信息.

# 修改管理员密码(默认是随机密码)
ALTER USER postgres WITH PASSWORD 'postgres';

# 退出
\q
```

## 安装服务端程序

```shell
# 上传SQ v8.3
scp /Users/sunweisheng/Downloads/sonarqube-8.3.1.34397.zip root@sq.bluersw.com:/opt/

# 上传Java 11
scp /Users/sunweisheng/Downloads/jdk-11.0.7_linux-x64_bin.tar root@sq.bluersw.com:/opt/

# 解压缩
yum install zip unzip

cd /opt
tar -xvf jdk-11.0.7_linux-x64_bin.tar
# 一定用ZIP解压缩原始文件，否则会产生很多._XXX的隐藏文件，使程序报错
unzip sonarqube-8.3.1.34397.zip

# 创建用户
groupadd sonar
useradd sonar -g sonar
passwd sonar

chown -R sonar.sonar /opt/jdk-11.0.7/
chown -R sonar.sonar /opt/sonarqube-8.3.1.34397/

```

## 创建数据库

```shell
su postgres

bash-4.2$ psql

# 创建用户
create user sonar with password 'sonar';

# 创建数据库指定所属者
create database sonarqube owner=sonar encoding='UTF8';

# 将dbtest所有权限赋值给sonar
grant all on database sonarqube to sonar;
```

## 配置SonarQube

```shell
# 修改sonar.properties配置文件($SONARQUBE-HOME/conf/sonar.properties)
cd /opt/sonarqube-8.3.1.34397/conf
vi sonar.properties

sonar.jdbc.url=jdbc:postgresql://localhost/sonarqube
sonar.jdbc.username=sonar
sonar.jdbc.password=sonar

# 系统安装的是Java 8，所以需要单独指定Java 11的路径
vi wrapper.conf

wrapper.java.command=/opt/jdk-11.0.7/bin/java

# elasticsearch需要改
vi /etc/sysctl.conf

vm.max_map_count=655360

sysctl -p

# sonar是启动elasticsearch的用户
vi /etc/security/limits.conf

sonar hard nofile 65536
sonar soft nofile 65536
```

## 手工启动检查日志排除错误

```shell
su sonar

# 第一次启动会有各种初始化过程
/opt/sonarqube-8.3.1.34397/bin/linux-x86-64/sonar.sh start

# 查看logs文件夹下的日志文件，排查错误。千万用ZIP解压缩否则产生一堆隐藏文件和莫名错误
cat /opt/sonarqube-8.3.1.34397/logs/sonar.log
cat /opt/sonarqube-8.3.1.34397/logs/es.log
```

## 创建服务

```shell
vi /etc/systemd/system/sonarqube.service
```

ExecStart中的路径请根据版本不同重新设置

```conf
[Unit]
Description=SonarQube service
After=syslog.target network.target

[Service]
Type=simple
User=sonar
Group=sonar
PermissionsStartOnly=true
ExecStart=/bin/nohup /opt/jdk-11.0.7/bin/java -Xms32m -Xmx32m -Djava.net.preferIPv4Stack=true -jar /opt/sonarqube-8.3.1.34397/lib/sonar-application-8.3.1.34397.jar
StandardOutput=syslog
LimitNOFILE=65536
LimitNPROC=8192
TimeoutStartSec=5
Restart=always
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

```shell
systemctl daemon-reload
systemctl enable sonarqube.service
systemctl start sonarqube.service
```

## 访问安装SonarQube

访问 http://192.168.0.5:9000/

默认用户名和密码都是：admin

## 下载SonarScanner

[下载地址](https://docs.sonarqube.org/latest/analysis/scan/sonarscanner/)

设置环境变量：

```shell
sudo chmod a+rwx /etc/profile
sudo vi /etc/profile
```

```conf
SONAR_SCANNER=/Users/sunweisheng/sonar-scanner-4.3.0.2102-macosx
export PATH=$PATH:$SONAR_SCANNER/bin
```

```shell
source /etc/profile
sudo chmod a-wx /etc/profile

# 测试
sonar-scanner -v

# 配置SonarQube服务地址
vi /Users/sunweisheng/sonar-scanner-4.3.0.2102-macosx/conf/sonar-scanner.properties

sonar.host.url=http://sq.bluersw.com:9000
```

## 使用SonarScanner进行代码扫描

```shell
# 在项目根目录创建sonar-project.properties配置文件
vi sonar-project.properties
```

```conf
# must be unique in a given SonarQube instance
sonar.projectKey=Jenkins:agent-server-parameter-plugin

sonar.projectVersion=1.0

# Path is relative to the sonar-project.properties file. Defaults to .
sonar.sources=src
sonar.sourceEncoding=UTF-8
sonar.java.binaries=./target/classes
```

```shell
# 在项目根目录下执行
sonar-scanner
```

```shell
INFO: Scanner configuration file: /Users/sunweisheng/sonar-scanner-4.3.0.2102-macosx/conf/sonar-scanner.properties
INFO: Project root configuration file: /Users/sunweisheng/Documents/Test-Jenkins-Plugin/agent-server-parameter-plugin/sonar-project.properties
INFO: SonarScanner 4.3.0.2102
INFO: Java 11.0.3 AdoptOpenJDK (64-bit)
INFO: Mac OS X 10.15.5 x86_64
INFO: User cache: /Users/sunweisheng/.sonar/cache
INFO: Scanner configuration file: /Users/sunweisheng/sonar-scanner-4.3.0.2102-macosx/conf/sonar-scanner.properties
INFO: Project root configuration file: /Users/sunweisheng/Documents/Test-Jenkins-Plugin/agent-server-parameter-plugin/sonar-project.properties
INFO: Analyzing on SonarQube server 8.3.1
INFO: Default locale: "zh_CN_#Hans", source code encoding: "UTF-8"
INFO: Load global settings
INFO: Load global settings (done) | time=110ms
INFO: Server id: 86E1FA4D-AXLVFrNXKIv5ZwSXjWeI
INFO: User cache: /Users/sunweisheng/.sonar/cache
INFO: Load/download plugins
INFO: Load plugins index
INFO: Load plugins index (done) | time=48ms
INFO: Load/download plugins (done) | time=93ms
INFO: Process project properties
INFO: Process project properties (done) | time=5ms
INFO: Execute project builders
INFO: Execute project builders (done) | time=2ms
INFO: Project key: Jenkins:agent-server-parameter-plugin
INFO: Base dir: /Users/sunweisheng/Documents/Test-Jenkins-Plugin/agent-server-parameter-plugin
INFO: Working dir: /Users/sunweisheng/Documents/Test-Jenkins-Plugin/agent-server-parameter-plugin/.scannerwork
INFO: Load project settings for component key: 'Jenkins:agent-server-parameter-plugin'
INFO: Load project settings for component key: 'Jenkins:agent-server-parameter-plugin' (done) | time=165ms
INFO: Load quality profiles
INFO: Load quality profiles (done) | time=143ms
INFO: Load active rules
INFO: Load active rules (done) | time=1348ms
WARN: SCM provider autodetection failed. Please use "sonar.scm.provider" to define SCM of your project, or disable the SCM Sensor in the project settings.
INFO: Indexing files...
INFO: Project configuration:
INFO: Load project repositories
INFO: Load project repositories (done) | time=87ms
INFO: 21 files indexed
INFO: Quality profile for java: Sonar way
INFO: Quality profile for js: Sonar way
INFO: Quality profile for web: Sonar way
INFO: ------------- Run sensors on module Jenkins:agent-server-parameter-plugin
INFO: Load metrics repository
INFO: Load metrics repository (done) | time=30ms
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by net.sf.cglib.core.ReflectUtils$1 (file:/Users/sunweisheng/.sonar/cache/52f5340dd05620cd162e2b9a45a57124/sonar-javascript-plugin.jar) to method java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain)
WARNING: Please consider reporting this to the maintainers of net.sf.cglib.core.ReflectUtils$1
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
INFO: Sensor JavaSquidSensor [java]
INFO: Configured Java source version (sonar.java.source): none
INFO: JavaClasspath initialization
WARN: Bytecode of dependencies was not provided for analysis of source files, you might end up with less precise results. Bytecode can be provided using sonar.java.libraries property.
INFO: JavaClasspath initialization (done) | time=7ms
INFO: JavaTestClasspath initialization
INFO: JavaTestClasspath initialization (done) | time=0ms
INFO: Java Main Files AST scan
INFO: 6 source files to be analyzed
INFO: 6/6 source files have been analyzed
INFO: Java Main Files AST scan (done) | time=1038ms
INFO: Java Test Files AST scan
INFO: 0 source files to be analyzed
INFO: Java Test Files AST scan (done) | time=0ms
INFO: 0/0 source files have been analyzed
INFO: Java Generated Files AST scan
INFO: 0 source files to be analyzed
INFO: Java Generated Files AST scan (done) | time=1ms
INFO: 0/0 source files have been analyzed
INFO: Sensor JavaSquidSensor [java] (done) | time=1163ms
INFO: Sensor SonarCSS Rules [cssfamily]
INFO: 4 source files to be analyzed
INFO: 4/4 source files have been analyzed
INFO: Sensor SonarCSS Rules [cssfamily] (done) | time=1885ms
INFO: Sensor JaCoCo XML Report Importer [jacoco]
INFO: 'sonar.coverage.jacoco.xmlReportPaths' is not defined. Using default locations: target/site/jacoco/jacoco.xml,target/site/jacoco-it/jacoco.xml,build/reports/jacoco/test/jacocoTestReport.xml
INFO: No report imported, no coverage information will be imported by JaCoCo XML Report Importer
INFO: Sensor JaCoCo XML Report Importer [jacoco] (done) | time=1ms
INFO: Sensor JavaScript analysis [javascript]
INFO: 1 source files to be analyzed
INFO: 1/1 source files have been analyzed
INFO: Sensor SonarJS [javascript]
INFO: 1 source files to be analyzed
INFO: Sensor SonarJS [javascript] (done) | time=93ms
INFO: 1/1 source files have been analyzed
INFO: Sensor JavaScript analysis [javascript] (done) | time=2181ms
INFO: Sensor SurefireSensor [java]
INFO: parsing [/Users/sunweisheng/Documents/Test-Jenkins-Plugin/agent-server-parameter-plugin/target/surefire-reports]
INFO: Sensor SurefireSensor [java] (done) | time=22ms
INFO: Sensor JavaXmlSensor [java]
INFO: Sensor JavaXmlSensor [java] (done) | time=1ms
INFO: Sensor HTML [web]
INFO: Sensor HTML [web] (done) | time=30ms
INFO: ------------- Run sensors on project
INFO: Sensor Zero Coverage Sensor
INFO: Sensor Zero Coverage Sensor (done) | time=7ms
INFO: Sensor Java CPD Block Indexer
INFO: Sensor Java CPD Block Indexer (done) | time=15ms
INFO: SCM Publisher No SCM system was detected. You can use the 'sonar.scm.provider' property to explicitly specify it.
INFO: CPD Executor 8 files had no CPD blocks
INFO: CPD Executor Calculating CPD for 3 files
INFO: CPD Executor CPD calculation finished (done) | time=6ms
INFO: Analysis report generated in 45ms, dir size=113 KB
INFO: Analysis report compressed in 32ms, zip size=32 KB
INFO: Analysis report uploaded in 83ms
INFO: ANALYSIS SUCCESSFUL, you can browse http://sq.bluersw.com:9000/dashboard?id=Jenkins%3Aagent-server-parameter-plugin
INFO: Note that you will be able to access the updated dashboard once the server has processed the submitted analysis report
INFO: More about the report processing at http://sq.bluersw.com:9000/api/ce/task?id=AXLWHEJaAMl7i6CpKAyw
INFO: Analysis total time: 9.871 s
INFO: ------------------------------------------------------------------------
INFO: EXECUTION SUCCESS
INFO: ------------------------------------------------------------------------
INFO: Total time: 10.649s
INFO: Final Memory: 17M/67M
INFO: ------------------------------------------------------------------------
```

访问 http://sq.bluersw.com:9000/dashboard?id=Jenkins%3Aagent-server-parameter-plugin 查看结果。

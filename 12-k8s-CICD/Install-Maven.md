# 安装 Maven

## 下载地址

[Downloading Apache Maven](http://maven.apache.org/download.cgi)

## 配置环境变量

```shell
# 解压缩后创建连接便于以后更新版本
sudo ln -s ~/apache-maven-3.6.3/ /usr/local/maven

# 配置环境变量
sudo vi /etc/profile
```

```conf
# 添加
export M2_HOME=/usr/local/maven/
export PATH=$M2_HOME/bin:$PATH
```

```shell
#重新加载
source /etc/profile
```

## 测试

```shell
mvn -v
```

## Mac系统

```shell
# 赋写权
sudo chmod a+rwx /etc/profile

# 编辑
vi  /etc/profile
source /etc/profile

# 把权限变更回来
sudo chmod a-wx /etc/profile

# 查看一下应该都是读权限
ls -l /etc/profile
```

```shell
# zsh加载优先级最高的配置文件
vi ~/.zshenv
```

```conf
# 添加如下命令
source /etc/profile
```

## 私有仓库设置

```shell
vi ~/.m2/settings.xml
```

添加如下内容：

```xml
<servers>
    <server>
      <id>releases</id>
      <username>admin</username>
      <password>**********</password>
    </server>
    <server>
      <id>snapshots</id>
      <username>admin</username>
      <password>**********</password>
    </server>
  </servers>

  <mirrors>
     <mirror>
      <id>repo.bluersw.com</id>
      <mirrorOf>*</mirrorOf>
      <name>Home Repository Mirror.</name>
      <url>http://repo.bluersw.com:8081/repository/maven-public/</url>
    </mirror>
  </mirrors>

  <profiles>
    <profile>
      <id>repo.bluersw.com</id>
      <repositories>
        <repository>
          <id>nexus</id>
          <name>Public Repositories</name>
          <url>http://repo.bluersw.com:8081/repository/maven-public/</url>
          <releases>
            <enabled>true</enabled>
          </releases>
        </repository>
        <repository>
          <id>central</id>
          <name>Central Repositories</name>
          <url>http://repo.bluersw.com:8081/repository/maven-central/</url>
          <releases>
            <enabled>true</enabled>
          </releases>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
        </repository>
        <repository>
          <id>release</id>
          <name>Release Repositories</name>
          <url>http://repo.bluersw.com:8081/repository/maven-releases/</url>
          <releases>
            <enabled>true</enabled>
          </releases>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
        </repository>
        <repository>
          <id>snapshots</id>
          <name>Snapshot Repositories</name>
          <url>http://repo.bluersw.com:8081/repository/maven-snapshots/</url>
          <releases>
            <enabled>true</enabled>
          </releases>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>plugins</id>
          <name>Plugin Repositories</name>
          <url>http://repo.bluersw.com:8081/repository/maven-public/</url>
        </pluginRepository>
      </pluginRepositories>
      </profile>
  </profiles>

  <activeProfiles>
    <activeProfile>repo.bluersw.com</activeProfile>
  </activeProfiles>

```

* 若项目版本号末尾带有 -SNAPSHOT，则会发布到snapshots快照版本仓库
* 若项目版本号末尾带有 -RELEASES 或什么都不带，则会发布到releases正式版本仓库

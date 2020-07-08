# 安装 Gradle

## 下载地址

[Downloading Gradle Build Tool](https://gradle.org/releases/)

## 配置环境变量

```shell
# 解压缩后创建连接便于以后更新版本
sudo ln -s ~/gradle-6.5/ /usr/local/gradle

# 配置环境变量
sudo vi /etc/profile
```

```conf
# 添加
GRADLE_HOME=/usr/local/gradle/
export GRADLE_HOME
export PATH=$PATH:$GRADLE_HOME/bin

```

```shell
#重新加载
source /etc/profile
```

## 测试

```shell
gradle -v
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

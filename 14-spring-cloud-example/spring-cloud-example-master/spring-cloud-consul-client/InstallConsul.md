# 安装Consul服务中心

首先下载对应版本的安装程序。[点击下载](https://www.consul.io/downloads.html)  

我下载的是macOS64位版本，下载文件是一个ZIP文件，下载后解压缩到一个你喜欢的位置，以开发模式启动consul服务：

```shell
#进入consul目录
cd ~/consul
#以开发模式启动服务，-server表示以服务模式启动
./consul agent -dev
```

![Alt text](http://static.bluersw.com/images/spring-cloud-consul-client-05.png)  
按照信息提示访问127.0.0.1:8500可以打开consul管理界面
![Alt text](http://static.bluersw.com/images/spring-cloud-consul-client-06.png)  

# kafka cluster依赖于zk,先安装zk
1. pvc.yaml是测试storageclass是否正确
2. kafka.yaml是部署文件
3. kafka-manager.yaml是kafka web gui

---
## 镜像都是在官方的镜像上制作而成,都上传到hub.docker.com上

1. **创建test topic** 
```
kafka-topics.sh --create --topic test  --zookeeper zk-0.zk-svc.scm.svc.cluster.local:2181,zk-1.zk-svc.scm.svc.cluster.local:2181,zk-2.zk-svc.scm.svc.cluster.local:2181 --partitions 3 --replication-factor 2
```
2. **查看topic**
```
kafka-topics.sh --list --zookeeper zk-0.zk-svc.scm.svc.cluster.local:2181,zk-1.zk-svc.scm.svc.cluster.local:2181,zk-2.zk-svc.scm.svc.cluster.local:2181
```
3. **删除**
```
kafka-topics.sh --delete --topic test  --zookeeper zk-0.zk-svc.scm.svc.cluster.local:2181,zk-1.zk-svc.scm.svc.cluster.local:2181,zk-2.zk-svc.scm.svc.cluster.local:2181
```

4. **发送信息**
```
kafka-console-producer.sh --broker-list localhost:31902 --topic test
1
2323
```
5. **消费**
```
kafka-console-consumer.sh --bootstrap-server localhost:31902 --topic test
```

---
### 使用kafka-manager.yaml.block这个kafka-manager启动之后，容器会一直重启，看了一下日志，没有发现有什么问题，生产中是好好的，替换了镜像也是一样的，所以替换成了yahoo-kafka-manager没有问题，一切正常
---
kafka-manager使用   
```
kubectl create -f kafka-manager.yaml -n scm
这里的 Kubernetes 集群地址为：192.168.2.11，并且在上面设置 Kafka-Manager 网络策略为 NodePort 方式，且设置端口为 30900，这里输入地址：http://192.168.2.11:30900 访问 Kafka Manager。

进入后先配置 Kafka Manager，增加一个 Zookeeper 地址。
配置三个必填参数:

Cluster Name：自定义一个名称，任意输入即可。
Zookeeper Hosts：输入 Zookeeper 地址，这里设置为 Zookeeper 服务名+端口。
Kafka Version：选择 kafka 版本。

kubectl exec -it kafka-0 -n scm -- sh
$ cd /opt
$ ls
kafka  kafka_2.11-0.10.2.0
$ ls -l
total 0
lrwxrwxrwx 1 root  root  24 Apr 17  2017 kafka -> /opt/kafka_2.11-0.10.2.0
drwxr-xr-x 1 kafka kafka 18 Jun  4 03:35 kafka_2.11-0.10.2.0
这里就可以看到是什么版本了，或者
find /opt/kafka/libs/ -name \*kafka_\* | head -1 | grep -o '\kafka[^\n]*'

进入后先配置 Kafka Manager，增加一个 Zookeeper 地址。
zk-svc:2181

这里的 Kubernetes 集群地址为：192.168.11.122，并且在上面设置 Kafka-Manager 网络策略为 NodePort 方式，且设置端口为 30581，这里输入地址：http://192.168.11.122:30581/ 访问 Kafka Manager。    
kafka-manager也可以添加认证    

Secure with basic authentication
Add the following env variables if you want to protect the web UI with basic authentication:

KAFKA_MANAGER_AUTH_ENABLED: "true"
KAFKA_MANAGER_USERNAME: username
KAFKA_MANAGER_PASSWORD: password

```

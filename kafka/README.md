# kafka cluster依赖于zk,先安装zk
1. pvc.yaml是测试storageclass是否正确
2. kafka.yaml是部署文件
3. kafka-manager.yaml是kafka web gui

---
## 镜像都是在官方的镜像上制作而成,都上传到hub.docker.com上
```
> **创建test topic**
kafka-topics.sh --create --topic test  --zookeeper zk-0.zk-svc.scm.svc.cluster.local:2181,zk-1.zk-svc.scm.svc.cluster.local:2181,zk-2.zk-svc.scm.svc.cluster.local:2181 --partitions 3 --replication-factor 2

> **查看topic**
kafka-topics.sh --list --zookeeper zk-0.zk-svc.scm.svc.cluster.local:2181,zk-1.zk-svc.scm.svc.cluster.local:2181,zk-2.zk-svc.scm.svc.cluster.local:2181

> **删除**
kafka-topics.sh --delete --topic test  --zookeeper zk-0.zk-svc.scm.svc.cluster.local:2181,zk-1.zk-svc.scm.svc.cluster.local:2181,zk-2.zk-svc.scm.svc.cluster.local:2181


> **发送信息**
kafka-console-producer.sh --broker-list localhost:319092 --topic test
1
2323

> **消费**
kafka-console-consumer.sh --bootstrap-server localhost:31902 --topic test
```


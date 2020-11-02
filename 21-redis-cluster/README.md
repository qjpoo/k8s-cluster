
## 文件说明
```
.
├── headless-service.yaml     无头服务
├── redis-access-service.yaml nodeport
├── redis.conf                配置文件
├── redis-conf.yaml           配置yaml文件
└── redis-sts.yaml            sts文件
single-redis.md               redis单实例

1. 安装
创建cm的方法:
kubectl create configmap redis-conf --from-file=redis.conf
如果有cm, 直接
kubectl create -f .

[root@master database]# kubectl get pods |grep redis
redis-app-0                              1/1     Running   0          4h16m
redis-app-1                              1/1     Running   0          4h15m
redis-app-2                              1/1     Running   0          4h14m
redis-app-3                              1/1     Running   0          4h14m
redis-app-4                              1/1     Running   0          4h13m
redis-app-5                              1/1     Running   0          4h13m

如上，可以看到这些Pods在部署时是以{0…N-1}的顺序依次创建的。注意，直到redis-app-0状态启动后达到Running状态之后，redis-app-1 才开始启动。

同时，每个Pod都会得到集群内的一个DNS域名，格式为$(podname).$(service name).$(namespace).svc.cluster.local ，也即是：

redis-app-0.redis-service.default.svc.cluster.local
redis-app-1.redis-service.default.svc.cluster.local
...以此类推...

这里我们可以验证一下
#kubectl run --rm curl --image=radial/busyboxplus:curl -it
kubectl run --rm -i --tty busybox --image=busybox:1.28 /bin/sh

$ nslookup redis-app-0.redis-service   #注意格式 $(podname).$(service name).$(namespace)

erver:    10.10.0.10
Address 1: 10.10.0.10 kube-dns.kube-system.svc.cluster.local

Name:      redis-app-0.redis-service
Address 1: 10.244.0.26 redis-app-0.redis-service.default.svc.cluster.local
/ # nslookup redis-app-1.redis-service
Server:    10.10.0.10
Address 1: 10.10.0.10 kube-dns.kube-system.svc.cluster.local

Name:      redis-app-1.redis-service
Address 1: 10.244.1.248 redis-app-1.redis-service.default.svc.cluster.local

在K8S集群内部，这些Pod就可以利用该域名互相通信。我们可以使用busybox镜像的nslookup检验这些域名(一条命令)


kubectl run -it --rm --image=busybox:1.28 --restart=Never busybox -- nslookup redis-app-0.redis-service

另外可以发现，我们之前创建的pv都被成功绑定了：

[root@master database]# kubectl get pv |grep redis
pvc-01b348a0-036f-4c0c-9d39-2142d8b19027   2Gi        RWX            Delete           Bound    default/redis-data-redis-app-5          dg-nfs-storage            4h15m
pvc-0c130587-78a6-45cb-bc78-917070b2f18e   2Gi        RWX            Delete           Bound    default/redis-data-redis-app-4          dg-nfs-storage            4h16m
pvc-0c21a3b1-135b-4167-9766-64eb28c9d2c4   2Gi        RWX            Delete           Bound    default/redis-data-redis-app-1          dg-nfs-storage            4h17m
pvc-8022f11f-fcee-45f1-9611-65781bec2851   2Gi        RWX            Delete           Bound    default/redis-data-redis-app-0          dg-nfs-storage            4h24m
pvc-91e944de-de84-48d1-b14b-a1c1c952cc46   2Gi        RWX            Delete           Bound    default/redis-data-redis-app-2          dg-nfs-storage            4h17m
pvc-bb8e94df-b005-4624-8132-0f76fceb76fa   2Gi        RWX            Delete           Bound    default/redis-data-redis-app-3          dg-nfs-storage            4h16m

2. 初始化Redis集群
创建好6个Redis Pod后，我们还需要利用常用的Redis-tribe工具进行集群的初始化

创建Ubuntu容器

由于Redis集群必须在所有节点启动后才能进行初始化，而如果将初始化逻辑写入Statefulset中，则是一件非常复杂而且低效的行为。这里，本人不得不称赞一下原项目作者的思路，值得学习。也就是说，我们可以在K8S上创建一个额外的容器，专门用于进行K8S集群内部某些服务的管理控制。 这里，我们专门启动一个Ubuntu的容器，可以在该容器中安装Redis-tribe，进而初始化Redis集群，执行：

1、#创建一个ubuntu容器
kubectl run -it ubuntu --image=ubuntu --restart=Never /bin/bash

#进入到容器
kubectl exec -it ubuntu /bin/bash

2、#我们使用阿里云的Ubuntu源，执行
$ cat > /etc/apt/sources.list << EOF
deb http://mirrors.aliyun.com/ubuntu/ bionic main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ bionic main restricted universe multiverse

deb http://mirrors.aliyun.com/ubuntu/ bionic-security main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ bionic-security main restricted universe multiverse

deb http://mirrors.aliyun.com/ubuntu/ bionic-updates main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ bionic-updates main restricted universe multiverse

deb http://mirrors.aliyun.com/ubuntu/ bionic-proposed main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ bionic-proposed main restricted universe multiverse
 
deb http://mirrors.aliyun.com/ubuntu/ bionic-backports main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ bionic-backports main restricted universe multiverse
EOF

3、#成功后，原项目要求执行如下命令安装基本的软件环境：
apt-get update
apt-get install -y vim wget python2.7 python-pip redis-tools dnsutils

4、#初始化集群
首先，我们需要安装redis-trib
pip install redis-trib==0.5.1

然后，创建只有Master节点的集群
redis-trib.py create \
  `dig +short redis-app-0.redis-service.default.svc.cluster.local`:6379 \
  `dig +short redis-app-1.redis-service.default.svc.cluster.local`:6379 \
  `dig +short redis-app-2.redis-service.default.svc.cluster.local`:6379

其次，为每个Master添加Slave
redis-trib.py replicate \
  --master-addr `dig +short redis-app-0.redis-service.default.svc.cluster.local`:6379 \
  --slave-addr `dig +short redis-app-3.redis-service.default.svc.cluster.local`:6379

redis-trib.py replicate \
  --master-addr `dig +short redis-app-1.redis-service.default.svc.cluster.local`:6379 \
  --slave-addr `dig +short redis-app-4.redis-service.default.svc.cluster.local`:6379

redis-trib.py replicate \
  --master-addr `dig +short redis-app-2.redis-service.default.svc.cluster.local`:6379 \
  --slave-addr `dig +short redis-app-5.redis-service.default.svc.cluster.local`:6379

至此，我们的Redis集群就真正创建完毕了，连到任意一个Redis Pod中检验一下：
$ kubectl exec -it redis-app-2 /bin/bash
root@redis-app-2:/data# /usr/local/bin/redis-cli -c
127.0.0.1:6379> cluster nodes
ff322ee28dbe16a45154427d7867ef2a3268d306 10.244.0.26:6379@16379 master - 0 1604304689126 0 connected 10923-16383
157f6d332949e3dfb5ecb85ba50a382fe8bf2f9c 10.244.1.248:6379@16379 master - 0 1604304690183 2 connected 5462-10922
e6e6f46b55774eb0b00a5263faddf821fa0a4ff9 10.244.2.227:6379@16379 slave 157f6d332949e3dfb5ecb85ba50a382fe8bf2f9c 0 1604304689537 2 connected
179be1b54aedf7fb5e9e7db79fcba53abbbfcedb 10.244.1.249:6379@16379 slave 5e64ec360d3eaf62d7f35a974ed3b9a1cd6cc589 0 1604304690000 1 connected
5e64ec360d3eaf62d7f35a974ed3b9a1cd6cc589 10.244.2.226:6379@16379 myself,master - 0 1604304688000 1 connected 0-5461
c725fe32dcf12db236379c71aed8b8f93c1f081d 10.244.0.27:6379@16379 slave ff322ee28dbe16a45154427d7867ef2a3268d306 0 1604304689000 0 connected

127.0.0.1:6379> cluster info
cluster_state:ok
cluster_slots_assigned:16384
cluster_slots_ok:16384
cluster_slots_pfail:0
cluster_slots_fail:0
cluster_known_nodes:6
cluster_size:3
cluster_current_epoch:5
cluster_my_epoch:1
cluster_stats_messages_ping_sent:11674
cluster_stats_messages_pong_sent:12303
cluster_stats_messages_meet_sent:1
cluster_stats_messages_sent:23978
cluster_stats_messages_ping_received:12301
cluster_stats_messages_pong_received:11675
cluster_stats_messages_meet_received:2
cluster_stats_messages_received:23978

edis挂载的数据：
$ ll /data/nfs/redis/pv3
total 12
-rw-r--r-- 1 root root  92 Jun  4 11:36 appendonly.aof
-rw-r--r-- 1 root root 175 Jun  4 11:36 dump.rdb
-rw-r--r-- 1 root root 794 Jun  4 11:49 nodes.conf


3. 创建用于访问Service

前面我们创建了用于实现StatefulSet的Headless Service，但该Service没有Cluster Ip，因此不能用于外界访问。所以，我们还需要创建一个Service，专用于为Redis集群提供访问和负载均衡：

#删除服务
kubectl delete -f redis-access-service.yaml

#编写yaml
cat >redis-access-service.yaml<<\EOF
apiVersion: v1
kind: Service
metadata:
  name: redis-access-service
  labels:
    app: redis
spec:
  type: NodePort
  ports:
  - name: redis-port
    protocol: "TCP"
    port: 6379
    targetPort: 6379
    nodePort: 30010
  selector:
    app: redis
    appCluster: redis-cluster
EOF

#如上，该Service名称为 redis-access-service，在K8S集群中暴露6379端口，并且会对labels name为app: redis或appCluster: redis-cluster的pod进行负载均衡。

#创建服务
kubectl apply -f redis-access-service.yaml

#查看svc
$ kubectl get svc redis-access-service -o wide
NAME                   TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE   SELECTOR
redis-access-service   NodePort   10.111.59.191   <none>        6379:30010/TCP   83m   app=redis,appCluster=redis-cluster

#如上，在K8S集群中，所有应用都可以通过 10.111.59.191:6379 来访问Redis集群。当然，为了方便测试，我们也可以为Service添加一个NodePort映射到物理机30010上。
#查看svc详情
$ kubectl describe svc redis-access-service
Name:                     redis-access-service
Namespace:                default
Labels:                   app=redis
Annotations:              kubectl.kubernetes.io/last-applied-configuration:
                            {"apiVersion":"v1","kind":"Service","metadata":{"annotations":{},"labels":{"app":"redis"},"name":"redis-access-service","namespace":"defau...
Selector:                 app=redis,appCluster=redis-cluster
Type:                     NodePort
IP:                       10.111.59.191
Port:                     redis-port  6379/TCP
TargetPort:               6379/TCP
NodePort:                 redis-port  30010/TCP
Endpoints:                10.244.1.230:6379,10.244.1.231:6379,10.244.1.232:6379 + 3 more...
Session Affinity:         None
External Traffic Policy:  Cluster
Events:                   <none>

4. 集群内测试（service ip 测试）

yum install redis -y
redis-cli -h 10.111.59.191 -p 6379 -c
10.111.59.191:6379> CLUSTER info
cluster_state:ok
cluster_slots_assigned:16384
cluster_slots_ok:16384
cluster_slots_pfail:0
cluster_slots_fail:0
cluster_known_nodes:5
cluster_size:3
cluster_current_epoch:3
cluster_my_epoch:3
cluster_stats_messages_ping_sent:766
cluster_stats_messages_pong_sent:790
cluster_stats_messages_meet_sent:2
cluster_stats_messages_sent:1558
cluster_stats_messages_ping_received:787
cluster_stats_messages_pong_received:768
cluster_stats_messages_meet_received:3
cluster_stats_messages_received:1558

5. 宿主机端口测试(使用集群协议测试)
redis-cli -h 10.198.1.156 -p 30010 -c
10.198.1.156:30010> cluster info
cluster_state:ok
cluster_slots_assigned:16384
cluster_slots_ok:16384
cluster_slots_pfail:0
cluster_slots_fail:0
cluster_known_nodes:5
cluster_size:3
cluster_current_epoch:3
cluster_my_epoch:2
cluster_stats_messages_ping_sent:907
cluster_stats_messages_pong_sent:901
cluster_stats_messages_meet_sent:3
cluster_stats_messages_sent:1811
cluster_stats_messages_ping_received:900
cluster_stats_messages_pong_received:910
cluster_stats_messages_meet_received:1
cluster_stats_messages_received:1811

6. 测试主从切换
在K8S上搭建完好Redis集群后，我们最关心的就是其原有的高可用机制是否正常。这里，我们可以任意挑选一个Master的Pod来测试集群的主从切换机制，如redis-app-0：

[root@master redis]# kubectl get pods redis-app-0 -o wide
NAME          READY     STATUS    RESTARTS   AGE       IP            NODE            NOMINATED NODE
redis-app-1   1/1       Running   0          3h        172.17.24.3   192.168.0.144   <none>

进入redis-app-0查看：
[root@master redis]# kubectl exec -it redis-app-0 /bin/bash
root@redis-app-0:/data# /usr/local/bin/redis-cli -c
127.0.0.1:6379> role
1) "master"
2) (integer) 13370
3) 1) 1) "172.17.63.9"
      2) "6379"
      3) "13370"
127.0.0.1:6379> 

如上可以看到，app-0为master，slave为172.17.63.9即redis-app-3。

接着，我们手动删除redis-app-0：
[root@master redis]# kubectl delete pod redis-app-0
pod "redis-app-0" deleted
[root@master redis]#  kubectl get pod redis-app-0 -o wide
NAME          READY     STATUS    RESTARTS   AGE       IP            NODE            NOMINATED NODE
redis-app-0   1/1       Running   0          4m        172.17.24.3   192.168.0.144   <none>

我们再进入redis-app-0内部查看：
[root@master redis]# kubectl exec -it redis-app-0 /bin/bash
root@redis-app-0:/data# /usr/local/bin/redis-cli -c
127.0.0.1:6379> role
1) "slave"
2) "172.17.63.9"
3) (integer) 6379
4) "connected"
5) (integer) 13958

如上，redis-app-0变成了slave，从属于它之前的从节点172.17.63.9即redis-app-3

7. 添加redis节点
更改redis的yml文件里面的replicas:字段,把这个字段改为8,然后升级运行

[root@rke redis]# kubectl apply -f redis.yml
Warning: kubectl apply should be used on resource created by either kubectl create --save-config or kubectl apply
statefulset.apps/redis-app configured

[root@rke redis]# kubectl get  pods
NAME                                     READY   STATUS    RESTARTS   AGE
redis-app-0                              1/1     Running   0          2h
redis-app-1                              1/1     Running   0          2h
redis-app-2                              1/1     Running   0          19m
redis-app-3                              1/1     Running   0          2h
redis-app-4                              1/1     Running   0          2h
redis-app-5                              1/1     Running   0          2h
redis-app-6                              1/1     Running   0          57s
redis-app-7                              1/1     Running   0          30s
添加集群节点
注意这个添加集群节点, 是用centos做基础镜像做的, 而上面是用ubuntu做的
[root@rke redis]#kubectl exec -it centos /bin/bash
[root@centos /]# redis-trib add-node \
`dig +short redis-app-6.redis-service.default.svc.cluster.local`:6379 \
`dig +short redis-app-0.redis-service.default.svc.cluster.local`:6379

[root@centos /]# redis-trib add-node \
`dig +short redis-app-7.redis-service.default.svc.cluster.local`:6379 \
`dig +short redis-app-0.redis-service.default.svc.cluster.local`:6379
add-node后面跟的是新节点的信息,后面是以前集群中的任意 一个节点

查看添加redis节点是否正常
[root@rke redis]# kubectl exec -it redis-app-0 bash
root@redis-app-0:/data# redis-cli
127.0.0.1:6379> cluster nodes
589b4f4f908a04f56d2ab9cd6fd0fd25ea14bb8f 10.42.1.15:6379@16379 slave e9f1f704ff7c8f060d6b39e23be9cd8e55cb2e46 0 1550564776000 7 connected
e9f1f704ff7c8f060d6b39e23be9cd8e55cb2e46 10.42.1.14:6379@16379 master - 0 1550564776000 7 connected 10923-16383
366abbba45d3200329a5c6305fbcec9e29b50c80 10.42.2.18:6379@16379 slave 4676f8913cdcd1e256db432531c80591ae6c5fc3 0 1550564777051 4 connected
505f3e126882c0c5115885e54f9b361bc7e74b97 10.42.0.15:6379@16379 master - 0 1550564776851 2 connected 5461-10922
cee3a27cc27635da54d94f16f6375cd4acfe6c30 10.42.0.16:6379@16379 slave 505f3e126882c0c5115885e54f9b361bc7e74b97 0 1550564775000 5 connected
e4697a7ba460ae2979692116b95fbe1f2c8be018 10.42.0.20:6379@16379 master - 0 1550564776549 0 connected
246c79682e6cc78b4c2c28d0e7166baf47ecb265 10.42.2.23:6379@16379 master - 0 1550564776548 8 connected
4676f8913cdcd1e256db432531c80591ae6c5fc3 10.42.2.17:6379@16379 myself,master - 0 1550564775000 1 connected 0-5460
重新分配哈希槽
redis-trib.rb reshard `dig +short redis-app-0.redis-service.default.svc.cluster.local`:6379
 输入要移动的哈希槽
 移动到哪个新的master节点(ID)
 all 是从所有master节点上移动
查看对应的节点信息
127.0.0.1:6379> cluster nodes
589b4f4f908a04f56d2ab9cd6fd0fd25ea14bb8f 10.42.1.15:6379@16379 slave e9f1f704ff7c8f060d6b39e23be9cd8e55cb2e46 0 1550566162000 7 connected
e9f1f704ff7c8f060d6b39e23be9cd8e55cb2e46 10.42.1.14:6379@16379 master - 0 1550566162909 7 connected 11377-16383
366abbba45d3200329a5c6305fbcec9e29b50c80 10.42.2.18:6379@16379 slave 4676f8913cdcd1e256db432531c80591ae6c5fc3 0 1550566161600 4 connected
505f3e126882c0c5115885e54f9b361bc7e74b97 10.42.0.15:6379@16379 master - 0 1550566161902 2 connected 5917-10922
cee3a27cc27635da54d94f16f6375cd4acfe6c30 10.42.0.16:6379@16379 slave 505f3e126882c0c5115885e54f9b361bc7e74b97 0 1550566162506 5 connected
246c79682e6cc78b4c2c28d0e7166baf47ecb265 10.42.2.23:6379@16379 master - 0 1550566161600 8 connected 0-453 5461-5916 10923-11376
4676f8913cdcd1e256db432531c80591ae6c5fc3 10.42.2.17:6379@16379 myself,master - 0 1550566162000 1 connected 454-5460


8. 疑问点
1. pod重启，ip变了，集群健康性如何维护

至此，大家可能会疑惑，前面讲了这么多似乎并没有体现出StatefulSet的作用，其提供的稳定标志redis-app-\*仅在初始化集群的时候用到，而后续Redis Pod的通信或配置文件中并没有使用该标志。我想说，是的，本文使用StatefulSet部署Redis确实没有体现出其优势，还不如介绍Zookeeper集群来的明显，不过没关系，学到知识就好。

那为什么没有使用稳定的标志，Redis Pod也能正常进行故障转移呢？这涉及了Redis本身的机制。因为，Redis集群中每个节点都有自己的NodeId（保存在自动生成的nodes.conf中），并且该NodeId不会随着IP的变化和变化，这其实也是一种固定的网络标志。也就是说，就算某个Redis Pod重启了，该Pod依然会加载保存的NodeId来维持自己的身份。我们可以在NFS上查看redis-app-1的nodes.conf文件

$ cat /usr/local/k8s/redis/pv1/nodes.conf 
96689f2018089173e528d3a71c4ef10af68ee462 192.168.169.209:6379@16379 slave d884c4971de9748f99b10d14678d864187a9e5d3 0 1526460952651 4 connected
237d46046d9b75a6822f02523ab894928e2300e6 192.168.169.200:6379@16379 slave c15f378a604ee5b200f06cc23e9371cbc04f4559 0 1526460952651 1 connected
c15f378a604ee5b200f06cc23e9371cbc04f4559 192.168.169.197:6379@16379 master - 0 1526460952651 1 connected 10923-16383
d884c4971de9748f99b10d14678d864187a9e5d3 192.168.169.205:6379@16379 master - 0 1526460952651 4 connected 5462-10922
c3b4ae23c80ffe31b7b34ef29dd6f8d73beaf85f 192.168.169.198:6379@16379 myself,slave c8a8f70b4c29333de6039c47b2f3453ed11fb5c2 0 1526460952565 3 connected
c8a8f70b4c29333de6039c47b2f3453ed11fb5c2 192.168.169.201:6379@16379 master - 0 1526460952651 6 connected 0-5461
vars currentEpoch 6 lastVoteEpoch 4

如上，第一列为NodeId，稳定不变；第二列为IP和端口信息，可能会改变。

这里，我们介绍NodeId的两种使用场景：

当某个Slave Pod断线重连后IP改变，但是Master发现其NodeId依旧， 就认为该Slave还是之前的Slave。

当某个Master Pod下线后，集群在其Slave中选举重新的Master。待旧Master上线后，集群发现其NodeId依旧，会让旧Master变成新Master的slave。
2. pvc绑定不上报错(storageclass.storage.k8s.io "nfs" not found报错)

$ kubectl describe pvc redis-data-redis-app-0

Warning  ProvisioningFailed  14s (x2 over 24s)  persistentvolume-controller  storageclass.storage.k8s.io "nfs" not found

原因为创建pv的时候，没有指定
storageClassName: nfs




3. 搭建redis集群时可能会出现的问题：
    如果pip install安装的redis-trib是0.6.1版本的，在执行redis-trib create命令时会报下面这个错误：
        TypeError: sequence index must be integer, not 'slice'
    解决方法：
        1.pip list | grep redis-trib
        2.pip uninstall redis-trib
        3.pip install redis-trib==0.5.1
    错误解决issues详见：
        https://github.com/TykTechnologies/tyk-kubernetes/issues/16

4. 此方法搭建redis-cluster的局限性：
    1.无法为k8s集群外的应用服务；
        这个方案只能提供给k8s集群内的应用使用，对集群外的应用根本用不了，因为一旦涉及到move命令，redis节点只会给出内部的pod ip，
        这个使得集群外的应用根本连不上，因为这个涉及到redis的源码，redis集群节点的相互通讯使用的redis进程所在的环境的ip，
        而这个ip就是pod-ip，相对的节点发送给客户端的move的ip也是pod ip;

Redis集群对客户端通信协议做了比较大的修改，为了追求性能最大化，并没有采用代理的方式而是采用客户端直连节点的方式。因此从单机切换到集群环境的应用，需要修改客户端代码。

	I am not Redis expert, but in Redis documentation you can read:
	Since cluster nodes are not able to proxy requests, clients may be redirected to other nodes using redirection errors
	This is why you are are having this issues with redis cluster behind LB and this is also the reason why it is (most probably) not going to work.
	You may probably need to use some proxy (e.g. official redis-cluster-poxy) that is running inside of k8s cluster, can reach all internal IPs of redis cluster and would handle redirects.

    2.如果想为K8s集群外提供服务, 要使用hostnetwork网络模式






```


# ElLFK( elasticsearch logstash filebeat kibana) on kubernetes

```
整体日志收信的架构如下：
 filebeat --> redis --> logstash -->elasticsearch --> kibana
启动的时候, 先把redis给启动了

文件功能说明：
.
|-- 00-namespace.yaml                 # 创建log名称空间
|-- 01-elasticsearch.yaml             # elasticsearch sts文件
|-- 02-kibana-cm.yaml                 # kibana配置文件
|-- 03-kibana.yaml                    # kibana deployment文件
|-- 04-logstash-cm.yaml               # logstash配置文件
|-- 05-logstash.yaml                  # logstash deployment文件
|-- 06-filebeat-ds.yaml               # filebeat ds文件
|-- 07-redis-deploy.yaml              # redis deployment 文件
|-- 08-elasticsearch-head.yaml        # head插件 不推荐使用
|-- 09-cerebro-cm.yaml                # cerebro 查看index工具
|-- 10-cerebro-deploy.yaml            # cerebro 查看index工具
|-- 11-curator                        # 管理index工具
|   |-- 00-config.yaml
|   `-- 01-curator.yaml
|-- elastic-certificates.p12          # elasticsearch 证书文件
`-- README.md                         # 说明文件

~/demo/system/elfk>:#k get po
NAME                        READY   STATUS    RESTARTS   AGE
cerebro-5db9b8479d-6526j    1/1     Running   0          42h
elasticsearch-0             1/1     Running   0          7d19h
elasticsearch-1             1/1     Running   0          7d18h
elasticsearch-2             1/1     Running   0          8d
filebeat-95qxj              1/1     Running   0          39h
filebeat-h45mc              1/1     Running   0          39h
filebeat-k7hqc              1/1     Running   0          39h
filebeat-s4lvf              1/1     Running   0          39h
filebeat-wzc86              1/1     Running   0          39h
filebeat-zrxc8              1/1     Running   0          39h
kibana-6d979dcf58-bxxs6     1/1     Running   0          7d19h
logstash-74799bcdc9-49jpw   1/1     Running   0          41h
logstash-74799bcdc9-hs4lr   1/1     Running   0          41h
redis-75d6d69d47-qhjsq      1/1     Running   0          24h

~/demo/system/elfk>:#k get svc
NAME            TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)             AGE
cerebro         NodePort    10.96.210.251    <none>        9000:30940/TCP      42h
elasticsearch   ClusterIP   None             <none>        9200/TCP,9300/TCP   8d
kibana          ClusterIP   10.110.119.191   <none>        5601/TCP            8d
logstash        NodePort    10.105.141.241   <none>        5040:30040/TCP      3d11h
redis           ClusterIP   10.100.169.3     <none>        6379/TCP            24h


~/demo/system/elfk>:#k get sts
NAME            READY   AGE
elasticsearch   3/3     8d

~/demo/system/elfk>:#k get ds
NAME       DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR   AGE
filebeat   6         6         6       6            6           <none>          39h

~/demo/system/elfk>:#k get cj
NAME      SCHEDULE      SUSPEND   ACTIVE   LAST SCHEDULE   AGE
curator   00 03 * * *   False     0        <none>          17h


-------------------------------------------------------->

各个文件注意点说明：
00-namespace.yaml
创建log名称空间，没有什么好说的


01-elasticsearch.yaml  elastic-certificates.p12
这个elasticsearch是带x-pack的, 这个sts注意设置 "ES_JAVA_OPTS"
这个重要的参数, 还有就是sc的名字, 设置成你自己的, 我的设置为dg-nfs-storage

生成elastic-certificates.p12有关的证书
先用yum安装一个elasticsearch
cd /usr/share/elasticsearch  # 使用yum方式安装的可执行文件路径
生成CA证书
bin/elasticsearch-certutil ca （CA证书：elastic-stack-ca.p12）
生成节点证书
bin/elasticsearch-certutil cert --ca elastic-stack-ca.p12  (节点证书：elastic-certificates.p12)
`# bin/elasticsearch-certutil cert -out /etc/elasticsearch/elastic-certificates.p12 -pass`# 生成证书到配置文件目录 (这一步暂不操作)
上面命令执行成功后，会在`/etc/elasticsearch/`文件夹下生成elastic-certificates.p12证书

生成elastic-certificates configmap文件
kubectl create configmap -n log elastic-certificates --from-file=elastic-certificates.p12=elastic-certificates.p12

还有我自己使用了nodeSelector, 自定义到节点
      nodeSelector:
        es: log

打标签:
k label nodes node2 es=log
修改标签
k label nodes node2 xx=oo --overwrite
删除
k label nodes node2 xx-
显示:
k get node --show-labels

生成空间, elasticsearch sts
k create -f 00-namespace.yaml -f 01-elasticsearch.yaml

进入到elasticsearch里面配置一下
kubectl exec -it -n log elasticsearch-0 -- bash

/usr/share/elasticsearch/bin/elasticsearch-setup-passwords interactive              #自定义密码
`
Enter password for [elastic]: mtg$5hmqhHU6ydAobkb
Reenter password for [elastic]: mtg$5hmqhHU6ydAobkb
Enter password for [apm_system]: mtg$5hmqhHU6ydAobkb
Reenter password for [apm_system]: mtg$5hmqhHU6ydAobkb
Enter password for [kibana]: mtg$5hmqhHU6ydAobkb
Reenter password for [kibana]: mtg$5hmqhHU6ydAobkb
Enter password for [logstash_system]: mtg$5hmqhHU6ydAobkb
Reenter password for [logstash_system]: mtg$5hmqhHU6ydAobkb
Enter password for [beats_system]: mtg$5hmqhHU6ydAobkb
Reenter password for [beats_system]: mtg$5hmqhHU6ydAobkb
Enter password for [remote_monitoring_user]: mtg$5hmqhHU6ydAobkb
Reenter password for [remote_monitoring_user]: mtg$5hmqhHU6ydAobkb
`


可以调试一下
kubectl port-forward elasticsearch-0  9200:9200 --namespace=log
Forwarding from 127.0.0.1:9200 -> 9200
Forwarding from [::1]:9200 -> 9200
Handling connection for 9200

`
要加上用户名和密码
curl -u 'elastic:mtg$5hmqhHU6ydAobkb' 'http://localhost:9200/_cluster/state?pretty'|more
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0{
  "cluster_name" : "elk",
  "cluster_uuid" : "72CdBWdmTo6VFUbob5iEsg",
  "version" : 1722,
  "state_uuid" : "sQLsoTmzRoG9XjjnX3mg8Q",
  "master_node" : "aDIALqnRQUK0Ak7geWR1oQ",
  "blocks" : { },
  "nodes" : {
    "aDIALqnRQUK0Ak7geWR1oQ" : {
      "name" : "elasticsearch-2",
      "ephemeral_id" : "TWU0PbbTQ_68CaOcEQ8uQg",
      "transport_address" : "172.16.2.182:9300",
      "attributes" : {
        "ml.machine_memory" : "4294967296",
        "ml.max_open_jobs" : "20",
        "xpack.installed" : "true",
        "transform.node" : "true"
      }
    },
...
...
省略

遇到的坑说明一下:
      - name: increase-fd-ulimit
        image: busybox
        command: ["sh", "-c", "ulimit -n 65536"]
        securityContext:
          privileged: true
这一个ulimit设置, 有可能不生效, 不知道为什么, 查看elasticsearch的日志, 有的节点上没有报错, 有的节点上报错了, 但是节点的ulimit是设置过了的, 如果节点不行, 就换一个节点试试吧

注意生产上sc的大小设置成500g左右吧, 堆内存建议给4G以上
`
-------------------------------------------------------->
02-kibana-cm.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: kibana-config
  namespace: log
data:
  kibana.yml: |
    server.port: 5601
    server.host: "0"
    kibana.index: ".kibana"
    elasticsearch.hosts: ["http://elasticsearch:9200"]
    elasticsearch.username: kibana_system
    elasticsearch.password: mtg$5hmqhHU6ydAobkb
    i18n.locale: "zh-CN"

注意用户名和密码设置
k create -f 02-kibana-cm.yaml
-------------------------------------------------------->
03-kibana.yaml
没有什么好讲的, k create -f 03-kibana.yaml

-------------------------------------------------------->
04-logstash-cm.yaml

logstash的input是从redis收集的, 然后grok, 在存入到elasticsearch中去的
logstash-cm包括logstash.yml配置文件, jvm.options java虚拟机的一些参数设置, grok表达式
logstash.yml里面要注意用户名和密码
jvm.options要注意-Xms和-Xmx内存设置
grok可以用grokdebug在线调试一下
我的原日志格式是:
{"log":"2022-01-07 14:20:37.429  INFO [dg-financial,,,] 1 --- [72.16.5.58:5672] c.d.m.f.r.producer.FinancialProducer     : Messages: correlationData: CorrelationData [id=1479337156080640001]\n","stream":"stdout","time":"2022-01-07T06:20:37.429968973Z"}
然后做了一个时间的修改, 根据名称空间自动的创建索引

-------------------------------------------------------->
05-logstash.yaml
创建logstash的deployment, 个数根据需要定
-------------------------------------------------------->
06-filebeat-ds.yaml
filebeat-ds会在各个节点创建ds, filebeat.inputs, 收集指定的jar服务
然后添加了一个k8s字段, 删除了原来的kubernetes字段
这里需要注意, docker的路径, 我的是/dgmall/docker/containers,默认的好像是/var/lib/docker
      - name: varlibdockercontainers
        hostPath:
          path: /dgmall/docker/containers
而且我还加了tolerations

-------------------------------------------------------->
07-redis-deploy.yaml
redis主要是配置文件, 不要用rdb,aof, 这样效果更佳
可以不使用持久存储
-------------------------------------------------------->
08-elasticsearch-head.yaml
看看就好, 不推荐使用

-------------------------------------------------------->
09-cerebro-cm.yaml
cerebro-cm的配置文件
-------------------------------------------------------->
10-cerebro-deploy.yaml
直接服用就可
-------------------------------------------------------->
11-curator
|-- 00-config.yaml
`-- 01-curator.yaml

00-config.yaml注意elasticsearch的用户名和密码, 我只保留8天的索引文件
个人根据需要设置吧, 定时任务, 每天3:00跑一次





-------------------------------------------------------->
后记:
head插件不推荐使用, 看了一下镜像, 有5年没有更新了
安装elasticsearch-head插件之后, 开启x-pack后访问head需要加上账号及密码
给head插件做一个traefik域名, head.dg.local
http://head.dg.local/?auth_user=elastic&auth_password=mtg$5hmqhHU6ydAobkb



优化:

1. logstash的优化相关配置
（1）可以优化的参数，可根据自己的硬件进行优化配置

① pipeline 线程数，官方建议是等于CPU内核数

默认配置 ---> pipeline.workers: 2

可优化为 ---> pipeline.workers: CPU内核数（或几倍cpu内核数）

 

② 实际output 时的线程数

默认配置 ---> pipeline.output.workers: 1

可优化为 ---> pipeline.output.workers: 不超过pipeline 线程数

 

③ 每次发送的事件数

默认配置 ---> pipeline.batch.size: 125

可优化为 ---> pipeline.batch.size: 1000

 

④ 发送延时

默认配置 ---> pipeline.batch.delay: 5

可优化为 ---> pipeline.batch.size: 10

 

（2）总结

　　通过设置-w参数指定pipeline worker数量，也可直接修改配置文件logstash.yml。这会提高filter和output的线程数，如果需要的话，将其设置为cpu核心数的几倍是安全的，线程在I/O上是空闲的。

　　默认每个输出在一个pipeline worker线程上活动，可以在输出output 中设置workers设置，不要将该值设置大于pipeline worker数。

　　还可以设置输出的batch_size数，例如ES输出与batch size一致。

　　filter设置multiline后，pipline worker会自动将为1，如果使用filebeat，建议在beat中就使用multiline，如果使用logstash作为shipper，建议在input 中设置multiline，不要在filter中设置multiline。

 

（3）Logstash中的JVM配置文件

　　Logstash是一个基于Java开发的程序，需要运行在JVM中，可以通过配置jvm.options来针对JVM进行设定。比如内存的最大最小、垃圾清理机制等等。JVM的内存分配不能太大不能太小，太大会拖慢操作系统。太小导致无法启动。默认如下：

-Xms256m #最小使用内存

-Xmx1g #最大使用内存

 

2. 引入Redis 的相关问题
（1）filebeat可以直接输入到logstash（indexer），但logstash没有存储功能，如果需要重启需要先停所有连入的beat，再停logstash，造成运维麻烦；另外如果logstash发生异常则会丢失数据；引入Redis作为数据缓冲池，当logstash异常停止后可以从Redis的客户端看到数据缓存在Redis中；

（2）Redis可以使用list(最长支持4,294,967,295条)或发布订阅存储模式；

（3）redis 做elk 缓冲队列的优化：

① bind 0.0.0.0 #不要监听本地端口

② requirepass ilinux.io #加密码，为了安全运行

③ 只做队列，没必要持久存储，把所有持久化功能关掉：快照（RDB文件）和追加式文件（AOF文件），性能更好

　　save "" 禁用快照

　　appendonly no 关闭RDB

④ 把内存的淘汰策略关掉，把内存空间最大

　　maxmemory 0 #maxmemory为0的时候表示我们对Redis的内存使用没有限制

 

3. elasticsearch 节点优化配置
（1）服务器硬件配置，OS 参数

（a） /etc/sysctl.conf 配置

vim /etc/sysctl.conf

① vm.swappiness = 1                     #ES 推荐将此参数设置为 1，大幅降低 swap 分区的大小，强制最大程度的使用内存，注意，这里不要设置为 0, 这会很可能会造成 OOM
② net.core.somaxconn = 65535     #定义了每个端口最大的监听队列的长度
③ vm.max_map_count= 262144    #限制一个进程可以拥有的VMA(虚拟内存区域)的数量。虚拟内存区域是一个连续的虚拟地址空间区域。当VMA 的数量超过这个值，OOM
④ fs.file-max = 518144                   #设置 Linux 内核分配的文件句柄的最大数量
[root@elasticsearch]# sysctl -p 生效一下

 

（b）limits.conf 配置

vim /etc/security/limits.conf

elasticsearch    soft    nofile          65535
elasticsearch    hard    nofile          65535
elasticsearch    soft    memlock         unlimited
elasticsearch    hard    memlock         unlimited
 

（c）为了使以上参数永久生效，还要设置两个地方

vim /etc/pam.d/common-session-noninteractive

vim /etc/pam.d/common-session

添加如下属性：

session required pam_limits.so

可能需重启后生效

 

（2）elasticsearch 中的JVM配置文件

-Xms2g

-Xmx2g

① 将最小堆大小（Xms）和最大堆大小（Xmx）设置为彼此相等。

② Elasticsearch可用的堆越多，可用于缓存的内存就越多。但请注意，太多的堆可能会使您长时间垃圾收集暂停。

③ 设置Xmx为不超过物理RAM的50％，以确保有足够的物理内存留给内核文件系统缓存。

④ 不要设置Xmx为JVM用于压缩对象指针的临界值以上；确切的截止值有所不同，但接近32 GB。不要超过32G，如果空间大，多跑几个实例，不要让一个实例太大内存

 

（3）elasticsearch 配置文件优化参数

① vim elasticsearch.yml

bootstrap.memory_lock: true  #锁住内存，不使用swap
#缓存、线程等优化如下
bootstrap.mlockall: true
transport.tcp.compress: true
indices.fielddata.cache.size: 40%
indices.cache.filter.size: 30%
indices.cache.filter.terms.size: 1024mb
threadpool:
    search:
        type: cached
        size: 100
        queue_size: 2000
 

② 设置环境变量

vim /etc/profile.d/elasticsearch.sh export ES_HEAP_SIZE=2g    #Heap Size不超过物理内存的一半，且小于32G

 

（4）集群的优化（我未使用集群）

① ES是分布式存储，当设置同样的cluster.name后会自动发现并加入集群；

② 集群会自动选举一个master，当master宕机后重新选举；

③ 为防止"脑裂"，集群中个数最好为奇数个

④ 为有效管理节点，可关闭广播 discovery.zen.ping.multicast.enabled: false，并设置单播节点组discovery.zen.ping.unicast.hosts: ["ip1", "ip2", "ip3"]

 

6、性能的检查
（1）检查输入和输出的性能

Logstash和其连接的服务运行速度一致，它可以和输入、输出的速度一样快。

 

（2）检查系统参数

① CPU

注意CPU是否过载。在Linux/Unix系统中可以使用top -H查看进程参数以及总计。

如果CPU使用过高，直接跳到检查JVM堆的章节并检查Logstash worker设置。

 

② Memory

注意Logstash是运行在Java虚拟机中的，所以它只会用到你分配给它的最大内存。

检查其他应用使用大量内存的情况，这将造成Logstash使用硬盘swap，这种情况会在应用占用内存超出物理内存范围时。

 

③ I/O 监控磁盘I/O检查磁盘饱和度

使用Logstash plugin（例如使用文件输出）磁盘会发生饱和。

当发生大量错误，Logstash生成大量错误日志时磁盘也会发生饱和。

在Linux中，可使用iostat，dstat或者其他命令监控磁盘I/O

 

④ 监控网络I/O

当使用大量网络操作的input、output时，会导致网络饱和。

在Linux中可使用dstat或iftop监控网络情况。

 

（3）检查JVM heap

　　heap设置太小会导致CPU使用率过高，这是因为JVM的垃圾回收机制导致的。

　　一个快速检查该设置的方法是将heap设置为两倍大小然后检测性能改进。不要将heap设置超过物理内存大小，保留至少1G内存给操作系统和其他进程。

　　你可以使用类似jmap命令行或VisualVM更加精确的计算JVM heap

```

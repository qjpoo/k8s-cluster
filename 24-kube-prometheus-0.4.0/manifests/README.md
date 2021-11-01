## 安装步骤及注意事项
```
注意: 我的kubernetes环境是1.16.3, 所以安装的kube-prometheus是kube-prometheus-0.4.0版本, 自己选择合适的安装版本, 不然会 有问题的.

安装之前说明:
所有的yaml文件, 我已经把grafana, prometheus, alertmanager都做了数据的持久化.
配置Prometheus、Alertmanager和Grafana的存储以及数据保留期限, 根据实际生产环境做调整

# 编辑manifests/prometheus-prometheus.yaml
vi manifests/prometheus-prometheus.yaml
在spec字段中添加如下8行
spec:
  ...
  retention: 7d                 # 保留7天
  storage:                      # 存储配置
    volumeClaimTemplate:
      spec:
        storageClassName: nfs-storage   # 假设sc名为nfs-storage
        resources:
          requests:
            storage: 40Gi

# 提交资源
kubectl apply -f manifests/prometheus-clusterRole.yaml


## Alertmanager
# 编辑manifests/alertmanager-alertmanager.yaml
vi manifests/alertmanager-alertmanager.yaml
在spec字段中添加如下8行
spec:
  ...
  retention: 168h               # 注意，这里最大的单位是h,没有d。
  storage:                      # 存储配置
    volumeClaimTemplate:
      spec:
        storageClassName: nfs-storage   # 假设sc名为nfs-storage
        resources:
          requests:
            storage: 40Gi

# 提交资源
kubectl apply -f manifests/alertmanager-alertmanager.yaml


## Grafana
# 编辑manifests/grafana-deployment.yaml
vi manifests/grafana-deployment.yaml
1.修改
      volumes:
      - emptyDir: {}
        name: grafana-storage
为
      volumes:
      - name: grafana-storage
        persistentVolumeClaim:
          claimName: grafana-pvc
2.在文本首先添加如下内容
kind: PersistentVolumeClaim
apiVersion: v1
metadata:
  name: grafana-pvc
  namespace: monitoring
spec:
  accessModes:
    - ReadWriteMany
  storageClassName: nfs-storage     # 假设sc名为nfs-storage
  resources:
    requests:
      storage: 40Gi
# 提交资源
kubectl apply -f manifests/grafana-deployment.yaml

开炮, 开炮, 开炮....

1. 安装setup目录下所有的yaml文件
   kubectl create -f .

2. 进入到bug文件下, 安装 04-grafana-pvc.yaml这个是grafana的pvc, 注意storageClassName, 替换成自己的

3. 进入到10-grafana文件夹下面, 执行如下命令, 生成grafana的配置cm
   kubectl create configmap "grafana-etc" --from-file=grafana.ini --namespace=monitoring
   grafana的pvc文件
   k create -f bug/04-grafana-pvc.yaml

4. 安装manifests所有文件
   我安装的时候, kube-state-metrics这个默认的版本是v1.9.2有问题, google一下, 说要换成v1.9.3版本, 解决. 修改kube-state-metrics-deployment.yaml文件, 58行
   58         image: quay.io/coreos/kube-state-metrics:v1.9.3
   kubectl create -f .

5. 为了省事, 直接设置grafana, prometheus, alertmanager的svc为nodePort, 可以访问, 
   1. grafana为30030
      kubectl  patch svc  grafana -n monitoring -p '{"spec":{"type":"NodePort","ports":[{"name":"http","port":3000,"protocol":"TCP","targetPort":"http","nodePort":30030}]}}'
   2. prometheus为30090
      kubectl  patch svc  prometheus-k8s -n monitoring -p '{"spec":{"type":"NodePort","ports":[{"name":"web","port":9090,"protocol":"TCP","targetPort":"web","nodePort":30090}]}}'
   3. alertmanager为30093
      kubectl  patch svc  alertmanager-main -n monitoring -p '{"spec":{"type":"NodePort","ports":[{"name":"web","port":9093,"protocol":"TCP","targetPort":"web","nodePort":30093}]}}'

   我这里用的是traefik, 安装traefik详见以前章节的的安装步骤, 在此不说废话了.
      1. grafana的ingressroute   
         root@<master|192.168.1.23|~/demo/system/traefik/prod>:#cat g.yaml
         apiVersion: traefik.containo.us/v1alpha1
         kind: IngressRoute
         metadata:
           name: grafana-route
           namespace: monitoring
         spec:
           entryPoints:
             - web
           routes:
             - match: Host(`g.dg.local`)
               kind: Rule
               services:
                 - name: grafana
                   port: 3000
   
      2. prometheus的ingressroute   
         root@<master|192.168.1.23|~/demo/system/traefik/prod>:#cat p.yaml
         apiVersion: traefik.containo.us/v1alpha1
         kind: IngressRoute
         metadata:
           name: prometheus-route
           namespace: monitoring
         spec:
           entryPoints:
             - web
           routes:
             - match: Host(`p.dg.local`)
               kind: Rule
               services:
                 - name: prometheus-k8s
                   port: 9090

      3. alertmanager的ingressroute   
         root@<master|192.168.1.23|~/demo/system/traefik/prod>:#cat a.yaml
         apiVersion: traefik.containo.us/v1alpha1
         kind: IngressRoute
         metadata:
           name: alertmanager-route
           namespace: monitoring
         spec:
           entryPoints:
             - web
           routes:
             - match: Host(`a.dg.local`)
               kind: Rule
               services:
                 - name: alertmanager-main
                   port: 9093
      4. 查看ingressroute
      root@<master|192.168.1.23|~/demo/system/traefik/prod>:#k get ingressroute
      NAME                 AGE
      alertmanager-route   80s
      grafana-route        4s
      prometheus-route     84s

6. get po, svc
   root@<master|192.168.1.23|~/demo/system/kube-prometheus-0.4.0/manifests/bug>:#k get po
   NAME                                  READY   STATUS    RESTARTS   AGE
   alertmanager-main-0                   2/2     Running   0          22m
   alertmanager-main-1                   2/2     Running   0          22m
   alertmanager-main-2                   2/2     Running   0          22m
   grafana-68b7f85c8-txfdk               1/1     Running   0          21m
   kube-state-metrics-7b4fdd4d77-ckpzr   3/3     Running   0          22m
   node-exporter-hr6k6                   2/2     Running   0          22m
   node-exporter-jlkl6                   2/2     Running   0          22m
   node-exporter-n22mw                   2/2     Running   0          22m
   node-exporter-q8jnc                   2/2     Running   0          22m
   node-exporter-t5jv9                   2/2     Running   0          22m
   node-exporter-zxfdg                   2/2     Running   0          22m
   prometheus-adapter-5b9c9b4f65-d72zc   1/1     Running   0          22m
   prometheus-k8s-0                      3/3     Running   0          10m
   prometheus-k8s-1                      3/3     Running   1          10m
   prometheus-operator-99dccdc56-xfk4s   1/1     Running   0          23m

   root@<master|192.168.1.23|~/demo/system/kube-prometheus-0.4.0/manifests/bug>:#k get svc
   NAME                    TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)                      AGE
   alertmanager-main       ClusterIP   10.106.41.223    <none>        9093/TCP                     23m
   alertmanager-operated   ClusterIP   None             <none>        9093/TCP,9094/TCP,9094/UDP   23m
   grafana                 ClusterIP   10.104.64.38     <none>        3000/TCP                     23m
   kube-state-metrics      ClusterIP   None             <none>        8443/TCP,9443/TCP            23m
   node-exporter           ClusterIP   None             <none>        9100/TCP                     23m
   prometheus-adapter      ClusterIP   10.101.195.173   <none>        443/TCP                      23m
   prometheus-k8s          ClusterIP   10.96.53.148     <none>        9090/TCP                     23m
   prometheus-operated     ClusterIP   None             <none>        9090/TCP                     13m
   prometheus-operator     ClusterIP   None             <none>        8080/TCP                     23m

7. 修复etcd
   生成secret etcd-certs  
   kubectl -n monitoring create secret generic etcd-certs --from-file=/etc/kubernetes/pki/etcd/healthcheck-client.crt  --from-file=/etc/kubernetes/pki/etcd/healthcheck-client.key  --from-file=/etc/kubernetes/pki/etcd/ca.crt 
   kubectl create -f 02-prometheus-serviceMonitorEtcd.yaml
   注意里面的IP地址, 选择正确
   kubectl create -f 03-prometheus-service-etcd.yaml

8. 修复 controller-manager
   注意里面的IP地址, 选择正确
   kubectl create -f 00-prometheus-kubeControllerManagerService.yaml

9. 修复scheduler
   注意里面的IP地址, 选择正确
   kubectl create -f 01-prometheus-kubeSchedulerService.yaml

10. 做个本地的dns解析: 
    192.168.1.23 g.dg.local
    192.168.1.23 p.dg.local
    192.168.1.23 a.dg.local
    打开http://p.dg.local网站, 打开prometheus中的target, 显示如下: 
     monitoring/alertmanager/0 (3/3 up) 
     monitoring/coredns/0 (1/1 up) 
     monitoring/grafana/0 (1/1 up) 
     monitoring/kube-apiserver/0 (1/1 up) 
     monitoring/kube-controller-manager/0 (1/1 up) 
     monitoring/kube-scheduler/0 (1/1 up) 
     monitoring/kube-state-metrics/0 (1/1 up) 
     monitoring/kube-state-metrics/1 (1/1 up) 
     monitoring/kubelet/0 (6/6 up) 
     monitoring/kubelet/1 (6/6 up) 
     monitoring/node-exporter/0 (6/6 up) 
     monitoring/prometheus-operator/0 (1/1 up) 
     monitoring/prometheus/0 (2/2 up) 
    全都是up, 代表正常

11. 重点说一下, prometheus-prometheus.yaml文件
    root@<master|192.168.1.23|~/demo/system/kube-prometheus-0.4.0/manifests>:#cat prometheus-prometheus.yaml
    apiVersion: monitoring.coreos.com/v1
    kind: Prometheus
    metadata:
      labels:
        prometheus: k8s
      name: k8s
      namespace: monitoring
    spec:
      alerting:
        alertmanagers:
        - name: alertmanager-main
          namespace: monitoring
          port: web
      baseImage: quay.io/prometheus/prometheus
      nodeSelector:
        kubernetes.io/os: linux
      podMonitorNamespaceSelector: {}
      podMonitorSelector: {}
      replicas: 2
      secrets:
      - etcd-certs           # etcd的证书secret名
      retention: 15d         # prometheus保存数据为15天
      resources:
        requests:
          memory: 400Mi      # 如果业务量很大的话, 建议做一定的优化, 修改为2g
      ruleSelector:
        matchLabels:
          prometheus: k8s
          role: alert-rules
      securityContext:
        fsGroup: 2000
        runAsNonRoot: true
        runAsUser: 1000
      #additionalScrapeConfigs:
      #  name: additional-configs
      #  key: prometheus-additional.yaml
      serviceAccountName: prometheus-k8s
      serviceMonitorNamespaceSelector: {}
      serviceMonitorSelector: {}
      version: v2.11.0
      storage:
        volumeClaimTemplate:  # prometheus的数据做持久化
          spec:
            storageClassName: dg-nfs-storage
            resources:
              requests:
                storage: 15Gi # 我观察了一下, 一天1g不到, 所以我给了个15G

    在调整一下alertmanager-alertmanager.yaml 资源
        apiVersion: monitoring.coreos.com/v1
        kind: Alertmanager
        metadata:
          labels:
            alertmanager: main
          name: main
          namespace: monitoring
        spec:
          baseImage: quay.io/prometheus/alertmanager
          nodeSelector:
            kubernetes.io/os: linux
          replicas: 3
          securityContext:
            fsGroup: 2000
            runAsNonRoot: true
            runAsUser: 1000
          serviceAccountName: alertmanager-main
          resources:
            requests:
              memory: 100Mi
            limits:
              memory: 1Gi
          version: v0.18.0
          retention: 168h               # 注意，这里最大的单位是h,没有d。
          storage:                      # 存储配置
            volumeClaimTemplate:
              spec:
                storageClassName: dg-nfs-storage   # 假设sc名为nfs-storage
                resources:
                  requests:
                    storage: 4Gi
   
   
12. 生成additional-config
    kubectl create secret generic additional-configs --from-file=prometheus-additional.yaml -n monitoring
    删除:
    k delete secret additional-configs

13. 部署black exporer, 安装bug/07-blackbox-exporter-file-sd/file-sd下面的00-blackbox-exporter-cm.yaml prometheus-additional.yaml
    查看pvc, 我用的是nfs, 找到prometheus-k8s的目录路径, 把monitor_urls.yaml  service_status.yaml这两个文件放进去
    root@<master|192.168.1.23|~/demo/system/kube-prometheus-0.4.0/manifests>:#k get pvc
    NAME                                       STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS     AGE
    alertmanager-main-db-alertmanager-main-0   Bound    pvc-f4b34841-06e9-4ab2-8353-da152df7a3cd   4Gi        RWO            dg-nfs-storage   10h
    alertmanager-main-db-alertmanager-main-1   Bound    pvc-56721775-7400-43c1-879a-a0b6a6c737d6   4Gi        RWO            dg-nfs-storage   10h
    alertmanager-main-db-alertmanager-main-2   Bound    pvc-f8255344-5e14-46fc-aacf-391f914285e9   4Gi        RWO            dg-nfs-storage   10h
    grafana-pvc                                Bound    pvc-57177364-83f1-4600-a283-db0dccce8ea7   2Gi        RWO            dg-nfs-storage   42h
    prometheus-k8s-db-prometheus-k8s-0         Bound    pvc-3f32a008-5476-4193-9738-94a834976dd9   15Gi       RWO            dg-nfs-storage   42h
    prometheus-k8s-db-prometheus-k8s-1         Bound    pvc-49aaa128-0600-4ef4-b9f1-505f68651a16   15Gi       RWO            dg-nfs-storage   42h

   当前nfs的路径, 注意在prometheus-db下面创建blackbox_exporter文件夹, 要和yaml里面的路径对应上
   [root@VM_1_15_centos blackbox_exporter]# pwd
   /dgmall/nfsroot/monitoring-prometheus-k8s-db-prometheus-k8s-0-pvc-3f32a008-5476-4193-9738-94a834976dd9/prometheus-db/blackbox_exporter
   [root@VM_1_15_centos blackbox_exporter]# ls
   monitor_urls.yaml  service_status.yaml
   注意,这里还有一个prometheus-k8s-1, 然后把prometheus-k8s-0里面的两个monitor_urls.yaml  service_status.yaml也复制到1里面去
   monitoring-prometheus-k8s-db-prometheus-k8s-0-pvc-3f32a008-5476-4193-9738-94a834976dd9/
   monitoring-prometheus-k8s-db-prometheus-k8s-1-pvc-49aaa128-0600-4ef4-b9f1-505f68651a16/

    先生成additional-configs secret, 然后在执行如下二步
    k create -f 00-blackbox-exporter-cm.yaml
    k create -f  prometheus-additional.yaml

    导入black exporter的grafana模版 9965
    
    
14. endpoints自动发现, 把这个job加入到prometheus-additional.yaml
    - job_name: 'kubernetes-endpoints'
      kubernetes_sd_configs:
      - role: endpoints
      relabel_configs:
      - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_scheme]
        action: replace
        target_label: __scheme__
        regex: (https?)
      - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_service_annotation_prometheus_io_port]
        action: replace
        target_label: __address__
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
      - action: labelmap
        regex: __meta_kubernetes_service_label_(.+)
      - source_labels: [__meta_kubernetes_namespace]
        action: replace
        target_label: kubernetes_namespace
      - source_labels: [__meta_kubernetes_service_name]
        action: replace
        target_label: kubernetes_name
      - source_labels: [__meta_kubernetes_pod_name]
        action: replace
        target_label: kubernetes_pod_name

    删除:
    k delete secret additional-configs
    添加
    kubectl create secret generic additional-configs --from-file=prometheus-additional.yaml -n monitoring

    prometheus中查看target效果:
    kubernetes-endpoints (1/1 up) 
    Endpoint	State	Labels	Last Scrape	Scrape Duration	Error
    http://172.16.5.64:9153/metrics
    UP	instance="172.16.5.64:9153" job="kubernetes-endpoints" k8s_app="kube-dns" kubernetes_io_cluster_service="true" kubernetes_io_name="CoreDNS" kubernetes_name="kube-dns" kubernetes_namespace="kube-system" kubernetes_pod_name="coredns-5c4b7b7b66-hrx52"	29.474s ago	4.738ms	
    因为只有一个coredns里面的sevice中有prometheus.io/scrape=true和prometheus.io/port, 所以才会收集
    
16. 设置告警
    # 先将之前的 secret 对象删除
    $ kubectl delete secret alertmanager-main -n monitoring
    secret "alertmanager-main" deleted
    $ kubectl create secret generic alertmanager-main --from-file=alertmanager.yaml -n monitoring
    secret "alertmanager-main" created
    进入到bug/12-warning, 执行
    kubectl create secret generic alertmanager-main --from-file=alertmanager.yaml --from-file=wechat.tmpl --from-file=email.tmpl  -n monitoring
    wechat和email同时能收到邮件

    执行里面的rules:
    k create -f bug/12-warning/rules/

    alertmanager.yaml.ok  这个文件是routes指定不同的labels来发送到不同的平台


17. 单个redis监控
    进入到bug/13-outside-redis-single-exporter
    修改里面的地址
    - addresses:
      - ip: 192.168.1.17
    k create -f outside-redis.yaml
    导入redis的grafana模版: https://grafana.com/grafana/dashboards/763

18. 监控集群redis
    进入到bug/14-outside-redis-cluster-exporter, 修改redis cluster的地址
    k create -f prometheus-additional.yaml
    grafana导入先导入上面的763模版, 然后在导入目录中的redis-cluster集群.json  模版文件即可

19. node_exporter
    先要到需要监控的目标节点上, 安装和启动 node_exporter
    进入到bug/11-outside-exporter, 修改里面要监控的节点IP
    k create -f .
    导入node_exporter的grafana模版: 8919

20. 监控elasticsearch集群
    先去下载elasticsearch-exporter, 执行如下命令, 我的是一台节点上跑了三个elasticsearch
    nohup ./elasticsearch_exporter --web.listen-address ":9700"  --es.uri http://192.168.1.15:9200 &>>/dev/null &
    nohup ./elasticsearch_exporter --web.listen-address ":9701"  --es.uri http://192.168.1.15:9201 &>>/dev/null &
    nohup ./elasticsearch_exporter --web.listen-address ":9702"  --es.uri http://192.168.1.15:9202 &>>/dev/null &
    查看有没有结果, 有返回, 说明是正确的
    curl 127.0.0.1:9700/metrics
    
    进入到bug/15-outside-elasearch-exporter目录中去, 执行如下命令
    k delete secret additional-configs
    kubectl create secret generic additional-configs --from-file=prometheus-additional.yaml -n monitoring 
    导入elasticsearch_exporter的grafana模版: 6483
    https://grafana.com/grafana/dashboards/6483
   
    如果elasticsearch有用户名和密码的话:
    /usr/local/elasticsearch_exporter/elasticsearch_exporter --web.listen-address ":9308" --es.uri=http://username:password@192.168.1.15:9200

    
----------------------------------------------------------------------------------------------
troubleshooting:
我把我遇到的问题, 尽可能回忆起来, 因为这一路坑太多了
1. 大致方向
   k logs -f prometheus-k8s-0 -c prometheus
   k logs -f prometheus-operator-99dccdc56-lw5mj
   secret和configmap, 或者Pod删除了在重建, 让配置生效
2. etcd, apiserver等没有监控, 可能是yaml文件绑定的是127.0.0.1的地址, 修改为0.0.0.0
#ll /etc/kubernetes/manifests/
total 16
-rw------- 1 root root 1784 2019-12-18 11:39 etcd.yaml
-rw------- 1 root root 2828 2019-12-23 11:30 kube-apiserver.yaml
-rw------- 1 root root 2775 2020-01-07 17:41 kube-controller-manager.yaml
-rw------- 1 root root 1148 2019-12-18 11:39 kube-scheduler.yaml
3. 多看日志吧 
```

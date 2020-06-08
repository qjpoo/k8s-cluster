

1. 打开文件metrics-server-deployment.yaml，新增一些内容，如下图为新增的内容：
```
command:
        - /metrics-server
        - --metric-resolution=30s
        - --kubelet-insecure-tls
        - --kubelet-preferred-address-types=InternalIP,Hostname,InternalDNS,ExternalDNS,ExternalIP
```
2. 还是在目录metrics-server-0.3.6/deploy/1.8+/，执行命令kubectl apply -f ./
3.验证功能
```
1.节点CPU,MEMORY情况   
[root@master 1.8+]# kubectl top node
NAME      CPU(cores)   CPU%   MEMORY(bytes)   MEMORY%
master    3272m        54%    5138Mi          66%
slave01   3032m        50%    3571Mi          46%
slave02   2978m        49%    2923Mi          37%
   
   
2.执行命令kubectl top pod -n kube-system查看kube-system这个namespace下所有pod的基本情况： 
[root@master 1.8+]# kubectl top pod -n kube-system
NAME                             CPU(cores)   MEMORY(bytes)
coredns-58cc8c89f4-mbdww         15m          11Mi
coredns-58cc8c89f4-xhdx9         18m          12Mi
etcd-master                      69m          315Mi
kube-apiserver-master            151m         272Mi
kube-controller-manager-master   70m          39Mi
kube-flannel-ds-amd64-9jx7b      12m          15Mi
kube-flannel-ds-amd64-h46h6      12m          13Mi
kube-flannel-ds-amd64-n9rbk      12m          12Mi
kube-proxy-px6dt                 8m           13Mi
kube-proxy-z7fb6                 3m           16Mi
kube-proxy-zcmn2                 21m          14Mi
kube-scheduler-master            7m           15Mi
metrics-server-65c88cd8-tdmdk    3m           13Mi

3.再来试试metrics-server的API服务，执行命令kubectl proxy --port=8080，用来开代理端口；

4.再开打一个同样的ssh连接，执行命令curl localhost:8080/apis/metrics.k8s.io/v1beta1/
```

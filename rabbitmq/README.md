##rabbitmq的密码是 guest / guest

---
**集群安装好了之后需要把rabbitmq-1,rabbitmq-2加入到集群之中**
```
kubectl exec -it  rabbitmq-1 -n scm -- sh
rabbitmqctl stop_app
rabbitmqctl join_cluster rabbit@rabbitmq-0.rabbitmq.scm.svc.cluster.local
rabbitmqctl start_app
rabbitmqctl cluster_status

kubectl exec -it  rabbitmq-2 -n scm -- sh
rabbitmqctl stop_app
rabbitmqctl join_cluster rabbit@rabbitmq-0.rabbitmq.scm.svc.cluster.local
rabbitmqctl start_app
rabbitmqctl cluster_status

[root@master system]# kubectl get pods -n scm
NAME         READY   STATUS    RESTARTS   AGE
kafka-0      1/1     Running   0          26h
kafka-1      1/1     Running   0          26h
kafka-2      1/1     Running   0          26h
rabbitmq-0   1/1     Running   0          19h
rabbitmq-1   1/1     Running   1          19h
rabbitmq-2   1/1     Running   0          19h
zk-0         1/1     Running   0          43h
zk-1         1/1     Running   0          43h
zk-2         1/1     Running   0          43h
You have new mail in /var/spool/mail/root
[root@master system]# kubectl get svc -n scm
NAME          TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)                          AGE
kafka-svc     ClusterIP   None           <none>        31902/TCP                        26h
rabbitmq      ClusterIP   None           <none>        5672/TCP                         19h
rabbitmq-lb   NodePort    10.10.78.119   <none>        15672:31672/TCP,5672:30672/TCP   19h
zk-svc        ClusterIP   None           <none>        2888/TCP,3888/TCP                43h
```

# install
```
1. download redis-cluster-operator
git  clone  https://github.com/ucloud/redis-cluster-operator.git

2. create namespace redis-cluster
kubectl create ns redis-cluster

3. create crd
kubectl apply -f deploy/crds/

4. create operator
kubectl create -f deploy/service_account.yaml
kubectl create -f deploy/cluster/cluster_role.yaml
kubectl create -f deploy/cluster/cluster_role_binding.yaml

kubectl create -f deploy/cluster/operator.yaml
[root@master redis-cluster-proxy]# k get deploy -n redis-cluster
NAME                     READY   UP-TO-DATE   AVAILABLE   AGE
redis-cluster-operator   1/1     1            1           2d22h

// cluster-scoped 
kubectl create -f deploy/service_account.yaml
kubectl create -f deploy/cluster/cluster_role.yaml
kubectl create -f deploy/cluster/cluster_role_binding.yaml
kubectl create -f deploy/cluster/operator.yaml

4. create redis-cluster
kubectl apply -f redis-cluster.yaml
[root@master redis-cluster-proxy]# k get po -n redis-cluster
NAME                                      READY   STATUS    RESTARTS   AGE
drc-example-distributedrediscluster-0-0   1/1     Running   0          4h55m
drc-example-distributedrediscluster-0-1   1/1     Running   0          4h54m
drc-example-distributedrediscluster-1-0   1/1     Running   0          4h55m
drc-example-distributedrediscluster-1-1   1/1     Running   0          4h54m
drc-example-distributedrediscluster-2-0   1/1     Running   0          4h55m
drc-example-distributedrediscluster-2-1   1/1     Running   0          4h54m
redis-cluster-operator-68b46c564f-b54mr   1/1     Running   0          2d22h


5. check cluster
[root@master redis-cluster-proxy]# kubectl exec -it  drc-example-distributedrediscluster-0-0 -n redis-cluster -- sh
/data # redis-cli -c -h redis-svc
redis-svc:6379> role
1) "master"
2) (integer) 24794
3) 1) 1) "10.244.2.213"
      2) "6379"
      3) "24794"
redis-svc:6379> cluster info
cluster_state:ok
cluster_slots_assigned:16384
cluster_slots_ok:16384
cluster_slots_pfail:0
cluster_slots_fail:0
cluster_known_nodes:6
cluster_size:3
cluster_current_epoch:5
cluster_my_epoch:0
cluster_stats_messages_ping_sent:17736
cluster_stats_messages_pong_sent:17073
cluster_stats_messages_meet_sent:1
cluster_stats_messages_sent:34810
cluster_stats_messages_ping_received:17069
cluster_stats_messages_pong_received:17737
cluster_stats_messages_meet_received:4
cluster_stats_messages_received:34810

6. delete redis-cluster
kubectl delete -f redis-cluster.yaml
kubectl delete -f operator.yaml 
kubectl delete -f cluster_role_binding.yaml 
kubectl delete -f cluster_role.yaml 
kubectl delete-f service_account.yaml 
kubectl delete -f deploy/crds/

delete pvc && pv
[root@master deploy]# k get pvc -n redis-cluster |grep redis
redis-data-drc-example-distributedrediscluster-0-0   Bound    pvc-10c88211-5d7d-4edc-85cd-94bde324f1ff   1Gi        RWO            dg-nfs-storage 5h
redis-data-drc-example-distributedrediscluster-0-1   Bound    pvc-e0147f74-ef2a-442c-b35d-41c0dbaff3ff   1Gi        RWO            dg-nfs-storage 4h59m
redis-data-drc-example-distributedrediscluster-1-0   Bound    pvc-50eae565-9f69-4b60-b620-418be0f036e3   1Gi        RWO            dg-nfs-storage 5h
redis-data-drc-example-distributedrediscluster-1-1   Bound    pvc-5cd3a5a3-377c-4b76-8a28-c174b011b7a3   1Gi        RWO            dg-nfs-storage 4h59m
redis-data-drc-example-distributedrediscluster-2-0   Bound    pvc-1341baf6-dd3f-4d5a-8afe-a80865cb4f9d   1Gi        RWO            dg-nfs-storage 5h
redis-data-drc-example-distributedrediscluster-2-1   Bound    pvc-7ee2f87e-eb71-4dce-8ce6-37a15f5248cd   1Gi        RWO            dg-nfs-storage 4h59m
k get pvc -n redis-cluster |grep -Po '^r[^ ]+'|xargs k -n redis-cluster delete pvc


[root@master deploy]# k get pv |grep redis-cluster
pvc-10c88211-5d7d-4edc-85cd-94bde324f1ff   1Gi        RWO            Delete           Bound    redis-cluster/redis-data-drc-example-distributedrediscluster-0-0   dg-nfs-storage            5h3m
pvc-1341baf6-dd3f-4d5a-8afe-a80865cb4f9d   1Gi        RWO            Delete           Bound    redis-cluster/redis-data-drc-example-distributedrediscluster-2-0   dg-nfs-storage            5h3m
pvc-50eae565-9f69-4b60-b620-418be0f036e3   1Gi        RWO            Delete           Bound    redis-cluster/redis-data-drc-example-distributedrediscluster-1-0   dg-nfs-storage            5h3m
pvc-5cd3a5a3-377c-4b76-8a28-c174b011b7a3   1Gi        RWO            Delete           Bound    redis-cluster/redis-data-drc-example-distributedrediscluster-1-1   dg-nfs-storage            5h2m
pvc-7ee2f87e-eb71-4dce-8ce6-37a15f5248cd   1Gi        RWO            Delete           Bound    redis-cluster/redis-data-drc-example-distributedrediscluster-2-1   dg-nfs-storage            5h3m
pvc-e0147f74-ef2a-442c-b35d-41c0dbaff3ff   1Gi        RWO            Delete           Bound    redis-cluster/redis-data-drc-example-distributedrediscluster-0-1   dg-nfs-storage            5h3m

k get pv|awk '/redis-cluster/{print $1}'|xargs k delete pv

```

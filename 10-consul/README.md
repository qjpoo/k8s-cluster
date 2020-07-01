# consul cluster install
1. 使用到了statefulset
2. 使用到traefik来访问consul ui
3. 使用到了storageclass
4. 使用的是默认的空间
```
kubectl create -f .

[root@master traefik-route-file]# kubectl get pods
NAME                                     READY   STATUS    RESTARTS   AGE
consul-0                                 1/1     Running   0          5m2s
consul-1                                 1/1     Running   0          4m49s
consul-2                                 1/1     Running   0          4m27s

```

1. myself-rabbitmq 这个是自己写的
2. official-rabbitmq 这个是官方的
3. rabitmq-v2  这个是改进版，直接运行就可以了, 三个rabbitmq共用一个pvc
```
当kubectl delete -f rabbitmq.yaml时候会把数据也删除
保持（Retain）:删除PV后后端存储上的数据仍然存在，如需彻底删除则需要手动删除后端存储volume
删除（Delete）：删除被PVC释放的PV和后端存储volume
回收（Recycle）：保留PV，但清空PV上的数据（已废弃）
修改PVC reclaim policy为 Retain
kubectl patch pv pvc-89ecc53b-725c-11ea-a8e2-005056aa2a6d -p '{"spec":{"persistentVolumeReclaimPolicy":"Retain"}}'
```
4. rabbitmq-v3 修改了一下申请的Pv

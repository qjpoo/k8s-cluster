## nginx目录文件说明
1. default.conf  默认的配置文件
2. nginx-config-pvc.yaml config pvc
3. nginx-deployment.yaml  deployment文件
3. nginx-html-pvc.yaml  html文档的pvc
4. nginx-log-pvc.yaml 日志pvc

## php
---
# 注意 nginx的目录和php的目录要是一样

```
先执行php的yaml，在执行nginx
kubectl create -f .
```

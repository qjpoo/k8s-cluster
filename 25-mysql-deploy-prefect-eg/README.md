## 简单说明
```
文件作用说明

[root@master mysql-deploy-prefect ]# tree
.
├── cm.yaml               # mysql的配置信息
├── cronjob.yaml          # 定时备份mysql的cronjob
├── deploy.yaml           # mysql的deploy文件
├── pv-backup.yaml        # 定时备份mysql文件的存储pv
├── pvc-backup.yaml       # 定时备份mysql文件的pvc
├── pvc.yaml              # 部署mysql所要持久化的数据pvc
├── pv.yaml               # 部署mysql所要持久化的数据pv
├── secret.yaml           # mysql涉及到的密码信息
├── service.yaml          # mysql的服务
└── storageclass.yaml     # 存储类, 用的是本地存储

deploy.yaml
1. 设置了容器时区
2. 加密了敏感数据
3. 容器健康检查
4. 容器初始化
5. 做了cronjob
6. 数据做了持久化, 其实可以用nfs来做


Local Persistent Volume 的设计
# 在master上执行
$ mkdir /mnt/disks
$ for vol in vol1 vol2 vol3; do
    mkdir /mnt/disks/$vol
    mount -t tmpfs $vol /mnt/disks/$vol
done


mysql的mysql-password和mysql-root-password密码都是admin

kubectl create -f .


查看运行情况:
[root@master mysql-deploy-prefect ]# k get po
NAME                                     READY   STATUS      RESTARTS   AGE
curl-69c656fd45-cfq9r                    1/1     Running     10         270d
mysql-backup-1637031540-frwcl            0/1     Completed   0          2m54s
mysql-backup-1637031600-s22ns            0/1     Completed   0          114s
mysql-backup-1637031660-k5fd4            0/1     Completed   0          54s
mysql-min-74cbfc9d8c-rh2vb               1/1     Running     0          20h
nfs-client-provisioner-b4d57dbdf-thz6b   1/1     Running     59         270d

[root@master mysql-deploy-prefect ]# k exec -it mysql-min-74cbfc9d8c-rh2vb -- sh
# mysql -uroot -padmin
mysql: [Warning] Using a password on the command line interface can be insecure.
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 70
Server version: 5.7.32 MySQL Community Server (GPL)

Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.

Oracle is a registered trademark of Oracle Corporation and/or its
affiliates. Other names may be trademarks of their respective
owners.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

mysql> show databases;
+--------------------+
| Database           |
+--------------------+
| information_schema |
| jian               |
| mysql              |
| performance_schema |
| sys                |
+--------------------+
5 rows in set (0.00 sec)

可以先创建一个jian的数据库, 插入一些数据, 做测试
create databases jian;
use jian;
create table t1(id, int);
insert into table t1 values(1), (2), (3);

日志备份:
[root@master mysql-deploy-prefect ]# k logs -f mysql-backup-1637031660-k5fd4
mysql-min admin
+ echo mysql-min admin
+ date +%Y%m%d
+ mysqldump --host=mysql-min --user=root --password=admin --routines --databases jian --single-transaction
mysqldump: [Warning] Using a password on the command line interface can be insecure.
+ /usr/bin/find /mysql-min-backup/mysql-20211115.sql /mysql-min-backup/mysql-jian-20211115.sql /mysql-min-backup/mysql-jian-20211116.sql -mtime +10 -exec rm {} ;

备份的文件
[root@master ~ ]# ls /mnt/disks/vol2
mysql-20211115.sql       mysql-jian-20211116.sql
mysql-jian-20211115.sql

```

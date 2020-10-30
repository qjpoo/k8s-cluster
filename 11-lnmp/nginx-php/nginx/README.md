# 各个文件作用说明
1. default.conf nginx的默认配置文件
2. nginx-config-pvc.yaml  nginx配置文件的pvc
3. nginx-deployment.yaml    nginx deploy
4. nginx-html-pvc.yaml  nginx放静态文件的pvc
5. nginx-log-pvc.yaml  nginx的日志pvc

# 注意nginx的html放的源码的路径要和Php的里面的路径要对应上，不然会找不到文件

```
server {
listen       80;
server_name  localhost;
location / {
    root   /var/www/html/apply/web;       #   这里的目录要和下面PHP的目录对应上
    index  index.php index.html index.htm ;
    if (!-e $request_filename) {
        rewrite  ^(.*)$  /index.php?s=/$1  last;
        break;
    }
}

location ~ \.php$ {
    root           /var/www/html/apply/web;
```
# 怕了忘记,做几点注意事项说明和补充
1. 由于k8s新增加了节点, 但是忘记了安装nfs客户端, 导致存储卷挂载不成功 
```
安装安户端: yum install nfs-utils rpcbind
启动: systemctl enable rpcbind.service   systemctl start rpcbind.service
```
2. nginx对应的存储卷里面没有放配置文件, 找到相应的pvc, 然后把配置文件放到NFS服务器对应的目录下面就可以了
```
root@<master|192.168.1.23|~>:#kubectl get pvc -n dg
NAME                          STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
datadir-kafka-0               Bound    pvc-7b35b863-c06f-4972-8e36-aa8ae133d0ab   10Gi       RWO            dg-nfs-storage   311d
datadir-kafka-1               Bound    pvc-71e3c627-5c42-41d6-a9e5-53d28a18b791   10Gi       RWO            dg-nfs-storage   311d
datadir-kafka-2               Bound    pvc-7a90d94c-2d78-4be4-bab9-a589e9532d07   10Gi       RWO            dg-nfs-storage   311d
jenkins-home-jenkins-0        Bound    pvc-e370dbbe-bf9a-424f-a9dd-e7bf3297ed70   10Gi       RWO            dg-nfs-storage   294d
nginx-bzj-system-config-pvc   Bound    pvc-8ecdfcfa-20a0-469f-981d-66551d80499a   1Gi        RWX            dg-nfs-storage   16h

找到对应NFS服务器存放nginx配置文件的目录, 如最后一行,即是:
[root@VM_1_15_centos nfsroot]# ls
default-jenkins-home-jenkins-0-pvc-4750e63e-adb1-46c9-b20c-c410e329c226
dg-datadir-kafka-0-pvc-7b35b863-c06f-4972-8e36-aa8ae133d0ab
dg-datadir-kafka-1-pvc-71e3c627-5c42-41d6-a9e5-53d28a18b791
dg-datadir-kafka-2-pvc-7a90d94c-2d78-4be4-bab9-a589e9532d07
dg-data-rabbitmq-0-pvc-e92347ab-1045-4ec5-91e2-4ce670380d2b
dg-data-rabbitmq-1-pvc-35257e16-5aae-4f28-927e-856b90646fb2
dg-data-rabbitmq-2-pvc-d38daaf2-99f6-41cf-a2ba-34184234ac9c
dg-jenkins-home-jenkins-0-pvc-e370dbbe-bf9a-424f-a9dd-e7bf3297ed70
dg-nginx-bzj-system-config-pvc-pvc-8ecdfcfa-20a0-469f-981d-66551d80499a

default.conf跟提供的有些许出入, 跟据实际的情况来修改.
[root@VM_1_15_centos dg-nginx-bzj-system-config-pvc-pvc-8ecdfcfa-20a0-469f-981d-66551d80499a]# cat default.conf
server {
listen       80;
server_name  localhost;
location / {
    root   /var/www/html/apply/web;
    index  index.php index.html index.htm ;
    if (!-e $request_filename) {
        #rewrite  ^(.*)$  /index.php?s=/$1  last;
	try_files $uri $uri/ /index.php$is_args$args;
    }
}

error_page   500 502 503 504  /50x.html;
location = /50x.html {
    root   html;
}

error_page   404  /40x.html;
location = /40x.html {
    root   html;
}

location ~ \.php$ {
    root           /var/www/html/apply/web;
    fastcgi_pass   php-fpm-bzj-system:9000;
    fastcgi_index  index.php;
    fastcgi_param  SCRIPT_FILENAME  $document_root$fastcgi_script_name;
    fastcgi_param  PHP_APP_STATUS   'dev';
    include        fastcgi_params;
}
}

```

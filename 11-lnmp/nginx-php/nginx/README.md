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

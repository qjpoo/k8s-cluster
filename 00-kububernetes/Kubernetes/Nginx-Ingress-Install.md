# 在K8S集群中使用NGINX Ingress V1.7

NGINX Ingress控制器有两个项目一个K8S官网维护（[官方项目github地址](https://github.com/kubernetes/ingress-nginx)），另一个是NGINX, Inc.维护的项目（[NGINX项目github地址](https://github.com/nginxinc/kubernetes-ingress)），因为差异很多两个项目不要弄混，以下示例是用NGINX维护的项目（[部署说明资料](https://github.com/nginxinc/kubernetes-ingress/tree/master/deployments)），我们采用Manifests文件部署方式[Installation with Manifests](https://docs.nginx.com/nginx-ingress-controller/installation/installation-with-manifests/)。

K8S的环境已经部署完成，详情请看[安装Kubernetes V1.18.2](Install-182.md)

## 希望可以达到的效果示意图

![Alt text](http://static.bluersw.com/images/Kubernetes/Nginx-Ingress-Install-01.png)  
如果增加Node节点需要手工修改宿主机的Nginx设置，URL路由使用NGINX Ingress控制，路由信息使用Ingress类型编写，K8S集群自己管理Ingress、Service和Pod之间的负载均衡。

## 安装NGINX, Inc.的NGINX Ingress控制器

```shell
mkdir ~/nginx-ingress
cd ~/nginx-ingress

#创建Ingress控制器的命名空间和服务账号
wget https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/release-1.7/deployments/common/ns-and-sa.yaml
kubectl apply -f ns-and-sa.yaml

#创建给服务账号绑定集群角色
wget https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/release-1.7/deployments/rbac/rbac.yaml
kubectl apply -f rbac.yaml

#TLS证书（仅用于测试）
wget https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/release-1.7/deployments/common/default-server-secret.yaml
#改上述文件的命名空间和名字可以用于其他命名空间，但也只是用于测试
kubectl apply -f default-server-secret.yaml

#MapConfig
wget https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/release-1.7/deployments/common/nginx-config.yaml
kubectl apply -f nginx-config.yaml

#VirtualServer and VirtualServerRoute and TransportServer资源
wget https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/release-1.7/deployments/common/ts-definition.yaml
kubectl apply -f ts-definition.yaml
wget https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/release-1.7/deployments/common/vs-definition.yaml
kubectl apply -f vs-definition.yaml
wget https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/release-1.7/deployments/common/vsr-definition.yaml
kubectl apply -f vsr-definition.yaml

#GlobalConfiguration
wget https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/release-1.7/deployments/common/gc-definition.yaml
kubectl apply -f gc-definition.yaml
wget https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/release-1.7/deployments/common/global-configuration.yaml
kubectl apply -f global-configuration.yaml

#使用DaemonSet方式部署到集群
wget https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/release-1.7/deployments/daemon-set/nginx-ingress.yaml
kubectl apply -f nginx-ingress.yaml

#创建服务(NodePort)
wget https://raw.githubusercontent.com/nginxinc/kubernetes-ingress/release-1.7/deployments/service/nodeport.yaml
kubectl apply -f nodeport.yaml

#查看结果
kubectl get pods --namespace=nginx-ingress
```

## 创建示例Pod、Service、Secret、ingress

[示例说明详情](https://github.com/nginxinc/kubernetes-ingress/tree/master/examples/complete-example)，命名空间我设置为nginx-ingress，虽然NGINX Ingress控制器是全命名空间监控的，但路由（Ingress）、服务（Service）、凭证（Secret）都要在一个命名空间下比较安全。

```shell
vi cafe-ingress.yaml
```

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: coffee
  namespace: nginx-ingress
spec:
  replicas: 2
  selector:
    matchLabels:
      app: coffee
  template:
    metadata:
      labels:
        app: coffee
    spec:
      containers:
      - name: coffee
        image: nginxdemos/nginx-hello:plain-text
        ports:
        - containerPort: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: coffee-svc
  namespace: nginx-ingress
spec:
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
    name: http
  selector:
    app: coffee
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tea
  namespace: nginx-ingress
spec:
  replicas: 2
  selector:
    matchLabels:
      app: tea
  template:
    metadata:
      labels:
        app: tea 
    spec:
      containers:
      - name: tea 
        image: nginxdemos/nginx-hello:plain-text
        ports:
        - containerPort: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: tea-svc
  namespace: nginx-ingress
  labels:
spec:
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
    name: http
  selector:
    app: tea
---
apiVersion: v1
kind: Secret
metadata:
  name: cafe-secret
  namespace: nginx-ingress
type: Opaque
data:
  tls.crt: LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURMakNDQWhZQ0NRREFPRjl0THNhWFdqQU5CZ2txaGtpRzl3MEJBUXNGQURCYU1Rc3dDUVlEVlFRR0V3SlYKVXpFTE1Ba0dBMVVFQ0F3Q1EwRXhJVEFmQmdOVkJBb01HRWx1ZEdWeWJtVjBJRmRwWkdkcGRITWdVSFI1SUV4MApaREViTUJrR0ExVUVBd3dTWTJGbVpTNWxlR0Z0Y0d4bExtTnZiU0FnTUI0WERURTRNRGt4TWpFMk1UVXpOVm9YCkRUSXpNRGt4TVRFMk1UVXpOVm93V0RFTE1Ba0dBMVVFQmhNQ1ZWTXhDekFKQmdOVkJBZ01Ba05CTVNFd0h3WUQKVlFRS0RCaEpiblJsY201bGRDQlhhV1JuYVhSeklGQjBlU0JNZEdReEdUQVhCZ05WQkFNTUVHTmhabVV1WlhoaApiWEJzWlM1amIyMHdnZ0VpTUEwR0NTcUdTSWIzRFFFQkFRVUFBNElCRHdBd2dnRUtBb0lCQVFDcDZLbjdzeTgxCnAwanVKL2N5ayt2Q0FtbHNmanRGTTJtdVpOSzBLdGVjcUcyZmpXUWI1NXhRMVlGQTJYT1N3SEFZdlNkd0kyaloKcnVXOHFYWENMMnJiNENaQ0Z4d3BWRUNyY3hkam0zdGVWaVJYVnNZSW1tSkhQUFN5UWdwaW9iczl4N0RsTGM2SQpCQTBaalVPeWwwUHFHOVNKZXhNVjczV0lJYTVyRFZTRjJyNGtTa2JBajREY2o3TFhlRmxWWEgySTVYd1hDcHRDCm42N0pDZzQyZitrOHdnemNSVnA4WFprWldaVmp3cTlSVUtEWG1GQjJZeU4xWEVXZFowZXdSdUtZVUpsc202OTIKc2tPcktRajB2a29QbjQxRUUvK1RhVkVwcUxUUm9VWTNyemc3RGtkemZkQml6Rk8yZHNQTkZ4MkNXMGpYa05MdgpLbzI1Q1pyT2hYQUhBZ01CQUFFd0RRWUpLb1pJaHZjTkFRRUxCUUFEZ2dFQkFLSEZDY3lPalp2b0hzd1VCTWRMClJkSEliMzgzcFdGeW5acS9MdVVvdnNWQTU4QjBDZzdCRWZ5NXZXVlZycTVSSWt2NGxaODFOMjl4MjFkMUpINnIKalNuUXgrRFhDTy9USkVWNWxTQ1VwSUd6RVVZYVVQZ1J5anNNL05VZENKOHVIVmhaSitTNkZBK0NuT0Q5cm4yaQpaQmVQQ0k1ckh3RVh3bm5sOHl3aWozdnZRNXpISXV5QmdsV3IvUXl1aTlmalBwd1dVdlVtNG52NVNNRzl6Q1Y3ClBwdXd2dWF0cWpPMTIwOEJqZkUvY1pISWc4SHc5bXZXOXg5QytJUU1JTURFN2IvZzZPY0s3TEdUTHdsRnh2QTgKN1dqRWVxdW5heUlwaE1oS1JYVmYxTjM0OWVOOThFejM4Zk9USFRQYmRKakZBL1BjQytHeW1lK2lHdDVPUWRGaAp5UkU9Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K
  tls.key: LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpNSUlFb3dJQkFBS0NBUUVBcWVpcCs3TXZOYWRJN2lmM01wUHJ3Z0pwYkg0N1JUTnBybVRTdENyWG5LaHRuNDFrCkcrZWNVTldCUU5semtzQndHTDBuY0NObzJhN2x2S2wxd2k5cTIrQW1RaGNjS1ZSQXEzTVhZNXQ3WGxZa1YxYkcKQ0pwaVJ6ejBza0lLWXFHN1BjZXc1UzNPaUFRTkdZMURzcGRENmh2VWlYc1RGZTkxaUNHdWF3MVVoZHErSkVwRwp3SStBM0kreTEzaFpWVng5aU9WOEZ3cWJRcCt1eVFvT05uL3BQTUlNM0VWYWZGMlpHVm1WWThLdlVWQ2cxNWhRCmRtTWpkVnhGbldkSHNFYmltRkNaYkp1dmRySkRxeWtJOUw1S0Q1K05SQlAvazJsUkthaTAwYUZHTjY4NE93NUgKYzMzUVlzeFR0bmJEelJjZGdsdEkxNURTN3lxTnVRbWF6b1Z3QndJREFRQUJBb0lCQVFDUFNkU1luUXRTUHlxbApGZlZGcFRPc29PWVJoZjhzSStpYkZ4SU91UmF1V2VoaEp4ZG01Uk9ScEF6bUNMeUw1VmhqdEptZTIyM2dMcncyCk45OUVqVUtiL1ZPbVp1RHNCYzZvQ0Y2UU5SNThkejhjbk9SVGV3Y290c0pSMXBuMWhobG5SNUhxSkpCSmFzazEKWkVuVVFmY1hackw5NGxvOUpIM0UrVXFqbzFGRnM4eHhFOHdvUEJxalpzVjdwUlVaZ0MzTGh4bndMU0V4eUZvNApjeGI5U09HNU9tQUpvelN0Rm9RMkdKT2VzOHJKNXFmZHZ5dGdnOXhiTGFRTC94MGtwUTYyQm9GTUJEZHFPZVBXCktmUDV6WjYvMDcvdnBqNDh5QTFRMzJQem9idWJzQkxkM0tjbjMyamZtMUU3cHJ0V2wrSmVPRmlPem5CUUZKYk4KNHFQVlJ6NWhBb0dCQU50V3l4aE5DU0x1NFArWGdLeWNrbGpKNkY1NjY4Zk5qNUN6Z0ZScUowOXpuMFRsc05ybwpGVExaY3hEcW5SM0hQWU00MkpFUmgySi9xREZaeW5SUW8zY2czb2VpdlVkQlZHWTgrRkkxVzBxZHViL0w5K3l1CmVkT1pUUTVYbUdHcDZyNmpleHltY0ppbS9Pc0IzWm5ZT3BPcmxEN1NQbUJ2ek5MazRNRjZneGJYQW9HQkFNWk8KMHA2SGJCbWNQMHRqRlhmY0tFNzdJbUxtMHNBRzR1SG9VeDBlUGovMnFyblRuT0JCTkU0TXZnRHVUSnp5K2NhVQprOFJxbWRIQ2JIelRlNmZ6WXEvOWl0OHNaNzdLVk4xcWtiSWN1YytSVHhBOW5OaDFUanNSbmU3NFowajFGQ0xrCmhIY3FIMHJpN1BZU0tIVEU4RnZGQ3haWWRidUI4NENtWmlodnhicFJBb0dBSWJqcWFNWVBUWXVrbENkYTVTNzkKWVNGSjFKelplMUtqYS8vdER3MXpGY2dWQ0thMzFqQXdjaXowZi9sU1JxM0hTMUdHR21lemhQVlRpcUxmZVpxYwpSMGlLYmhnYk9jVlZrSkozSzB5QXlLd1BUdW14S0haNnpJbVpTMGMwYW0rUlk5WUdxNVQ3WXJ6cHpjZnZwaU9VCmZmZTNSeUZUN2NmQ21mb09oREN0enVrQ2dZQjMwb0xDMVJMRk9ycW40M3ZDUzUxemM1em9ZNDR1QnpzcHd3WU4KVHd2UC9FeFdNZjNWSnJEakJDSCtULzZzeXNlUGJKRUltbHpNK0l3eXRGcEFOZmlJWEV0LzQ4WGY2ME54OGdXTQp1SHl4Wlp4L05LdER3MFY4dlgxUE9ucTJBNWVpS2ErOGpSQVJZS0pMWU5kZkR1d29seHZHNmJaaGtQaS80RXRUCjNZMThzUUtCZ0h0S2JrKzdsTkpWZXN3WEU1Y1VHNkVEVXNEZS8yVWE3ZlhwN0ZjanFCRW9hcDFMU3crNlRYcDAKWmdybUtFOEFSek00NytFSkhVdmlpcS9udXBFMTVnMGtKVzNzeWhwVTl6WkxPN2x0QjBLSWtPOVpSY21Vam84UQpjcExsSE1BcWJMSjhXWUdKQ2toaVd4eWFsNmhZVHlXWTRjVmtDMHh0VGwvaFVFOUllTktvCi0tLS0tRU5EIFJTQSBQUklWQVRFIEtFWS0tLS0tCg==
---
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: cafe-ingress
  namespace: nginx-ingress
spec:
  tls:
  - hosts:
    - cafe.example.com
    secretName: cafe-secret
  rules:
  - host: cafe.example.com
    http:
      paths:
      - path: /tea
        backend:
          serviceName: tea-svc
          servicePort: 80
      - path: /coffee
        backend:
          serviceName: coffee-svc
          servicePort: 80
```

PS：host节点必须有！

```sehll
kubectl apply -f cafe-ingress.yaml
```

## 在集群内部检查部署效果

```shell
kubectl get pod,svc,secret,ingress -n nginx-ingress -o wide
```

![Alt text](http://static.bluersw.com/images/Kubernetes/Nginx-Ingress-Install-02.png)

```shell
#查看路由信息详情
kubectl describe ingress cafe-ingress  -n nginx-ingress
```

![Alt text](http://static.bluersw.com/images/Kubernetes/Nginx-Ingress-Install-04.png)

在Ingress控制器内部会创建一个Nginx配置文件，这个例子里文件名是nginx-ingress-cafe-ingress.conf，可以使用下列命令查看：

```shell
kubectl exec  nginx-ingress-dbxhj -n nginx-ingress -- cat /etc/nginx/conf.d/nginx-ingress-cafe-ingress.conf
```

该配置内容通过nginx-ingress的配置模版自动创建，默认模块可参考[默认模版](https://github.com/nginxinc/kubernetes-ingress/blob/master/internal/configs/version1/nginx.tmpl)。找到nginx-ingress控制器pod的Node节点IP和暴露的端口（我的环境是192.168.122.5），执行下列命令测试效果（nginx-ingress控制器的部署文件里已经声明了hostPort端口，所以可以直接使用NodeIP和Node端口）：

```shell
curl --resolve cafe.example.com:443:192.168.122.5 https://cafe.example.com:443/tea --insecure
```

![Alt text](http://static.bluersw.com/images/Kubernetes/Nginx-Ingress-Install-03.png)
两次请求已经由cafe-ingress进行了路由（/tea），然后tea-svc服务进行了负载均衡，所以两次是访问不同的Pod（都是tea的Pod）。

## 将示例中的HTTPS改为HTTP（只是为了省事）

```shell
vi cafe-ingress.yaml
```

```yaml
#去掉tls节点
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: cafe-ingress
  namespace: nginx-ingress
spec:
  rules:
  - host: cafe.example.com
    http:
      paths:
      - path: /tea
        backend:
          serviceName: tea-svc
          servicePort: 80
      - path: /coffee
        backend:
          serviceName: coffee-svc
          servicePort: 80
```

```shell
kubectl apply -f cafe-ingress.yaml

#测试一下
curl --resolve cafe.example.com:80:192.168.122.5 http://cafe.example.com/tea
```

## 修改宿主机Nginx配置

[安装 Nginx](https://github.com/sunweisheng/kvm/blob/master/Install-Nginx.md)

```shell
vi /etc/nginx/conf.d/proxy_svrs
```

```conf
upstream proxy_svrs
{
  server 192.168.122.4:80;
  server 192.168.122.5:80;
}
```

```shell
vi /etc/nginx/conf.d/k8s.conf
```

```conf
server{
    listen 80 default_server;
    server_name _;
    location / {
        include ./conf.d/head.conf;
        proxy_pass http://proxy_svrs;
    }
}
```

```shell
vi /etc/nginx/nginx.conf
```

```conf
include /etc/nginx/conf.d/proxy_svrs; #添加
include /etc/nginx/conf.d/*.conf;
```

```shell
nginx -s reload
```

## 测试外部访问

在本机hosts文件中添加：

```conf
192.168.0.5 cafe.example.com
```

```shell
#执行
curl http://cafe.example.com/tea
```

![Alt text](http://static.bluersw.com/images/Kubernetes/Nginx-Ingress-Install-05.png)

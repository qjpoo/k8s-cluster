# Kubernetes V1.18.2部署Dashboard V2.0

## 部署Kubernetes V1.18.2

[安装Kubernetes V1.18.2](Install-182.md)

## 下载并修改Dashboard安装脚本（在Master上执行）

参照[官网安装说明](https://github.com/kubernetes/dashboard)在master上执行：

```shell
cd ~
mkdir Dashboard
cd Dashboard
wget https://raw.githubusercontent.com/kubernetes/dashboard/v2.0.0/aio/deploy/recommended.yaml
```

修改recommended.yaml文件内容(vi recommended.yaml)：

```yaml
---
#增加直接访问端口
kind: Service
apiVersion: v1
metadata:
  labels:
    k8s-app: kubernetes-dashboard
  name: kubernetes-dashboard
  namespace: kubernetes-dashboard
spec:
  type: NodePort #增加
  ports:
    - port: 443
      targetPort: 8443
      nodePort: 30008 #增加
  selector:
    k8s-app: kubernetes-dashboard

---
#因为自动生成的证书很多浏览器无法使用，所以我们自己创建，注释掉kubernetes-dashboard-certs对象声明
#apiVersion: v1
#kind: Secret
#metadata:
#  labels:
#    k8s-app: kubernetes-dashboard
#  name: kubernetes-dashboard-certs
#  namespace: kubernetes-dashboard
#type: Opaque

---
```

## 创建证书

```shell
mkdir dashboard-certs

cd dashboard-certs/

#创建命名空间
kubectl create namespace kubernetes-dashboard

# 创建key文件
openssl genrsa -out dashboard.key 2048

#证书请求
openssl req -days 36000 -new -out dashboard.csr -key dashboard.key -subj '/CN=dashboard-cert'

#自签证书
openssl x509 -req -in dashboard.csr -signkey dashboard.key -out dashboard.crt

#创建kubernetes-dashboard-certs对象
kubectl create secret generic kubernetes-dashboard-certs --from-file=dashboard.key --from-file=dashboard.crt -n kubernetes-dashboard
```

## 安装Dashboard

```shell
#安装
kubectl create -f  ~/Dashboard/recommended.yaml

#检查结果
kubectl get pods -A  -o wide
kubectl get service -n kubernetes-dashboard  -o wide
```

## 创建dashboard管理员

```shell
#创建账号
vi dashboard-admin.yaml
```

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  labels:
    k8s-app: kubernetes-dashboard
  name: dashboard-admin
  namespace: kubernetes-dashboard
```

```shell
#保存退出后执行
kubectl create -f dashboard-admin.yaml

#为用户分配权限
vi dashboard-admin-bind-cluster-role.yaml
```

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: dashboard-admin-bind-cluster-role
  labels:
    k8s-app: kubernetes-dashboard
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-admin
subjects:
- kind: ServiceAccount
  name: dashboard-admin
  namespace: kubernetes-dashboard
```

```shell
#保存退出后执行
kubectl create -f dashboard-admin-bind-cluster-role.yaml

#查看并复制用户Token
kubectl -n kubernetes-dashboard describe secret $(kubectl -n kubernetes-dashboard get secret | grep dashboard-admin | awk '{print $1}')
```

因为宿主机没有开通30008端口并且没有转发给Node节点，所以我们先开记录登录Token，并开通端口设置转发规则:

```shell
#在宿主机上开通30008端口
firewall-cmd --add-port=30008/tcp --permanent
firewall-cmd --reload

#用SSH实现转发
ssh -CNg -L 30008:192.168.122.4:30008 root@127.0.0.1
```

访问：[https://192.168.0.5:30008](https://192.168.0.5:30008)，谷歌浏览器不行，但其他浏览器可以，比如Safari，选择Token登录，输入刚才复制的密钥，注意密钥没有前面的空格：
![Alt text](http://static.bluersw.com/images/Kubernetes/Kubernetes-Dashboard-09.png)  
因为没有安装metrics-server所以Pods的CPU、内存情况是看不到的。

## 安装metrics-server

Ps：heapster已经被metrics-server取代

在所有Node节点上下载镜像文件：

```shell
docker pull bluersw/metrics-server-amd64:v0.3.6
docker tag bluersw/metrics-server-amd64:v0.3.6 k8s.gcr.io/metrics-server-amd64:v0.3.6  
```

根据[官方说明](https://github.com/kubernetes-sigs/metrics-server)在Master上执行安装：

```shell
cd ~

mkdir metrics-server

cd metrics-server

wget https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.3.6/components.yaml
```

修改安装脚本：

```shell
vi components.yaml
```

Deployment脚本修改如下：

```yaml
template:
    metadata:
      name: metrics-server
      labels:
        k8s-app: metrics-server
    spec:
      serviceAccountName: metrics-server
      volumes:
      # mount in tmp so we can safely use from-scratch images and/or read-only containers
      - name: tmp-dir
        emptyDir: {}
      containers:
      - name: metrics-server
        image: k8s.gcr.io/metrics-server-amd64:v0.3.6
        imagePullPolicy: IfNotPresent
        args:
          - --cert-dir=/tmp
          - --secure-port=4443
          - --kubelet-preferred-address-types=InternalIP #添加
          - --kubelet-insecure-tls #添加
        ports:
        - name: main-port
          containerPort: 4443
```

```shell
#修改 Kubernetes apiserver 启动参数
vi /etc/kubernetes/manifests/kube-apiserver.yaml
#在kube-apiserver项中添加如下配置选项 修改后apiserver会自动重启
--enable-aggregator-routing=true

#安装
kubectl create -f components.yaml

#1-2分钟后查看结果
kubectl top nodes
```

再回到dashboard界面可以看到CPU和内存使用情况了：  
![Alt text](http://static.bluersw.com/images/Kubernetes/Kubernetes-Dashboard-11.png)  

# 安装Kubernetes V1.16.2

## 准备硬件环境

利用VirtualBox准备两台Linux虚拟机（K8S集群2台起步），系统用CentOS（我用的是的CentOS-7-x86_64-DVD-1810），虚拟机配置是2颗CPU和2G内存（K8S最低要求的配置），网络使用桥接网卡方式并使用静态IP：

* 192.168.0.4 K8S集群-Master（CentOS-1）
* 192.168.0.7 K8S集群-Node1（CentOS-2）

将虚拟机系统安装好，配置好网络设置。
![Alt text](http://static.bluersw.com/images/Kubernetes/Kubernetes-Install-01.png)  

## 安装Kubernetes(两台都要操作)

### 安装前准备(两台都要操作)

在两台虚拟机上都进行以下操作：  
关闭防火墙：

```shell
systemctl disable firewalld
systemctl stop firewalld
```

修改服务器名称：

```shell
//将192.168.0.4的服务器名称修改为master
hostnamectl set-hostname master  

//将192.168.0.7的服务器名称修改为node1
hostnamectl set-hostname node1  
```

进行时间校时(用aliyun的NTP服务器)：

```shell
yum install -y ntp
ntpdate ntp1.aliyun.com
```

### 安装软件(两台都要安装)

安装常用软件:

```shell
yum update

yum install wget

yum install -y yum-utils \
  device-mapper-persistent-data \
  lvm2
```

安装Docker：

```shell
cd /etc/yum.repos.d/

yum-config-manager \
    --add-repo \
    https://download.docker.com/linux/centos/docker-ce.repo

yum -y install docker-ce
```

安装kubelet kubeadm kubectl (需要VPN)：

```shell
cat <<EOF > /etc/yum.repos.d/kubernetes.repo
[kubernetes]
name=Kubernetes
baseurl=https://packages.cloud.google.com/yum/repos/kubernetes-el7-x86_64
enabled=1
gpgcheck=1
repo_gpgcheck=1
gpgkey=https://packages.cloud.google.com/yum/doc/yum-key.gpg https://packages.cloud.google.com/yum/doc/rpm-package-key.gpg
exclude=kube*
EOF

yum install -y kubelet kubeadm kubectl --disableexcludes=kubernetes
```

### 安装后设置(两台都要安装)

关闭SELINUX：

```shell
setenforce 0
sed -i 's/^SELINUX=enforcing$/SELINUX=permissive/' /etc/selinux/config
```

设置iptables：

```shell
cd /etc/sysctl.d/

cat <<EOF >  /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
EOF

sysctl --system
```

启动Docker：

```shell
systemctl start docker
#开机启动Docker
systemctl enable docker
```

关闭SWAP:

```shell
vi /etc/fstab
注释swap分区
# /dev/mapper/centos-swap swap                    swap    defaults        0 0

#保存退出vi后执行
swapoff -a
```

启动kubelet：

```shell
#开机启动kubelet
systemctl enable kubelet
```

## 下载Master节点需要的镜像（在Master上执行）

因为k8s.gcr.io访问不了，手动下载docker镜像，Master需要下载的镜像如下：  

* k8s.gcr.io/kube-apiserver:v1.16.2
* k8s.gcr.io/kube-controller-manager:v1.16.2
* k8s.gcr.io/kube-scheduler:v1.16.2
* k8s.gcr.io/kube-proxy:v1.16.2
* k8s.gcr.io/pause:3.1
* k8s.gcr.io/etcd:3.3.15-0
* k8s.gcr.io/coredns:1.6.2
* quay.io/coreos/flannel:v0.11.0-amd64

```shell
docker pull bluersw/kube-apiserver:v1.16.2 #替代docker pull k8s.gcr.io/kube-apiserver:v1.16.2
docker tag bluersw/kube-apiserver:v1.16.2 k8s.gcr.io/kube-apiserver:v1.16.2

docker pull bluersw/kube-controller-manager:v1.16.2 #替代docker pull k8s.gcr.io/kube-controller-manager:v1.16.2
docker tag bluersw/kube-controller-manager:v1.16.2 k8s.gcr.io/kube-controller-manager:v1.16.2

docker pull bluersw/kube-scheduler:v1.16.2 #替代docker pull k8s.gcr.io/kube-scheduler:v1.16.2
docker tag bluersw/kube-scheduler:v1.16.2 k8s.gcr.io/kube-scheduler:v1.16.2

docker pull bluersw/kube-proxy:v1.16.2 #替代docker pull k8s.gcr.io/kube-proxy:v1.16.2
docker tag bluersw/kube-proxy:v1.16.2 k8s.gcr.io/kube-proxy:v1.16.2

docker pull bluersw/pause:3.1 #替代docker pull k8s.gcr.io/pause:3.1
docker tag bluersw/pause:3.1 k8s.gcr.io/pause:3.1

docker pull bluersw/etcd:3.3.15-0 #替代docker pull k8s.gcr.io/etcd:3.3.15-0
docker tag bluersw/etcd:3.3.15-0 k8s.gcr.io/etcd:3.3.15-0

docker pull bluersw/coredns:1.6.2 #替代docker pull k8s.gcr.io/coredns:1.6.2
docker tag bluersw/coredns:1.6.2 k8s.gcr.io/coredns:1.6.2

docker pull bluersw/flannel:v0.11.0-amd64 #替代 docker pull quay.io/coreos/flannel:v0.11.0-amd64
docker tag bluersw/flannel:v0.11.0-amd64 quay.io/coreos/flannel:v0.11.0-amd64
```

## Master节点初始化（在Master上执行）

执行kubeadm init初始化命令：

```shell
kubeadm init  --kubernetes-version=v1.16.2 --apiserver-advertise-address=192.168.0.4 --pod-network-cidr=10.244.0.0/16 --service-cidr=10.1.0.0/16
```

* --kubernetes-version=v1.16.2 ： 加上该参数后启动相关镜像（刚才下载的那一堆）
* --pod-network-cidr=10.244.0.0/16 ：（Pod 中间网络通讯我们用flannel，flannel要求是10.244.0.0/16，这个IP段就是Pod的IP段）
* --service-cidr=10.1.0.0/16 ： Service（服务）网段（和微服务架构有关）

在初始化结果输出里找到类似下面这段信息：

```shell
kubeadm join 192.168.0.4:6443 --token 4tylf5.av0mhvxmg7gorwfz \
    --discovery-token-ca-cert-hash sha256:e67d5f759dd248a81b2e79cd8f9250b44c41d4102ef433d0f0e26268b90a10e8
```

后面Node1节点加入集群会用到。

初始化成功后执行：

```shell
#把密钥配置加载到自己的环境变量里
export KUBECONFIG=/etc/kubernetes/admin.conf

#每次启动自动加载$HOME/.kube/config下的密钥配置文件（K8S自动行为）
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

```

## 下载Node1节点需要的镜像（在Node1上执行）

因为k8s.gcr.io访问不了，手动下载docker镜像，Node1需要下载的镜像如下：  

* k8s.gcr.io/pause:3.1
* k8s.gcr.io/kube-proxy:v1.16.2
* quay.io/coreos/flannel:v0.11.0-amd64

```shell
docker pull bluersw/kube-proxy:v1.16.2 #替代docker pull k8s.gcr.io/kube-proxy:v1.16.2
docker tag bluersw/kube-proxy:v1.16.2 k8s.gcr.io/kube-proxy:v1.16.2

docker pull bluersw/pause:3.1 #替代docker pull k8s.gcr.io/pause:3.1
docker tag bluersw/pause:3.1 k8s.gcr.io/pause:3.1

docker pull bluersw/flannel:v0.11.0-amd64 #替代 docker pull quay.io/coreos/flannel:v0.11.0-amd64
docker tag bluersw/flannel:v0.11.0-amd64 quay.io/coreos/flannel:v0.11.0-amd64
```

## Node1服务器加入集群网络（在Node1上执行）

加入集群网络：

``` shell
kubeadm join 192.168.0.4:6443 --token 4tylf5.av0mhvxmg7gorwfz \
    --discovery-token-ca-cert-hash sha256:e67d5f759dd248a81b2e79cd8f9250b44c41d4102ef433d0f0e26268b90a10e8
```

## 在Master上安装flannel（在Master上执行）

参照[官网](https://github.com/coreos/flannel)执行：

```shell
kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
```

## 检查

完成后观察Master上运行的pod,执行kubectl get -A pods -o wide：  
![Alt text](http://static.bluersw.com/images/Kubernetes/Kubernetes-Install-02-2.png)  
执行kubectl get nodes查看节点：  
![Alt text](http://static.bluersw.com/images/Kubernetes/Kubernetes-Install-04.png)  
查看各个服务器上的镜像文件：  
![Alt text](http://static.bluersw.com/images/Kubernetes/Kubernetes-Install-05.png)  

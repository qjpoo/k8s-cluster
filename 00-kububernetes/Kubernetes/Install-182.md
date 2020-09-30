# 安装Kubernetes V1.18.2

## 准备虚拟机环境

[用KVM组建NAT网络虚拟机集群](https://github.com/sunweisheng/kvm/blob/master/Create-NAT-Cluster.md)

(如果你使用VirtualBox搭建虚拟机集群，请确保hostname -i可以返回一个路由可达的IP，否则请修改虚拟机的hosts文件)

## 设置系统

```shell
#关闭防火墙（所有节点）
systemctl disable firewalld
systemctl stop firewalld

#关闭SELINUX（所有节点）
setenforce 0
sed -i 's/^SELINUX=enforcing$/SELINUX=permissive/' /etc/selinux/config

#设置iptables（所有节点）
cd /etc/sysctl.d/

cat <<EOF >  /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
EOF

sysctl --system

#关闭swap分区（所有节点）
vi /etc/fstab

# /dev/mapper/centos-swap swap                    swap    defaults        0 0

swapoff -a
```

## 安装Docker

```shell
#在所有节点上安装
cd /etc/yum.repos.d/

yum-config-manager --add-repo http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo

yum -y install docker-ce

#开机启动Docker
systemctl start docker
systemctl enable docker
```

## 安装kubernetes

```shell
#在所有节点上安装
cat <<EOF > /etc/yum.repos.d/kubernetes.repo
[kubernetes]
name=Kubernetes
baseurl=http://mirrors.aliyun.com/kubernetes/yum/repos/kubernetes-el7-x86_64
enabled=1
gpgcheck=0
repo_gpgcheck=0
gpgkey=http://mirrors.aliyun.com/kubernetes/yum/doc/yum-key.gpg
       http://mirrors.aliyun.com/kubernetes/yum/doc/rpm-package-key.gpg
EOF

yum install -y kubelet kubeadm kubectl --disableexcludes=kubernetes

#开机启动kubelet
systemctl enable kubelet
```

## 初始化K8S的Master节点

```shell
#初始化（要增加registry.aliyuncs.com/google_containers才能下载google的镜像文件，初始化时要下载镜像文件要耐心的等）
kubeadm init  --kubernetes-version=v1.18.2 --image-repository=registry.aliyuncs.com/google_containers  --apiserver-advertise-address=192.168.122.3 --pod-network-cidr=10.244.0.0/16 --service-cidr=10.1.0.0/16

#按照成功安装后的提示执行
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

* apiserver-advertise-address：是Master节点上的管理API地址。
* pod-network-cidr：是pod的网络IP段，用于k8s的pod通信。
* service-cidr：是服务的虚IP网络段，用于使用服务资源。

## Node节点加入K8S集群

```shell
#从Master安装成功提示中找到下面命令，在所有Node节点上执行
kubeadm join 192.168.122.3:6443 --token t0ie01.1ya3iwrt0uofx45p \
    --discovery-token-ca-cert-hash sha256:14e36c483bb8180e740e953c8141d4e747a19e325a09007c675ba8fc64a95e27
```

## 在Master节点上安装flannel网络

```shell
kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
```

## 查看安装结果

```shell
#查看所有pod
kubectl get pod -A

#查看所有NODE
kubectl get node -A
```

![Alt text](http://static.bluersw.com/images/Kubernetes/Kubernetes-Install-182-01.png)  
![Alt text](http://static.bluersw.com/images/Kubernetes/Kubernetes-Install-182-02.png)

## 此版本可能会遇到的问题

首先安装一个检查coredns的pod：

```sehll
vi ndsutils.yaml
```

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: dnsutils
spec:
  containers:
  - name: dnsutils
    image: mydlqclub/dnsutils:1.3
    imagePullPolicy: IfNotPresent
    command: ["sleep","3600"]
```

```shell
kubectl create -f ndsutils.yaml -n kube-system
```

进入容器进行测试：

```shell
kubectl exec -it dnsutils /bin/sh -n kube-system

#检查外网DNS解析和外网的网络连通性
ping www.baidu.com

#检查内网DNS解析和内网的网络连通性
ping kubernetes.default

#检查DNS解析情况
nslookup kubernetes.default

#检查虚IP的连通性
ping 10.1.0.1
```

## 解决CLUSTER-IP不能ping，连接不通的问题

我发现的问题是CLUSTER-IP不能ping，此问题用修改kube-proxy为 ipvs模式的方案解决。

```shell
kubectl edit cm kube-proxy -n kube-system
```

将mode修改为ipvs:

```yaml
    ipvs:
      excludeCIDRs: null
      minSyncPeriod: 0s
      scheduler: ""
      strictARP: false
      syncPeriod: 0s
      tcpFinTimeout: 0s
      tcpTimeout: 0s
      udpTimeout: 0s
    kind: KubeProxyConfiguration
    metricsBindAddress: ""
    mode: "ipvs" #修改
    nodePortAddresses: null
    oomScoreAdj: null
    portRange: ""
    showHiddenMetricsForVersion: ""
```

添加ip_vs相关模块:

```shell
#添加ip_vs模块
cat > /etc/sysconfig/modules/ipvs.modules <<EOF
 #!/bin/bash 
 modprobe -- ip_vs 
 modprobe -- ip_vs_rr 
 modprobe -- ip_vs_wrr 
 modprobe -- ip_vs_sh 
 modprobe -- nf_conntrack_ipv4 
EOF

chmod 755 /etc/sysconfig/modules/ipvs.modules && bash /etc/sysconfig/modules/ipvs.modules && lsmod | grep -e ip_vs -e nf_conntrack_ipv4

#重启kube-proxy 的pod
kubectl get pod -n kube-system | grep kube-proxy |awk '{system("kubectl delete pod "$1" -n kube-system")}'
```

CLUSTER-IP的问题解决但IPVS引发了新的问题。

## 解决使用IPVS导致CoreDNS无法解析域名的问题

重启服务器，重新测试后发现，内外网都域名无法解析，原因是这个版本的K8S使用IPVS模块比较新，但CentOS系统内核的IPVS模块比较老导致，采用更新内核的办法解决(所有节点服务器)：

```shell
#载入公钥
rpm --import https://www.elrepo.org/RPM-GPG-KEY-elrepo.org

#安装 ELRepo 最新版本
yum install -y https://www.elrepo.org/elrepo-release-7.el7.elrepo.noarch.rpm

#列出可以使用的 kernel 包版本
yum list available --disablerepo=* --enablerepo=elrepo-kernel

#安装指定的 kernel 版本：
yum install -y kernel-lt-4.4.223-1.el7.elrepo --enablerepo=elrepo-kernel

#查看系统可用内核
cat /boot/grub2/grub.cfg | grep menuentry

menuentry 'CentOS Linux (4.4.223-1.el7.elrepo.x86_64) 7 (Core)'
menuentry 'CentOS Linux (3.10.0-1127.el7.x86_64) 7 (Core)'

#设置开机从新内核启动
grub2-set-default "CentOS Linux (4.4.223-1.el7.elrepo.x86_64) 7 (Core)"

#查看内核启动项
grub2-editenv list

#重启
reboot

#启动完成查看内核版本是否更新
uname -r
```

重新进行测试，都没有问题了（终于完成了）。

参考资料：

* [在集群的POD内不能访问clusterIP和service](https://blog.51cto.com/13641616/2442005)
* [coredns无法正常域名解析问题分析](https://blog.csdn.net/networken/article/details/105604173)

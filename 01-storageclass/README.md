



## 主要是修改名称空间和NFS服务器的地址和路径就可以了
```
高版本(v1.20)的kubernetes好像是弃用nfs了, 要使用在apiserver的yaml文件添加参数:

修改/etc/kubernetes/manifests/kube-apiserver.yaml 文件
添加添加- --feature-gates=RemoveSelfLink=false

然后重新生成一下pod
mv /etc/kubernetes/manifests/kube-apiserver.yaml /tmp/kube-apiserver.yaml
mv /tmp/kube-apiserver.yaml /etc/kubernetes/manifests/kube-apiserver.yaml
```
---
## 另附nfs服务器的安装步骤     
```    
----------------------NFS------------------------------------
> * 安装nfs
在NFS服务端和所有k8s的master和worker都要安装
yum install nfs-utils rpcbind
server端：
vim /etc/exports增加：
/data/nfsroot/k8sstorage 192.168.11.0/24(rw,async,no_root_squash)
先为rpcbind和nfs做开机启动：(必须先启动rpcbind服务)
systemctl enable rpcbind.service
systemctl enable nfs-server.service
然后分别启动rpcbind和nfs服务：
systemctl start rpcbind.service
systemctl start nfs-server.service
确认NFS服务器启动成功：
rpcinfo -p
检查 NFS 服务器是否挂载我们想共享的目录 /data/nfsroot/k8sstorage：
exportfs -r
#使配置生效
exportfs
#可以查看到已经ok
/data/nfsroot/k8sstorage
		192.168.11.0/24

> * clinet
yum install nfs-utils rpcbind
首先是安裝nfs，同上，然后启动rpcbind服务
先为rpcbind做开机启动：
systemctl enable rpcbind.service
然后启动rpcbind服务：
systemctl start rpcbind.service
注意：客户端不需要启动nfs服务
检查 NFS 服务器端是否有目录共享：
showmount -e nfs服务器的IP
showmount -e 192.168.11.125
在从机上使用 mount 挂载服务器端的目录/home/nfs到客户端某个目录下：
cd /home && mkdir /nfs
mount -t nfs 192.168.11.125:/data/nfsroot/k8sstorage /home/nfs
df -h 查看是否挂载成功。    
```   

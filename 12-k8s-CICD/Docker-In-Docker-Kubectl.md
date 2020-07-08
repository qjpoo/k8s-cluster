# Jenkins在Pod中实现Docker in Docker并用kubectl进行部署

## 准备工作

* [安装Jenkins](Install-Jenkins.md)
* [Jenkins的kubernetes-plugin使用方法](Jenkins-Kubernetes.md)

## 说明

Jenkins的kubernetes-plugin在执行构建时会在kubernetes集群中自动创建一个Pod，并在Pod内部创建一个名为jnlp的容器，该容器会连接Jenkins并运行Agent程序，形成一个Jenkins的Master和Slave架构，然后Slave会执行构建脚本进行构建，但如果构建内容是要创建Docker Image就要实现Docker In Docker方案（在Docker里运行Docker），如果要在集群集群内部进行部署操作可以使用kubectl执行命令，要解决kubectl的安装和权限分配问题。

因为默认的jnlp容器可以执行的命令比较少，所以要实现Docker In Docker和执行kubectl命令，就要自定义构建Docker Image，因为一个Pod内部可以运行多个容器，所以可以用自定义的Docker容器实现上述目的。

## 实现Docker In Docker

构建自定义镜像：

```Dockerfile
FROM scratch
ADD centos-7-x86_64-docker.tar.xz /

LABEL \
    org.label-schema.schema-version="1.0" \
    org.label-schema.name="CentOS Base Image" \
    org.label-schema.vendor="CentOS" \
    org.label-schema.license="GPLv2" \
    org.label-schema.build-date="20200504" \
    org.opencontainers.image.title="CentOS Base Image" \
    org.opencontainers.image.vendor="CentOS" \
    org.opencontainers.image.licenses="GPL-2.0-only" \
    org.opencontainers.image.created="2020-05-04 00:00:00+01:00"

USER root

COPY docker-ce.repo /etc/yum.repos.d/docker-ce.repo
COPY kubernetes.repo /etc/yum.repos.d/kubernetes.repo

RUN yum install -y docker-ce kubectl

RUN systemctl enable docker

CMD ["/bin/bash"]
```

[需要的文件下载](https://github.com/sunweisheng/Docker/tree/master/Build-Images/centos-7-docker-kubectl)

[更多镜像](https://github.com/sunweisheng/Docker/tree/master/Build-Images)

```shell
# 构建命令
docker build -t bluersw/centos-7-docker-kubectl:2.0 .

# 试运行命令
docker run -v /var/run/docker.sock:/var/run/docker.sock -it bluersw/centos-7-docker-kubectl:2.0 /bin/bash

# Pull命令
docker pull bluersw/centos-7-docker-kubectl:2.0
```

运行容器时需要将宿主机的/var/run/docker.sock挂载到容器中去，因为容器内运行不了Docker Daemon，但这样有安全隐患因为可以通过docker.sock提权进而获得宿主机root权限，所以只能运行安全可靠的镜像。

## 配置Pod Templates

为了方便配置一个Pod Templates，在配置kubernetes连接内容的下面，这里的模板只是模板（与类一样使用时还要实例化过程）,名称和标签列表不要以为是Pod的name和label，这里的名称和标签列表只是Jenkins查找选择模板时使用的，Jenkins自动创建Pod的name是项目名称+随机字母的组合，所以我们填写jenkins-slave-temp，命名空间填写jenkins-ops（创建命令：kubectl create namespace jenkins-ops），Pod内添加一个容器名称是jnlp-docker（默认的jnlp容器会自动创建），Docker镜像填写：repo.bluersw.com:8083/bluersw/centos-7-docker-kubectl:2.0，repo.bluersw.com:8083是家里的Docker私有仓库（[搭建Docker私有仓库](https://github.com/sunweisheng/kvm/blob/master/Nexus-Repository.md)），下面增加两个Host Path Volume：/var/run/docker.sock、/etc/docker/daemon.json，保存回到系统管理页面。

![Alt text](http://static.bluersw.com/images/Jenkins/j-k-05-1.png)
![Alt text](http://static.bluersw.com/images/Jenkins/j-k-07.png)

## 测试运行

修改构建脚本

```groovy
podTemplate (inheritFrom: "jenkins-slave-temp"){
    node(POD_LABEL) {
        container('jnlp'){
            stage('Run shell') {
                sh 'echo hello world'
            }
        }
        container('jnlp-docker'){
            stage("Run docker"){
                sh 'docker info'
            }
        }
    }
}
```

* podTemplate：用Pod模版示例化一个Pod配置并在kubernetes内自动创建
* inheritFrom：意思是创建的Pod配置继承自jenkins-slave-temp模版
* POD_LABEL：自动创建Pod的label
* container：选择哪个容器执行脚本

执行构建结果：

```txt
Running on jenkins-test-10-dp8sp-8zxtg-m4x35 in /home/jenkins/agent/workspace/Jenkins-Test
[Pipeline] {
[Pipeline] container
[Pipeline] {
[Pipeline] stage
[Pipeline] { (Run shell)
[Pipeline] sh
+ echo hello world
hello world
[Pipeline] }
[Pipeline] // stage
[Pipeline] }
[Pipeline] // container
[Pipeline] container
[Pipeline] {
[Pipeline] stage
[Pipeline] { (Run docker)
[Pipeline] sh
+ docker info
Client:
 Debug Mode: false

Server:
 Containers: 32
  Running: 19
  Paused: 0
  Stopped: 13
 Images: 11
 Server Version: 19.03.9
 Storage Driver: overlay2
  Backing Filesystem: xfs
  Supports d_type: true
  Native Overlay Diff: true
 Logging Driver: json-file
 Cgroup Driver: cgroupfs
 Plugins:
  Volume: local
  Network: bridge host ipvlan macvlan null overlay
  Log: awslogs fluentd gcplogs gelf journald json-file local logentries splunk syslog
 Swarm: inactive
 Runtimes: runc
 Default Runtime: runc
 Init Binary: docker-init
 containerd version: 7ad184331fa3e55e52b890ea95e65ba581ae3429
 runc version: dc9208a3303feef5b3839f4323d9beb36df0a9dd
 init version: fec3683
 Security Options:
  seccomp
   Profile: default
 Kernel Version: 4.4.224-1.el7.elrepo.x86_64
 Operating System: CentOS Linux 7 (Core)
 OSType: linux
 Architecture: x86_64
 CPUs: 4
 Total Memory: 3.858GiB
 Name: centos7-k8s-node1
 ID: 3R5I:DJGZ:YRZY:ESCH:VW7H:VGAD:5SCC:GYZV:QZZS:EX5M:MV3N:246K
 Docker Root Dir: /var/lib/docker
 Debug Mode: false
 Registry: https://index.docker.io/v1/
 Labels:
 Experimental: false
 Insecure Registries:
  repo.bluersw.com:8083
  repo.bluersw.com:8082
  127.0.0.0/8
 Live Restore Enabled: false

[Pipeline] }
[Pipeline] // stage
[Pipeline] }
[Pipeline] // container
[Pipeline] }
[Pipeline] // node
[Pipeline] }
[Pipeline] // podTemplate
[Pipeline] End of Pipeline
Finished: SUCCESS
```

注意：我K8S集群使用root运行的所以权限很高，你如果使用其他账号运行的K8S集群，会遇到/var/run/docker.sock没有访问权限的问题，因为Docker必须是root权限运行，解决办法是：

```shell
# Docker 服务重启要重新执行
chmod 777 /var/run/docker.sock
```

## Pod内使用Kubectl命令

在所有Node节点上执行：

```shell
mkdir -p $HOME/.kube
cp -i /etc/kubernetes/kubelet.conf  $HOME/.kube/config
chown $(id -u):$(id -g) $HOME/.kube/config

# 测试一下
kubectl get pod -A
```

## 修改Pod Templates配置

在Pod Templates配置中增加两个Host Path Volume：/root/.kube、/var/lib/kubelet/pki/

![Alt text](http://static.bluersw.com/images/Jenkins/j-k-08.png)

## 测试运行kubectl

修改构建脚本：

```groovy
podTemplate (inheritFrom: "jenkins-slave-temp"){
    node(POD_LABEL) {
        container('jnlp'){
            stage('Run shell') {
                sh 'echo hello world'
            }
        }
        container('jnlp-docker'){
            stage("Run docker"){
                sh 'kubectl get pods -A'
            }
        }
    }
}
```

运行结果：

```txt
Running on jenkins-test-11-7m94s-nxrj4-j2z0g in /home/jenkins/agent/workspace/Jenkins-Test
[Pipeline] {
[Pipeline] container
[Pipeline] {
[Pipeline] stage
[Pipeline] { (Run shell)
[Pipeline] sh
+ echo hello world
hello world
[Pipeline] }
[Pipeline] // stage
[Pipeline] }
[Pipeline] // container
[Pipeline] container
[Pipeline] {
[Pipeline] stage
[Pipeline] { (Run docker)
[Pipeline] sh
+ kubectl get pods -A
NAMESPACE              NAME                                         READY   STATUS    RESTARTS   AGE
jenkins-ops            jenkins-test-11-7m94s-nxrj4-j2z0g            2/2     Running   0          15s
kube-system            coredns-7ff77c879f-ck49p                     1/1     Running   9          5d2h
kube-system            coredns-7ff77c879f-d2xfc                     1/1     Running   10         5d2h
kube-system            dnsutils                                     1/1     Running   16         5d1h
kube-system            etcd-centos7-k8s-master                      1/1     Running   11         5d2h
kube-system            kube-apiserver-centos7-k8s-master            1/1     Running   6          4d
kube-system            kube-controller-manager-centos7-k8s-master   1/1     Running   12         5d2h
kube-system            kube-flannel-ds-amd64-52vcn                  1/1     Running   9          5d1h
kube-system            kube-flannel-ds-amd64-vtw58                  1/1     Running   12         5d1h
kube-system            kube-flannel-ds-amd64-xm8d5                  1/1     Running   10         5d1h
kube-system            kube-proxy-l8875                             1/1     Running   18         5d1h
kube-system            kube-proxy-p5fdr                             1/1     Running   9          5d1h
kube-system            kube-proxy-pdvz2                             1/1     Running   16         5d1h
kube-system            kube-scheduler-centos7-k8s-master            1/1     Running   10         5d2h
kube-system            metrics-server-7f6d95d688-vjsbj              1/1     Running   7          4d
kubernetes-dashboard   dashboard-metrics-scraper-6b4884c9d5-8hblg   1/1     Running   7          4d
kubernetes-dashboard   kubernetes-dashboard-7b544877d5-tz4xj        1/1     Running   7          4d
nginx-ingress          coffee-5f56ff9788-2745d                      1/1     Running   6          3d23h
nginx-ingress          coffee-5f56ff9788-c8jlx                      1/1     Running   6          3d23h
nginx-ingress          nginx-ingress-hjqzc                          1/1     Running   6          3d23h
nginx-ingress          nginx-ingress-jfh6h                          1/1     Running   6          3d23h
nginx-ingress          tea-69c99ff568-cpp2k                         1/1     Running   6          3d23h
nginx-ingress          tea-69c99ff568-rmnr2                         1/1     Running   6          3d23h
[Pipeline] }
[Pipeline] // stage
[Pipeline] }
[Pipeline] // container
[Pipeline] }
[Pipeline] // node
[Pipeline] }
[Pipeline] // podTemplate
[Pipeline] End of Pipeline
Finished: SUCCESS
```

虽然运行成功了，但目前kubectl使用的账号权限只能用于查询，如果项目进行部署是不行的，所以要创建新的账号供Jenkins使用，权限在某个命名空间内可以管理Pod资源即可。

## 为Jenkins创建kubernetes集群账号和证书

在Master上执行：

```shell
# 进入集群CA证书所在目录
cd /etc/kubernetes/pki

# 执行创建证书命令
(umask 077;openssl genrsa -out jenkins.key 2048)

# O=组织信息，CN=用户名
openssl req -new -key jenkins.key -out jenkins.csr -subj "/O=kubernetes/CN=jenkins"

# 签署证书
openssl  x509 -req -in jenkins.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out jenkins.crt -days 3650
```

将jenkins.crt和jenkins.key两个文件复制到所有Node节点上的/etc/kubernetes/pki目录内：

```shell
# jenkins.crt
cat jenkins.crt

-----BEGIN CERTIFICATE-----
MIICuDCCAaACCQD6pvA8Ecor7zANBgkqhkiG9w0BAQsFADAVMRMwEQYDVQQDEwpr
dWJlcm5ldGVzMB4XDTIwMDUyOTE2MDE0OFoXDTMwMDUyNzE2MDE0OFowJzETMBEG
A1UECgwKa3ViZXJuZXRlczEQMA4GA1UEAwwHamVua2luczCCASIwDQYJKoZIhvcN
AQEBBQADggEPADCCAQoCggEBALQS8ft94y2inZF7rWgc3xfkUP+4RUgab4FGBE7r
iQ5eSqQ5Hxwgx0mbYqh12xs/IhGp4YY/NUqU5hXchQ7urEKdefmQjD3CcPPWgIsJ
8MA1uAdc6wG4d9eo0qcUcisPk6giPmXOtqw4EukH0VZLTPrRp/zle5SQHUSpSyuP
CciuFSoWnm6xMo2fvTeH3WWM7MCFyCn7+OJkIaWlFWmp/qUGtsYI8lq2D4BVk0lf
jw51KmYznj1izKxyEm8Kn/qpJ+myFHdc0GdxjLUCpFXpeHLxCEFAhJBnpUtuOA4O
2uBIDxp2j7f4BnLzQvPifnMPb5o6WF7Q/2fYOKcMvC1MWtECAwEAATANBgkqhkiG
9w0BAQsFAAOCAQEAaLpkGaFhpjLe6zO4vvDJXvGWhaY0XZKv8HrFQq6+Vqv+yf7f
LCMwDaO2sLPZNVY8ruSYgbbG+Qbj8KNsDwKrMyf++fmxcpyo9XXkfnsh209hbL9C
oMImnRRiw5bVX5nto2EEgpjPoI1EW79dfMUN8+KLj4IKe910vJ1rK3PsaNmh7T7m
3bVrfZFwz8yamHn629gxxvxZZfoN4f0kc2PqGSFsLxwYuRGGueZjNK9z5ixis/S3
yEaGnjXBPZuoTC78X+avJhxBKLczVNLIiet+HWAcUsic9Ot49QfYZj4ovIrR+Uqg
HJeI+VavIrgYd9T42XeWGBKTnEhfcniKwEZxog==
-----END CERTIFICATE-----

# jenkins.key
cat jenkins.key

-----BEGIN RSA PRIVATE KEY-----
MIIEowIBAAKCAQEAtBLx+33jLaKdkXutaBzfF+RQ/7hFSBpvgUYETuuJDl5KpDkf
HCDHSZtiqHXbGz8iEanhhj81SpTmFdyFDu6sQp15+ZCMPcJw89aAiwnwwDW4B1zr
Abh316jSpxRyKw+TqCI+Zc62rDgS6QfRVktM+tGn/OV7lJAdRKlLK48JyK4VKhae
brEyjZ+9N4fdZYzswIXIKfv44mQhpaUVaan+pQa2xgjyWrYPgFWTSV+PDnUqZjOe
PWLMrHISbwqf+qkn6bIUd1zQZ3GMtQKkVel4cvEIQUCEkGelS244Dg7a4EgPGnaP
t/gGcvNC8+J+cw9vmjpYXtD/Z9g4pwy8LUxa0QIDAQABAoIBAHajjL4u4H/uhXWW
UFcpvmoVSLBSDYNFt3UqVihQ0gmfYfn0kGSNy/7Y2xU2INdArweIL0etWUT7+OMq
WJfP87on2nbsHxmJg7WC+0mfkPhx6/8d3s9RY9O4LKFbvSRVrOi3NvkISh4JC5xw
RCFglyUhAFaEMvlcQYw9JYNbSAzoWRLAdDIZJ5bOsedOtcJkpxQczr3ngYvJ2nXC
0sLCY6/Je9BDl01K4IHXIpKVrEmhJNj+KV9L8Umrkwr87RGr4jb25aHURW2abRY4
16qU8YUrdTG8eQqT/xMZtea1QcERyRr3y29FK8pO1ID+tyoNc/KEG3oEGbJPKzHj
WB9CxukCgYEA5RLulHqyEAb2vEwaIRnbA5KPqDS0nfbe5L87FYO3J6KcJBokMlBm
aNqlCiTThu5H4SOMaYJO4u9yxMPGYDNbPKdwt27trgqYoCso1tWcDv3DQsgi/M0q
vip/ciH37vK3f+AlaQeSrtJi4TH7xTJYwgsN3X9TuToq958+F5qqTe8CgYEAyT2O
GBinOOn349IDeHxYbzFgjmLtW2YEGp8FnpU/sziluPNCA9muTUUiBm9VUHN7YSEm
DDQmohhee4IeseIrMBnHGuKkwpBDl+235tVCtCNXeTIx/hAGALw1BnNMVrHKspt2
nqMch0+VMPypZ9HyA81I+Xqd5Zr3QA1IR/J8oz8CgYBjzIO0nF/HK8GC94TKtwD7
5XZAyfWGfG9PKSEMln3M/sMX12u9n9l+BQOyD6k4N8eJBnu928+Sfs95efGLJ9Sv
8CLjR6i1Eli8LxFzx0xeG6BeD+NuT9Q3VTyA9NuXdpcLVxP1Vh9Jms8JXUVa/Dw/
DaHUxgwrvnPJvc7HadKYcQKBgCYR3QW19DySFnEk079BVsGCR8/n6xs1S2V12+xK
M8jF2KQKcNylm5HGmE87VJppnlebm8UHQJ+9mHIpBYGFVcI9virZ4W1lOUROllG2
2m2VmgC1fDuh8GDHOgjEWxazf7MWMfSEyurWJVUlFy8qymvps/puNdyv2kJlwNzL
hMSlAoGBAI/OWcteFYjPsYQM6d6FL9HMeuksN75nVqaZYOA59e+BUnt0e9r7D4cG
4wupqWA66IHo89t/JkkZ7Utxw/MO7kuyXy1tnxO6/Of1p6XWn3ckPQkGMADXfP/u
1osgYu8jLmVDMJ4nTTAZ3i/O6T3pqnl3TDbIgL9FRLxyggp5ZwRN
-----END RSA PRIVATE KEY-----
```

复制文件完成后在所有Node节点上执行：

```shell
# 创建jenkins用户信息
kubectl config set-credentials jenkins --client-certificate=jenkins.crt --client-key=jenkins.key --embed-certs=true

# 设置上下文信息,jenkins用户与集群建立关系
kubectl config set-context jenkins@default-cluster --cluster=default-cluster --user=jenkins

# 查看结果
kubectl config view
```

```yaml
apiVersion: v1
clusters:
- cluster:
    certificate-authority-data: DATA+OMITTED
    server: https://192.168.122.3:6443
  name: default-cluster
contexts:
- context:
    cluster: default-cluster
    namespace: default
    user: default-auth
  name: default-context
- context:
    cluster: default-cluster
    user: jenkins
  name: jenkins@default-cluster
current-context: default-context
kind: Config
preferences: {}
users:
- name: default-auth
  user:
    client-certificate: /var/lib/kubelet/pki/kubelet-client-current.pem
    client-key: /var/lib/kubelet/pki/kubelet-client-current.pem
- name: jenkins
  user:
    client-certificate-data: REDACTED
    client-key-data: REDACTED
```

切换刚创建的上下文（切换用户）

```shell
# 切换用户
kubectl config use-context jenkins@default-cluster

# 测试
kubectl get pod

# 没有权限
Error from server (Forbidden): pods is forbidden: User "jenkins" cannot list resource "pods" in API group "" in the namespace "default"
```

目前新账号没有分配权限无法使用，创建dev-test命名空间，并创建管理该命名空间下pod资源的角色，然后绑定到jenkins账户：

```shell
# 创建yaml内容但不执行,查看资源yaml可以加--dry-run -o yaml参数
kubectl create namespace dev-test
```

创建角色：

```yaml
apiVersion: rbac.authorization.k8s.io/v1beta1
kind: Role
metadata:
  name: jenkins-role
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["create","delete","get","list","patch","update","watch"]
- apiGroups: [""]
  resources: ["pods/exec"]
  verbs: ["create","delete","get","list","patch","update","watch"]
- apiGroups: [""]
  resources: ["pods/log"]
  verbs: ["get","list","watch"]
- apiGroups: [""]
  resources: ["events"]
  verbs: ["watch"]
- apiGroups: [""]
  resources: ["secrets"]
  verbs: ["get"]
- apiGroups: ["apps"]
  resources: ["deployments"]
  verbs: ["create","delete","get","list","patch","update","watch"]
- apiGroups: [""]
  resources: ["services"]
  verbs: ["create","delete","get","list","patch","update","watch"]
- apiGroups: ["extensions"]
  resources: ["ingresses"]
  verbs: ["create","delete","get","list","patch","update","watch"]
```

```shell
# 创建
kubectl apply -f jenkins-role.yaml -n dev-test
```

绑定账号与角色：

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: jenkins-role-bind
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: jenkins-role
subjects:
- apiGroup: rbac.authorization.k8s.io
  kind: User
  name: jenkins
```

```shell
# 创建
kubectl apply -f jenkins-role-bind.yaml -n dev-test
```

注意：Role和RoleBinding的命名空间都是dev-test权限才生效，否则是不会生效的，账号jenkins此时拥有对dev-test命名空间pod的管理权限。

测试权限：

```shell
# 在Node节点执行
[root@centos7-k8s-node1 ~]# kubectl apply -f ndsutils.yaml -n dev-test
pod/dnsutils created
[root@centos7-k8s-node1 ~]# kubectl get pod -n dev-test
NAME       READY   STATUS    RESTARTS   AGE
dnsutils   1/1     Running   0          11s
[root@centos7-k8s-node1 ~]# kubectl get pod -n default
Error from server (Forbidden): pods is forbidden: User "jenkins" cannot list resource "pods" in API group "" in the namespace "default"
```

## 测试kubectl删除和创建Pod

在PodTemplate中增加Host Path Volume：/root/yaml，里面放入一个pod资源的yaml文件

这个Pod资源使用以前提到的dnsutils，yaml资源内容如下：

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
#先创建后面通过Jenkins删除再创建
kubectl apply -f /root/yaml/dnsutils.yaml -n dev-test
```

修改构建脚本：

```groovy
podTemplate (inheritFrom: "jenkins-slave-temp"){
    node(POD_LABEL) {
        container('jnlp'){
            stage('Run shell') {
                sh 'echo hello world'
            }
        }
        container('jnlp-docker'){
            stage("Run docker"){
                sh 'kubectl config use-context jenkins@default-cluster'
                sh 'kubectl delete -f /root/yaml/dnsutils.yaml -n dev-test'
                sh 'kubectl get pod -n dev-test'
                sh 'kubectl apply -f /root/yaml/dnsutils.yaml -n dev-test'
                sh 'kubectl get pod -n dev-test'
            }
        }
    }
}
```

构建运行结果：

```txt
Running on jenkins-test-13-lpcwk-fjw2j-4rzxf in /home/jenkins/agent/workspace/Jenkins-Test
[Pipeline] {
[Pipeline] container
[Pipeline] {
[Pipeline] stage
[Pipeline] { (Run shell)
[Pipeline] sh
+ echo hello world
hello world
[Pipeline] }
[Pipeline] // stage
[Pipeline] }
[Pipeline] // container
[Pipeline] container
[Pipeline] {
[Pipeline] stage
[Pipeline] { (Run docker)
[Pipeline] sh
+ kubectl config use-context jenkins@default-cluster
Switched to context "jenkins@default-cluster".
[Pipeline] sh
+ kubectl delete -f /root/yaml/dnsutils.yaml -n dev-test
pod "dnsutils" deleted
[Pipeline] sh
+ kubectl get pod -n dev-test
No resources found in dev-test namespace.
[Pipeline] sh
+ kubectl apply -f /root/yaml/dnsutils.yaml -n dev-test
pod/dnsutils created
[Pipeline] sh
+ kubectl get pod -n dev-test
NAME       READY   STATUS              RESTARTS   AGE
dnsutils   0/1     ContainerCreating   0          1s
[Pipeline] }
[Pipeline] // stage
[Pipeline] }
[Pipeline] // container
[Pipeline] }
[Pipeline] // node
[Pipeline] }
[Pipeline] // podTemplate
[Pipeline] End of Pipeline
Finished: SUCCESS
```

## 为jenkins账号赋予集群角色

创建集群角色，此集群角色只有查看Pod的权限：

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: cluster-reader
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get","list","watch"]
```

绑定账号和集群角色：

```yaml
apiVersion: rbac.authorization.k8s.io/v1beta1
kind: ClusterRoleBinding
metadata:
  name: cluster-reader-jenkins
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-reader
subjects:
- apiGroup: rbac.authorization.k8s.io
  kind: User
  name: jenkins
```

集群角色没有命名空间的概念，集群角色是在所有命名空间有效。

```shell
# 创建
kubectl apply -f cluster-reader.yaml

# 绑定
kubectl apply -f cluster-reader-jenkins.yaml
```

在Node节点上执行：

```shell
[root@centos7-k8s-node1 yaml]# kubectl get pod -A
NAMESPACE              NAME                                         READY   STATUS    RESTARTS   AGE
dev-test               dnsutils                                     1/1     Running   0          7m17s
kube-system            coredns-7ff77c879f-ck49p                     1/1     Running   9          5d3h
kube-system            coredns-7ff77c879f-d2xfc                     1/1     Running   10         5d3h
kube-system            etcd-centos7-k8s-master                      1/1     Running   11         5d3h
kube-system            kube-apiserver-centos7-k8s-master            1/1     Running   6          4d1h
kube-system            kube-controller-manager-centos7-k8s-master   1/1     Running   12         5d3h
kube-system            kube-flannel-ds-amd64-52vcn                  1/1     Running   9          5d3h
kube-system            kube-flannel-ds-amd64-vtw58                  1/1     Running   12         5d3h
kube-system            kube-flannel-ds-amd64-xm8d5                  1/1     Running   10         5d3h
kube-system            kube-proxy-l8875                             1/1     Running   18         5d3h
kube-system            kube-proxy-p5fdr                             1/1     Running   9          5d3h
kube-system            kube-proxy-pdvz2                             1/1     Running   16         5d3h
kube-system            kube-scheduler-centos7-k8s-master            1/1     Running   10         5d3h
kube-system            metrics-server-7f6d95d688-vjsbj              1/1     Running   7          4d1h
kubernetes-dashboard   dashboard-metrics-scraper-6b4884c9d5-8hblg   1/1     Running   7          4d1h
kubernetes-dashboard   kubernetes-dashboard-7b544877d5-tz4xj        1/1     Running   7          4d1h
nginx-ingress          coffee-5f56ff9788-2745d                      1/1     Running   6          4d1h
nginx-ingress          coffee-5f56ff9788-c8jlx                      1/1     Running   6          4d1h
nginx-ingress          nginx-ingress-hjqzc                          1/1     Running   6          4d1h
nginx-ingress          nginx-ingress-jfh6h                          1/1     Running   6          4d1h
nginx-ingress          tea-69c99ff568-cpp2k                         1/1     Running   6          4d1h
nginx-ingress          tea-69c99ff568-rmnr2                         1/1     Running   6          4d1h
```

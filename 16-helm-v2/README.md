## 安装helm,为安装loki + protail + grafana做准备
---
```
1. Helm安装部署，本文基于v2.14.3

Helm包含：HelmClient  和  TillerServer

a）下载HelmClient 

wget https://get.helm.sh/helm-v2.14.3-linux-amd64.tar.gz && tar zxvf helm-v2.14.3-linux-amd64.tar.gz
cd helm-v2.14.3-linux-amd64
chmod +x helm
cp helm /usr/local/bin
helm version

Client: &version.Version{SemVer:"v2.14.3", GitCommit:"0e7f3b6637f7af8fcfddb3d2941fcc7cbebb0085", GitTreeState:"clean"}
b）安装TillerServer，在k8s,k3s集群中需要配置ServiceAccount: tiller，并赋予cluster-admin角色权限，采用rbac.yaml配置

apiVersion: v1
kind: ServiceAccount
metadata:
  name: tiller
  namespace: kube-system
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: tiller
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-admin
subjects:
  - kind: ServiceAccount
    name: tiller
    namespace: kube-system
kubectl apply -f rbac.yaml

再通过helm init命令来创建TillerServer

#k8s集群中
helm init --service-account tiller --tiller-image registry.cn-hangzhou.aliyuncs.com/google_containers/tiller:v2.14.3

#k3s集群中，由于版本原因，deployment资源的template需要修改一下，否则会报错
helm init --tiller-image registry.cn-hangzhou.aliyuncs.com/google_containers/tiller:v2.14.3 --service-account tiller --override spec.selector.matchLabels.'name'='tiller',spec.selector.matchLabels.'app'='helm' --output yaml | sed 's@apiVersion: extensions/v1beta1@apiVersion: apps/v1@' | k3s kubectl apply -f -
综上，a)，b)两步之后，通过helm version

Client: &version.Version{SemVer:"v2.14.3", GitCommit:"0e7f3b6637f7af8fcfddb3d2941fcc7cbebb0085", GitTreeState:"clean"}
Server: &version.Version{SemVer:"v2.14.3", GitCommit:"0e7f3b6637f7af8fcfddb3d2941fcc7cbebb0085", GitTreeState:"clean"}
注意: 在创建完tiller用户之后，helm init之后，执行helm version报错：

root@aaa:~/helm# helm version
Client: &version.Version{SemVer:"v2.14.3", GitCommit:"0e7f3b6637f7af8fcfddb3d2941fcc7cbebb0085", GitTreeState:"clean"}
Error: Get http://localhost:8080/api/v1/namespaces/kube-system/pods?labelSelector=app%3Dhelm%2Cname%3Dtiller: dial tcp [::1]:8080: connect: connection refused

#解决办法：
kubectl config view --raw > ~/.kube/config
#猜想helm需要访问k8s的KUBECONFIG文件，而默认文件的位置在~/.kube/config下。所以很多时候这个位置的默认文件不能丢

遇到的问题及解决办法:
先要安装helm,可能遇到的问题
1. 每个节点都要安装 缺少socat  yum install socat
2. helm 跟kubectl 一样，从.kube/config 读取配置证书跟k8s通讯，先确保kubectl能够可用，否则出现错误
3. .RBAC权限问题，如果集群启用RBAC
解决方法：
给tiller增加权限：
a. 创建sa
kubectl create serviceaccount --namespace kube-system tiller
b. 给sa绑定cluster-admin规则
kubectl create clusterrolebinding tiller-cluster-rule --clusterrole=cluster-admin --serviceaccount=kube-system:tiller
c.编辑 Tiller Deployment 名称为： tiller-deploy.
kubectl edit deploy --namespace kube-system tiller-deploy
插入一行 （serviceAccount: tiller） in the spec: template: spec section of the file:
...
spec:
  replicas: 1
  selector:
    matchLabels:
      app: helm
      name: tiller
  strategy:
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 1
    type: RollingUpdate
  template:
    metadata:
      creationTimestamp: null
      labels:
        app: helm
        name: tiller
    spec:
      serviceAccount: tiller
      containers:
      - env:
        - name: TILLER_NAMESPACE
          value: kube-system
...
    
   
   
常用的操作:
1. 删除服务端  
helm reset   
Tiller (the helm server side component) has been uninstalled from your Kubernetes Cluster.
2. 添加chart仓库  
helm repo add loki https://grafana.github.io/loki/charts
helm repo add stable https://kubernetes-charts.storage.googleapis.com
3. 更新char仓库
helm repo update
```

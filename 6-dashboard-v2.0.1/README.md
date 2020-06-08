1. **修改dashboard.yaml(已修改)**
```
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
```
---
2. **因为自动生成的证书很多浏览器无法使用，所以我们自己创建，注释掉dashboard.yaml中kubernetes-dashboard-certs对象声明**
#apiVersion: v1
#kind: Secret
#metadata:
#  labels:
#    k8s-app: kubernetes-dashboard
#  name: kubernetes-dashboard-certs
#  namespace: kubernetes-dashboard
#type: Opaque

3. **创建证书**
   * 下载mkcert工具，一键生成证书
     ![mkcert](https://github.com/FiloSottile/mkcert)
   * 生成证书
     ```
      mkcert -install
      mkcert example.com "*.example.com" example.test localhost 127.0.0.1 ::1
      生成的证书目录会有提示，把key，和crt修改一下名字为dashboard.key, dashboard.crt
     

    ```

   * 创建命名空间
     kubectl create namespace kubernetes-dashboard

   * 创建kubernetes-dashboard-certs对象
     kubectl create secret generic kubernetes-dashboard-certs --from-file=dashboard.key --from-file=dashboard.crt -n kubernetes-dashboard

4. 安装Dashboard
```
#安装,所有的.yaml文件
kubectl create -f  .

#检查结果
kubectl get pods -A  -o wide

kubectl get service -n kubernetes-dashboard  -o wide

创建dashboard管理员
创建账号：

vi dashboard-admin.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  labels:
    k8s-app: kubernetes-dashboard
  name: dashboard-admin
  namespace: kubernetes-dashboard
#保存退出后执行
kubectl create -f dashboard-admin.yaml
为用户分配权限：

vi dashboard-admin-bind-cluster-role.yaml
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
#保存退出后执行
kubectl create -f dashboard-admin-bind-cluster-role.yaml
查看并复制用户Token：

kubectl -n kubernetes-dashboard describe secret $(kubectl -n kubernetes-dashboard get secret | grep dashboard-admin | awk '{print $1}')
```

5. 访问
https://192.168.11.122:30008

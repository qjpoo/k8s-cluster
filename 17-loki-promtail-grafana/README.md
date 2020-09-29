## loki的组成:
1. loki是主服务器,负责存储日志和处理查询
2. promtail是代理,负责收集日志并将其发送给loki
3. grafana用于ui的展示

## 安装loki + promtail + grafana首先要安装helm2, 注意是helm2, 我是以helm2这个版本来安装的
```
[root@master ~]# helm version
Client: &version.Version{SemVer:"v2.16.9", GitCommit:"8ad7037828e5a0fca1009dabe290130da6368e39", GitTreeState:"clean"}
Server: &version.Version{SemVer:"v2.16.9", GitCommit:"8ad7037828e5a0fca1009dabe290130da6368e39", GitTreeState:"clean"}
```
![安装helm2](https://github.com/qjpoo/k8s-cluster/tree/master/16-helm-v2)


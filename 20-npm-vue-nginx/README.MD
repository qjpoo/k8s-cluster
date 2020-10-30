## 这个是前端的项目, 是基于nodejs vue的, 所以只要把代码用Npm打包好, 放在nginx的镜像里面就可以了
### 文件说明
```
├── bzj-h5.yaml yaml文件
├── dockerfile 打包前端代码为一个nginx的镜像包
└── README.MD  说明文件
```

```
这个yaml文件, 为了省事, 没有把nginx的log日志挂载到物理机上机, 配置文件用的是configmap方式
里面有两个conf文件, 一个是默认的, 还有一个是项目的
nginx.conf和default.conf
```

1. npm打包, 是调用的是dockerfile文件, dockerfile文件跟代码放在一起, 从git上把代码和dockerfile拉下来, 然后打包push到harbor
2. 替换一下namespace, image url等



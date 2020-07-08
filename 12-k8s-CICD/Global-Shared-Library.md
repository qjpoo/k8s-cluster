# 创建Global Shared Library项目

[Jenkins Global Shared Library的说明](https://www.jenkins.io/doc/book/pipeline/shared-libraries/)

## Maven设置中设置仓库地址

```conf
    <repositories>
        <repository>
        <id>jenkins-ci-releases</id>
        <url>https://repo.jenkins-ci.org/releases/</url>
        </repository>
        ...
    </repositories>
```

## 创建项目

```shell
# 在工作目录下执行
mvn -U archetype:generate -Dfilter=io.jenkins.archetypes:

Choose archetype:
1: remote -> io.jenkins.archetypes:empty-plugin (-)
2: remote -> io.jenkins.archetypes:global-configuration-plugin (Skeleton of a Jenkins plugin with a POM and an example piece of global configuration.)
3: remote -> io.jenkins.archetypes:global-shared-library (Uses the Jenkins Pipeline Unit mock library to test the usage of a Global Shared Library)
4: remote -> io.jenkins.archetypes:hello-world-plugin (Skeleton of a Jenkins plugin with a POM and an example build step.)
5: remote -> io.jenkins.archetypes:scripted-pipeline (Uses the Jenkins Pipeline Unit mock library to test the logic inside a Pipeline script.)
# 选择3 带测试的Global Shared Library项目
Choose a number or apply filter (format: [groupId:]artifactId, case sensitive contains): : 3
Choose io.jenkins.archetypes:global-shared-library version:
1: 1.4
2: 1.5
3: 1.6
# 选择3
Choose a number: 3: 3
[INFO] Using property: groupId = org.sample
# 输入项目目录名称
Define value for property 'artifactId': global-shared-library-frame  
# 版本号默认即可
Define value for property 'version' 1.0-SNAPSHOT: :
[INFO] Using property: package = io.jenkins.pipeline.sample
Confirm properties configuration:
groupId: org.sample
artifactId: global-shared-library-frame
version: 1.0-SNAPSHOT
package: io.jenkins.pipeline.sample
# 同意 y
 Y: : y
[INFO] ----------------------------------------------------------------------------
[INFO] Using following parameters for creating project from Archetype: global-shared-library:1.6
[INFO] ----------------------------------------------------------------------------
[INFO] Parameter: groupId, Value: org.sample
[INFO] Parameter: artifactId, Value: global-shared-library-frame
[INFO] Parameter: version, Value: 1.0-SNAPSHOT
[INFO] Parameter: package, Value: io.jenkins.pipeline.sample
[INFO] Parameter: packageInPathFormat, Value: io/jenkins/pipeline/sample
[INFO] Parameter: version, Value: 1.0-SNAPSHOT
[INFO] Parameter: package, Value: io.jenkins.pipeline.sample
[INFO] Parameter: groupId, Value: org.sample
[INFO] Parameter: artifactId, Value: global-shared-library-frame
[INFO] Project created from Archetype in dir: /Users/sunweisheng/Documents/Test-Jenkins-Plugin/global-shared-library-frame
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:49 min
[INFO] Finished at: 2020-07-04T21:04:27+08:00
[INFO] ------------------------------------------------------------------------
```

完成后用IDEA打开项目即可。

![Alt text](http://static.bluersw.com/images/Jenkins/global-shared-library-01.png)

打开项目后将pipelineUsingSharedLib.groovy改名为pipelineUsingSharedLib.Jenkinsfile，TestSharedLibrary测试类里runScript()中的地址也要改一下，红色的错误提示就没了，之后可以把各个文件夹中的目录名（包名）改成你希望的目录名（包名）。

## 修改POM

有两个POM文件，项目根目录下的POM文件修改groupId、description即可。

示例：

```conf
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.bluersw</groupId>
    <artifactId>global-shared-library-frame-parent</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>Global Shared Library - Parent</name>
    <description>Global Shared Library Frame</description>
    <modules>
        <module>unit-tests</module>
    </modules>
</project>
```

另一个POM文件在项目目录下的unit-tests子目录下：

首先把groupId、name、description、url等进行修改，示例如下：

```conf
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.bluersw</groupId>
    <artifactId>global-shared-library-frame</artifactId>
    <version>1.0-SNAPSHOT</version>
    <name>Global Shared Library Frame</name>
    <description>Global Shared Library Frame</description>
    <url>https://github.com/sunweisheng/global-shared-library-frame</url>
```

然后修改jenkins-pipline-unit的版本号为1.5：

```conf
<jenkins-pipeline-unit.version>1.5</jenkins-pipeline-unit.version>
```

最后增加一些需要的依赖引用，比如：

```conf
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.jenkins.tools.bom</groupId>
                <artifactId>bom-2.164.x</artifactId>
                <version>3</version>
                <scope>import</scope>
                <type>pom</type>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

如果使用CpsScript、WorkflowRun、WorkflowJob这些类型的话，需要添加如下依赖：

```conf
        <dependency>
            <groupId>org.jenkins-ci.plugins.workflow</groupId>
            <artifactId>workflow-job</artifactId>
            <version>2.35</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.jenkins-ci.plugins.workflow</groupId>
            <artifactId>workflow-cps</artifactId>
            <version>2.74</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.jenkins-ci.main</groupId>
            <artifactId>jenkins-core</artifactId>
            <version>2.164.3</version>
        </dependency>
```

如果需要JSONObject类型的话，需要添加如下依赖：

```conf
        <dependency>
            <groupId>org.kohsuke.stapler</groupId>
            <artifactId>json-lib</artifactId>
            <version>2.4-jenkins-2</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>1.7.25</version>
            <scope>compile</scope>
        </dependency>
```

## 测试基类

在测试类的定义中可以选择BasePipelineTest或DeclarativePipelineTest进行继承，区别是：

BasePipelineTest测试的构建脚本：

```conf
node(){
    .....
}
```

DeclarativePipelineTest测试的构建脚本：

```conf
pipeline {
    agent none
    stages {
        stage('Example Build') {
            agent { docker 'maven:3-alpine' }
            steps {
                .....
            }
        }
        stage('Example Test') {
            agent { docker 'openjdk:8-jre' }
            steps {
                .....
            }
        }
    }
}
```

## 使用Pipeline Utility Steps定义的方法

首先看看[Pipeline Utility Steps Plugin 源码](https://github.com/jenkinsci/pipeline-utility-steps-plugin)是怎么实现的，然后自己在测试类模拟，以readJSON方法为例：

```java
    @Test
    void library_annotation() throws Exception {
        boolean exception = false
        def library = library().name('shared-library')
                .defaultVersion("master")
                .allowOverride(false)
                .implicit(false)
                .targetPath(sharedLibs)
                .retriever(localSource(sharedLibs))
                .build()
        helper.registerSharedLibrary(library)
        helper.registerAllowedMethod(MethodSignature.method("readJSON",String.class),{file->
        return this.readJSON((String)file)
        })
        runScript('com/bluersw/LibHelper.Jenkinsfile')
        printCallStack()
    }

    public JSONObject readJSON(String path){
        FileInputStream fs = new FileInputStream(path);
        String text = fs.getText();
        JSONObject jo = (JSONObject)JSONSerializer.toJSON(text);
        return jo;
    }
```

## 使用@NonCPS

一些数据类型无法串行化，使用的时候Jenkins会报错（Jenkins构建可以暂停和继续需要串行化所有数据），可以使用@NonCPS注解避免报错，同样暂停和继续可能也不能使用了：

```java
@NonCPS
static List<Integer> nonCpsDouble(List<Integer> integers) {
    integers.collect { it * 2 }
}
```

## 更多资料

[JenkinsPipelineUnit使用说明](https://github.com/jenkinsci/JenkinsPipelineUnit)

[示例项目 Global Shared Library Frame](https://github.com/sunweisheng/global-shared-library-frame)

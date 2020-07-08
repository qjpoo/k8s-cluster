# Jenkins插件开发

## 插件功能

在Jenkins构建之前选择Slave Server进行构建。
[Slave Server搭建](https://github.com/sunweisheng/Jenkins/blob/master/Jenkins-Slave.md)

## 准备工作

[安装Java](https://github.com/sunweisheng/Kvm/blob/master/Install-Java-18.md)

[安装Maven](https://github.com/sunweisheng/Jenkins/blob/master/Install-Maven.md)

Maven设置中添加仓库地址

```conf
<settings>
  <pluginGroups>
    <pluginGroup>org.jenkins-ci.tools</pluginGroup>
  </pluginGroups>
  <profiles>
    <profile>
      <id>jenkins</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <repositories>
        <repository>
          <id>repo.jenkins-ci.org</id>
          <url>Index of public/</url>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>repo.jenkins-ci.org</id>
          <url>Index of public/</url>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>
</settings>
```

## 命名规约

artifactId：

- 使用小写 ID ，并根据需要使用连字符分隔术语
- 除非名称有任何意义，否则不要在 ID 中包含 jenkins 或 plugin
- 本示例的artifactId是：slave-server-parameter

插件名称：

- 插件的名称在 Jenkins UI 和其它地方（如：插件站点）展示给用户
- 建议使用简短的描述性名称，如 Subversion
- 本示例的插件名称叫：Slave Server Parameter Plug-In

groupId：

- 推荐使用 io.jenkins.plugins 或 org.jenkins-ci.plugins 作为 groupId
- 但是不禁止其他组织 ID ，除非它们是恶意的
- 本示例的GroupId是：io.jenkins.plugins

Java 源代码：

- 一般遵循Oracle Java 代码规约
- 本示例的IDE使用IntelliJ IDEA (Community Edition)，并安装了Alibaba Java Code Guidelines插件规范代码规约

## 创建项目

```shell
# 在项目文件夹下执行
mvn -U archetype:generate -Dfilter=io.jenkins.archetypes:
```

PS：在Generating project in Interactive mode会等一会儿。

选择创建一个空项目

```shell
Choose archetype:
1: remote -> io.jenkins.archetypes:empty-plugin (-)
2: remote -> io.jenkins.archetypes:global-configuration-plugin (Skeleton of a Jenkins plugin with a POM and an example piece of global configuration.)
3: remote -> io.jenkins.archetypes:global-shared-library (Uses the Jenkins Pipeline Unit mock library to test the usage of a Global Shared Library)
4: remote -> io.jenkins.archetypes:hello-world-plugin (Skeleton of a Jenkins plugin with a POM and an example build step.)
5: remote -> io.jenkins.archetypes:scripted-pipeline (Uses the Jenkins Pipeline Unit mock library to test the logic inside a Pipeline script.)
Choose a number or apply filter (format: [groupId:]artifactId, case sensitive contains): : 1
Choose io.jenkins.archetypes:empty-plugin version:
1: 1.0
2: 1.1
3: 1.2
4: 1.3
5: 1.4
6: 1.5
7: 1.6
Choose a number: 7: 7
Downloading from repo.bluersw.com: http://repo.bluersw.com:8081/repository/maven-public/io/jenkins/archetypes/empty-plugin/1.6/empty-plugin-1.6.pom
Downloaded from repo.bluersw.com: http://repo.bluersw.com:8081/repository/maven-public/io/jenkins/archetypes/empty-plugin/1.6/empty-plugin-1.6.pom (717 B at 991 B/s)
Downloading from repo.bluersw.com: http://repo.bluersw.com:8081/repository/maven-public/io/jenkins/archetypes/empty-plugin/1.6/empty-plugin-1.6.jar
Downloaded from repo.bluersw.com: http://repo.bluersw.com:8081/repository/maven-public/io/jenkins/archetypes/empty-plugin/1.6/empty-plugin-1.6.jar (1.5 kB at 3.7 kB/s)
[INFO] Using property: groupId = unused
Define value for property 'artifactId': slave-server-parameter
Define value for property 'version' 1.0-SNAPSHOT: :
[INFO] Using property: package = unused
Confirm properties configuration:
groupId: unused
artifactId: slave-server-parameter
version: 1.0-SNAPSHOT
package: unused
 Y: : y
[INFO] ----------------------------------------------------------------------------
[INFO] Using following parameters for creating project from Archetype: empty-plugin:1.6
[INFO] ----------------------------------------------------------------------------
[INFO] Parameter: groupId, Value: unused
[INFO] Parameter: artifactId, Value: slave-server-parameter
[INFO] Parameter: version, Value: 1.0-SNAPSHOT
[INFO] Parameter: package, Value: unused
[INFO] Parameter: packageInPathFormat, Value: unused
[INFO] Parameter: version, Value: 1.0-SNAPSHOT
[INFO] Parameter: package, Value: unused
[INFO] Parameter: groupId, Value: unused
[INFO] Parameter: artifactId, Value: slave-server-parameter
[INFO] Project created from Archetype in dir: /Users/sunweisheng/Documents/Test-Jenkins-Plugin/slave-server-parameter
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  05:05 min
[INFO] Finished at: 2020-06-13T08:42:26+08:00
[INFO] ------------------------------------------------------------------------
sunweisheng@localhost Test-Jenkins-Plugin %
```

PS:将项目目录下的文件拷贝到GitHub仓库目录下面，并用IDEA打开项目。

![Alt text](http://static.bluersw.com/images/Jenkins/sssp-01.png)

## Debug

```shell
export MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

mvn hpi:run -Djetty.port=8090
```

![Alt text](http://static.bluersw.com/images/Jenkins/sssp-04.png)

在IDEA右上方Edit Configurations...
![Alt text](http://static.bluersw.com/images/Jenkins/sssp-03.png)

创建Remote-Debug

![Alt text](http://static.bluersw.com/images/Jenkins/sssp-06.png)
![Alt text](http://static.bluersw.com/images/Jenkins/sssp-02.png)

应用启动后，在右上方点击Debug ‘Remote-Debug’
![Alt text](http://static.bluersw.com/images/Jenkins/sssp-05.png)

访问http://localhost:8090/jenkins/ 进入Debug模式。

## IntelliJ IDEA设置

因为插件的国际化使用Resource Bundle实现，中文在.properties文件中需要用Unicode的编码存储，所以需要设置IDE自动对properties文件内容的中文用native2ascii工具进行转码。在IDEA设置界面查找File Encodings，勾选Transparent native-to-ascii conversion。
![Alt text](http://static.bluersw.com/images/Jenkins/sssp-07.png)

因为Jenkins中插件对象创建后串行化到文件中，使用的时候再并行化到内存，所以类的serialVersionUID属性就很重要了，这个属性可以设置IDEA自动生成，在IDEA的设置界面搜索Inspections，在右侧再搜索Serialization issues，勾选所有选项，在类名上Alt+Enter就可以看见“Add 'serialVersionUID' field”选项了。
![Alt text](http://static.bluersw.com/images/Jenkins/sssp-08.png)

创建JavaDOC注释的快捷键，在IDEA的设置中点击Keymap，在右侧搜索Fix doc comment，双击搜索结果设置快捷键。
![Alt text](http://static.bluersw.com/images/Jenkins/sssp-09.png)

## 构建参数定义类

```java
/**
 * @author sunweisheng
 * Jenkins 构建参数：Slave服务器选择（Jenkins build parameters: Slave server selection）
 */
public class SlaveParameterDefinition extends ParameterDefinition implements  Comparable<SlaveParameterDefinition>
```

该类继承了ParameterDefinition代表这是一个构建参数，ParameterDefinition是Jenkins提供扩展点之一（[Jenkins扩展点官方资料](https://www.jenkins.io/doc/developer/extensions/jenkins-core/#parameterdefinition)），实现Comparable接口是因为一个构建项目可能包含多个我们编写的Slave服务器参数。

该类里面主要方法：

```java
/**
* 构造函数在构建项目配置构建参数时调用（The constructor is called when the build project configures build parameter）
* @param name 构建参数名称 （Build parameter name）
* @param defaultValue 该构建参数的默认值，会随着每次用户选择Slave服务器而改变（The default value of this build parameter will change each time the user selects the slave server）
*/
@DataBoundConstructor
public SlaveParameterDefinition(String name,String defaultValue) {
	super(name, DESCRIPTION);
	this.uuid = UUID.randomUUID();
	this.setDefaultValue(defaultValue);
}
```

构建的函数的参数：参数名称和默认值，如果定义name是slave-name以后在构建脚本里就可以使用params['slave-name']读取该参数的内容，创建该对象后Jenkins就会将该对象串行化到硬盘上进行存储，使用时再并行化到内存中使用，所以该对象内的数据都会被保存起来。

对应的配置页面在项目目录/src/main/resources/com/bluersw/SlaveParameterDefinition/config.jelly，这里的文件路径和SlaveParameterDefinition类的文件路径要保持一致，SlaveParameterDefinition的文件路径是：
项目目录/src/main/java/com/bluersw/SlaveParameterDefinition.java。

```xml
<?jelly escape-by-default='true'?>
<j:jelly xmlns:j="jelly:core" xmlns:st="jelly:stapler" xmlns:d="jelly:define" xmlns:l="/lib/layout" xmlns:t="/lib/hudson" xmlns:f="/lib/form">
	<f:entry title="${%name}" field="name">
		<f:textbox />
	</f:entry>
	<f:advanced>
		<f:entry title="${%defaultValue}" field="defaultValue" description="${%defaultValueDescr}">
			<f:textbox />
		</f:entry>
	</f:advanced>
</j:jelly>
```

其中title和description绑定的数据是从同目录下的*.properties文件中读取的，field是提交时在类中定义的参数值，语言会根据系统语言自动选择。

```conf
Name=Name
defaultValue=默认的Slave服务器名称
defaultValueDescr=请输入一个Slave服务器名称。
```

展现效果如下：
![Alt text](http://static.bluersw.com/images/Jenkins/sssp-10.png)

点击保存的时候会调用验证name是否合法的方法，这个方法是在SlaveParameterDefinition类中定义一个名叫DescriptorImpl的内部静态类：

```java
/**
* 参数描述类，实现了与UI交互的方法。（The parameter description class implements the method of interacting with the UI.）
*/
@Symbol("slaveParameter")
@Extension
public static class DescriptorImpl extends ParameterDescriptor
```

该类继承ParameterDescriptor扩展点，基本上和UI交互的方法都定义在这个类中了，就连实例化SlaveParameterDefinition对象的方法也是定义这个类中，可以说Jenkins主要通过ParameterDescriptor类操作各种构建参数对象。

验证方法如下：

```java
/**
* 验证用户输入的参数名称是否合法，注意一定是“doCheck”+“要检查的参数名称”形式为方法名称。（Verify that the parameter name entered by the user is legal. Note that it must be in the form of "doCheck" + "parameter name to be checked" as the method name.）
* @param name 要检查的Name内容。（Name content to check.）
* @return 检查是否通过，如果没有通过返回错误信息。（Check if it passes, and return an error message if it fails.）
* @throws IOException
* @throws ServerCloneException
*/
public FormValidation doCheckName(@QueryParameter String name)throws IOException, ServerCloneException{
	if(name.length() == 0){
	    return FormValidation.error(Messages.SlaveParameterDefinition_DescriptorImpl_errors_missingName());
	}

	return FormValidation.ok();
}
```

注意一定是“doCheck”+“要检查的参数名称”形式定义方法名称。

创建构建参数后构建按钮就变为Build with Parameters了。

![Alt text](http://static.bluersw.com/images/Jenkins/sssp-11.png)

负责展现参数的页面是在项目目录/src/main/resources/com/bluersw/SlaveParameterDefinition/index.jelly中，该文件中定义了一个下拉菜单显示Jenkins中的所有Slave服务器的名称，每次选择Slave服务器后JS脚本会调用服务器端的方法更新默认值，下次用户再构建就不用从新选择了。

![Alt text](http://static.bluersw.com/images/Jenkins/sssp-12.png)

```xml
<?jelly escape-by-default='true'?>
<j:jelly xmlns:j="jelly:core" xmlns:st="jelly:stapler" xmlns:d="jelly:define"
		 xmlns:l="/lib/layout" xmlns:t="/lib/hudson" xmlns:f="/lib/form"
		 xmlns:i="jelly:fmt" xmlns:p="/lib/hudson/project">
	<st:adjunct includes="com.bluersw.jquery"/>
	<st:adjunct includes="com.bluersw.slaveParameter"/>
	<j:set var="instance" value="${it}" />
	<j:set var="descriptor" value="${it.descriptor}"/>
	<f:entry title="${it.name}" description="${it.description}">
		<div name="parameter" id="${it.divId}" style="white-space:nowrap" >
			<input type="hidden" name="name" value="${it.name}" />
			<f:select id="slaveParameterSelect" field="value" default="${it.defaultValue}" style="width:auto;"/>
			<div id="result_message"></div>
		</div>
		<script type="text/javascript">
			var parentDiv = jQuery('#${it.divId}');
			var requestBasicUrl = "${h.getCurrentDescriptorByNameUrl()}/${it.descriptor.descriptorUrl}/setDefaultValue?name=${it.name}";
			bindOnChange(parentDiv,requestBasicUrl);
		</script>
	</f:entry>
</j:jelly>
```

```xml
<j:set var="descriptor" value="${it.descriptor}"/>
```

没有这句会报错，导入jquery需要先添加一个jelly文件指定脚本文件位置，再用<st:adjunct includes="com.bluersw.jquery"/>导入到页面中。

```xml
<?jelly escape-by-default='true'?>
<j:jelly xmlns:j="jelly:core" xmlns:st="jelly:stapler">
	<script src="${it.packageUrl}/javascript/jQuery-3.5.1-min.js" type="text/javascript" />
</j:jelly>
```

该文件位置在项目目录/src/main/resources/com/bluersw/jquery.jelly，所以includes="com.bluersw.jquery"是类似命名空间的位置定位，其他js脚本导入也是类似方法。

<f:select/>元素的数据绑定，是根据该元素的field内容，调用DescriptorImpl静态类的方法实现，该方法命名必须是"doFill"+要绑定数据的页面元素field属性值+"Items"（示例中select元素的field的值是value）：

```java
/**
* 在项目构建页面设置参数时调用，将所有可以用于构建的服务器名称，绑定到下拉菜单中让用户选择，方法名称必须是"doFill"+要绑定数据的页面元素field属性值+"Items"。（Called when setting parameters on the project construction page, bind all server names that can be used for construction to the drop-down menu for the user to choose, the method name must be "doFill" + field attribute value of the page element to be bound data + "Items ".）
* @param job 当前项目的构建任务。（The build task of the current project.）
* @param name Slave Server Parameter的名称。（The name of the Slave Server Parameter.）
* @return Slave名称列表是Select元素，返回此元素的内容。（The Slave name list is the Select element, and returns the content of this element.）
*/
public ListBoxModel doFillValueItems(@AncestorInPath Job job, @QueryParameter String name)
```

name参数的值来自页面中的input元素：

```xml
<input type="hidden" name="name" value="${it.name}" />
```

类似表单提交你需要什么元素的值就在参数中声明即可（@QueryParameter String 元素名称）。

用户选择Slave服务器名称后js脚本会用ajax调用服务器端接口更新该参数对象的默认值，服务器端接口的命名必须是"do"+"方法名"：

```java
/**
* 客户端选择Slave服务器时JS脚本调用的服务器端方法，作用是更新此参数的默认值，以便于方便用户下次项目构建时不需要再次选择Slave服务器。方法名一定是"do"+"方法名"。（When the client selects the Slave server, the server-side method called by the JS script is to update the default value of this parameter, so that the user does not need to select the Slave server again when the project is built next time.The method name must be "do" + "method name".）
* @param job 当前项目的构建任务。（The build task of the current project.）
* @param name Slave Server Parameter的名称。（The name of the Slave Server Parameter.）
* @param value 用户选择的Slave服务器的名称。（The name of the slave server selected by the user.）
* @return 操作结果说明。（Explanation of operation result.）
*/
public String doSetDefaultValue(@AncestorInPath Job job, @QueryParameter String name, @QueryParameter String value)
```

js调用地址是.../setDefaultValue?name=xxx&value=xxx，调用函数是：

```javascript
jQuery.noConflict();

function bindOnChange(parent,requestBasicUrl){
	var selectE = parent.find('#slaveParameterSelect');
	var messageD = parent.find('#result_message');
	selectE.change(function(){
		requestUrl = requestBasicUrl + "&value=" + jQuery(this).children('option:selected').val();
		jQuery.ajax({url:requestUrl,success:function(result){
			messageD.text(result);
			}})
		}
	)
}
```

因为"$"符号和jelly的数据绑定符号冲突，所以用jQuery.noConflict();换成jQuery，该文件在/src/main/resources/com/bluersw/javascript/slave-parameter.js,每次选择都会调用DescriptorImpl类的doSetDefaultValue方法更新默认值。

选择参数点击“构建”按钮之后，会将页面元素的值都通过Json格式传到服务器端调用SlaveParameterDefinition类的createValue方法，创建参数返回值后续交给构建脚本使用：

```java
/**
* 创建Slave服务器参数的参数结果对象（Create parameter result object for Slave server parameter）
* @param staplerRequest StaplerRequest对象（StaplerRequest object）
* @param jsonObject Slave服务器参数的结果对象，Json格式（Slave Server Parameter result object, Json format）
* @return 参数结果对象 （Parameter result object）
*/
@CheckForNull
@Override
public ParameterValue createValue(StaplerRequest staplerRequest, JSONObject jsonObject)
```

参数jsonObject中有name和value连个页面元素的值，这样就可以创建用于脚本使用的变量和值的健值对了。

总结：

SlaveParameterDefinition负责创建参数类对象、绑定属性类数据、创建构建参数结果对象（参数值），DescriptorImpl内部静态类负责创建SlaveParameterDefinition对象、实现页面元素的绑定方法、实现页面请求的接口方法、验证用户输入、显示该构建参数的名称。

其他两个类内容很少请看完整的项目代码：

- SlaveParameterValue：是构建的参数的返回值，用参数名称和选择的值组成。
- SlaveParameterRebuild：是在构建结果“参数”页面查看的内容。

因为Jenkins已经取消了术语Slave，所以项目改名为Agent Server Parameter。

[完整项目代码:jenkinsci/agent-server-parameter-plugin](https://github.com/jenkinsci/agent-server-parameter-plugin)

## 测试

添加构建参数:

![Alt text](http://static.bluersw.com/images/Jenkins/sssp-10.png)

构建脚本：

```groovy
node{
    print params['slave-name']
}
```

选择构建服务器:

![Alt text](http://static.bluersw.com/images/Jenkins/sssp-12.png)

构建结果:

```text
由用户 unknown or anonymous 启动
Running in Durability level: MAX_SURVIVABILITY
[Pipeline] Start of Pipeline
[Pipeline] node
Running on Jenkins in /Users/sunweisheng/Documents/HomeCode/slave-server-parameter-plugin/work/workspace/Test-pip
[Pipeline] {
[Pipeline] echo
master
[Pipeline] }
[Pipeline] // node
[Pipeline] End of Pipeline
Finished: SUCCESS
```

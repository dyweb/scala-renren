# scarenren

scarenren是一个基于scala的人人分析工具

## 构建

项目使用sbt构建，构建环境为`jdk1.7 scala2.11 sbt 0.13.8`

## 输出

### 好友学校统计

在生成的`Output-Data.txt`中

### 好友关系图

以JFrame的形式展示，并生成在`headless_simple.png`和`headless_simple.svg`中

### 文本格式的好友关系

在`network.txt`中，是语言无关的，可以基于该文本做很多事情。其格式为

	-Friend(uid, location, name, url)  // 1
	--Friend(uid, location, name, url) // 2
	--Friend(uid, location, name, url) // 3
	...
	-Friend(uid, location, name, url)  // 2
	--Friend(uid, location, name, url) // 5

其中-代表是节点，--代表由之前-节点指向该--节点的边。比如在此例中，行1为一个节点，行2代表一个由行1节点指向行2节点的一条边。

PS：存储方式略显拙计。

## 使用

首先，新建`userinfo.ini`文件，内容与`userinfo.ini.example`内类似，用于爬取。

然后，编译并运行需要指令`sbt run`，代码只在os x环境下使用过，不知道其他系统会不会有问题，第一次编译可能需要时间比较长，需要下载各种库，之后会快很多。

PS: gephi在导出的时候，会有报错，形如
	
	java.lang.InterruptedException: sleep interrupted
		at java.lang.Thread.sleep(Native Method)
		at org.gephi.data.attributes.event.AttributeEventManager.run(AttributeEventManager.java:87)
		at java.lang.Thread.run(Thread.java:745)
	java.lang.InterruptedException
		at java.lang.Object.wait(Native Method)
		at java.lang.Object.wait(Object.java:503)
		at org.gephi.graph.dhns.core.GraphStructure$ViewDestructorThread.run(GraphStructure.java:240)

但是对结果没有影响，run的结果还会是success。这些异常似乎catch不住，有待解决。

## 代码阅读指南（程序执行指南）

程序最主要的类是在renren.scala中，首先程序会读取在`userinfo.ini`中的用户名密码，然后取得用户的好友列表，最后根据好友列表，生成好友关系图。样例如下，原本是带有名字标签在图上的，但为了保护隐私，已和谐。

<figure>
	<img src="http://gaocegege.com/scala-renren/example.png" height="450">
</figure>

另外在画图的时候使用了gephi-toolkit，现行的情况是，根据[Modularity](https://en.wikipedia.org/wiki/Modularity_\(networks\))来决定节点的颜色，节点度来决定节点的大小。

## 未完成

* 优化friendnetwork.scala中的marshal，不需要读入文件时都存入内存，边读入边遍历，可使用解释器模式
* grapher.scala优化画图风格
* 寻找更合适的actor使用方法，以及如何terminate一个actor
# scarenren

scarenren是一个基于scala的人人分析工具

# 构建

项目使用sbt构建，构建环境为jdk1.7 scala2.11 sbt 0.13.8

# 使用

项目主要是

# 未完成

* 优化friendnetwork.scala中的marshal，不需要读入文件时都存入内存，边读入边遍历
* grapher.scala优化画图风格
* actor效率分析，寻找更合适的actor使用方法
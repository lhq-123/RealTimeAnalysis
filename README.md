# RealTimeAnalysis
基于Flink+Redis的实时数据分析

启动前端

```properties
去到HTML对应的路径下
npm run dev
```

启动后端

```properties
启动WebApplication.java
```

启动Flink

```properties
1.保证集群中zookeeper、Kafka正常启动着
2.启动CreateKafkaMsg.java
3.启动StartFlinkKafkaServer.java
```

代码结构:

```
├─HTML                                 #前端页面
│  ├─node_modules                      #前端所需要的包
│  ├─src
│  └─static
├─logs                                 #后端运行日志
├─RealTimeProcessing                   #流处理程序
│  ├─src
│  │  ├─main
│  │  │  ├─java
│  │  │  │  └─com
│  │  │  │      └─alex
│  │  │  │          ├─bean              #订单实体类
│  │  │  │          ├─mock              #数据模拟器并发送到kafka
│  │  │  │          ├─service           #Flink公共算子
│  │  │  │          ├─sink              #数据处理完sink到Redis
│  │  │  │          ├─source            #读取kafka的数据
│  │  │  │          └─task              #Flink消费逻辑
│  │  │  └─resources
│  │  └─test
│  │      └─java
└─Web                                   
    ├─src
    │  ├─main
    │  │  ├─java
    │  │  │  └─com
    │  │  │      └─alex
    │  │  │          └─web
    │  │  │              ├─bean
    │  │  │              ├─config
    │  │  │              ├─controller
    │  │  │              ├─exception
    │  │  │              ├─service
    │  │  │              └─util
    |  |  |              └─WebApplication #后端主程序     
    │  │  └─resources
```

后端:

![Snipaste_2022-12-27_21-52-39](assets/Snipaste_2022-12-27_21-52-39.png)

前端:

![Snipaste_2022-12-27_21-52-54](assets/Snipaste_2022-12-27_21-52-54.png)

模拟数据程序:

![Snipaste_2022-12-27_21-52-28](assets/Snipaste_2022-12-27_21-52-28.png)

Flink程序:

![Snipaste_2022-12-27_21-52-17](assets/Snipaste_2022-12-27_21-52-17.png)

实时大屏: 

![Snipaste_2022-12-28_18-25-41](assets/Snipaste_2022-12-28_18-25-41.png)

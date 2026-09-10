# 红柳煤矿智慧矿山管控平台项目

> 相关链接：
> * [山西保德煤矿AI智能视频分析平台-腾讯文档协作空间](https://docs.qq.com/document/DUGNVbkRHeHNnZnlz)
> * [Coding项目管理](https://adv-dlut.coding.net/)
> * [Gitlab项目地址（仅限校园网访问）](http://210.30.97.174:31000/)
> * [Apipost-Api文档、调试-邀请链接（密码987654321）](https://console-docs.apipost.cn/preview/59c8e6b10fed4b37/470637884d5e4e10)
> * [语雀协作（2期）](https://elemdatum.yuque.com/dashboard)

> 管理链接：
> * [Gitlab项目地址（仅限校园网访问）](http://210.30.97.174:31000/)
> * [Nacos服务注册、配置中心管理后台（仅限连接VPN后访问）用户名 nacos 密码 nacos](http://nacos-cs.mine-smart.192.168.31.182.nip.io:32000/nacos/)
> * [RocketMQ-dashboard（仅限连接VPN后访问）](http://rocketmq-dashboard.mine-smart.192.168.31.182.nip.io:32000/)

## 功能模块

|     名称      |            模块名            |     包名     |   缩写   |         可独立运行          |              模块说明              |
|:-----------:|:-------------------------:|:----------:|:------:|:----------------------:|:------------------------------:|
|    公共代码库    |        mineai-core        |    core    |   -    |           ✖            | [文档](mineai-core/README.md) |
|    系统服务     |       mineai-system       |   system   | **SS** |           ✔            | [文档](mineai-core/README.md) |
|  监控设备纳管服务   |  mineai-monitor-accessor  |  monitor   | **MA** |           ✔            | [文档](mineai-core/README.md) |
|  工作服务器纳管服务  |   mineai-worker-manager   |   worker   | **WM** |           ✔            | [文档](mineai-core/README.md) |
|   算法纳管服务    |   mineai-model-manager    |   model    | **MM** |       ✔                | [文档](mineai-core/README.md) |
|   数据纳管服务    |    mineai-data-manager    |    data    | **DM** |           ✔            | [文档](mineai-core/README.md) |
| 算力控制器纳管服务 | mineai-controller-manager | controller | **CM** |           ✔            | [文档](mineai-core/README.md) |
|     前端      |         mineai-ui         |     -      |   -    |           ✔            | [文档](mineai-core/README.md) |

## 技术选型

TODO

## 连接配置

### Mysql

|   key    |                                value                                |
|:--------:|:-------------------------------------------------------------------:|
|    IP    |                           192.168.31.182                            |
|   port   |                                30100                                |
| database | mineai (dev)<br/>mineai_test (test)<br/>mineai_prod (prod) |
| username |                                user                                 |
| password |                            Aa123456789.                             |

示例配置文件(``application.yml``)：

```yaml
spring:
  datasource:
    url: jdbc:mysql://192.168.31.182:30100/mineai?useUnicode=true&characterEncoding=utf-8&useSSL=false
    username: user
    password: Aa123456789.
```

### TDEngine

|   key    |     value      |
|:--------:|:--------------:|
|    IP    | 192.168.31.182 |
|   port   |     30200      |
| database |   mineai    |
| username |      root      |
| password |    taosdata    |

示例URL：

```
jdbc:TAOS-RS://192.168.31.182:30200/timezone=UTC-8&charset=UTF-8&locale=en_US.UTF-8
```

示例配置文件(``application.yml``)：

```yaml
# TODO: @qiaohaiyang
```

### Redis

|   key    |               value               |
|:--------:|:---------------------------------:|
|    IP    |          192.168.31.182           |
|   port   |               30002               |
| database | 2 (dev)<br/>1 (test)<br/>0 (prod) |
| password |               (无密码)               |

示例配置文件(``application.yml``)：

```yaml
spring:
  redis:
    # Redis库索引（默认为0）dev为2，test为1，prod为0
    database: 2
    # Redis服务器地址
    host: 192.168.31.182
    # Redis服务端口
    port: 30002
    # Redis连接密码（默认为空）
    password:
    lettuce:
      pool:
        # 连接池最大连接数（默认为8）
        max-active: 8
        # 连接池最大空闲连接（默认为8）
        max-idle: 8
        # 连接池最小空闲连接（默认为0）
        min-idle: 0
        # 连接池最大阻塞等待时间
        max-wait: -1ms
```

### Nacos

|     key     |                       value                        |
|:-----------:|:--------------------------------------------------:|
| server-addr |     nacos-cs.mine-smart.192.168.31.182.nip.io      |
|    port     |                       32000                        |
|  namespace  | DEV_ID (dev)<br/>TEST_ID (test)<br/>PROD_ID (prod) |

示例配置文件(``application.yml``)：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: nacos-cs.mine-smart.192.168.31.182.nip.io:32000
        namespace: DEVAI_S1_SUBSYSTEM_ID
      config:
        server-addr: nacos-cs.mine-smart.192.168.31.182.nip.io:32000
        file-extension: yml
        namespace: DEVAI_S1_SUBSYSTEM_ID
```

### RocketMQ

|     key     |        value         |
|:-----------:|:--------------------:|
| name-server | 192.168.31.182:30001 |

示例配置文件(``application.yml``)：

```yaml
spring:
  cloud:
    stream:
      rocketmq:
        binder:
          name-server: 192.168.31.182:30001 # 不能用服务网关，需要暴露NodePort
```

## 开发示例
> 本节示例均以``mineai-system``模块为例

### 服务配置中心

> 服务配置中心——将原本内嵌于各个模块内部的模块配置抽出，在服务配置中心统一管理，动态下发，统一修改
> * [Nacos管理后台（仅限连接VPN后访问）用户名 nacos 密码 nacos](http://nacos-cs.mine-smart.192.168.31.182.nip.io:32000/nacos/)

[mineai-system/src/main/resources](mineai-system/src/main/resources)
目录内为模块配置文件``application.yml``, ``bootstrap.yml``。

其中 ``bootstrap.yml``在模块启动前优先加载，``application.yml``之后加载。

``bootstrap.yml``存储的是不可变的模块配置，如连接配置中心的地址，
``application.yml``存储的是可变配置，如数据库的连接信息。

模块已配置连接nacos配置中心，**``application.yml``内的可变内容已提取出置于nacos配置中心内**，如需修改配置，请直接前往配置中心修改。

登录nacos后台以后，选择``配置管理 -> 配置列表``， 切换至 ``DEVAI`` 命名空间，对相应的配置文件编辑修改即可。

### 服务调用（OpenFeign）
> 服务调用——模块间以类似于Java原生的函数调用的方式，相互调用不同模块的接口

以
[FeignService.java](mineai-system/src/main/java/org/dlut/adv/mineai/system/service/FeignService.java)
为例，
```java
@Component
@FeignClient(value = "mineai-system")
public interface FeignService {
    @GetMapping("/login/login")
    String login(@RequestParam String username,
                 @RequestParam String password);
}
```

其中``@FeignClient(value = "mineai-system")``指明了要调用的模块的名称，
（在本示例中，为了简单起见，``mineai-system``调用的模块也是``mineai-system``自己， 实际使用中可以替换为其他外部模块名称，
如``mineai-net-manager``）

``@GetMapping("/login/login")``指明了**以GET的方式**调用模块的Restful接口，也就是
[LoginController.java](mineai-system/src/main/java/org/dlut/adv/mineai/system/controller/LoginController.java)
里的``login()``方法。

形参``@RequestParam``会传递给Restful接口，并被接口所自动解析。

使用这段代码，也就把远程的``mineai-system``模块的``/login/login``接口映射为了本地的原生Java方法。

接下来只要调用``FeignService.login()``即可：

```java
class Controller {
    @Resource
    private FeignService feignService;
    
    public void remoteLogin() {
        feignService.login("username1","password1");
    }

}
```

### 消息队列
> 消息队列——模块间实现的订阅-消费算法，一方作为生产者发送消息，另一方作为消费者监听消息的产生并处理

以``mineai-system``（生产者）和``mineai-device-accessor``（消费者）和为例

#### 生产者：
在
[MqService.java](mineai-system/src/main/java/org/dlut/adv/mineai/system/service/MqService.java)
中，通过``streamBridge.send("newUser-out-0", user);`` 完成消息发送。
其中``"newUser-out-0"``为消息队列的代号。不同代号的队列可以认为相互独立。

在``mineai-system``的配置文件中（nacos配置中心的``mineai-system.yml``），需要同时设置好配置：
```yaml
spring:
  cloud:
    stream:
      rocketmq:
        bindings:
          newUser-out-0:
            producer:
              group: dev

      bindings:
        newUser-out-0:
          destination: SS-DA-newUser
          group: dev
```
这里``newUser-out-0``要和上文匹配，``destination: SS-DA-newUser``指明了队列的名称，该名称在消费者也要指定。
> 队列名命名规范：{生产者模块缩写}-{消费者模块缩写}-{代称名}

#### 消费者：
在
[MqService.java](mineai-device-accessor/src/main/java/org/dlut/adv/mineai/device/service/MqService.java)
中，通过函数式编程，实现消费者：
```java
public class MqService {

    @Bean
    Consumer<User> newUser() {
        return user -> {
            System.out.println("消息接收到:" + user.getUsername());
        };
    }
}
```

其中``"newUser-in-0"``为消息队列的代号。但采用函数式编程遵循默认约定，无需显式编写代码。

在``mineai-monitor-accessor``的配置文件中（nacos配置中心的``mineai-monitor-accessor.yml``），需要同时设置好配置：
```yaml
spring:
  stream:
    rocketmq:
      bindings:
        newUser-out-0:
          producer:
            group: dev

    bindings:
      newUser-out-0:
        destination: SS-DA-newUser
        group: dev
```
这里``newUser-in-0``要和上文匹配，``destination: SS-MA-newUser``指明了队列的名称。

> 延伸知识：
>
> ``Consumer``, ``Supplier``和``Function``为Java函数式编程的三个基本单位。
>
> 其中``Consumer``仅有输入，无输出，接入MQ时必须绑定``xxxxx-in-0``；
>
> ``Supplier``仅有输出，无输入，接入MQ时必须绑定``xxxxx-out-0``；
>
> ``Function``既有输入，又有输出，接入MQ时既需要绑定``xxxxx-in-0``，也需要绑定``xxxxx-out-0``；
>
> 通过``Consumer``, ``Supplier``和``Function``的组合使用，可以基于消息队列构造**不同微服务间协作的**处理链：
>
> 产生消息（``Supplier``）-> 处理消息（``Function``）-> 进一步处理消息（``Function``）-> 结束处理（``Consumer``）

由于``Supplier``仅有输出没有输入，一般并不是产生消息的最优选择。
因此本示例采用``streamBridge.send("newUser-out-0", user);``作为消息生产者，能够动态发送``user``对象作为消息体，
更符合我们的实际需求。

### Redis缓存
> Redis为key-value键值存储引擎，内存数据库。使用Redis可以缓存一些频繁查询的数据，以减少重复查询对后端的压力，
> 或缓存需要预先查询的数据，以降低数据读取时的延迟。Redis的k-v也可以在整个后端不同微服务间共享读取，
> 非常适合**登录状态**等全局数据的暂存。

在
[DemoController.java](mineai-system/src/main/java/org/dlut/adv/mineai/system/controller/DemoController.java)
中，定义了一个示例Redis使用方法``insertRedis()``。

在需要使用缓存的位置，声明缓存服务：
```java
@Resource
private RedisService redisService;
```

插入缓存：
```java
redisService.setCacheObject("test-key",user);
```

读取缓存：
```java
User result=redisService.getCacheObject("test-key");
```

删除缓存：
```java
redisService.deleteObject(key);
```

> Redis缓存key命名规范：{缓存写入模块的缩写}-{名称}，如``mineai-system``负责该缓存的
> 写入，则key需要命名为``SS-xxxx``

### mineai-core

该模块非微服务，无法独立运行，负责抽出整个系统中的全局性共享功能、API、接口规范类。

如
[User.java](mineai-core/src/main/java/org/dlut/adv/mineai/core/entity/User.java)
实体类，既需要在``mineai-system``模块增删改查，其他模块也需要，用于鉴权等验证。

因此应抽出于``mineai-core``模块以便所有其他模块均可共享。

类似地，定义了Restful接口的消息传递规范的
[Msg.java](mineai-core/src/main/java/org/dlut/adv/mineai/core/entity/Msg.java)
和消息错误类型基础接口
[MsgCodeInf.java](mineai-core/src/main/java/org/dlut/adv/mineai/core/api/MsgCodeInf.java)
也需要抽出于``mineai-core``模块。

为其他所有模块提供缓存服务的
[RedisService.java](mineai-core/src/main/java/org/dlut/adv/mineai/core/service/RedisService.java)
也需要抽出于``mineai-core``模块。

类似地，日志记录工具类等需要被全系统其他模块调用的工具类，后续也应于该模块内实现。

> 如果引入org.dlut.adv.mineai.core.xxx报错，需要使用IDEA右上角的MAVEN工具，
> 对``mineai-core``模块执行一遍``生命周期 -> install``操作。
> 
> 修改``mineai-core``模块后如果其他依赖模块报错，也需重新执行一次``生命周期 -> install``操作。

### JPA, Entity和Repository
> JPA是ORM框架，即将Java实体类（Java Bean，标有```@Entity``注解的类）与数据库的SQL语句关联的框架。
> 
> 使用JPA，无需直接编写SQL语句。如果想要根据``id``, ``username``等字段查询对应的实体类，直接通过JPA的Java接口
> 查询即可。得到的结果就是实体对象，无需进行手动转换。

JPA实现了``Repository``接口，使得基础的增删改查的工作量进一步降低，开发者只需要通过方法名表达自己想怎么查，
具体代码逻辑的实现完全由JPA自动接管。

例如
[UserRepo.java](mineai-system/src/main/java/org/dlut/adv/mineai/system/repository/UserRepo.java)
中，继承``PagingAndSortingRepository``类可以提供绝大多数我们需要的基本增删改查、分页操作。

如我们想基于``username``和``password``字段，查询数据库中是否有对应的用户记录，
只需要声明``find{类名}By{字段名}``接口函数即可自动实现，也就是``findUserByUsernameAndPassword()``，
无需编写该方法的具体查询代码。

在需要调用该查询的位置，如
[SystemService.java](mineai-system/src/main/java/org/dlut/adv/mineai/system/service/SystemService.java)
，简单调用即可：
```java
userRepo.findUserByUsernameAndPassword(username,password);
```

### MsgCode
为了统一前后端数据传输规范，定义了通用消息传递接口
[Msg.java](mineai-core/src/main/java/org/dlut/adv/mineai/core/entity/Msg.java)
和消息错误类型基础接口
[MsgCodeInf.java](mineai-core/src/main/java/org/dlut/adv/mineai/core/api/MsgCodeInf.java)
。

在不同模块内部，需要定制特定的消息状态码，例如
[MsgCode.java](mineai-system/src/main/java/org/dlut/adv/mineai/system/api/MsgCode.java)
，以便统一管理。

在前端接口Controller类返回值时，必须使用
[Msg.java](mineai-core/src/main/java/org/dlut/adv/mineai/core/entity/Msg.java)
接口作为全系统通用的返回值模板。

例如在
[LoginController.java](mineai-system/src/main/java/org/dlut/adv/mineai/system/controller/LoginController.java)
的``login()``函数中，

如果想要返回“失败”，且不附加其他对象，则应使用
```java
return new Msg<>(MsgCode.LOGIN_USER_NOT_EXIST);
```
如果想要返回“成功”，且附加对象``user``一并返回给前端，则应使用
```java
return new Msg<>(MsgCode.LOGIN_SUCCEED,user);
```

在失败返回值下，前端接收到的json串为：
```json
{"code":"SS_40001","text":"登录失败，用户名或密码错误","payload":null}
```
在成功返回值下，前端接收到的json串的``payload``字段包含了``user``对象的字段。

### 服务网关Gateway
> 服务网关可以视作统一认证的后端入口，同时起到类似于nginx的反向代理的作用
> 
> 在本项目中，后端所有微服务均可多实例运行，其IP和Port是动态变化的。
> 通过服务网关访问，可以动态路由至特定的服务，而无需清楚其IP和Port。

如果不使用服务网关，则每个微服务（以``mineai-system``为例）运行时需要访问``localhost:xxxx``才能访问。

同时启动服务网关后，只需访问``localhost/ss/``即可访问到``mineai-system``模块。

服务网关在Nacos配置中心的配置内容如下：
```yaml
server:
  port: 80
  ssl:
    enabled: false

spring:
  cloud:
    gateway:
      routes:
        - id: mineai-system
          uri: lb://mineai-system
          predicates:
            - Path=/ss/**
          filters:
            - StripPrefix=1

        - id: mineai-monitor-accessor
          uri: lb://mineai-monitor-accessor
          predicates:
            - Path=/ma/**
          filters:
            - StripPrefix=1

        - id: mineai-worker-manager
          uri: lb://mineai-worker-manager
          predicates:
            - Path=/wm/**
          filters:
            - StripPrefix=1

        - id: mineai-controller-manager
          uri: lb://mineai-controller-manager
          predicates:
            - Path=/cm/**
          filters:
            - StripPrefix=1

        - id: mineai-data-manager
          uri: lb://mineai-data-manager
          predicates:
            - Path=/dm/**
          filters:
            - StripPrefix=1

        - id: mineai-model-manager
          uri: lb://mineai-model-manager
          predicates:
            - Path=/mm/**
          filters:
            - StripPrefix=1





        - id: mineai-ui
          uri: http://localhost:3100/ui/
          predicates:
            - Path=/ui/**,/basic-api/**,/upload/**



```

可以看出，不同微服务的访问路径为：

mineai-system -> http://localhost/ss/

mineai-model-manager -> http://localhost/mm/

mineai-data-manager -> http://localhost/dm/

mineai-controller-manager -> http://localhost/cm/

mineai-worker-manager -> http://localhost/wm/

mineai-monitor-accessor -> http://localhost/ma/

mineai-ui -> http://localhost/ui/

topstack（组态） -> http://localhost/ （未匹配到上述任何微服务，缺省默认值走组态）

如需要前后端联动，务必通过服务网关访问前端，从而避免跨域问题。

## 前端说明

前端基于``https://github.com/vbenjs/vue-vben-admin`` 框架实现，位于[mineai-ui](mineai-ui)路径下。

其中页面路由代码位于
[mineai-ui/src/router/routes/modules/mineai](mineai-ui/src/router/routes/modules/mineai)
中，修改或增删文件夹内的代码会自动引起前端左侧菜单边栏的变化。

页面路由代码内``import``指向的页面源码位于
[mineai-ui/src/views/mineai](mineai-ui/src/views/mineai)
内，修改对应文件名的代码，会自动引起对应页面内容的变化。

## 需求迭代说明

甲方有新需求或者发现新bug时，这些任务会统一放在coding之中，请各位同学自行认领，具体流程如下：
1. 进入coding---项目协同---缺陷中，认领任务，选择处理人，将状态改为处理中。
2. 当完成任务之后，将状态改为待验证，接着点击打开事项，点击编辑描述，在 “解决” 下面附上修改截图，在 “代码” 下面附上提交地址。
3. 由相关负责人进行验收，验收通过将状态改为已关闭。

至此，一个任务就算完毕。

有关截止时间：当天的需求，在没有特殊情况下应当在第二天下午6:00之前完成，遇上不明确的问题应及时沟通交流。
有关截止时间：当天的需求，在没有特殊情况下应当在第二天下午6:00之前完成，遇上不明确的问题应及时沟通交流。
# 保德矿AI平台守护进程模块

> 相关链接：
>
> * [Golang Web基础](https://astaxie.gitbooks.io/build-web-application-with-golang/content/zh/01.1.html)
> * [Gin 中文文档](https://gin-gonic.com/zh-cn/docs/)
> * [Go/Gin 环境搭建](https://www.jianshu.com/p/e145f3504322)

## 模块介绍

|    包名    |                          功能说明                          |           示例           |
| :--------: | :--------------------------------------------------------: |:----------------------:|
|     ❎      |            项目启动的入口，开启HTTP服务器和协程            |        main.go         |
|   router   | HTTP的路由服务，嵌套规定访问路径对应的controller和请求方式 |     DemoRouter.go      |
|   model    |      项目中说需要使用的实体Struct(Json),以及静态常量       |        Demo.go         |
|    load    |      工作服务器负载更新功能，提供功能入口TimedFunc()       |        Test.go         |
| controller |            使用gin.Context实现不同的controller             | DemoController.go |
|   config   |              项目的配置文件以及对应的读取方法              |      config.tmol       |

## 服务器 API开发示例

> 本节示例为如何在项目开发一个完整的HTTP的api接口及测试

### 1. 设计路由组

在router包下新建DemoRouter.go, 从gin.Engine中获取路由引擎，并设计好路由组，作为返回值提供给服务器使用

```go
package router

import (
	"github.com/gin-gonic/gin"
	"mineai-worker-server/controller"
)

func DemoRouter() *gin.Engine {
	//获取gin默认路由引擎
	router := gin.Default()
	//规定路由及其对应的handlers也就是controller
	router.GET("/demo", controller.DemoController)
	//也可以设置嵌套的路由组
	DemoGroup := router.Group("/demo")
	{
		//设定组内路由 /demo/get 的controller :ip表示获取url中数据
		DemoGroup.GET("/get/:ip", controller.DemoGetController)

		DemoGroup.POST("/post", controller.DemoPostController)

		//可以继续嵌套
		NextGroup := DemoGroup.Group("/next")
		{
			// 访问路径为： /demo/next/get
			NextGroup.GET("/get", controller.DemoNextGetController)
		}
	}

	return router
}
```

### 2. 设计controller

> 考虑到我们需要使用json传输信息，在go中我们需要使用结构体来映射成json对象进行传输，所以我们需要先在model中建立对应json的结构体

#### 设计model

在model包内新建Demo.go 并设计对应的产量，注意在go中没有结构体常量，所以我们需要设计对应方法来返回固定的结构体 结构体设计注意事项：[stuct 映射 json](https://blog.csdn.net/SiuKong_Ngau/article/details/114373964)

```go
package model

type Demo struct {
	DemoText   string      `json:"demoText" binding:"required"` //json传输过程需要将key值转成小写 并且要求一定不为空
	DemoInt    int         `json:"demoInt,string"`              //将int类型在输出json时转sting
	DemoOption interface{} `json:"demoOption,string,omitempty"` //omitempty表示可选，当value为空是不输出 接口类型 相当于泛型
}

func DemoSuccess() Demo {
	DemoSuccess := Demo{
		DemoText: "接受成功",
		DemoInt:  200,
	}
	return DemoSuccess
}
```

#### 设计contoler方法

在contoller包内新建DemoController.go 实现对应路由的方法

```go
package controller

import (
	"fmt"
	"github.com/gin-gonic/gin"
	"mineai-worker-server/model"
	"net/http"
	"reflect"
)

func DemoController(ctx *gin.Context) {
	//回传字符串
	ctx.String(http.StatusOK, "DemoGet")
}

func DemoGetController(ctx *gin.Context) {
	//从url中取数据
	ip := ctx.Params.ByName("ip")

	//回传json
	//使用gin.H{}直接构造需要的json
	ctx.JSON(http.StatusOK, gin.H{
		"text": ip, "int": 200,
	})

	//使用结构体构造json
	//ctx.JSON(http.StatusOK, model.DemoSuccess())
}

func DemoPostController(ctx *gin.Context) {
	var demo model.Demo
	err := ctx.ShouldBind(&demo)
	if err != nil { //err不为空 说明将json映射成struct失败了 说明Demo中没有demoText 或者为空
		ctx.String(http.StatusBadRequest, `the body format is wrong`)
	} else {
		//遍历结构体对象
		t := reflect.TypeOf(demo)
		v := reflect.ValueOf(demo)
		for i := 0; i < t.NumField(); i++ {
			fmt.Printf("%s -- %v \n", t.Field(i).Tag, v.Field(i).Interface())
		}
		ctx.JSON(http.StatusOK, model.DemoSuccess())
	}
}

func DemoNextGetController(ctx *gin.Context) {
	//从body中读表单数据
	name := ctx.PostForm("name")

	//没有拿到数据
	if name == "" {
		name = "没有设置name"
	}

	//自定义结构体转json
	Demo := model.Demo{
		DemoText:   name,
		DemoInt:    200,
		DemoOption: nil,
	}
	ctx.JSON(http.StatusOK, Demo)
}
```

### 3.启动服务

> 我们需要将DemoRouter() 返回的gin.Enginer 绑定端口上并启动监听服务

```go
package main

import (
	"github.com/gin-gonic/gin"
	"mineai-worker-server/router"
	"net/http"
	"time"
)

func main() {
	//Debug运行模式
	gin.SetMode(gin.DebugMode)
	//生产运行模式
	//gin.SetMode(gin.ReleaseMode)

	// Listen and Server in localhost:10000
	DemoRouter := router.DemoRouter()
	DemoServer := &http.Server{
		Addr:         ":10000",
		Handler:      DemoRouter,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
	}
	DemoServer.ListenAndServe()
}
```

### 4.测试接口

在当前目录下执行 go run main.go 启动项目，使用ApiPost 测试接口 

1. http://localhost:10000/demo  GET

2. http://localhost:10000/demo/get/192.0.0.1 GET
3. http://localhost:10000/demo/next/get   GET 设置body表单格式数据 name
4. http://localhost:10000/demo/post POST 设置body为  Demo结构体json，并且一定要提供demoText字段 如果有其他字段和结构体一致 需要确保数据类型相同 成功 控制到会有结构体数据输出

## 访问Spring开发示例

> 本节示例为如何在项目中访问微服务后端接口 load包为WM模块相关内容 我们以访问http://localhost/wm/test/addWorker 为例开发

#### 1.微服务对应接口

```java
package org.dlut.adv.mineai.worker.controller;

import com.alibaba.fastjson.JSON;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.core.entity.Worker;
import org.dlut.adv.mineai.worker.api.MsgCode;
import org.dlut.adv.mineai.worker.entity.WorkerLoad;
import org.springframework.web.bind.annotation.*;


/**
 * @author dingyadong
 */
@RestController
@RequestMapping("/test")
public class TestController {
    @RequestMapping("/addWorker")
    public Msg<String> add(@RequestBody Worker worker) {
        String workerJson = JSON.toJSONString(worker);
        System.out.println(workerJson);
        return new Msg<>(MsgCode.SUCCEED);
    }

}
```

#### 2. 设计发送请求方法 

在load包中新建test.go 设计发送http请求的方法 TestAddWorker()

```go
package load

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"mineai-worker-server/model"
	"net/http"
	"time"
)

func TestAddWorker() {
	//访问后端上传worker测试
	url := "http://localhost/wm/test/addWorker"
	//接口需要的参数对应的接口体 这里同样需要在model中设计对应的struct
	worker := model.Worker{Ip: "192.0.0.10", Address: "Gin_test", Port: 22, CpuInfo: "Gin_cpu", CpuCores: 8, CpuNum: 2, GpuInfo: "Gin_gpu", GpuNum: 2, GpuMemorySize: 12,
		RamSize: 16, DiskSize: 600, Status: model.WorkerOn, LastModifiedTime: time.Now().Format("2006-01-02 15:04:05")}
	//JSON序列化
	Data, _ := json.Marshal(worker)
	param := bytes.NewBuffer(Data)
	//构建http请求
	client := &http.Client{}
	req, err := http.NewRequest("POST", url, param)
	if err != nil {
		fmt.Println(err)
	} else {
		//发送请求
		req.Header.Add("Content-Type", "application/json")
		res, err := client.Do(req)
		if err != nil {
			fmt.Println(err)
		} else {
			//返回结果
			body, err := io.ReadAll(res.Body)
			if err != nil {
				fmt.Println(err)
			} else {
				fmt.Println(string(body))
			}
			res.Body.Close()
		}
	}
}

```

### 3.开启协程

在main.go中开启一个协程 来执行TestAddWorker() 定时任务可以放到在TimedFunc()中执行

```go
package main

import (
	"github.com/gin-gonic/gin"
	"mineai-worker-server/load"
	"mineai-worker-server/router"
	"net/http"
	"time"
)

func main() {
	//开启一个协程执行TimeFunc
	go load.TimedFunc(5)
}
```

```go
package load

import (
   "fmt"
   "time"
)

func TimedFunc(num int) {
   //利用计时器定时执行任务
   for range time.Tick(time.Second * time.Duration(num)) {
      fmt.Println(time.Now().Format("2006-01-02 15:04:05"))
      //每5s访问一次微服务的接口
      TestAddWorker()
   }
}
```
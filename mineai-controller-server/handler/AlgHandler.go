package handler

import (
	"fmt"
	"github.com/gin-gonic/gin"
	"mineai-controller-server/client"
	"mineai-controller-server/model"
	"mineai-controller-server/orm"
	"mineai-controller-server/orm/entity"
	"net/http"
)

// ModelHeartbeatHandler 处理接收算法的心跳接口 并进行配置下发
func ModelHeartbeatHandler(ctx *gin.Context) {
	var heartBeat model.HeartBeat
	err := ctx.ShouldBindJSON(&heartBeat)
	if err != nil {
		fmt.Println(err)
		ctx.JSON(http.StatusOK, model.ErrorMsg(err.Error()))
		return
	}
	fmt.Println("Receive Heartbeat:")
	fmt.Println(heartBeat.String())
	var flag = true
	var service entity.Service
	for _, value := range heartBeat.Payload.AlgNameList {
		service = orm.GetService(value)
		if len(service.ServiceName) != 0 {
			flag = false
			break
		}
	}
	if flag {
		ctx.JSON(http.StatusOK, model.ErrorMsg("算法名称字段错误"))
		fmt.Println("算法名称字段错误1")
		return
	}
	//更新心跳时间
	if !orm.UpdateHeartBeatTime(service.ServiceName) {
		ctx.JSON(http.StatusOK, model.ErrorMsg("算法名称字段错误"))
		fmt.Println("算法名称字段错误2")
		return
	}
	//如果当前算法模型进程未启动需要更新端口号 开启心跳检测 以及配置初始化
	if heartBeat.Payload.Started == false {
		if orm.UpdateApiPort(service.ID, heartBeat.Payload.ApiPort) == false {
			ctx.JSON(http.StatusOK, model.ErrorMsg("服务端口冲突"))
			fmt.Println("服务端口冲突")
			return
		}
		//向指定服务下发初始算法配置
		go client.ConfigFirstSend(service.ID)
		//开启心跳检测
		//go util.HeartBeatDetection(service.ServiceName, heartBeat.Payload.Interval)
	}
	ctx.JSON(http.StatusOK, model.SuccessMsg())
}

// ConfigUploadHandler 处理算法模型进程配置上报请求
func ConfigUploadHandler(ctx *gin.Context) {
	var configUpload model.ConfigUpload
	err := ctx.ShouldBindJSON(&configUpload)
	if err != nil {
		fmt.Println(err)
		ctx.JSON(http.StatusOK, model.ErrorMsg(err.Error()))
		return
	}
	fmt.Println("Receive ConfigUpload:")
	fmt.Println(configUpload.String())
	//算法模型配置上报平台
	go client.UploadConfigUpdate(configUpload)
	ctx.JSON(http.StatusOK, model.SuccessMsg())
}

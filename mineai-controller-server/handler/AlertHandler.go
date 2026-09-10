package handler

//handler包 用于定义gin处理请求的函数

import (
	"fmt"
	"github.com/gin-gonic/gin"
	"mineai-controller-server/client"
	"mineai-controller-server/model"
	"net/http"
)

// AlertHandler 处理报警上传接口，接收算法模型进程的报警信息
func AlertHandler(ctx *gin.Context) {
	var alertWarn model.AlertWarn
	err := ctx.ShouldBindJSON(&alertWarn)
	if err != nil {
		fmt.Println(err)
		ctx.JSON(http.StatusOK, model.ErrorMsg(err.Error()))
		return
	}
	fmt.Println("Receive Alert:")
	fmt.Println(alertWarn.String())
	//上传报警信息到中央服务器 同时上传文件
	go client.UploadAlter(alertWarn)
	ctx.JSON(http.StatusOK, model.SuccessMsg())
}

package router

import (
	"github.com/gin-gonic/gin"
	"mineai-controller-server/handler"
)

// 平台访问相关路由
func setControllerRouter(router *gin.Engine) *gin.Engine {
	ControllerGroup := router.Group("/controller")
	{
		//控制器负载信息
		ControllerGroup.GET("/load", handler.CenterServerGetLoadHandler)
		//平台心跳接口
		ControllerGroup.GET("/heartBeat", handler.CenterServerHeartbeatHandler)
		//平台下发镜像、权重并启动docker
		ControllerGroup.POST("/runDocker", handler.CenterServerRunDockerHandler)
		//平台查询gpu接口
		ControllerGroup.GET("/getGpuInfo", handler.GetGpuInfoHandler)

		ConfigGroup := ControllerGroup.Group("/config")
		{
			//平台配置下发
			ConfigGroup.POST("/update", handler.ConfigUpdateHandler)
		}
	}

	return router
}

package router

import (
	"github.com/gin-gonic/gin"
	"mineai-controller-server/handler"
)

// 算法模型进程访问相关路由
func setApiRouter(router *gin.Engine) *gin.Engine {
	ApiGroup := router.Group("/api")
	{
		//算法模型报警上传
		ApiGroup.POST("/alert", handler.AlertHandler)

		AlgGroup := ApiGroup.Group("/alg")
		{
			//算法模型心跳接口
			AlgGroup.POST("/heartbeat", handler.ModelHeartbeatHandler)
			//算法模型配置上报
			AlgGroup.POST("/configUpload", handler.ConfigUploadHandler)
		}
	}

	return router
}

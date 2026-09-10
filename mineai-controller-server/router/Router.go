package router

// router包 定义了gin的路由组

import "github.com/gin-gonic/gin"

// GetRouter 设置gin多级路由组
func GetRouter() *gin.Engine {
	router := gin.Default()

	//设置多个路由组
	//接收算法模型进程
	router = setApiRouter(router)
	//接收中央平台
	router = setControllerRouter(router)

	return router
}

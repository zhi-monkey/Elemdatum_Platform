package util

import (
	"fmt"
	"mineai-controller-server/orm"
	"time"
)

// HeartBeatDetection 当第一次收到心跳时开始检测改服务的后续心跳
func HeartBeatDetection(serviceName string, interval int) {
	for range time.Tick(time.Second * time.Duration(interval)) {
		//说明中央服务器删除了该服务不需要监听了
		if !orm.CheckService(serviceName) {
			break
		}
		t := time.UnixMilli(orm.GetHeartBeatTimeByServiceName(serviceName)).Unix()
		//心跳超时
		if (time.Now().Unix() - t) >= int64(interval) {
			fmt.Println(time.Now().Format("2006-01-02 15:04:05") + "\t" + serviceName + "\t" + "异常")
			go RestartService(serviceName)
			//TODO 报告中央服务器
			break
		}
	}
}

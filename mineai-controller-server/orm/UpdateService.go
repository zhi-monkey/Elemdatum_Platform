package orm

import (
	"gorm.io/datatypes"
	"mineai-controller-server/orm/entity"
	"time"
)

// UpdateApiPort 更新service的apiPort
func UpdateApiPort(id uint, port int) bool {
	result := dataBase.Model(&entity.Service{}).
		Where("id = ?", id).
		Update("api_port", port)
	if result.RowsAffected == 0 {
		return false
	}
	return true
}

// UpdateCustomConfig 通过算法和摄像头的名称更新所属的CustomConfig
func UpdateCustomConfig(algName string, monitorName string, customConfig datatypes.JSON) bool {
	result := dataBase.Model(&entity.AlgorithmMonitor{}).
		Where("algorithm_name = ? AND monitor_name = ?", algName, monitorName).
		Update("custom_config", customConfig)
	if result.RowsAffected == 0 {
		return false
	}
	return true
}

// UpdatePushStream 通过算法和摄像头的名称更新所属的PushStream
func UpdatePushStream(algName string, monitorName string, pushStream bool) bool {
	result := dataBase.Model(&entity.AlgorithmMonitor{}).
		Where("algorithm_name = ? AND monitor_name = ?", algName, monitorName).
		Update("push_stream", pushStream)
	if result.RowsAffected == 0 {
		return false
	}
	return true
}

// UpdateHeartBeatTime 更新服务的心跳时间
func UpdateHeartBeatTime(serviceName string) bool {
	result := dataBase.Model(&entity.Service{}).
		Where("service_name", serviceName).
		Update("heart_beat_time", time.Now().UnixMilli()) //最近一次心跳时间
	if result.RowsAffected == 0 {
		return false
	}
	return true
}

// UpdateCenterServerIp 更新中央服务器ip
func UpdateCenterServerIp(address string) {
	var centerServer = entity.CenterServer{
		ID:      1,
		Address: address,
	}
	var oldServer entity.CenterServer
	if dataBase.Model(&entity.CenterServer{}).First(&oldServer, centerServer.ID).Error != nil {
		dataBase.Model(&entity.CenterServer{}).Create(&centerServer)
	} else {
		if address != oldServer.Address {
			dataBase.Model(&entity.CenterServer{}).Where("id = ?", centerServer.ID).Updates(&centerServer)
		}
	}
}

package orm

import (
	"gorm.io/datatypes"
	"gorm.io/gorm/clause"
	"mineai-controller-server/orm/entity"
)

//查询不对数据库产生修改，查询失败则返回的结果为零值

// GetCustomConfig 通过算法和摄像头的名称拿到所属的CustomConfig
func GetCustomConfig(algName string, monitorName string) datatypes.JSON {
	var algorithmMonitor entity.AlgorithmMonitor
	dataBase.Model(&entity.AlgorithmMonitor{}).
		Where("algorithm_name = ? AND monitor_name = ?", algName, monitorName).
		Find(&algorithmMonitor)
	return algorithmMonitor.CustomConfig
}

// GetVideoName 通过算法和摄像头的名称拿到所属的VideoName
func GetVideoName(algName string, monitorName string) string {
	var algorithmMonitor entity.AlgorithmMonitor
	dataBase.Model(&entity.AlgorithmMonitor{}).
		Where("algorithm_name = ? AND monitor_name = ?", algName, monitorName).
		Find(&algorithmMonitor)
	return algorithmMonitor.VideoName
}

// GetPushStream 通过算法和摄像头的名称拿到所属的PushStream
func GetPushStream(algName string, monitorName string) bool {
	var algorithmMonitor entity.AlgorithmMonitor
	dataBase.Model(&entity.AlgorithmMonitor{}).
		Where("algorithm_name = ? AND monitor_name = ?", algName, monitorName).
		Find(&algorithmMonitor)
	return algorithmMonitor.PushStream
}

// GetStreamUrl 通过算法和摄像头的名称拿到所属的StreamUrl
func GetStreamUrl(algName string, monitorName string) string {
	var algorithmMonitor entity.AlgorithmMonitor
	dataBase.Model(&entity.AlgorithmMonitor{}).
		Where("algorithm_name = ? AND monitor_name = ?", algName, monitorName).
		Find(&algorithmMonitor)
	return algorithmMonitor.StreamUrl
}

// GetAllServiceName 拿到所有service的name
func GetAllServiceName() []string {
	var services []entity.Service
	dataBase.Model(&entity.Service{}).
		Select("service_name").
		Find(&services)
	var ServiceNames []string
	for _, value := range services {
		ServiceNames = append(ServiceNames, value.ServiceName)
	}
	return ServiceNames
}

// GetService 根据algName拿到绑定的Service
func GetService(algName string) entity.Service {
	var alg entity.Algorithm
	dataBase.Model(&entity.Algorithm{}).Find(&alg, "name = ?", algName)
	var service entity.Service
	dataBase.Model(&entity.Service{}).Find(&service, alg.ServiceID)
	return service
}

// GetAlgorithmsByServiceId 根据serviceId拿到所有算法 预加载monitor
func GetAlgorithmsByServiceId(id uint) []entity.Algorithm {
	var algorithms []entity.Algorithm
	dataBase.Preload(clause.Associations).
		Where("service_id = ? ", id).
		Find(&algorithms)
	return algorithms
}

// GetServiceByServiceName 根据ServiceName获取service
func GetServiceByServiceName(serviceName string) entity.Service {
	var service entity.Service
	dataBase.Model(&entity.Service{}).Where("service_name", serviceName).Find(&service)
	return service
}

// GetApiPortById  获取service的apiPort
func GetApiPortById(id uint) int {
	var service entity.Service
	dataBase.Model(&entity.Service{}).Find(&service, id)
	return service.ApiPort
}

// GetHeartBeatTimeByServiceName  获取service的heartBeatTime
func GetHeartBeatTimeByServiceName(serviceName string) int64 {
	var service entity.Service
	dataBase.Model(&entity.Service{}).Where("service_name", serviceName).Find(&service)
	return service.HeartBeatTime
}

// CheckService 检查service是否还存在
func CheckService(serviceName string) bool {
	var service entity.Service
	dataBase.Model(&entity.Service{}).Where("service_name", serviceName).Find(&service)
	if len(service.ServiceName) == 0 {
		return false
	}
	return true
}

// GetCenterServerAddress 获取中央服务器ip
func GetCenterServerAddress() string {
	var centerServer entity.CenterServer
	dataBase.Model(&entity.CenterServer{}).First(&centerServer)
	return centerServer.Address
}

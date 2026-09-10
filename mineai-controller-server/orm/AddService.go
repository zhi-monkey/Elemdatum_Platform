package orm

import (
	"gorm.io/datatypes"
	"gorm.io/gorm/clause"
	"mineai-controller-server/orm/entity"
)

func AddService(id uint, name string) {
	var service = entity.Service{
		ID:          id,
		ApiPort:     int(id),
		ServiceName: name,
	}
	dataBase.Model(&entity.Service{}).Clauses(clause.OnConflict{DoNothing: true}).Create(&service)
}

func AddAlgorithm(name string, cfg datatypes.JSON, id uint) {
	var alg = entity.Algorithm{
		Name:       name,
		MainConfig: cfg,
		ServiceID:  id,
	}
	dataBase.Model(&entity.Algorithm{}).Clauses(clause.OnConflict{DoNothing: true}).Create(&alg)
}

func AddMonitor(name string, url string) {
	var monitor = entity.Monitor{
		Name:       name,
		MonitorUrl: url,
	}
	dataBase.Model(&entity.Monitor{}).Clauses(clause.OnConflict{DoNothing: true}).Create(&monitor)
}

func AddAlgorithmMonitor(algName string, monitorName string, videoName string, pushStream bool, streamUrl string, cfg datatypes.JSON) {
	var algorithmMonitor = entity.AlgorithmMonitor{
		AlgorithmName: algName,
		MonitorName:   monitorName,
		StreamUrl:     streamUrl,
		VideoName:     videoName,
		PushStream:    pushStream,
		CustomConfig:  cfg,
	}
	dataBase.Model(&entity.AlgorithmMonitor{}).Clauses(clause.OnConflict{DoNothing: true}).Create(&algorithmMonitor)
}

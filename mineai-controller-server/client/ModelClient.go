package client

import (
	"fmt"
	"mineai-controller-server/config"
	"mineai-controller-server/model"
	"mineai-controller-server/orm"
	"time"
)

// ConfigFirstSend 初始配置下发
func ConfigFirstSend(id uint) {
	//拿信息
	var algorithms = orm.GetAlgorithmsByServiceId(id)
	//组装下发的结构体
	cfg := model.ConfigFirstSend{
		Code:    0,
		Ts:      time.Now().UnixMilli(),
		Payload: []model.ConfigFirstSendPayload{},
	}
	for _, algorithmValue := range algorithms {
		for _, monitorValue := range algorithmValue.Monitors {
			var algCfg = model.ConfigFirstSendPayload{
				AlgName:      algorithmValue.Name,
				MainConfig:   algorithmValue.MainConfig,
				MonitorName:  monitorValue.Name,
				MonitorUrl:   monitorValue.MonitorUrl,
				StreamUrl:    orm.GetStreamUrl(algorithmValue.Name, monitorValue.Name),
				VideoName:    orm.GetVideoName(algorithmValue.Name, monitorValue.Name),
				PushStream:   orm.GetPushStream(algorithmValue.Name, monitorValue.Name),
				CustomConfig: orm.GetCustomConfig(algorithmValue.Name, monitorValue.Name),
			}
			cfg.Payload = append(cfg.Payload, algCfg)
		}
	}
	fmt.Println("初始配置下发：")
	fmt.Println(cfg.String())
	restyPost(config.AlgFirstConfigURL(orm.GetApiPortById(id)), cfg)
}

// ConfigUpdate 配置更新（未使用，以 每次重启服务后初始配置下发 代替）
func ConfigUpdate(configUpdatePayLoad []model.ConfigUpdateSendPayLoad) {
	service := orm.GetService(configUpdatePayLoad[0].AlgName)
	var cfg = model.ConfigUpdateSend{
		Code:    0,
		Ts:      time.Now().UnixMilli(),
		Payload: configUpdatePayLoad,
	}
	fmt.Println("配置更新：")
	fmt.Println(cfg.String())
	restyPost(config.AlgUpdateConfigURL(orm.GetApiPortById(service.ID)), cfg)
}

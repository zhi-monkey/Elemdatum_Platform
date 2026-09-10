package entity

import (
	"bytes"
	"encoding/json"
	"fmt"
	"gorm.io/datatypes"
)

type CenterServer struct {
	ID      uint `gorm:"primaryKey"`
	Address string
}

// Service 算法注册为一个系统服务 可以包含多个算法
type Service struct {
	ID            uint   `gorm:"primaryKey"`
	ServiceName   string `gorm:"unique"`
	ApiPort       int    `gorm:"unique"`
	HeartBeatTime int64
	Algorithms    []Algorithm
}

// Algorithm 算法名称全局唯一且只属于一个模型服务 ServiceID 为外键
type Algorithm struct {
	Name       string `gorm:"primaryKey"`
	MainConfig datatypes.JSON
	ServiceID  uint
	Monitors   []Monitor `gorm:"many2many:algorithm_monitors;"`
}

type Monitor struct {
	Name       string `gorm:"primaryKey"`
	MonitorUrl string
}

type AlgorithmMonitor struct {
	AlgorithmName string `gorm:"primaryKey"`
	MonitorName   string `gorm:"primaryKey"`
	VideoName     string
	PushStream    bool
	StreamUrl     string
	CustomConfig  datatypes.JSON
	ModelAlert    []ModelAlert
}

type ModelAlert struct {
	ID                            uint `gorm:"primaryKey"`
	AlgorithmMonitorAlgorithmName string
	AlgorithmMonitorMonitorName   string
	Text                          string
	Image                         bool
	ImagePath                     string
	Video                         bool
	VideoPath                     string
	Audio                         bool
	AudioPath                     string
	Data                          datatypes.JSON
}

func (args *Service) String() string {
	b, err := json.Marshal(*args)
	if err != nil {
		return fmt.Sprintf("%+v", *args)
	}
	var out bytes.Buffer
	err = json.Indent(&out, b, "", "    ")
	if err != nil {
		return fmt.Sprintf("%+v", *args)
	}
	return out.String()
}

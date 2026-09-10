package model

import (
	"bytes"
	"encoding/json"
	"fmt"
	"gorm.io/datatypes"
)

// ConfigFirstSend 初始配置下发的json
type ConfigFirstSend struct {
	Code    int                      `json:"code"`
	Ts      int64                    `json:"ts" binding:"required"`
	Payload []ConfigFirstSendPayload `json:"payload" binding:"required"`
}

type ConfigFirstSendPayload struct {
	AlgName      string         `json:"algName" binding:"required"`
	MainConfig   datatypes.JSON `json:"mainConfig" binding:"required"`
	VideoName    string         `json:"videoName" binding:"required"`
	PushStream   bool           `json:"pushStream" binding:"required"`
	MonitorName  string         `json:"monitorName" binding:"required"`
	MonitorUrl   string         `json:"monitorUrl" binding:"required"`
	StreamUrl    string         `json:"streamUrl" binding:"required"`
	CustomConfig datatypes.JSON `json:"customConfig" binding:"required"`
}

func (args *ConfigFirstSend) String() string {
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

// ConfigUpdateSend 配置更新下发的的json
type ConfigUpdateSend struct {
	Code    int                       `json:"code"`
	Ts      int64                     `json:"ts" binding:"required"`
	Payload []ConfigUpdateSendPayLoad `json:"payload" binding:"required"`
}

type ConfigUpdateSendPayLoad struct {
	AlgName      string         `json:"algName" binding:"required"`
	MonitorName  string         `json:"monitorName" binding:"required"`
	CustomConfig datatypes.JSON `json:"customConfig" binding:"required"`
	MainConfig   datatypes.JSON `json:"mainConfig" binding:"required"`
}

type RunDockerConfig struct {
	WeightUrl   string `json:"weightUrl" binding:"required"`
	ImageName   string `json:"imageName" binding:"required"`
	AlgName     string `json:"algName" binding:"required"`
	DockerShell string `json:"dockerShell" binding:"required"`
	JobId       int64  `json:"jobId" binding:"required"`
}

func (args *ConfigUpdateSend) String() string {
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

func (args *ConfigUpdateSendPayLoad) String() string {
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

// ConfigSync 和中央服务器配置同步时的json
type ConfigSync struct {
	Code    int               `json:"code"`
	Text    string            `json:"text"`
	PayLoad ConfigSyncPayLoad `json:"payLoad"`
}

type ConfigSyncPayLoad struct {
	Service []ConfigSyncPayLoadService `json:"service"`
}

type ConfigSyncPayLoadService struct {
	Name       string                           `json:"name"`
	ConfigList []ConfigSyncPayLoadServiceConfig `json:"configList"`
}

type ConfigSyncPayLoadServiceConfig struct {
	AlgName      string         `json:"algName"`
	MainConfig   datatypes.JSON `json:"mainConfig"`
	VideoName    string         `json:"videoName"`
	PushStream   bool           `json:"pushStream"`
	MonitorName  string         `json:"monitorName"`
	MonitorUrl   string         `json:"monitorUrl"`
	StreamUrl    string         `json:"streamUrl"`
	CustomConfig datatypes.JSON `json:"customConfig"`
}

func (args *ConfigSync) String() string {
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

func (args *ConfigSyncPayLoadService) String() string {
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

package model

import (
	"bytes"
	"encoding/json"
	"fmt"
)

// HeartBeat 解析心跳接口json
type HeartBeat struct {
	Code    int              `json:"code"`
	Ts      int64            `json:"ts" binding:"required"`
	Payload HeartBeatPayLoad `json:"payload" binding:"required"`
}

type HeartBeatPayLoad struct {
	Interval     int      `json:"interval" binding:"required"`
	HeartbeatNum int      `json:"heartbeatNum" binding:"min=0,max=255"`
	AlgNameList  []string `json:"algNameList" binding:"required"`
	Started      bool     `json:"started"`
	ApiPort      int      `json:"apiPort" binding:"required"`
}

func (args *HeartBeat) String() string {
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

// ConfigUpload 配置上报json
type ConfigUpload struct {
	Code    int                   `json:"code"`
	Ts      int64                 `json:"ts" binding:"required"`
	Payload []ConfigUploadPayLoad `json:"payload" binding:"required,dive"`
}

type ConfigUploadPayLoad struct {
	AlgName   string    `json:"algName" binding:"required"`
	AlgConfig AlgConfig `json:"algConfig" binding:"required"`
}

type AlgConfig struct {
	AreaLabelingTip string       `json:"areaLabelingTip"`
	CustomAttrs     []CustomAttr `json:"customAttrs" binding:"required,dive"`
}

type CustomAttr struct {
	Field    string `json:"field" binding:"required"`
	Label    string `json:"label,omitempty"`
	Default  string `json:"default,omitempty"`
	Required bool   `json:"required,omitempty"`
	Min      int    `json:"min,omitempty"`
	Max      int    `json:"max,omitempty"`
	Msg      string `json:"msg,omitempty"`
}

func (args *ConfigUpload) String() string {
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

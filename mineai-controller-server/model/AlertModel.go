package model

import (
	"bytes"
	"encoding/json"
	"fmt"
	"gorm.io/datatypes"
)

// AlertWarn 解析报警信息json
type AlertWarn struct {
	Code    int              `json:"code"`
	Ts      int64            `json:"ts" binding:"required"`
	Payload AlertWarnPayLoad `json:"payload" binding:"required"`
}

type AlertWarnPayLoad struct {
	AlgName     string         `json:"algName" binding:"required"`
	MonitorName string         `json:"monitorName" binding:"required"`
	Text        string         `json:"text,omitempty"`
	Image       bool           `json:"image"`
	ImagePath   string         `json:"imagePath,omitempty" binding:"required_with=Image"`
	Video       bool           `json:"video"`
	VideoPath   string         `json:"videoPath,omitempty" binding:"required_with=Video"`
	Audio       bool           `json:"audio"`
	AudioPath   string         `json:"audioPath,omitempty" binding:"required_with=Audio"`
	Data        datatypes.JSON `json:"data" binding:"required"`
}

func (args *AlertWarn) String() string {
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

// AlertFile 解析报警文件
type AlertFile struct {
	ParamName string `json:"paramName"`
	FileName  string `json:"fileName"`
	FilePath  string `json:"filePath"`
}

func (args *AlertFile) String() string {
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

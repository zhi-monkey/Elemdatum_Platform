package model

//model包 定义共用的结构体 用于生成/解析json

import (
	"time"
)

// Msg 响应接口的json
type Msg struct {
	Code int    `json:"code"`
	Ts   int64  `json:"ts"`
	Text string `json:"text,omitempty"`
}

// SuccessMsg 返回正常的消息
func SuccessMsg() Msg {
	SuccessMsg := Msg{
		Code: 0,
		Ts:   time.Now().UnixMilli(),
	}
	return SuccessMsg
}

// ErrorMsg 返回错误的消息
func ErrorMsg(text string) Msg {
	ErrorMsg := Msg{
		Code: -1,
		Ts:   time.Now().UnixMilli(),
		Text: text,
	}
	return ErrorMsg
}

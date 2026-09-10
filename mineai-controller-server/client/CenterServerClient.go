package client

import (
	"encoding/json"
	"fmt"
	"mineai-controller-server/config"
	"mineai-controller-server/model"
	"mineai-controller-server/orm"
	"net/url"
	"strconv"
	"strings"
)

// UploadAlter 上传报警信息到中央服务器 同时上传文件
func UploadAlter(alert model.AlertWarn) {
	url := "http://" + orm.GetCenterServerAddress() + config.CenterServerConfig.Api.AlertAddress
	if alert.Payload.Image == true {
		index := strings.LastIndex(alert.Payload.ImagePath, "/")
		fileName := alert.Payload.ImagePath[index:]
		paramName := getDatasetRootPath() + "/modelAlert"
		UploadFile(model.AlertFile{
			ParamName: paramName,
			FileName:  fileName,
			FilePath:  alert.Payload.ImagePath,
		})
		alert.Payload.ImagePath = "modelAlert" + fileName
	}
	if alert.Payload.Video == true {
		index := strings.LastIndex(alert.Payload.VideoPath, "/")
		fileName := alert.Payload.VideoPath[index:]
		paramName := getDatasetRootPath() + "/modelAlert"
		UploadFile(model.AlertFile{
			ParamName: paramName,
			FileName:  fileName,
			FilePath:  alert.Payload.VideoPath,
		})
		alert.Payload.VideoPath = "modelAlert" + fileName
	}
	if alert.Payload.Audio == true {
		index := strings.LastIndex(alert.Payload.AudioPath, "/")
		fileName := alert.Payload.AudioPath[index:]
		paramName := getDatasetRootPath() + "/modelAlert"
		UploadFile(model.AlertFile{
			ParamName: paramName,
			FileName:  fileName,
			FilePath:  alert.Payload.AudioPath,
		})
		alert.Payload.AudioPath = "modelAlert" + fileName
	}
	fmt.Println(url)
	fmt.Println(alert.String())
	restyPost(url, alert)
}

// UploadFile 上传文件到中央服务器
func UploadFile(fileUpload model.AlertFile) {
	url := "http://" + orm.GetCenterServerAddress() + config.CenterServerConfig.Api.FileUploadAddress
	fmt.Println(url)
	fmt.Println(fileUpload.String())
	restyUpload(url, fileUpload.ParamName, fileUpload.FileName, fileUpload.FilePath)
}

// UploadConfigUpdate 上传算法算法的配置更新到中央服务器（自定义配置上报）
func UploadConfigUpdate(configUpload model.ConfigUpload) {
	url := "http://" + orm.GetCenterServerAddress() + config.CenterServerConfig.Api.ConfigUpdateAddress
	restyPost(url, configUpload)
}

// 获取平台存储数据集的根路径
func getDatasetRootPath() string {
	url := "http://" + orm.GetCenterServerAddress() + config.CenterServerConfig.Api.DatasetRootPathAddress
	var datasetRootPathJson datasetRootPathJson
	err := json.Unmarshal([]byte(restyGet(url).String()), &datasetRootPathJson)
	if err != nil {
		return "/home/dataset"
	}
	return datasetRootPathJson.Payload
}

type datasetRootPathJson struct {
	Code    string `json:"code"`
	Text    string `json:"text"`
	Payload string `json:"payload"`
}

// DockerStartResult 容器启动结果发到中央服务器
func DockerStartResult(id int64, status int) {
	urlStr := "http://" + orm.GetCenterServerAddress()
	params := url.Values{}
	params.Set("id", strconv.FormatInt(id, 10))
	params.Set("status", strconv.Itoa(status))
	urlStr = urlStr + "?" + params.Encode()
	restyGet(urlStr)
}

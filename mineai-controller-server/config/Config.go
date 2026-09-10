package config

import (
	"strconv"
)

//config包 用于定义项目相关的配置信息如端口号 接口地址

// ServerPort gin服务器端口号
const ServerPort string = "8849"

//算法接口

// AlgFirstConfigURL 初始配置下发地址
func AlgFirstConfigURL(port int) string {
	return "http://localhost:" + strconv.Itoa(port) + "/api/configDownload" //itoa int->string
}

// AlgUpdateConfigURL 配置更新下发地址
func AlgUpdateConfigURL(port int) string {
	return "http://localhost:" + strconv.Itoa(port) + "/api/configUpdate"
}

// CenterServer 中央服务器接口路径
type CenterServer struct {
	Api Api
}

type Api struct {
	FileUploadAddress      string
	AlertAddress           string
	ConfigUpdateAddress    string
	DatasetRootPathAddress string
	SetJobStatusByJobId    string
}

// CenterServerConfig 调用平台接口获取上传到中央服务器的文件路径
var CenterServerConfig = CenterServer{
	Api: Api{
		DatasetRootPathAddress: "/dm/dataset/getDatasetRootPath",
		FileUploadAddress:      "/dm/storage/uploadModelAlertFile",
		AlertAddress:           "/ma/daemon/alertUpload",
		ConfigUpdateAddress:    "/ma/daemon/configUpload",
		SetJobStatusByJobId:    "/mm/job/setJobStatusByJobId",
	},
}

// Root 报警文件存储目录
var Root = "~/apps/aiserver/AiServer/log/"

// Size 文件存储阈值，单位为字节
var Size int64 = 10240

// 权重文件目录
var WeightDir = "/root/ptModel/"

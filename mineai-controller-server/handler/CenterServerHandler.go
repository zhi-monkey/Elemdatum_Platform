package handler

import (
	"encoding/csv"
	"encoding/json"
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/disk"
	"github.com/shirou/gopsutil/v3/mem"
	"log"
	"mineai-controller-server/client"
	"mineai-controller-server/config"
	"mineai-controller-server/model"
	"mineai-controller-server/orm"
	"mineai-controller-server/util"
	"net/http"
	"os/exec"
	"path"
	"strconv"
	"strings"
	"time"
)

// ConfigUpdateHandler 接收中央服务器的配置下发，停止服务、更新配置（先删后存）、重启服务（模型发送心跳，进行配置下发）
func ConfigUpdateHandler(ctx *gin.Context) {
	var configSync model.ConfigSync
	err := ctx.ShouldBindJSON(&configSync)
	if err != nil {
		fmt.Println(err)
		ctx.JSON(http.StatusOK, model.ErrorMsg(err.Error()))
		return
	}
	fmt.Println("Receive ConfigUpdate:")
	fmt.Println(configSync.String())
	var code = configSync.Code
	// code=0 需要重启服务
	if code == 0 {
		for _, value := range orm.GetAllServiceName() {
			util.StopService(value)
		}
		orm.DeleteAll()
		for serviceIndex, serviceValue := range configSync.PayLoad.Service {
			orm.AddService(uint(serviceIndex+1), serviceValue.Name)
			for _, configValue := range serviceValue.ConfigList {
				orm.AddAlgorithm(configValue.AlgName, configValue.MainConfig, uint(serviceIndex+1))
				orm.AddMonitor(configValue.MonitorName, configValue.MonitorUrl)
				orm.AddAlgorithmMonitor(configValue.AlgName, configValue.MonitorName, configValue.VideoName, configValue.PushStream, configValue.StreamUrl, configValue.CustomConfig)
			}
		}
		for _, value := range orm.GetAllServiceName() {
			util.StartService(value)
		}
	} else {
		for _, serviceValue := range configSync.PayLoad.Service {
			for _, configValue := range serviceValue.ConfigList {
				orm.UpdatePushStream(configValue.AlgName, configValue.MonitorName, configValue.PushStream)
				orm.UpdateCustomConfig(configValue.AlgName, configValue.MonitorName, configValue.CustomConfig)
			}
			//向指定服务下发初始算法配置
			go client.ConfigFirstSend(orm.GetServiceByServiceName(serviceValue.Name).ID)
		}
	}
	ctx.JSON(http.StatusOK, model.SuccessMsg())
}

// CenterServerHeartbeatHandler  处理来自中央服务器的心跳，获取中央平台ip并存库
func CenterServerHeartbeatHandler(ctx *gin.Context) {
	address, _ := ctx.GetQuery("address")
	orm.UpdateCenterServerIp(address)
	ctx.JSON(http.StatusOK, model.SuccessMsg())
}

// CenterServerGetLoadHandler 中央服务器查询负载信息
func CenterServerGetLoadHandler(ctx *gin.Context) {
	ctx.JSON(http.StatusOK, getLocalLoad())
}

// 获取本地负载信息 单位MB
func getLocalLoad() model.Load {
	cpuInfo, _ := cpu.Info()
	count, _ := cpu.Counts(false)
	cpuPercent, _ := cpu.Percent(time.Duration(time.Second), false)

	memory, _ := mem.VirtualMemory()

	//parts, _ := disk.Partitions(true) //带虚拟分区
	parts, _ := disk.Partitions(false) //物理硬盘

	var diskSize uint64
	var diskUsed uint64
	for _, part := range parts {
		if strings.HasPrefix(part.Mountpoint, "/var") ||
			strings.HasPrefix(part.Mountpoint, "/snap") ||
			strings.HasPrefix(part.Mountpoint, "/etc") ||
			strings.HasPrefix(part.Mountpoint, "/usr") ||
			strings.HasPrefix(part.Mountpoint, "/lib") {
			continue
		}
		diskInfo, _ := disk.Usage(part.Mountpoint)
		if diskInfo != nil {
			diskSize += diskInfo.Total
			diskUsed += diskInfo.Used
		}
		fmt.Println("分区:", part.Mountpoint, " 总大小(GB):", int64(diskInfo.Total/1024/1024/1024), " 已使用(GB):", int64(diskInfo.Used/1024/1024/1024))
	}

	return model.Load{
		CpuInfo:        cpuInfo[0].ModelName,
		CpuCores:       count,
		CpuUsedPercent: float32(cpuPercent[0]),
		RamSize:        int64(memory.Total / 1024 / 1024),
		RamUsed:        int64(memory.Used / 1024 / 1024),
		DiskSize:       int64(diskSize / 1024 / 1024),
		DiskUsed:       int64(diskUsed / 1024 / 1024),
	}
}

// CenterServerRunDockerHandler 中央服务器下发镜像并启动
func CenterServerRunDockerHandler(ctx *gin.Context) {
	var runDockerConfig model.RunDockerConfig
	err := ctx.ShouldBindJSON(&runDockerConfig)
	if err != nil {
		fmt.Println(err)
		ctx.JSON(http.StatusBadRequest, model.ErrorMsg("传参格式错误"+err.Error()))
		return
	}
	fmt.Println("Receive RunDockerConfig:")
	jsonStr, _ := json.Marshal(runDockerConfig)
	fmt.Println(string(jsonStr))
	//拉取权重文件
	url := runDockerConfig.WeightUrl
	filePath := path.Join(config.WeightDir, runDockerConfig.AlgName+".pt")
	err = util.DownloadFile(url, filePath)
	if err != nil {
		fmt.Println(err)
		ctx.JSON(http.StatusServiceUnavailable, model.ErrorMsg("下载文件失败"+err.Error()))
		return
	}
	// 停下并删除所有容器
	err = util.StopAndDeleteAllDockers()
	if err != nil {
		fmt.Println(err)
		ctx.JSON(http.StatusServiceUnavailable, model.ErrorMsg("删除容器失败"+err.Error()))
		return
	}
	// 拉取镜像
	var imageName = runDockerConfig.ImageName
	err = util.PullDocker(imageName)
	if err != nil {
		fmt.Println(err)
		ctx.JSON(http.StatusServiceUnavailable, model.ErrorMsg("镜像拉取失败"+err.Error()))
		return
	}
	// 启动容器
	var dockerShell = runDockerConfig.DockerShell
	go func() {
		flag := util.StartDocker(dockerShell)
		if flag == true {
			client.DockerStartResult(runDockerConfig.JobId, 1)
		} else {
			client.DockerStartResult(runDockerConfig.JobId, -1)
		}
	}()
	ctx.JSON(http.StatusOK, model.SuccessMsg())
}

// GetGpuInfoHandler  处理来自中央服务器的gpu查询
func GetGpuInfoHandler(ctx *gin.Context) {
	ctx.JSON(http.StatusOK, GetGpuInfo())
}

func GetGpuInfo() model.GpuInfosPayload {
	// 列出nvidia显卡信息
	cmd := exec.Command(
		"/bin/sh",
		"-c",
		"nvidia-smi --query-gpu=index,name,memory.free,memory.total,utilization.gpu --format=csv",
	)
	output, err := cmd.CombinedOutput()
	if err != nil {
		log.Printf("Error running nvidia-smi: %v\n", err)
		return model.GpuInfosPayload{}
	}

	// 去除输出中的BOM（如果存在），以免影响CSV解析
	outputStr := strings.TrimPrefix(string(output), "\uFEFF")

	// 初始化CSV阅读器
	reader := csv.NewReader(strings.NewReader(outputStr))

	// 跳过标题行（如果需要）
	reader.Read()

	// 读取CSV数据并填充到结构体数组中
	var gpuInfos []model.GpuInfo
	for {
		record, err := reader.Read()
		if err == nil {
			index, _ := strconv.Atoi(strings.TrimSpace(record[0]))
			//切分MiB并且去掉前后空格
			free, _ := strconv.Atoi(strings.TrimSpace(strings.Split(record[2], "MiB")[0]))
			total, _ := strconv.Atoi(strings.TrimSpace(strings.Split(record[3], "MiB")[0]))
			utilization, _ := strconv.Atoi(strings.TrimSpace(strings.Split(record[4], "%")[0]))
			// 创建GPUInfo实例并添加到数组
			gpuInfos = append(gpuInfos, model.GpuInfo{
				Index:       index,
				Name:        strings.TrimSpace(record[1]),
				Free:        free,
				Total:       total,
				Utilization: utilization,
			})
		} else if err == csv.ErrFieldCount || err == csv.ErrTrailingComma {
			log.Println("处理错误，可能是格式问题")
			continue
		} else {
			break // 读取完毕
		}
	}
	//如果数组为空
	if gpuInfos == nil || len(gpuInfos) == 0 {
		return model.GpuInfosPayload{}
	} else {
		return model.GpuInfosPayload{
			GpuInfos:  gpuInfos,
			Timestamp: time.Now().Unix(),
		}
	}
}

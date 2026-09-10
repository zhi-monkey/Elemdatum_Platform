package model

type Load struct {
	CpuInfo        string  `json:"cpuInfo"`
	CpuCores       int     `json:"cpuCores"`
	CpuUsedPercent float32 `json:"cpuUsedPercent"`
	RamSize        int64   `json:"ramSize"`
	RamUsed        int64   `json:"ramUsed"`
	DiskSize       int64   `json:"diskSize"`
	DiskUsed       int64   `json:"diskUsed"`
}

type GpuInfo struct {
	Index       int    `json:"index"`
	Name        string `json:"name"`
	Free        int    `json:"free"`
	Total       int    `json:"total"`
	Utilization int    `json:"utilization"`
}

type GpuInfosPayload struct {
	GpuInfos  []GpuInfo `json:"gpuInfos"`
	Timestamp int64     `json:"timestamp"`
}

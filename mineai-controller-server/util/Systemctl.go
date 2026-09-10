package util

import (
	"bytes"
	"fmt"
	"mineai-controller-server/orm"
	"os/exec"
	"strings"
)

// todo：k8s，docker启动、停止、重启
func SystemStart() {
	var serviceNames = orm.GetAllServiceName()
	for index, value := range serviceNames {
		fmt.Println(index)
		fmt.Println("启动" + value)
		if !StartService(value) {
			fmt.Println(value + "启动失败")
		}
	}
}

func StartService(serviceName string) bool {
	fmt.Println("启动" + serviceName)
	cmd := exec.Command("/bin/sh", "-c", "systemctl start "+serviceName)
	err := cmd.Run()
	if err != nil {
		fmt.Println(serviceName + "启动失败")
		return false
	} else {
		return true
	}
}

func StopService(serviceName string) bool {
	fmt.Println("停止" + serviceName)
	cmd := exec.Command("/bin/sh", "-c", "systemctl stop "+serviceName)
	err := cmd.Run()
	if err != nil {
		return false
	} else {
		return true
	}
}

func RestartService(serviceName string) bool {
	cmd := exec.Command("/bin/sh", "-c", "systemctl restart "+serviceName)
	err := cmd.Run()
	if err != nil {
		fmt.Println(serviceName + "重启失败")
		return false
	} else {
		return true
	}
}
func StartDocker(dockerShell string) bool {
	fmt.Println("开始启动" + dockerShell)
	cmd := exec.Command("/bin/sh", "-c", dockerShell)
	err := cmd.Run()
	if err != nil {
		fmt.Println("启动失败" + dockerShell)
		return false
	} else {
		return true
	}
}

func StopDocker(containerName string) bool {
	fmt.Println("停止" + containerName)
	cmd := exec.Command("/bin/sh", "-c", "docker stop "+containerName)
	err := cmd.Run()
	if err != nil {
		return false
	} else {
		return true
	}
}

func RestartDocker(containerName string) bool {
	fmt.Println("重启" + containerName)
	cmd := exec.Command("/bin/sh", "-c", "docker restart "+containerName)
	err := cmd.Run()
	if err != nil {
		fmt.Println(containerName + "重启失败")
		return false
	} else {
		return true
	}
}

func PullDocker(imageName string) error {
	// 列出已安装的镜像
	cmd0 := exec.Command("/bin/sh", "-c", "docker images --format {{.Repository}}:{{.Tag}}")
	var out bytes.Buffer
	var stderr bytes.Buffer
	cmd0.Stdout = &out
	cmd0.Stderr = &stderr
	err0 := cmd0.Run()
	if err0 != nil {
		fmt.Println(fmt.Sprint(err0) + ": " + stderr.String())
		return err0
	}
	outString := out.String()
	fmt.Println("Result: " + outString)
	// 将输出字节转换为字符串，并检查是否包含目标镜像信息
	if strings.Contains(outString, imageName) {
		return nil
	}
	fmt.Println("开始拉取远程镜像" + imageName)
	cmd := exec.Command("/bin/sh", "-c", "docker pull "+imageName)
	err := cmd.Run()
	if err != nil {
		return err
	} else {
		return nil
	}
}

func StopAndDeleteAllDockers() error {
	cmd0 := exec.Command("/bin/sh", "-c", "docker ps -aq")
	var out bytes.Buffer
	var stderr bytes.Buffer
	cmd0.Stdout = &out
	cmd0.Stderr = &stderr
	err0 := cmd0.Run()
	if err0 != nil {
		fmt.Println(fmt.Sprint(err0) + ": " + stderr.String())
		return err0
	}
	fmt.Println("Result: " + out.String())
	if len(strings.TrimSpace(out.String())) == 0 {
		return nil
	}
	fmt.Println("开始停止所有容器")
	cmd1 := exec.Command("/bin/sh", "-c", "docker stop $(docker ps -aq)")
	err1 := cmd1.Run()
	if err1 != nil {
		return err1
	}
	fmt.Println("开始删除所有容器")
	cmd2 := exec.Command("/bin/sh", "-c", "docker rm $(docker ps -aq)")
	err2 := cmd2.Run()
	if err2 != nil {
		return err2
	}
	return nil
}

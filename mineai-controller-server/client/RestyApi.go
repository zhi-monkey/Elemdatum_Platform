package client

//client包 用于发送请求调用外部接口

import (
	"fmt"
	"github.com/go-resty/resty/v2"
	"gorm.io/datatypes"
)

// restyPost 使用Resty发送Post请求
func restyPost(url string, content interface{}) {
	client := resty.New()
	resp, err := client.R().
		SetBody(content).
		Post(url)
	fmt.Println("Response Info:")
	fmt.Println("  Error      :", err)
	fmt.Println("  Status Code:", resp.StatusCode())
	fmt.Println("  Body       :\n", resp)
}

// restyUpload 使用Resty上传文件
func restyUpload(url string, paramName string, fileName string, filePath string) {
	client := resty.New()
	resp, err := client.R().
		SetFile("file", filePath).
		Post(url + "?path=" + paramName + "&objectName=" + fileName)
	defer func() {
		if err := recover(); err != any(nil) {
			fmt.Println(filePath + "文件读取失败")
		}
	}()
	fmt.Println("Response Info:")
	fmt.Println("  Error      :", err)
	fmt.Println("  Status Code:", resp.StatusCode())
	fmt.Println("  Body       :\n", resp)
}

// restyGet 使用Resty获取json
func restyGet(url string) datatypes.JSON {
	client := resty.New()
	resp, err := client.R().Get(url)
	fmt.Println("Response Info:")
	fmt.Println("  Error      :", err)
	fmt.Println("  Status Code:", resp.StatusCode())
	fmt.Println("  Body       :\n", resp)
	json, _ := datatypes.JSON.MarshalJSON(resp.Body())
	return json
}

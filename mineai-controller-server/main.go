package main

import (
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/urfave/cli/v2"
	"log"
	"mineai-controller-server/config"
	"mineai-controller-server/orm"
	"mineai-controller-server/router"
	"mineai-controller-server/util"
	"net/http"
	"os"
	"time"
)

func main() {
	var root string
	//var logFile string
	app := &cli.App{
		Flags: []cli.Flag{
			&cli.StringFlag{
				//名称
				Name: "dir",
				//默认值
				Value: config.Root,
				//别名
				Aliases: []string{"d"},
				//释义
				Usage: "`directory` of alert files",
				//变量地址
				Destination: &root,
				//帮助文档默认值
				DefaultText: config.Root,
			},
			//&cli.StringFlag{
			//	//名称
			//	Name: "logFile",
			//	//默认值
			//	Value: path.Join(config.Root, "gin.log"),
			//	//别名
			//	Aliases: []string{"l"},
			//	//释义
			//	Usage: "file path of `logFile`",
			//	//变量地址
			//	Destination: &logFile,
			//	//帮助文档默认值
			//	DefaultText: path.Join(config.Root, "gin.log"),
			//},
		},
		Action: func(c *cli.Context) error {
			// 判读路径是否可达
			_, err := os.Stat(root)
			if os.IsNotExist(err) {
				fmt.Println("报警文件目录不存在")
				return err
			}
			//// 创建日志文件
			//Logfile, err := os.Create("gin.log")
			//if err != nil {
			//	fmt.Println("创建日志文件失败")
			//	return err
			//}
			fmt.Println("报警文件目录: ", root)
			//fmt.Println("日志打印路径: ", logFile)

			orm.SqliteConnect()
			util.SystemStart() //k8s来做
			gin.DisableConsoleColor()
			//Debug运行模式
			gin.SetMode(gin.DebugMode)
			//生产运行模式
			//gin.SetMode(gin.ReleaseMode)

			//定时清理本地文件
			go util.Delete(root)

			//// 同时将日志写入文件和控制台
			//gin.DefaultWriter = io.MultiWriter(Logfile, os.Stdout)

			r := router.GetRouter()
			s := &http.Server{
				Addr:         ":" + config.ServerPort,
				Handler:      r,
				ReadTimeout:  10 * time.Second,
				WriteTimeout: 10 * time.Second,
			}
			_ = s.ListenAndServe()

			return nil
		},
	}
	err := app.Run(os.Args)
	if err != nil {
		log.Fatal(err)
	}

}

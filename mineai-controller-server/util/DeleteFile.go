package util

import (
	"fmt"
	"mineai-controller-server/config"
	"os"
	"path/filepath"
	"sort"
	"time"

	"github.com/robfig/cron/v3"
)

// Delete 定时删除过期文件
func Delete(root string) error {
	// 判读路径是否可达
	_, err := os.Stat(root)
	if os.IsNotExist(err) {
		return err
	}

	//创建定时器，精确到秒
	crontab := cron.New(cron.WithSeconds())

	//定时cron列表，每天凌晨一点进行一次
	if _, err := crontab.AddFunc("0 0 1 * * ?", func() {
		if err := DeleteExpiredFiles(root); err != nil {
			fmt.Println(err)
		}
	}); err != nil {
		fmt.Println("Add Func DeleteByTime fail")
	}

	crontab.Start()
	return nil
}

// GetAllFile 获取所有文件路径
func GetAllFile(root string) (files []string, err error) {
	var filePaths []string
	err = filepath.Walk(root, func(path string, info os.FileInfo, err error) error {
		if !info.IsDir() {
			filePaths = append(filePaths, path)
		}
		return nil
	})

	if err != nil {
		fmt.Println("Read AllFile error")
		return nil, err
	}

	return filePaths, nil
}

// SecondToTime 把秒级的时间戳转为time格式
/*
func SecondToTime(sec int64) time.Time {
	return time.Unix(sec, 0)
}
*/

// DeleteByTime 删除七天前的文件
func DeleteByTime(root string) error {
	FilePaths, err := GetAllFile(root)
	if err != nil {
		fmt.Println("DeleteByTime GetAllFile error")
	}

	DeleteTime := time.Now().AddDate(0, 0, -7)

	for _, fi := range FilePaths {
		FileInfo, err := os.Stat(fi)
		if err != nil {
			fmt.Println("read file-info fail:", err)
		}

		// 获取修改时间
		FileModTime := FileInfo.ModTime()

		if FileModTime.Before(DeleteTime) {
			err := os.Remove(fi)
			if err != nil {
				fmt.Println("remove file error")
			}
		}
	}

	return nil
}

// GetSize 计算文件夹内所有文件占用总空间
func GetSize(root string) (int64, error) {
	var size int64 = 0

	filePaths, err := GetAllFile(root)
	if err != nil {
		fmt.Println("GetSize GetAllFile error")
		return 0, err
	}

	for _, fi := range filePaths {
		fileIn, _ := os.Stat(fi)
		size += fileIn.Size()
	}
	return size, nil
}

type PathTime struct {
	FilePath string
	Time     time.Time
}
type PathTimes []PathTime

func (pt PathTimes) Len() int {
	return len(pt)
}
func (pt PathTimes) Swap(i, j int) {
	pt[i], pt[j] = pt[j], pt[i]
}
func (pt PathTimes) Less(i, j int) bool {
	return pt[i].Time.Before(pt[j].Time)
}

// SortByTime 文件按时间排序
func SortByTime(root string) (FilePaths []string, err error) {
	FilePathTmp, _ := GetAllFile(root)

	var FilePathTimes []PathTime
	for _, fi := range FilePathTmp {
		FileInfo, _ := os.Stat(fi)
		FilePathTimes = append(FilePathTimes, PathTime{fi, FileInfo.ModTime()})
	}

	sort.Sort(PathTimes(FilePathTimes))

	for _, fi := range FilePathTimes {
		FilePaths = append(FilePaths, fi.FilePath)
	}

	return FilePaths, nil
}

// DeleteBySize 文件总大小超过阈值，删除时间靠前文件
func DeleteBySize(root string) error {
	filePaths, err := SortByTime(root)
	if err != nil {
		fmt.Println("DeleteBySize SortByTime error")
	}

	for _, file := range filePaths {
		err := os.Remove(file)
		if err != nil {
			fmt.Println("Remove file error")
			return err
		}
		if _, err := GetSize(root); err != nil {
			fmt.Println("DeleteBySize GetSize error")
			return err
		}
		if size, err := GetSize(root); size < config.Size && err == nil {
			break
		}
	}

	return nil
}

func DeleteDir(root string) error {
	var filePaths []string
	err := filepath.Walk(root, func(path string, info os.FileInfo, err error) error {
		if info.IsDir() {
			filePaths = append(filePaths, path)
		}
		return nil
	})
	if err != nil {
		fmt.Println("Read AllDir error")
		return err
	}

	//从里往外删除空文件夹
	for i := len(filePaths) - 1; i >= 0; i-- {
		var files []string
		files, _ = GetAllFile(filePaths[i])
		if len(files) == 0 {
			if filePaths[i] != root {
				err := os.Remove(filePaths[i])
				if err != nil {
					fmt.Println("RemoveDir error")
				}
			}
		}
	}

	return nil
}

func DeleteExpiredFiles(root string) error {
	if err := DeleteByTime(root); err != nil {
		fmt.Println("DeleteExpiredFiles DeleteByTime error")
		return err
	}

	if _, err := GetSize(root); err != nil {
		fmt.Println("DeleteExpiredFiles GetSize error")
		return err
	}
	if size, err := GetSize(root); size > config.Size && err == nil {
		DeleteBySize(root)
	}

	if err := DeleteDir(root); err != nil {
		fmt.Println("DeleteExpiredFiles DeleteByTime error")
		return err
	}
	return nil
}

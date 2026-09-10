package util

import (
	"io"
	"net/http"
	"os"
)

func DownloadFile(url, filePath string) error {
	out, err := os.Create(filePath)
	if err != nil {
		return err
	}
	defer out.Close()

	resp, err := http.Get(url)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	_, err = io.Copy(out, resp.Body)
	if err != nil {
		return err
	}

	return nil
}

/*
func DownloadFile(url, filePath string) error {
	out, err := os.Create(filePath)
	if err != nil {
		return err
	}
	defer out.Close()

	resp, err := http.Head(url)
	if err != nil {
		return err
	}

	size, err := strconv.Atoi(resp.Header.Get("Content-Length"))
	if err != nil {
		return err
	}

	concurrency := 10 // 并发数

	var bg int64 // 起始位置
	var ed int64 // 结束位置
	for i := 0; i < concurrency; i++ {
		bg = int64(i) * int64(size/concurrency)
		ed = bg + int64(size/concurrency) - 1

		go func(idx int, bg, ed int64) {
			req, _ := http.NewRequest(http.MethodGet, url, nil)
			req.Header.Set("Range", fmt.Sprintf("bytes=%v-%v", bg, ed))

			client := &http.Client{}
			resp, err := client.Do(req)
			if err != nil {
				fmt.Println(err)
			}
			defer resp.Body.Close()

			out.Seek(bg, 0)
			_, err = io.Copy(out, resp.Body)
			if err != nil {
				fmt.Println(err)
			}

			log.Printf("[%d] Done.", idx)
		}(i, bg, ed)
	}

	return nil
}
*/

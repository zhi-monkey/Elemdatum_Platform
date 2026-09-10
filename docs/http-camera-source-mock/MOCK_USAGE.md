# HTTP 模拟服务使用说明

## 一、用途说明

这个 Python 模拟服务用于本地联调 MineAI 数据回流（HTTP 流采集）功能，模拟甲方平台接口：

- 获取流任务列表：`GET /dataclt/rawtask/infos`
- 按流 ID 下载图片压缩包：`GET /dataclt/file/download?dir_name={taskid}&delRaw=true`

其中 `taskid` 作为平台侧“流ID”（后端按这个 ID 匹配）。

服务特性：

- 下载接口返回 zip 压缩包
- 压缩包中图片数量每次可能不同（1-3 张）
- 压缩包中包含多级目录嵌套
- 支持在线动态切换流状态（在线/未工作/已删除/空包）


## 二、环境准备

1. Python 环境

- 建议 Python 3.8+
- 无第三方依赖，使用标准库即可运行

2. 目录与图片

- 服务脚本：`C:\Project\http-camera-source-mock\mock_http_source.py`
- 可选图片目录：`C:\Project\http-camera-source-mock\images`

说明：

- 若 `images` 目录存在 jpg/jpeg/png，下载 zip 会轮询使用这些本地图片
- 若没有图片，则回退到内置小图

3. 认证配置

- 启动时可加 `--auth-token demo-token`
- 调用接口时需带请求头：`Authorization: Bearer demo-token`


## 三、启动方式

### 1) 单实例（推荐）

```bash
python C:\Project\http-camera-source-mock\mock_http_source.py --host 127.0.0.1 --port 18080 --auth-token demo-token --scenario normal
```

### 2) 双端口隔离联调（可选）

```bash
python C:\Project\http-camera-source-mock\mock_http_source.py --host 127.0.0.1 --port 18081 --auth-token demo-token --scenario normal
```


## 四、接口清单

业务接口：

- `GET /dataclt/rawtask/infos`
- `GET /dataclt/file/download?dir_name={taskid}&delRaw=true`

管理接口（用于测试状态切换）：

- `GET /mock/admin/tasks`
- `POST /mock/admin/set-state?taskid=...&running=...&cltstart=...&isdeleted=...&empty=...`
- `POST /mock/admin/reset`


## 五、Windows CMD 下的 curl 用法

注意：下面命令是 **CMD** 语法（不是 PowerShell）。

先设置变量：

```cmd
set BASE=http://127.0.0.1:18081
set TOKEN=demo-token
set TASKID=ab34fb88-331b-11f1-bccd-0242ac110003
```

### 1) 获取流列表

```cmd
curl -H "Authorization: Bearer %TOKEN%" "%BASE%/dataclt/rawtask/infos"
```

### 2) 下载图片压缩包

```cmd
curl -H "Authorization: Bearer %TOKEN%" -o sample.zip "%BASE%/dataclt/file/download?dir_name=%TASKID%&delRaw=true"
```

### 3) 查看当前模拟状态

```cmd
curl -H "Authorization: Bearer %TOKEN%" "%BASE%/mock/admin/tasks"
```


## 六、常用测试场景

### 场景 A：流未工作（应停止采集任务）

```cmd
curl -X POST -H "Authorization: Bearer %TOKEN%" "%BASE%/mock/admin/set-state?taskid=%TASKID%&running=false&cltstart=false"
```

恢复：

```cmd
curl -X POST -H "Authorization: Bearer %TOKEN%" "%BASE%/mock/admin/set-state?taskid=%TASKID%&running=true&cltstart=true&isdeleted=false"
```

### 场景 B：流已删除（应停止采集任务）

```cmd
curl -X POST -H "Authorization: Bearer %TOKEN%" "%BASE%/mock/admin/set-state?taskid=%TASKID%&isdeleted=true"
```

恢复：

```cmd
curl -X POST -H "Authorization: Bearer %TOKEN%" "%BASE%/mock/admin/set-state?taskid=%TASKID%&isdeleted=false&running=true&cltstart=true"
```

### 场景 C：流在线但暂无新图片（不应失败，应继续轮询）

```cmd
curl -X POST -H "Authorization: Bearer %TOKEN%" "%BASE%/mock/admin/set-state?taskid=%TASKID%&running=true&cltstart=true&isdeleted=false&empty=true"
```

恢复正常出图：

```cmd
curl -X POST -H "Authorization: Bearer %TOKEN%" "%BASE%/mock/admin/set-state?taskid=%TASKID%&empty=false"
```


## 七、重置模拟服务状态

```cmd
curl -X POST -H "Authorization: Bearer %TOKEN%" "%BASE%/mock/admin/reset"
```

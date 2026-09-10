# 自迭代训练功能开发指南

> 本文档面向后续接手开发的同学，记录了自迭代训练功能的整体架构设计、数据流、已完成部分、待实现部分以及关键注意事项。
>
> 状态机的详细设计（状态流转图、状态码速查表、事件速查表）见 `docs/self-iteration-state-machine.md`。

---

## 一、功能概述

**自迭代训练**：周期性从摄像头拉取图片 → 自动标注（可选人工审核）→ 训练 → 打包 → 下发模型，形成闭环，让模型随现场数据持续优化。

核心理念：**数据随轮次累积，模型随轮次进化**。每轮新采集的数据都加入训练集，第 N 轮的标注优先使用第 N-1 轮训练出的权重，标注质量也随轮次提升。

---

## 二、整体架构

### 模块归属

```
mineai-core
└── entity/
    ├── SelfIterationTask.java   # 父任务（自迭代训练配置 + 生命周期）
    └── SelfIterationJob.java    # 子任务（一轮迭代）

mineai-model-manager
├── controller/
│   └── SelfIterationController.java        # REST 接口 + 回调接口
├── service/
│   └── SelfIterationService.java           # 核心业务逻辑
├── repository/
│   ├── SelfIterationTaskRepo.java
│   └── SelfIterationJobRepo.java
├── statusMachine/
│   ├── constant/
│   │   ├── SelfIterationTaskStatusConstant.java
│   │   ├── SelfIterationJobStatusConstant.java
│   │   └── SelfIterationStateMachineConstant.java
│   ├── state/
│   │   ├── AbstractSelfIterationTaskState.java
│   │   ├── AbstractSelfIterationJobState.java
│   │   ├── task/  （TaskIdleState、TaskRunningState ...）
│   │   └── job/   （JobCreatedState、JobCollectingState ...）
│   └── statemachine/
│       ├── SelfIterationTaskStateMachine.java
│       └── SelfIterationJobStateMachine.java
├── dto/
│   └── AutoLabelJobRequest.java   # 新增 directWeightPath 字段
├── client/
│   └── DubheDataFeign.java        # 新增两个回流相关 Feign 接口
└── vo/
    └── RtspCaptureExecutionVO.java

docs/
├── self-iteration-state-machine.md   # 状态机详细设计
└── self-iteration-dev-guide.md       # 本文档
```

### 与现有系统的关系

| 现有组件 | 自迭代的使用方式 |
|---------|----------------|
| `RtspCaptureTask` / `RtspCaptureExecution` | 每轮触发 `startCapture`，产出含图片的 Dataset |
| `JobController.createAutoLabelJob` | 自动标注，第1轮 `useDefault=true`，第N轮传入上一轮权重 |
| `ModelJobService.createTrainJob` | 训练，`trainDatasetVersions` 传入历史全量版本 |
| `ModelJobService.createConvertJob` | 模型格式转换 |
| `DatasetMergeRequest` / `datasetAssembleFromMultipleVersionAsync` | 多版本合并后训练 |
| `mineai-package-manager` | 打包（预留，暂未实现） |

---

## 三、完整数据流（一轮）

```
[CREATED]
  ↓ startTask() 或上一轮 completeRound()

[DATA_COLLECT → COLLECTING]
  for each captureTaskId in task.dataSourceIds:
    dubheDataFeign.startCaptureAndGetExecutionId(captureTaskId)
    → 产出 executionId（记录到 job.captureExecutionIds）
    → dubhe 侧在 RtspCaptureTask.datasetGroupId 下新建一个 Dataset，图片写入
  等待所有 execution 状态变为 SUCCESS
  → 各 execution 完成时触发 onCaptureComplete(executionId) 回调
  → 全部完成 → jobCollectDoneEvent

[DATA_ANNOTATE → AUTO_LABELING]
  for each executionId in job.captureExecutionIds:
    execution = dubheDataFeign.getCaptureExecution(executionId)
    datasetId = execution.datasetId
    
    // 通知 dubhe 侧开始标注
    dubheDataFeign.autoLabelStartWithNamesAndClearOption(datasetId, labelNames, ...)
    
    // 提交 K8s 自动标注 Job
    JobController.createAutoLabelJob({
      useDefault     = (round == 1),        // 第1轮用镜像默认权重
      imageUrl       = task.autoLabelImageUrl,
      directWeightPath = (round > 1) ? prevJob.trainJobId → weightPath : null,
      labelNames     = ModelApplicationZipLabelService.getBoundLabelIds → dubheDataFeign.findLabelByIds,
      ...
    })
  
  标注完成 → dubhe 侧触发回调 onAutoLabelComplete(jobId, datasetId)
  → 每完成一个 dataset：发版（dubheDataFeign.publish）→ 记录 versionId 到 job.datasetVersionIds
  → 全部完成：
      task.allDatasetVersionIds += job.datasetVersionIds  // 累积所有历史版本
      if task.requireManualReview → WAITING_REVIEW（父任务 PAUSED，等待前端 confirmReview）
      else → jobLabelDoneEvent

[MODEL_TRAIN → TRAINING]
  createTrainJob（待实现）:
    trainDatasetVersions = task.allDatasetVersionIds   // 全量历史版本
    preWeightPath = getPrevWeightPath(task, round)     // 第N轮 finetune
  → 训练完成 → onTrainComplete(jobId, modelJobId) → jobTrainDoneEvent

[CONVERTING]
  createConvertJob（待实现）
  → 转换完成 → onConvertComplete(jobId, convertModelJobId) → jobConvertDoneEvent

[PACKAGING → 暂跳过]
  打包逻辑预留，当前 onConvertComplete 直接调用 completeRound

[轮次结束]
  completeRound():
    job → COMPLETED
    task.currentRound++
    if currentRound >= maxRounds → taskCompleteEvent（父任务 COMPLETED）
    else → 创建下一轮 SelfIterationJob，循环
```

---

## 四、关键设计决策

### 1. 自动标注镜像来源（最重要）

```
第1轮（round == 1）：
  useDefault = true
  imageUrl   = task.autoLabelImageUrl  ← 创建父任务时从 model.autoLabelModelVersion.url 存入
  directWeightPath = null
  效果：K8s Job 使用镜像自带的默认模型权重做标注

第N轮（round > 1）：
  useDefault = false
  imageUrl   = task.autoLabelImageUrl  ← 同一个镜像
  directWeightPath = prevJob.trainJobId → ModelJob.weightPath  ← 上一轮训练产出
  效果：使用上一轮训练权重，标注精度随轮次提升
```

`directWeightPath` 是对 `AutoLabelJobRequest` 的扩展字段，在 `JobController.createAutoLabelJob` 中新增了对应的处理分支（原有引导式逻辑完全不变）。

### 2. 数据集版本累积策略（方案 B）

每次 `startCapture` 在对应 `DatasetGroup` 下**新建 Dataset**（不复用旧的），每轮产出多个新 Dataset，标注完成后分别发版，版本 ID 追加到：
- `SelfIterationJob.datasetVersionIds`（本轮产出）
- `SelfIterationTask.allDatasetVersionIds`（跨轮累积）

训练时将 `allDatasetVersionIds` 全量传入 `DatasetMergeRequest.versions`，由 dubhe 侧异步合并后训练。

**不采用"版本继承累积"的原因**：回流任务每次新建 Dataset，不在同一 Dataset 上追加图片，无法利用 dubhe 版本的继承机制。

### 3. autoLabelImageUrl 的存储时机

**在创建 `SelfIterationTask` 时**（Service 层 `create` 方法，待实现），从 `task.modelApplication.model.autoLabelModelVersion.url` 取值存入 `task.autoLabelImageUrl`，避免每轮标注时重复 join 查询。

### 4. 回调通知机制（待确认）

当前设计了两种回调路径，dubhe 侧需选择一种实现：

| 事件 | 回调接口 | 备注 |
|------|---------|------|
| 采集完成 | `POST /selfIteration/callback/captureComplete/{executionId}` | dubhe-data 在 `RtspCaptureExecutionRunnerServiceImpl` 完成时调用 |
| 采集失败 | `POST /selfIteration/callback/captureFailed/{executionId}` | 同上，失败时调用 |
| 标注完成 | `POST /selfIteration/callback/autoLabelComplete?jobId=&datasetId=` | dubhe-data `autoLabelEnd` 时调用 |
| 标注失败 | `POST /selfIteration/callback/autoLabelFailed?jobId=&datasetId=` | 同上 |
| 训练完成 | `POST /selfIteration/callback/trainComplete?jobId=&modelJobId=` | 复用现有 ModelJob 完成通知 |
| 转换完成 | `POST /selfIteration/callback/convertComplete?jobId=&convertModelJobId=` | 同上 |

> **注意**：`jobId` 指 `SelfIterationJob.id`，不是 `ModelJob.id`。`onAutoLabelComplete` 回调中需要知道当前是哪个 `SelfIterationJob`，通过 `jobId` 参数传递，dubhe 侧标注 Job 名称中包含了 `jobId`（格式：`autolabel-iter{taskId}-r{round}-ds{datasetId}`），可从 Job 名解析或在触发标注时一起传给 dubhe。

---

## 五、已完成部分

| 文件 | 状态 | 说明 |
|------|------|------|
| `SelfIterationTask.java` | ✅ 完成 | 实体类，含状态常量、`allDatasetVersionIds`、`autoLabelImageUrl` |
| `SelfIterationJob.java` | ✅ 完成 | 实体类，含阶段/状态常量、`datasetVersionIds`（列表） |
| 状态机常量（3个文件） | ✅ 完成 | 状态码 + 事件名常量 |
| 抽象状态类（2个文件） | ✅ 完成 | 所有状态方法默认抛出异常 |
| 父任务具体状态（6个文件） | ✅ 完成 | Idle/Running/Paused/Completed/Failed/Cancelled |
| 子任务具体状态（11个文件） | ✅ 完成 | Created 到 Cancelled 全部空实现（Bean 注册） |
| `SelfIterationTaskStateMachine.java` | ✅ 完成 | 状态机主类 |
| `SelfIterationJobStateMachine.java` | ✅ 完成 | 状态机主类 |
| `SelfIterationTaskRepo.java` | ✅ 完成 | JPA Repository |
| `SelfIterationJobRepo.java` | ✅ 完成 | JPA Repository |
| `AutoLabelJobRequest.java` | ✅ 完成 | 新增 `directWeightPath` 字段 |
| `JobController.java` | ✅ 完成 | 新增 `directWeightPath` 处理分支 |
| `DubheDataFeign.java` | ✅ 完成 | 新增 `startCaptureAndGetExecutionId`、`getCaptureExecution` |
| `RtspCaptureExecutionVO.java` | ✅ 完成 | Feign 返回 VO |
| `SelfIterationService.java` | ✅ 骨架完成 | 流程骨架 + 大部分逻辑，2个阶段 TODO |
| `SelfIterationController.java` | ✅ 完成 | REST 接口 + 回调接口 |
| `docs/self-iteration-state-machine.md` | ✅ 完成 | 状态机设计文档 |

---

## 六、待实现部分

### 6.1 SelfIterationService — 训练阶段（高优先级）

**文件**：`mineai-model-manager/.../service/SelfIterationService.java`

**方法**：`startTrainPhase(SelfIterationTask task, SelfIterationJob job)`

参考 `ModelJobService.createTrainJob` 实现，关键参数：
```java
// 参考 ModelJobService 约 540 行
trainDatasetVersions = task.getAllDatasetVersionIds()   // 全量历史版本
preWeightPath        = getPrevWeightPath(task, job.getRound())  // null=从头训，非null=finetune
hyperParams          = task.getHyperParams()
hardwareParams       = task.getHardwareParams()
gpuMode              = task.getGpuMode()
gpuCount             = task.getGpuCount()
// modelGenerationId 需要一个"代理" ModelGeneration，或直接扩展 createTrainJob 支持自迭代调用
```

> **注意**：`ModelJobService.createTrainJob` 当前依赖 `ModelGeneration` 对象，自迭代没有 `ModelGeneration`。建议在 `ModelJobService` 中提取一个底层方法，或者自迭代自己组装参数后直接调用 `AsyncTaskService.submitDatasetCutAndCreateJob`。

### 6.2 SelfIterationService — 转换阶段（高优先级）

**方法**：`startConvertPhase(SelfIterationTask task, SelfIterationJob job)`

参考 `ModelJobService.createConvertJob` 实现。

### 6.3 SelfIterationService — CRUD（中优先级）

需要实现父任务的创建/查询/更新接口：
- `createTask(SelfIterationTask task)`：在此处从 `modelApplication.model.autoLabelModelVersion.url` 填入 `autoLabelImageUrl`
- 列表查询支持分页和状态筛选

### 6.4 dubhe-data 侧配套（必需）

以下接口需要 dubhe-data 侧实现：

| 接口 | 说明 |
|------|------|
| `POST /rtsp-capture/tasks/{taskId}/start` 返回 `Long executionId` | 现有 `startCapture` 改为返回 executionId，或新增接口 |
| `GET /rtsp-capture/executions/{executionId}` 返回 `RtspCaptureExecutionVO` | 查询执行记录（含 datasetId） |
| 采集完成/失败时回调 mineai 的 `/selfIteration/callback/captureComplete(Failed)/{executionId}` | 在 `RtspCaptureExecutionRunnerServiceImpl` 执行完成/失败时触发 |
| 标注完成/失败时回调 mineai 的 `/selfIteration/callback/autoLabelComplete(Failed)` | 在 `DatasetServiceImpl.autoLabelEnd` 时触发，需传入 `jobId`（从标注 Job 名解析）和 `datasetId` |

### 6.5 打包阶段（低优先级，已预留）

状态机骨架已完成，`packageTaskId` 字段已预留，`SelfIterationController` 中 `onPackageComplete/onPackageFailed` 回调已实现。对接 `mineai-package-manager` 后直接填充 `startPackagePhase` 方法。

### 6.6 数据库建表（部署前必需）

JPA 实体会通过 `spring.jpa.hibernate.ddl-auto` 自动建表，但需确认以下附属表：
```sql
-- 关联表（由 @ElementCollection 自动创建）
self_iteration_task_dataset_versions (self_iteration_task_id, dataset_version_id)
self_iteration_task_data_source      (self_iteration_task_id, rtsp_capture_task_id)
self_iteration_task_hyper_params     (self_iteration_task_id, param_key, param_value)
self_iteration_job_capture_executions(self_iteration_job_id, capture_execution_id)
self_iteration_job_dataset_versions  (self_iteration_job_id, dataset_version_id)
```

---

## 七、注意事项

### 1. jobId 透传问题

`onAutoLabelComplete` 回调依赖 `jobId`（`SelfIterationJob.id`）。触发标注时，Job 名称格式为：
```
autolabel-iter{taskId}-r{round}-ds{datasetId}
```
dubhe 侧可从 Job 名解析 `taskId + round`，再通过 `SelfIterationJobRepo.findBySelfIterationTask_IdAndRound` 查到 `jobId`；或在触发标注时把 `jobId` 作为 env 变量传给 K8s Job，由 Job 在回调时携带。推荐后者，更简单。

### 2. 并发标注的完成计数

`onAutoLabelComplete` 通过比较 `job.datasetVersionIds.size()` 与 `job.captureExecutionIds.size()` 判断是否全部完成。这里存在并发写竞争（多个 dataset 同时标注完成时并发触发回调），需要：
- 方案 A：加分布式锁（Redis）保护 `datasetVersionIds` 的追加操作
- 方案 B：用数据库原子更新 + 查询判断

当前实现未加锁，**生产前务必处理**。

### 3. 状态机的内存状态问题

`SelfIterationTaskStateMachine` 和 `SelfIterationJobStateMachine` 都是 Spring Bean（单例），内部通过 `memoryState` 字段维护当前状态。这意味着：
- 服务重启后 `memoryState` 丢失，需要在服务启动时从数据库恢复（参考 `ModelJobStateMachine` 的做法）
- 同一时刻多个父任务/子任务使用同一个 Bean 实例会有线程安全问题

参考现有系统的 `StateMachineFactory` 处理方式，必要时改为每个任务实例独立的状态机。

### 4. 第一轮的 autoLabelImageUrl

`task.autoLabelImageUrl` 必须在 **创建父任务时** 填入（从 `model.autoLabelModelVersion.url` 取），否则第 1 轮标注时为 null，K8s Job 无法启动。在 `createTask` 方法中务必加校验。

### 5. ModelVersion.isAutoLabel 字段

`Model` 实体有 `autoLabelModelVersion`（`@ManyToOne`），直接通过 `task.modelApplication.model.autoLabelModelVersion.url` 获取，不需要遍历 `ModelVersion` 列表。

---

## 八、快速开始（接手开发）

```
1. 读 docs/self-iteration-state-machine.md — 理解状态流转
2. 读本文第三节"完整数据流" — 理解每轮的执行顺序
3. 优先实现 SelfIterationService.startTrainPhase — 参考 ModelJobService.createTrainJob
4. dubhe-data 侧补两个 Feign 接口并添加回调调用
5. 实现 createTask（CRUD），填充 autoLabelImageUrl
6. 测试单轮（maxRounds=1），验证采集→标注→训练→转换→完成链路
7. 测试多轮（maxRounds=3），验证数据累积和权重继承
```

---

## 九、关键文件速查

| 需要了解的内容 | 看哪个文件 |
|--------------|----------|
| 状态机全局设计 | `docs/self-iteration-state-machine.md` |
| 父/子任务实体字段 | `mineai-core/.../entity/SelfIterationTask.java` / `SelfIterationJob.java` |
| 核心业务流程 | `mineai-model-manager/.../service/SelfIterationService.java` |
| REST 接口列表 | `mineai-model-manager/.../controller/SelfIterationController.java` |
| 自动标注 K8s Job 创建 | `mineai-model-manager/.../kubernetes/controller/JobController.java`（1077行，`createAutoLabelJob`） |
| directWeightPath 扩展 | `JobController.java` 约1118行，`AutoLabelJobRequest.java` |
| 数据集多版本合并训练 | `mineai-model-manager/.../service/AsyncTaskService.java`（`submitDatasetCutAndCreateJob`） |
| dubhe 侧标注完成 | `dubhe-data/.../service/impl/DatasetServiceImpl.java`（`autoLabelEnd`） |
| 回流执行详情 | `dubhe-data/.../service/impl/RtspCaptureExecutionRunnerServiceImpl.java` |

# 自迭代训练 状态机设计文档

## 一、总体状态流转图

### 父任务（SelfIterationTask）

```
                       taskStartEvent
  ┌─────────┐  ──────────────────────────►  ┌───────────┐
  │  IDLE   │                               │  RUNNING  │◄─────────────────────┐
  │   (0)   │                               │    (1)    │                      │
  └─────────┘                               └─────┬─────┘                      │
                                                  │                            │
                        taskPauseForReviewEvent   │                taskResumeEvent
                        （子任务→WAITING_REVIEW）  ▼               （审核确认完成）
                                           ┌──────────┐                        │
                                           │  PAUSED  │ ───────────────────────┘
                                           │   (-1)   │
                                           └──────────┘
                                                │
          ┌─────────────────────────────────────┼─────────────────────────────┐
          │ taskCancelEvent                      │ taskFailEvent               │ taskCompleteEvent
          ▼                                      ▼                             ▼
  ┌─────────────┐                       ┌──────────────┐              ┌──────────────┐
  │  CANCELLED  │                       │    FAILED    │              │  COMPLETED   │
  │   (-10)     │                       │    (-2)      │              │    (2)       │
  └─────────────┘                       └──────────────┘              └──────────────┘
  （手动取消）                            （子任务失败）                （达到 maxRounds
                                                                        或手动停止）
```

### 子任务（SelfIterationJob）

```
  ┌─────────┐  jobStartCollectEvent  ┌─────────────┐  jobCollectDoneEvent  ┌───────────────┐
  │ CREATED │ ──────────────────────►│  COLLECTING │ ────────────────────►│ AUTO_LABELING │
  │   (0)   │                        │    (1)      │                       │     (2)       │
  └─────────┘                        └──────┬──────┘                       └──────┬────────┘
                                            │                                      │
                            jobCollectFailed│Event                    requireManualReview?
                                            ▼                         ┌────────────┴────────────┐
                                   ┌────────────────┐               YES（true）            NO（false）
                                   │ COLLECT_FAILED │                 │                         │
                                   │    (11)        │   jobLabelNeedReviewEvent     jobLabelDoneEvent
                                   └────────────────┘                 │                         │
                                                                       ▼                         │
                                                              ┌─────────────────┐                │
                                            ┌── PAUSED ──────│ WAITING_REVIEW  │                │
                                            │  （父任务）      │     (21)        │                │
                                            │                └────────┬────────┘                │
                                            │                         │                          │
                                            │             jobLabelDoneEvent                      │
                                            │             （前端确认审核完成）                    │
                                            └── RUNNING ─────────────┘                          │
                                               （父任务恢复）                                     │
                                                                       │                         │
                                                  jobLabelFailedEvent  │                         │
                                                         │             ▼                         │
                                                         │      ┌──────────┐◄────────────────────┘
                                              ┌──────────┴──┐   │ TRAINING │
                                              │  ANNOTATE_  │   │   (3)    │
                                              │   FAILED    │   └────┬─────┘
                                              │   (22)      │        │
                                              └─────────────┘        │ jobTrainDoneEvent
                                                                      ▼
                                                              ┌─────────────┐
                                                              │  CONVERTING │
                                                              │   (31)      │
                                                              └──────┬──────┘
                                                                     │
                                         jobTrainFailedEvent         │ jobConvertDoneEvent
                                      ┌──────────────────────────────┤
                                      │  （训练或转换任一失败均→此态） │
                                      ▼                              ▼
                              ┌──────────────┐              ┌─────────────┐
                              │ TRAIN_FAILED │              │  PACKAGING  │
                              │    (32)      │              │    (4)      │
                              └──────────────┘              └──────┬──────┘
                                                                   │
                                          jobPackageFailedEvent    │ jobPackageDoneEvent
                                      ┌────────────────────────────┤
                                      ▼                            ▼
                              ┌──────────────┐           ┌──────────────────┐
                              │ PACKAGE_FAILED│           │  PENDING_DEPLOY  │
                              │    (41)      │           │      (5)         │
                              └──────────────┘           │    【预留】       │
                                                         └────────┬─────────┘
                                                                  │
                                                       jobCompleteEvent
                                                                  │
                                                                  ▼
                                                         ┌──────────────┐
                                                         │  COMPLETED   │
                                                         │    (6)       │
                                                         └──────────────┘

  任意活跃状态 ──── jobCancelEvent ────► CANCELLED (-10)
```

---

## 二、状态码速查表

### 父任务状态（SelfIterationTask.status）

| 值   | 常量名      | 说明                             | 是否终态 |
|------|-------------|----------------------------------|--------|
| 0    | IDLE        | 空闲/初始化，未开始               | 否     |
| 1    | RUNNING     | 运行中（某轮子任务进行中）         | 否     |
| -1   | PAUSED      | 暂停（等待人工审核）               | 否     |
| 2    | COMPLETED   | 已完成（达到 maxRounds 或手动停止）| **是** |
| -2   | FAILED      | 失败（子任务失败导致终止）         | **是** |
| -10  | CANCELLED   | 已取消                            | **是** |

### 子任务状态（SelfIterationJob.status）

| 值   | 常量名          | 所属阶段       | 说明                                  | 是否终态 |
|------|-----------------|--------------|---------------------------------------|--------|
| 0    | CREATED         | -            | 已创建，待启动                         | 否     |
| 1    | COLLECTING      | DATA_COLLECT | 数据采集中（RtspCaptureExecution 运行）| 否     |
| 11   | COLLECT_FAILED  | DATA_COLLECT | 数据采集失败                           | **是** |
| 2    | AUTO_LABELING   | DATA_ANNOTATE| 自动标注中                             | 否     |
| 21   | WAITING_REVIEW  | DATA_ANNOTATE| 等待人工审核（父任务同步 PAUSED）       | 否     |
| 22   | ANNOTATE_FAILED | DATA_ANNOTATE| 标注失败                               | **是** |
| 3    | TRAINING        | MODEL_TRAIN  | 训练中（ModelJob TRAIN 运行）          | 否     |
| 31   | CONVERTING      | MODEL_TRAIN  | 转换中（ModelJob CONVERT 运行）        | 否     |
| 32   | TRAIN_FAILED    | MODEL_TRAIN  | 训练或转换失败                         | **是** |
| 4    | PACKAGING       | MODEL_TRAIN  | 打包中（package-manager Task 运行）    | 否     |
| 41   | PACKAGE_FAILED  | MODEL_TRAIN  | 打包失败                               | **是** |
| 5    | PENDING_DEPLOY  | MODEL_DEPLOY | 待下发（预留，当前跳过）               | 否     |
| 6    | COMPLETED       | -            | 本轮完成                               | **是** |
| -10  | CANCELLED       | -            | 已取消                                 | **是** |

### 子任务阶段（SelfIterationJob.phase）

| 值 | 常量名          | 涵盖的状态                          |
|----|-----------------|-------------------------------------|
| 1  | DATA_COLLECT    | COLLECTING / COLLECT_FAILED         |
| 2  | DATA_ANNOTATE   | AUTO_LABELING / WAITING_REVIEW / ANNOTATE_FAILED |
| 3  | MODEL_TRAIN     | TRAINING / CONVERTING / TRAIN_FAILED / PACKAGING / PACKAGE_FAILED |
| 4  | MODEL_DEPLOY    | PENDING_DEPLOY（预留）              |

---

## 三、事件名称速查表

### 父任务事件

| 事件常量                     | 触发时机                        | 源状态          | 目标状态    |
|-----------------------------|---------------------------------|-----------------|-------------|
| `taskStartEvent`            | 手动触发第一轮迭代               | IDLE            | RUNNING     |
| `taskPauseForReviewEvent`   | 子任务进入 WAITING_REVIEW 时     | RUNNING         | PAUSED      |
| `taskResumeEvent`           | 人工审核确认完成                 | PAUSED          | RUNNING     |
| `taskCompleteEvent`         | 达到 maxRounds 或手动停止        | RUNNING         | COMPLETED   |
| `taskFailEvent`             | 子任务进入任一失败终态            | RUNNING / PAUSED| FAILED      |
| `taskCancelEvent`           | 手动取消                        | RUNNING / PAUSED| CANCELLED   |

### 子任务事件

| 事件常量                    | 触发时机                                  | 源状态                       | 目标状态        |
|----------------------------|------------------------------------------|------------------------------|-----------------|
| `jobStartCollectEvent`     | 子任务创建后启动                          | CREATED                      | COLLECTING      |
| `jobCollectFailedEvent`    | RtspCaptureExecution 失败                | COLLECTING                   | COLLECT_FAILED  |
| `jobCollectDoneEvent`      | 所有回流执行完成                          | COLLECTING                   | AUTO_LABELING   |
| `jobLabelNeedReviewEvent`  | 自动标注完成且 requireManualReview=true   | AUTO_LABELING                | WAITING_REVIEW  |
| `jobLabelDoneEvent`        | 无需审核直接完成 / 审核已确认             | AUTO_LABELING / WAITING_REVIEW | TRAINING      |
| `jobLabelFailedEvent`      | 自动标注失败                             | AUTO_LABELING                | ANNOTATE_FAILED |
| `jobTrainDoneEvent`        | ModelJob(TRAIN) 成功                     | TRAINING                     | CONVERTING      |
| `jobTrainFailedEvent`      | ModelJob(TRAIN/CONVERT) 失败             | TRAINING / CONVERTING        | TRAIN_FAILED    |
| `jobConvertDoneEvent`      | ModelJob(CONVERT) 成功                   | CONVERTING                   | PACKAGING       |
| `jobPackageFailedEvent`    | 打包任务失败                             | PACKAGING                    | PACKAGE_FAILED  |
| `jobPackageDoneEvent`      | 打包任务完成                             | PACKAGING                    | PENDING_DEPLOY  |
| `jobCompleteEvent`         | 下发完成或跳过下发                       | PENDING_DEPLOY               | COMPLETED       |
| `jobCancelEvent`           | 手动取消                                 | 任意活跃状态                  | CANCELLED       |

---

## 四、父子任务联动规则

| 子任务状态变化                       | 父任务动作                                                   |
|--------------------------------------|--------------------------------------------------------------|
| → WAITING_REVIEW                     | 触发 `taskPauseForReviewEvent`，父任务 RUNNING → PAUSED      |
| WAITING_REVIEW → TRAINING（审核确认）| 触发 `taskResumeEvent`，父任务 PAUSED → RUNNING              |
| → COMPLETED（未达 maxRounds）         | 父任务保持 RUNNING，currentRound++，自动创建下一轮子任务      |
| → COMPLETED（已达 maxRounds）         | 触发 `taskCompleteEvent`，父任务 RUNNING → COMPLETED         |
| → COLLECT_FAILED / ANNOTATE_FAILED / TRAIN_FAILED / PACKAGE_FAILED | 触发 `taskFailEvent`，父任务 → FAILED |
| → CANCELLED                          | 视父任务是否有其他活跃子任务决定是否触发 `taskCancelEvent`   |

---

## 五、关键设计决策

### 1. TRAIN_FAILED 合并训练和转换失败

训练（TRAINING→CONVERTING）和转换（CONVERTING）失败都归入同一个状态 `TRAIN_FAILED(32)`，
原因是两者都属于"模型训练阶段"失败，对父任务的处理方式相同（触发 `taskFailEvent`），
无需在父任务层面区分。

### 2. WAITING_REVIEW 是唯一的自动暂停点

其他失败状态（COLLECT_FAILED / ANNOTATE_FAILED / TRAIN_FAILED / PACKAGE_FAILED）
都直接导致父任务进入 FAILED（终态），而 WAITING_REVIEW 是唯一让父任务"等待"并可恢复的状态。

### 3. PENDING_DEPLOY 当前直接跳过

下发逻辑尚未实现，当前 `jobPackageDoneEvent` 进入 PENDING_DEPLOY 后，
由调用方立即触发 `jobCompleteEvent` 跳过，保持状态机完整性的同时不阻断流程。

### 4. 配置修改的生效时机

修改父任务配置（hyperParams / hardwareParams / requireManualReview 等）：
- 对**已完成**的历史轮次：无影响，历史子任务数据只读
- 对**当前进行中**的轮次：不影响（子任务创建时已快照配置）
- 对**下一轮**子任务：创建时重新读取父任务配置生效

---

## 六、相关文件索引

| 文件 | 路径 |
|------|------|
| 父任务实体 | `mineai-core/.../entity/SelfIterationTask.java` |
| 子任务实体 | `mineai-core/.../entity/SelfIterationJob.java` |
| 父任务状态码 | `mineai-model-manager/.../statusMachine/constant/SelfIterationTaskStatusConstant.java` |
| 子任务状态码 | `mineai-model-manager/.../statusMachine/constant/SelfIterationJobStatusConstant.java` |
| 事件名称常量 | `mineai-model-manager/.../statusMachine/constant/SelfIterationStateMachineConstant.java` |
| 父任务抽象状态 | `mineai-model-manager/.../statusMachine/state/AbstractSelfIterationTaskState.java` |
| 子任务抽象状态 | `mineai-model-manager/.../statusMachine/state/AbstractSelfIterationJobState.java` |
| 父任务状态实现 | `mineai-model-manager/.../statusMachine/state/task/Task*.java` |
| 子任务状态实现 | `mineai-model-manager/.../statusMachine/state/job/Job*.java` |
| 父任务状态机 | `mineai-model-manager/.../statusMachine/statemachine/SelfIterationTaskStateMachine.java` |
| 子任务状态机 | `mineai-model-manager/.../statusMachine/statemachine/SelfIterationJobStateMachine.java` |

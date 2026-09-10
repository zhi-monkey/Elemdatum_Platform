import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum, DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
import { downloadByUrl } from '/@/utils/file/download';
import {
  SelfIterationChildTaskDetail,
  SelfIterationFormOptions,
  SelfIterationParentTask,
  SelfIterationParentTaskForm,
  DataSourceConfig,
  ParentTaskStatus,
  ChildTaskStatus,
  ChildTaskCurrentStep,
} from '../types/selfIteration';
import { childTaskDetailsMock } from '../mock/selfIteration';
import { normalizeSplitStringForBackend } from '/@/views/mineai/train/utils/datasetSplit';

const clone = <T>(payload: T): T => JSON.parse(JSON.stringify(payload)) as T;

function withFallbackParams(
  source: Record<string, unknown> | undefined,
  fallback: Record<string, number | string | boolean> | undefined,
) {
  const normalized = normalizeAnyParamMap(source);
  return Object.keys(normalized).length > 0 ? normalized : clone(fallback || {});
}

function normalizeParamMap(
  params: Record<string, number | string | boolean> | undefined,
): Record<string, string> {
  if (!params) return {};
  return Object.entries(params).reduce((acc, [key, value]) => {
    if (value === null || value === undefined) {
      return acc;
    }
    acc[key] = String(value);
    return acc;
  }, {} as Record<string, string>);
}

function sumDataSourceImageQuantity(
  dataSources: DataSourceConfig[] | undefined | null,
): number | null {
  if (!Array.isArray(dataSources) || dataSources.length === 0) return null;
  const total = dataSources.reduce((sum, source) => {
    const value = Number(source?.imageQuantity ?? 0);
    return sum + (Number.isFinite(value) && value > 0 ? value : 0);
  }, 0);
  return total > 0 ? total : null;
}

function buildParentTaskPayload(form: SelfIterationParentTaskForm, id?: number) {
  const datasetGroupName = form.datasetGroupName?.trim() || '';
  const datasetGroupId = form.datasetGroupId ?? null;
  const targetAccuracy = form.targetAccuracy != null ? Number(form.targetAccuracy) : null;
  const targetRecall = form.targetRecall != null ? Number(form.targetRecall) : null;
  const initialAutoLabelSource = form.initialAutoLabelSource ?? 'BOUND_DEFAULT';
  const iterationStartImageQuantity =
    form.iterationStartImageQuantity != null
      ? Number(form.iterationStartImageQuantity)
      : sumDataSourceImageQuantity(form.dataSources);

  return {
    ...(id !== undefined ? { id } : {}),
    taskName: form.taskName?.trim(),
    applicationName: form.applicationName,
    deviceFirmware: form.deviceFirmware,
    datasetGroupId,
    datasetGroupName: datasetGroupId !== null ? '' : datasetGroupName,
    confidenceThreshold: Number(form.confidenceThreshold ?? 0.5),
    requireManualReview: Boolean(form.requireManualReview),
    autoLabelImageUrl:
      initialAutoLabelSource === 'DEFAULT_IMAGE' ? form.autoLabelImageUrl || null : null,
    initialAutoLabelSource,
    initialAutoLabelTrainSource:
      initialAutoLabelSource === 'TRAINED_MODEL'
        ? form.initialAutoLabelTrainSource ?? 'STANDARD'
        : null,
    initialAutoLabelModelGenerationId:
      initialAutoLabelSource === 'TRAINED_MODEL'
        ? form.initialAutoLabelModelGenerationId ?? null
        : null,
    initialAutoLabelStandardJobName:
      initialAutoLabelSource === 'TRAINED_MODEL' &&
      (form.initialAutoLabelTrainSource ?? 'STANDARD') === 'STANDARD'
        ? form.initialAutoLabelStandardJobName || null
        : null,
    deleteRawAfterCollect: form.deleteRawAfterCollect !== false,
    dataSources: (form.dataSources || []).map((source) => ({
      httpCameraId: source.httpCameraId,
      captureInterval: Number(source.captureInterval),
      imageQuantity: Number(source.imageQuantity),
    })),
    trainParams: normalizeParamMap(form.trainParams),
    convertParams: normalizeParamMap(form.convertParams),
    inferenceDeviceIds: (form.inferenceDeviceIds || [])
      .map((id) => Number(id))
      .filter((id) => Number.isFinite(id) && id > 0),
    gpuUrlTargetIds: (form.gpuUrlTargetIds || [])
      .map((id) => Number(id))
      .filter((id) => Number.isFinite(id) && id > 0),
    splitSize: normalizeSplitStringForBackend(form.splitSize),
    hardwareParamsId: form.hardwareParamsId,
    gpuMode: form.gpuMode,
    gpuCount: Number(form.gpuCount ?? 1),
    maxRounds: form.maxRounds != null ? Number(form.maxRounds) : 10,
    iterationStartImageQuantity,
    targetAccuracy,
    targetRecall,
    reuseAnnotationModel: form.reuseAnnotationModel ?? 'ALWAYS',
    reuseTrainModel: form.reuseTrainModel ?? 'ALWAYS',
    autoDeployEnabled: form.dispatchMode === 'auto',
  };
}

// ── 状态码映射 ──────────────────────────────────────

// SelfIterationTask 状态码 → 前端字符串
const TASK_STATUS_MAP: Record<number, ParentTaskStatus> = {
  0: 'pending',
  1: 'running', // STATUS_RUNNING
  [-1]: 'paused',
  2: 'completed',
  [-2]: 'failed',
  [-10]: 'failed',
};

// SelfIterationJob status/phase → 前端子任务状态字符串
const JOB_STATUS_MAP: Record<number, ChildTaskStatus> = {
  // Current backend status codes (SelfIterationJob)
  0: 'pending',
  1: 'collecting',
  2: 'annotating',
  3: 'training',
  4: 'packaging',
  5: 'dispatching',
  6: 'completed',
  11: 'failed', // collect failed
  21: 'waiting_review',
  22: 'failed', // annotate failed
  31: 'converting',
  32: 'failed', // train/convert failed
  41: 'failed', // package failed
  [-10]: 'failed', // cancelled

  // Legacy compatibility mapping (keep to avoid breaking old data)
  7: 'failed',
  8: 'training',
  9: 'converting',
  10: 'failed',
  12: 'completed',
  13: 'failed',
};

// SelfIterationJob phase → 前端 currentStep
const JOB_PHASE_MAP: Record<number, ChildTaskCurrentStep> = {
  1: 'dataCollection',
  2: 'dataAnnotation',
  3: 'modelIteration',
  4: 'modelDispatch',
};

// ── 类型适配 ─────────────────────────────────────────

interface RawSelfIterationTask {
  id: number;
  name: string;
  currentRound: number;
  status: number;
  statusLabel?: string;
  createTime: string;
  applicationName?: string | null;
  deviceFirmware?: string | null;
  datasetGroupId?: number | null;
  datasetGroupName?: string | null;
  dataSources?: Array<{
    httpCameraServerId?: number | null;
    httpCameraId?: number | null;
    captureInterval?: number | null;
    imageQuantity?: number | null;
  }>;
  deleteRawAfterCollect?: boolean | null;
  confidenceThreshold?: number | null;
  requireManualReview?: boolean;
  autoLabelImageUrl?: string | null;
  initialAutoLabelSource?: string | null;
  initialAutoLabelTrainSource?: string | null;
  initialAutoLabelModelGenerationId?: number | null;
  initialAutoLabelStandardJobName?: string | null;
  maxRounds?: number | null;
  iterationStartImageQuantity?: number | null;
  targetAccuracy?: number | string | null;
  targetRecall?: number | string | null;
  reuseAnnotationModel?: string | null;
  reuseTrainModel?: string | null;
  autoDeployEnabled?: boolean;
  splitSize?: string | null;
  hardwareParamsId?: number | null;
  hardwareParamsTitle?: string | null;
  gpuMode?: string | null;
  gpuCount?: number | null;
  hyperParams?: Record<string, unknown>;
  convertParams?: Record<string, unknown>;
  authCode?: string | null;
  inferenceDeviceIds?: number[];
  gpuUrlTargetIds?: number[];
}

interface RawSelfIterationJob {
  id: number;
  round: number;
  phase: number;
  status: number;
  createTime: string;
  updateTime: string;
  trainAccuracy?: number | null;
  trainRecall?: number | null;
  epochDetail?: string | null;
}

function normalizeAnyParamMap(
  params: Record<string, unknown> | undefined,
): Record<string, number | string | boolean> {
  if (!params) return {};
  return Object.entries(params).reduce((acc, [key, value]) => {
    if (value === null || value === undefined) {
      return acc;
    }
    if (typeof value === 'number' || typeof value === 'boolean' || typeof value === 'string') {
      acc[key] = value;
    } else {
      acc[key] = String(value);
    }
    return acc;
  }, {} as Record<string, number | string | boolean>);
}

function buildDetailForm(raw: Record<string, any>): SelfIterationParentTaskForm {
  const dataSources: DataSourceConfig[] =
    Array.isArray(raw.dataSources) && raw.dataSources.length > 0
      ? raw.dataSources.map((source: any) => ({
          httpCameraServerId: source.httpCameraServerId ?? null,
          httpCameraId: source.httpCameraId ?? null,
          captureInterval: Number(source.captureInterval ?? 5),
          imageQuantity: Number(source.imageQuantity ?? 200),
        }))
      : [
          {
            httpCameraServerId: null,
            httpCameraId: null,
            captureInterval: 5,
            imageQuantity: 200,
          },
        ];

  const initialAutoLabelSource = (
    raw.initialAutoLabelSource ??
    (raw.autoLabelImageUrl ? 'DEFAULT_IMAGE' : 'BOUND_DEFAULT')
  ) as SelfIterationParentTaskForm['initialAutoLabelSource'];
  const initialAutoLabelTrainSource = (raw.initialAutoLabelTrainSource ??
    null) as SelfIterationParentTaskForm['initialAutoLabelTrainSource'];

  return {
    taskName: raw.name ?? '',
    applicationName: raw.applicationName ?? null,
    deviceFirmware: raw.deviceFirmware ?? null,
    datasetGroupId: raw.datasetGroupId ?? null,
    datasetGroupName: raw.datasetGroupName ?? '',
    dataSources,
    deleteRawAfterCollect: raw.deleteRawAfterCollect !== false,
    confidenceThreshold: Number(raw.confidenceThreshold ?? 0.5),
    requireManualReview: Boolean(raw.requireManualReview),
    autoLabelImageUrl: raw.autoLabelImageUrl ?? null,
    initialAutoLabelSource,
    initialAutoLabelTrainSource,
    initialAutoLabelModelGenerationId: raw.initialAutoLabelModelGenerationId ?? null,
    initialAutoLabelStandardJobName: raw.initialAutoLabelStandardJobName ?? null,
    splitSize: normalizeSplitStringForBackend(raw.splitSize),
    trainParams: normalizeAnyParamMap(raw.hyperParams),
    convertParams: normalizeAnyParamMap(raw.convertParams),
    authCode: raw.authCode ?? '',
    inferenceDeviceIds: Array.isArray(raw.inferenceDeviceIds)
      ? raw.inferenceDeviceIds
          .map((id: any) => Number(id))
          .filter((id: number) => Number.isFinite(id) && id > 0)
      : [],
    gpuUrlTargetIds: Array.isArray(raw.gpuUrlTargetIds)
      ? raw.gpuUrlTargetIds
          .map((id: any) => Number(id))
          .filter((id: number) => Number.isFinite(id) && id > 0)
      : [],
    hardwareParamsId: raw.hardwareParamsId ?? null,
    gpuMode: raw.gpuMode ?? 'single-exclusive',
    gpuCount: Number(raw.gpuCount ?? 1),
    maxRounds: raw.maxRounds != null ? Number(raw.maxRounds) : null,
    iterationStartImageQuantity:
      raw.iterationStartImageQuantity != null
        ? Number(raw.iterationStartImageQuantity)
        : sumDataSourceImageQuantity(dataSources),
    targetAccuracy: raw.targetAccuracy != null ? Number(raw.targetAccuracy) : null,
    targetRecall: raw.targetRecall != null ? Number(raw.targetRecall) : null,
    reuseAnnotationModel: raw.reuseAnnotationModel ?? 'ALWAYS',
    reuseTrainModel: raw.reuseTrainModel ?? 'ALWAYS',
    dispatchMode: raw.autoDeployEnabled ? 'auto' : 'manual',
  };
}

function adaptTask(
  raw: RawSelfIterationTask,
  childTasks: SelfIterationParentTask['childTasks'],
): SelfIterationParentTask {
  return {
    id: raw.id,
    taskName: raw.name,
    currentIteration: raw.currentRound,
    status: TASK_STATUS_MAP[raw.status] ?? 'running',
    createTime: raw.createTime,
    form: null as any, // 列表页不需要 form，详情页按需加载
    childTasks,
    hardwareParamsId: raw.hardwareParamsId ?? null,
    hardwareParamsTitle: raw.hardwareParamsTitle ?? null,
    gpuMode: raw.gpuMode ?? 'single-exclusive',
    gpuCount: Number(raw.gpuCount ?? 1),
    maxRounds: raw.maxRounds != null ? Number(raw.maxRounds) : null,
    iterationStartImageQuantity:
      raw.iterationStartImageQuantity != null ? Number(raw.iterationStartImageQuantity) : null,
    targetAccuracy: raw.targetAccuracy != null ? Number(raw.targetAccuracy) : null,
    targetRecall: raw.targetRecall != null ? Number(raw.targetRecall) : null,
    currentRound: raw.currentRound,
  };
}

export async function startSelfIterationParentTask(taskId: number): Promise<boolean> {
  try {
    await maHttp.post(
      { url: `selfIteration/start/${taskId}` },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    return true;
  } catch {
    return false;
  }
}

export async function pauseSelfIterationParentTask(taskId: number): Promise<boolean> {
  try {
    await maHttp.post(
      { url: `selfIteration/pause/${taskId}` },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    return true;
  } catch {
    return false;
  }
}

export async function resumeSelfIterationParentTask(taskId: number): Promise<boolean> {
  try {
    await maHttp.post(
      { url: `selfIteration/resume/${taskId}` },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    return true;
  } catch {
    return false;
  }
}

export async function cancelSelfIterationParentTask(taskId: number): Promise<boolean> {
  try {
    await maHttp.post(
      { url: `selfIteration/cancel/${taskId}` },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    return true;
  } catch {
    return false;
  }
}

function adaptJob(
  raw: RawSelfIterationJob,
  parentTaskId: number,
): SelfIterationParentTask['childTasks'][0] {
  return {
    id: raw.id,
    parentTaskId,
    round: raw.round,
    status: JOB_STATUS_MAP[raw.status] ?? 'pending',
    currentStep: JOB_PHASE_MAP[raw.phase] ?? 'dataCollection',
    createTime: raw.createTime,
    updateTime: raw.updateTime,
    trainAccuracy: raw.trainAccuracy ?? null,
    trainRecall: raw.trainRecall ?? null,
    epochDetail: raw.epochDetail ?? null,
  };
}

// ── API 函数 ──────────────────────────────────────────

export async function fetchSelfIterationParentTasks(): Promise<SelfIterationParentTask[]> {
  const tasks: RawSelfIterationTask[] = await maHttp.get(
    { url: 'selfIteration/list' },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );

  if (!Array.isArray(tasks)) return [];

  // 不再自动加载子任务，返回空数组，由展开时懒加载
  return tasks.map((task) => adaptTask(task, []));
}

export async function fetchSelfIterationChildTasks(
  parentTaskId: number,
): Promise<SelfIterationParentTask['childTasks']> {
  try {
    const jobs: RawSelfIterationJob[] = await maHttp.get(
      { url: `selfIteration/${parentTaskId}/jobs` },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    return Array.isArray(jobs) ? jobs.map((job) => adaptJob(job, parentTaskId)) : [];
  } catch {
    return [];
  }
}

export async function fetchSelfIterationFormOptions(): Promise<SelfIterationFormOptions> {
  try {
    const [datasetGroupResp, selfIterationOptionsResp] = await Promise.all([
      maHttp.get(
        { url: 'datasets/group/getAllDatasetGroup', timeout: 6000 },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET, errorMessageMode: 'none' },
      ),
      maHttp
        .get(
          { url: 'selfIteration/formOptions', timeout: 6000 },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER, errorMessageMode: 'none' },
        )
        .catch(() => ({})),
    ]);

    const datasetGroupOptions = Array.isArray(datasetGroupResp)
      ? datasetGroupResp.map((g: any) => ({ label: g.name, value: g.id }))
      : [];

    const inferenceDeviceOptions = Array.isArray(selfIterationOptionsResp?.inferenceDeviceOptions)
      ? selfIterationOptionsResp.inferenceDeviceOptions.map((item: any) => ({
          label:
            item.label ||
            `${item.deviceName || '设备'} (${item.inferenceIp || '-'}:${
              item.inferencePort || '-'
            })`,
          value: Number(item.id),
        }))
      : [];

    const gpuUrlTargetOptions = Array.isArray(selfIterationOptionsResp?.gpuUrlTargetOptions)
      ? selfIterationOptionsResp.gpuUrlTargetOptions.map((item: any) => ({
          label:
            item.label ||
            `${item.name || '推理分析装置地址'} (${item.ip || '-'}:${item.port || '-'})`,
          value: Number(item.id),
        }))
      : [];

    return {
      modelOptions: [],
      rtspSourceOptions: [],
      modelApplicationOptions: selfIterationOptionsResp?.modelApplicationOptions || [],
      deviceFirmwareOptions: [],
      datasetGroupOptions,
      inferenceDeviceOptions,
      gpuUrlTargetOptions,
      labelOptions: [],
      trainParamConfigList: selfIterationOptionsResp?.trainParamConfigList || [],
      convertParamConfigList: selfIterationOptionsResp?.convertParamConfigList || [],
    };
  } catch (error) {
    console.error('Failed to fetch form options:', error);
    return {
      modelOptions: [],
      rtspSourceOptions: [],
      modelApplicationOptions: [],
      deviceFirmwareOptions: [],
      datasetGroupOptions: [],
      inferenceDeviceOptions: [],
      gpuUrlTargetOptions: [],
      labelOptions: [],
      trainParamConfigList: [],
      convertParamConfigList: [],
    };
  }
}

export async function createSelfIterationParentTask(
  form: SelfIterationParentTaskForm,
): Promise<SelfIterationParentTask> {
  const result = await maHttp.post(
    {
      url: 'selfIteration/create',
      data: buildParentTaskPayload(form),
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
  const task = adaptTask(result, []);
  task.form = buildDetailForm(result || {});
  if (!task.form.applicationName) task.form.applicationName = form.applicationName;
  if (!task.form.deviceFirmware) task.form.deviceFirmware = form.deviceFirmware;
  if (!task.form.datasetGroupId) task.form.datasetGroupId = form.datasetGroupId;
  if (!task.form.datasetGroupName) task.form.datasetGroupName = form.datasetGroupName;
  if (!task.form.dataSources?.length) task.form.dataSources = clone(form.dataSources || []);
  task.form.deleteRawAfterCollect = form.deleteRawAfterCollect !== false;
  task.form.requireManualReview = form.requireManualReview;
  task.form.confidenceThreshold = form.confidenceThreshold;
  task.form.trainParams = withFallbackParams(result?.hyperParams, form.trainParams);
  task.form.convertParams = withFallbackParams(result?.convertParams, form.convertParams);
  task.form.authCode = (result?.authCode ?? form.authCode ?? '').toString();
  task.form.gpuUrlTargetIds = Array.isArray(result?.gpuUrlTargetIds)
    ? result.gpuUrlTargetIds
        .map((id: any) => Number(id))
        .filter((id: number) => Number.isFinite(id) && id > 0)
    : clone(form.gpuUrlTargetIds || []);
  return task;
}

export async function updateSelfIterationParentTask(
  id: number,
  form: SelfIterationParentTaskForm,
): Promise<SelfIterationParentTask | null> {
  const result = await maHttp.put(
    {
      url: 'selfIteration/update',
      data: buildParentTaskPayload(form, id),
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
  if (!result) return null;
  const task = adaptTask(result, []);
  task.form = buildDetailForm(result || {});
  if (!task.form.applicationName) task.form.applicationName = form.applicationName;
  if (!task.form.deviceFirmware) task.form.deviceFirmware = form.deviceFirmware;
  if (!task.form.datasetGroupId) task.form.datasetGroupId = form.datasetGroupId;
  if (!task.form.datasetGroupName) task.form.datasetGroupName = form.datasetGroupName;
  if (!task.form.dataSources?.length) task.form.dataSources = clone(form.dataSources || []);
  task.form.deleteRawAfterCollect = form.deleteRawAfterCollect !== false;
  task.form.requireManualReview = form.requireManualReview;
  task.form.confidenceThreshold = form.confidenceThreshold;
  task.form.trainParams = withFallbackParams(result?.hyperParams, form.trainParams);
  task.form.convertParams = withFallbackParams(result?.convertParams, form.convertParams);
  task.form.authCode = (result?.authCode ?? form.authCode ?? '').toString();
  task.form.gpuUrlTargetIds = Array.isArray(result?.gpuUrlTargetIds)
    ? result.gpuUrlTargetIds
        .map((id: any) => Number(id))
        .filter((id: number) => Number.isFinite(id) && id > 0)
    : clone(form.gpuUrlTargetIds || []);
  return task;
}

export async function deleteSelfIterationParentTask(id: number): Promise<boolean> {
  try {
    await maHttp.delete(
      { url: `selfIteration/delete/${id}` },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    return true;
  } catch {
    return false;
  }
}

export async function getSelfIterationParentTaskById(
  id: number,
): Promise<SelfIterationParentTask | undefined> {
  try {
    const result = await maHttp.get(
      { url: `selfIteration/detail/${id}` },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    if (!result) return undefined;
    const jobs: RawSelfIterationJob[] = result.jobList ?? [];
    const childTasks = jobs.map((job: RawSelfIterationJob) => adaptJob(job, id));
    const task = adaptTask(result, childTasks);
    task.form = buildDetailForm(result);
    return task;
  } catch {
    return undefined;
  }
}

const childTaskDetailStore = clone(childTaskDetailsMock);

const emptyDataAnnotation = {
  baselineModelName: '-',
  labels: [],
  confidenceThreshold: 0,
  canOperate: false,
  datasetStatuses: [],
};

function adaptChildTaskDetail(raw: any): SelfIterationChildTaskDetail {
  const fallback = Object.values(childTaskDetailStore)[0];
  return {
    id: raw?.id ?? fallback.id,
    parentTaskId: raw?.parentTaskId ?? fallback.parentTaskId,
    round: raw?.round ?? fallback.round,
    status: (raw?.status as any) ?? fallback.status,
    currentStep: (raw?.currentStep as any) ?? fallback.currentStep,
    stepStatus: raw?.stepStatus ?? fallback.stepStatus,
    createTime: raw?.createTime ?? fallback.createTime,
    updateTime: raw?.updateTime ?? fallback.updateTime,
    needManualReview: Boolean(raw?.needManualReview),
    dataCollection: {
      cameras: raw?.dataCollection?.cameras ?? [],
      datasetCascade: raw?.dataCollection?.datasetCascade ?? [],
      overview: raw?.dataCollection?.overview ?? {
        totalDatasetCount: 0,
        totalImageCount: 0,
        totalVideoCount: 0,
        collectedImageCount: 0,
      },
      canRetry: Boolean(raw?.dataCollection?.canRetry),
    } as any,
    dataAnnotation: raw?.dataAnnotation ?? emptyDataAnnotation,
    modelIteration: raw?.modelIteration ?? fallback.modelIteration,
    modelDispatch: raw?.modelDispatch ?? fallback.modelDispatch,
  } as SelfIterationChildTaskDetail;
}

export async function fetchSelfIterationChildTaskDetail(
  childTaskId: number,
): Promise<SelfIterationChildTaskDetail | undefined> {
  try {
    const raw = await maHttp.get(
      { url: `selfIteration/job/${childTaskId}/detail-readonly` },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    if (raw) {
      return adaptChildTaskDetail(raw);
    }
  } catch {
    // ignore and use mock fallback
  }
  const detail = childTaskDetailStore[childTaskId];
  if (detail) {
    return {
      ...clone(detail),
      dataAnnotation: clone(emptyDataAnnotation),
    };
  }
  return {
    ...clone(Object.values(childTaskDetailStore)[0]),
    dataAnnotation: clone(emptyDataAnnotation),
  };
}

export async function manualDispatchSelfIterationJob(jobId: number): Promise<void> {
  await maHttp.post(
    { url: `selfIteration/job/${jobId}/manualDispatch` },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function retrySelfIterationCollect(jobId: number): Promise<boolean> {
  try {
    await maHttp.post(
      { url: `selfIteration/job/${jobId}/retryCollect` },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    return true;
  } catch {
    return false;
  }
}

/**
 * 完成人工审核，继续下一阶段
 */
export async function confirmReviewApi(taskId: number | string): Promise<void> {
  await maHttp.post(
    { url: `/selfIteration/confirmReview/${taskId}` },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function fetchSelfIterationModelArtifacts(jobId: number): Promise<any[]> {
  if (!jobId) return [];
  try {
    const result = await maHttp.get(
      {
        url: 'model/getWeightPathWithSize',
        params: { jobId },
        headers: { ignoreCancelToken: true } as any,
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    return Array.isArray(result) ? result : [];
  } catch {
    return [];
  }
}

export async function downloadSelfIterationArtifact(filePath: string): Promise<void> {
  const rawPath = filePath.startsWith('/weight') ? filePath : `/weight${filePath}`;
  downloadByUrl({
    url: `raw/${rawPath.replace(/^\/+/, '')}`,
    target: '_self',
    fileName: rawPath.split('/').pop() || 'download',
  });
}

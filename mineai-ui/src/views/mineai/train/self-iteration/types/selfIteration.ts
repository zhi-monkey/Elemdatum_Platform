export type StepState = 'wait' | 'process' | 'finish' | 'error';

export type ParentTaskStatus = 'pending' | 'running' | 'paused' | 'completed' | 'failed';

export type ChildTaskStatus =
  | 'pending'
  | 'collecting'
  | 'annotating'
  | 'training'
  | 'converting'
  | 'packaging'
  | 'dispatching'
  | 'completed'
  | 'failed'
  | 'waiting_review';

export type ChildTaskCurrentStep =
  | 'dataCollection'
  | 'dataAnnotation'
  | 'modelIteration'
  | 'modelDispatch';

export type ParamInputType = 'numeric' | 'string' | 'boolean';

export type InitialAutoLabelSource = 'BOUND_DEFAULT' | 'DEFAULT_IMAGE' | 'TRAINED_MODEL';

export type InitialAutoLabelTrainSource = 'STANDARD' | 'GUIDED';

export interface OptionItem {
  label: string;
  value: number;
}

export interface DynamicParamConfig {
  id: number;
  field: string;
  label?: string | null;
  defaultNum?: string;
  defaultValue?: string;
  required: boolean;
  min: number | null;
  max: number | null;
  msg: string;
  type: ParamInputType;
  inputDescription: string;
}

export interface TrainParamItem {
  field: string;
  label: string;
  value: number | string | boolean;
  type: ParamInputType;
  required: boolean;
  min?: number | null;
  max?: number | null;
  msg?: string;
  inputDescription?: string;
}

export interface ConvertParamItem {
  field: string;
  label: string;
  value: number | string | boolean;
  type: ParamInputType;
  required: boolean;
  min?: number | null;
  max?: number | null;
  msg?: string;
  inputDescription?: string;
}

export interface DataSourceConfig {
  httpCameraServerId?: number | null;
  httpCameraId: number | null;
  captureInterval: number;
  imageQuantity: number;
}

export interface SelfIterationParentTaskForm {
  taskName: string;
  applicationName: string | null;
  deviceFirmware: string | null;
  datasetGroupId: number | null;
  datasetGroupName: string;
  dataSources: DataSourceConfig[];
  deleteRawAfterCollect: boolean;
  confidenceThreshold: number;
  requireManualReview: boolean;
  autoLabelImageUrl?: string | null;
  initialAutoLabelSource?: InitialAutoLabelSource;
  initialAutoLabelTrainSource?: InitialAutoLabelTrainSource | null;
  initialAutoLabelModelGenerationId?: number | null;
  initialAutoLabelStandardJobName?: string | null;
  splitSize: string;
  trainParams: Record<string, number | string | boolean>;
  convertParams: Record<string, number | string | boolean>;
  authCode: string;
  // 历史字段名，实际业务语义为“模型接收地址”ID 列表。
  inferenceDeviceIds: number[];
  gpuUrlTargetIds: number[];
  hardwareParamsId: number | null;
  gpuMode: string;
  gpuCount: number;
  maxRounds?: number | null;
  iterationStartImageQuantity?: number | null;
  targetAccuracy: number | null;
  targetRecall: number | null;
  reuseAnnotationModel?: string;
  reuseTrainModel?: string;
  dispatchMode: 'manual' | 'auto';
}

export interface SelfIterationParentTask {
  id: number;
  taskName: string;
  currentIteration: number;
  createTime: string;
  status: ParentTaskStatus;
  form: SelfIterationParentTaskForm;
  childTasks: SelfIterationChildTask[];
  hardwareParamsId?: number | null;
  hardwareParamsTitle?: string | null;
  gpuMode?: string | null;
  gpuCount?: number | null;
  maxRounds?: number | null;
  iterationStartImageQuantity?: number | null;
  targetAccuracy?: number | null;
  targetRecall?: number | null;
  currentRound?: number;
}

export interface SelfIterationStepStatus {
  dataCollection: StepState;
  dataAnnotation: StepState;
  modelIteration: StepState;
  modelDispatch: StepState;
}

export interface SelfIterationChildTask {
  id: number;
  parentTaskId: number;
  round: number;
  status: ChildTaskStatus;
  currentStep: ChildTaskCurrentStep;
  createTime: string;
  updateTime: string;
  trainAccuracy?: number | null;
  trainRecall?: number | null;
  epochDetail?: string | null;
}

export interface CameraCollectionCard {
  rtspSourceName: string;
  cameraName: string;
  requiredImages: number;
  collectedImages: number;
  sampleStatus: 'success' | 'running' | 'failed' | 'pending';
  sampleInterval: number;
  executionId?: number;
  captureTaskId?: number;
  datasetId?: number;
  message?: string;
}

export interface DatasetNode {
  datasetGroupName: string;
  datasets: Array<{
    datasetId?: number;
    datasetName: string;
    imageCount: number;
    videoCount: number;
  }>;
}

export interface DatasetOverview {
  totalDatasetCount: number;
  totalImageCount: number;
  totalVideoCount: number;
  collectedImageCount: number;
}

export interface AnnotationDatasetStatusCard {
  datasetId?: number;
  datasetName: string;
  annotatedImages: number;
  totalImages: number;
  extractedVideos: number;
  totalVideos: number;
  annotateType?: number;
  annotationType: 'auto' | 'manual' | 'hybrid';
  status: 'pending' | 'processing' | 'waiting_review' | 'completed' | 'failed';
}

export interface TrainStageInfo {
  status: ChildTaskStatus;
  startTime: string;
  endTime: string;
  epoch: number;
  learningRate: number;
  batchSize: number;
  map50: number;
  loss: number;
  params: TrainParamItem[];
}

export interface ConvertStageInfo {
  status: ChildTaskStatus;
  framework: string;
  inputSize: string;
  outputType: string;
  startTime: string;
  endTime: string;
  durationSeconds: number;
  params: ConvertParamItem[];
}

export interface PackageFile {
  fileName: string;
  fileSize: string;
  createTime: string;
  downloadUrl: string;
  status: 'ready' | 'generating' | 'failed';
}

export interface DispatchSummary {
  successCount: number;
  failedCount: number;
  dispatchTime: string;
  modelName: string;
  modelVersion: string;
  sourceIteration: number;
}

export interface DispatchTarget {
  targetName: string;
  targetType: 'device' | 'camera' | 'gpuUrl';
  status: 'success' | 'failed' | 'pending';
}

export interface MetricCompareItem {
  metricName: string;
  currentValue: number;
  previousValue: number;
  trend: 'up' | 'down' | 'flat';
}

export interface SelfIterationChildTaskDetail {
  id: number;
  parentTaskId: number;
  round: number;
  status: ChildTaskStatus;
  currentStep: ChildTaskCurrentStep;
  stepStatus: SelfIterationStepStatus;
  createTime: string;
  updateTime: string;
  needManualReview: boolean;
  dataCollection: {
    cameras: CameraCollectionCard[];
    datasetCascade: DatasetNode[];
    overview: DatasetOverview;
    canRetry?: boolean;
  };
  dataAnnotation: {
    baselineModelName: string;
    labels: string[];
    confidenceThreshold: number;
    canOperate: boolean;
    datasetStatuses: AnnotationDatasetStatusCard[];
  };
  modelIteration: {
    train: TrainStageInfo;
    convert: ConvertStageInfo;
    packages: PackageFile[];
    trainJobId?: number | null;
    convertJobId?: number | null;
    packageTaskId?: string | null;
    appZipPath?: string | null;
  };
  modelDispatch: {
    summary: DispatchSummary;
    targets: DispatchTarget[];
    metrics: MetricCompareItem[];
    suggestions: string[];
    autoDeployEnabled: boolean;
    jobId: number;
    status?: 'success' | 'failed' | 'pending' | 'processing' | 'dispatching' | 'completed';
  };
}

export interface SelfIterationFormOptions {
  modelOptions: OptionItem[];
  rtspSourceOptions: OptionItem[];
  modelApplicationOptions: OptionItem[];
  deviceFirmwareOptions: OptionItem[];
  datasetGroupOptions: OptionItem[];
  // 历史字段名，实际业务语义为“模型接收地址”选项。
  inferenceDeviceOptions: OptionItem[];
  gpuUrlTargetOptions: OptionItem[];
  labelOptions: string[];
  trainParamConfigList: DynamicParamConfig[];
  convertParamConfigList: DynamicParamConfig[];
}

<template>
  <a-modal
    :visible="visible"
    :title="modalTitle"
    :width="1300"
    wrap-class-name="self-iteration-modal"
    :body-style="{ padding: '20px 26px 14px', background: '#1f1f1f' }"
    :confirm-loading="loading"
    destroy-on-close
    @cancel="handleClose"
    @ok="handleSubmit"
  >
    <AConfigProvider :get-popup-container="getSelectPopupContainer">
      <a-alert
        v-if="mode === 'edit'"
        type="info"
        show-icon
        message="修改后的参数将从下一轮迭代生效"
        style="margin-bottom: 16px"
      />

      <a-form ref="formRef" :model="formModel" :rules="rules" layout="vertical">
        <a-card title="基础信息" size="small" class="section-card">
          <a-row :gutter="12">
            <a-col :span="12">
              <a-form-item label="任务名称" name="taskName">
                <a-input
                  v-model:value="formModel.taskName"
                  placeholder="请输入任务名称"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="选择应用" name="applicationName">
                <a-select
                  v-model:value="formModel.applicationName"
                  :options="applicationOptions"
                  placeholder="请选择应用"
                  show-search
                  @change="handleApplicationChange"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="选择设备-固件" name="deviceFirmware">
                <a-select
                  v-model:value="formModel.deviceFirmware"
                  :options="deviceFirmwareOptions"
                  placeholder="请选择设备固件"
                  show-search
                  @change="handleDeviceFirmwareChange"
                />
              </a-form-item>
              <div
                v-if="applicationBoundLabels.length > 0"
                style="margin-top: -12px; margin-bottom: 12px"
              >
                <span style="color: #999; font-size: 12px">应用绑定的标签：</span>
                <div style="margin-top: 4px">
                  <a-tag
                    v-for="(label, idx) in applicationBoundLabels"
                    :key="label.id || label.name"
                    :color="LABEL_COLORS[idx % LABEL_COLORS.length]"
                  >
                    {{ label.name }}
                  </a-tag>
                </div>
              </div>
            </a-col>
            <a-col :span="12">
              <a-form-item label="数据集组" name="datasetGroupId">
                <div style="display: flex; gap: 8px">
                  <a-select
                    v-model:value="formModel.datasetGroupId"
                    :options="toSelectOptions(formOptions.datasetGroupOptions)"
                    placeholder="选择已有数据集组"
                    allow-clear
                    style="flex: 1"
                    @change="handleDatasetGroupSelect"
                  />
                  <span style="line-height: 32px; color: #999; white-space: nowrap">或新建：</span>
                  <a-input
                    v-model:value="formModel.datasetGroupName"
                    placeholder="填写新数据集组名称"
                    style="flex: 1"
                    @input="handleDatasetGroupNameInput"
                  />
                </div>
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>

        <a-card title="标注信息" size="small" class="section-card">
          <a-row :gutter="12">
            <a-col :span="24">
              <a-form-item label="首轮自动标注模型来源" name="initialAutoLabelSource">
                <a-radio-group
                  v-model:value="formModel.initialAutoLabelSource"
                  @change="handleInitialAutoLabelSourceChange"
                >
                  <a-radio value="BOUND_DEFAULT">应用任务绑定模型</a-radio>
                  <a-radio value="DEFAULT_IMAGE">默认模型</a-radio>
                  <a-radio value="TRAINED_MODEL">训练模型</a-radio>
                </a-radio-group>
              </a-form-item>
            </a-col>
            <a-col v-if="formModel.initialAutoLabelSource === 'DEFAULT_IMAGE'" :span="12">
              <a-form-item label="首轮自动标注镜像" name="autoLabelImageUrl">
                <a-select
                  v-model:value="formModel.autoLabelImageUrl"
                  placeholder="请选择默认镜像"
                  allow-clear
                  show-search
                  :loading="imageOptionsLoading"
                  :options="imageOptions"
                  @change="handleImageChange"
                />
              </a-form-item>
              <div
                v-if="selectedImageLabels.length > 0"
                style="margin-top: -12px; margin-bottom: 12px"
              >
                <span style="color: #999; font-size: 12px">该镜像支持的标签：</span>
                <div style="margin-top: 4px">
                  <a-tag
                    v-for="(label, idx) in selectedImageLabels"
                    :key="label.id || label.name"
                    :color="LABEL_COLORS[idx % LABEL_COLORS.length]"
                  >
                    {{ label.name }}
                  </a-tag>
                </div>
              </div>
            </a-col>
            <template v-if="formModel.initialAutoLabelSource === 'TRAINED_MODEL'">
              <a-col :span="12">
                <a-form-item label="训练类型" name="initialAutoLabelTrainSource">
                  <a-select
                    v-model:value="formModel.initialAutoLabelTrainSource"
                    style="width: 100%"
                    @change="handleInitialAutoLabelTrainSourceChange"
                  >
                    <a-select-option value="STANDARD">标准化训练</a-select-option>
                    <a-select-option value="GUIDED">引导式训练</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col v-if="formModel.initialAutoLabelTrainSource === 'STANDARD'" :span="12">
                <a-form-item label="训练名称" name="initialAutoLabelModelGenerationId">
                  <a-select
                    v-model:value="formModel.initialAutoLabelModelGenerationId"
                    placeholder="请选择训练名称"
                    allow-clear
                    show-search
                    :loading="trainedGenerationLoading"
                    :options="trainedGenerationOptions"
                    @change="handleStandardGenerationChange"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item
                  :label="
                    formModel.initialAutoLabelTrainSource === 'STANDARD'
                      ? '训练任务'
                      : '引导式训练任务'
                  "
                >
                  <CardSelector
                    v-if="formModel.initialAutoLabelTrainSource === 'STANDARD'"
                    ref="standardCardSelectorRef"
                    type="standard"
                    :request-param="formModel.initialAutoLabelModelGenerationId"
                    :selected-value="formModel.initialAutoLabelStandardJobName"
                    :required-labels="applicationBoundLabelNames"
                    @update:selected="handleStandardTaskSelect"
                  />
                  <CardSelector
                    v-else
                    ref="guidedCardSelectorRef"
                    type="guided"
                    :request-param="guidedCardRequestParam"
                    :application-name="formModel.applicationName"
                    :device-firmware="formModel.deviceFirmware"
                    :selected-value="formModel.initialAutoLabelModelGenerationId"
                    :required-labels="applicationBoundLabelNames"
                    @update:selected="handleGuidedTaskSelect"
                  />
                </a-form-item>
              </a-col>
            </template>
            <a-col :span="12">
              <a-form-item label="人工审核">
                <a-select v-model:value="formModel.requireManualReview" style="width: 100%">
                  <a-select-option :value="true">开启</a-select-option>
                  <a-select-option :value="false">关闭</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="置信度">
                <a-input-number
                  v-model:value="formModel.confidenceThreshold"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="标注模型复用策略">
                <a-select v-model:value="formModel.reuseAnnotationModel" style="width: 100%">
                  <a-select-option value="ALWAYS">始终复用上一轮模型</a-select-option>
                  <a-select-option value="NEVER">始终使用默认模型</a-select-option>
                  <a-select-option value="METRIC_COMPARE">基于指标对比决定</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>

        <a-card title="训练信息" size="small" class="section-card">
          <a-row :gutter="12">
            <a-col :span="12">
              <a-form-item label="训练模型复用策略">
                <a-select v-model:value="formModel.reuseTrainModel" style="width: 100%">
                  <a-select-option value="ALWAYS">始终复用上一轮模型</a-select-option>
                  <a-select-option value="NEVER">始终从头训练</a-select-option>
                  <a-select-option value="METRIC_COMPARE">基于指标对比决定</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <div class="param-group-title">迭代结束条件</div>
            </a-col>
            <a-col :span="8">
              <a-form-item label="最大轮次" name="maxRounds">
                <a-input-number
                  v-model:value="formModel.maxRounds"
                  :min="1"
                  :max="100"
                  placeholder="请输入最大轮次"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="最低准确率" name="targetAccuracy">
                <a-input-number
                  v-model:value="formModel.targetAccuracy"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  placeholder="0-1，小数形式"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="最低召回率" name="targetRecall">
                <a-input-number
                  v-model:value="formModel.targetRecall"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  placeholder="0-1，小数形式"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>

        <a-card title="数据源配置" size="small" class="section-card">
          <a-row :gutter="12">
            <a-col :span="8">
              <a-form-item label="迭代开始图片数量" name="iterationStartImageQuantity">
                <a-input-number
                  v-model:value="formModel.iterationStartImageQuantity"
                  :min="1"
                  :step="100"
                  placeholder="请输入迭代开始图片数量"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="采集后原始图片">
                <a-radio-group v-model:value="formModel.deleteRawAfterCollect">
                  <a-radio :value="true">删除</a-radio>
                  <a-radio :value="false">保留</a-radio>
                </a-radio-group>
              </a-form-item>
            </a-col>
          </a-row>
          <div
            v-for="(source, index) in formModel.dataSources"
            :key="index"
            class="data-source-item"
          >
            <div class="data-source-header">
              <span class="data-source-title">数据源 {{ index + 1 }}</span>
              <a-button
                v-if="formModel.dataSources.length > 1"
                type="link"
                danger
                size="small"
                @click="removeDataSource(index)"
              >
                删除
              </a-button>
            </div>
            <a-row :gutter="12">
              <a-col :span="8">
                <a-form-item
                  :label="`数据收集服务器${index + 1}`"
                  :name="['dataSources', index, 'httpCameraServerId']"
                  :rules="[
                    {
                      required: true,
                      type: 'number',
                      message: '请选择数据收集服务器',
                      trigger: 'change',
                    },
                  ]"
                >
                  <a-select
                    v-model:value="source.httpCameraServerId"
                    :options="httpServerOptions"
                    placeholder="请选择数据收集服务器"
                    show-search
                    @change="handleHttpCameraServerChange(index, $event)"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item
                  :label="`摄像机`"
                  :name="['dataSources', index, 'httpCameraId']"
                  :rules="[
                    {
                      required: true,
                      type: 'number',
                      message: '请选择摄像机',
                      trigger: 'change',
                    },
                  ]"
                >
                  <a-select
                    v-model:value="source.httpCameraId"
                    :options="getAvailableCameraOptions(index)"
                    placeholder="请选择摄像机"
                    show-search
                    :dropdown-match-select-width="false"
                    :dropdown-style="{ width: 'max-content' }"
                    :disabled="!source.httpCameraServerId"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="4">
                <a-form-item
                  :label="`收集间隔(秒)`"
                  :name="['dataSources', index, 'captureInterval']"
                  :rules="[
                    {
                      required: true,
                      type: 'number',
                      message: '请输入收集间隔',
                      trigger: 'change',
                    },
                  ]"
                >
                  <a-input-number
                    v-model:value="source.captureInterval"
                    :min="1"
                    :step="5"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="4">
                <a-form-item
                  :label="`收集总数`"
                  :name="['dataSources', index, 'imageQuantity']"
                  :rules="[
                    {
                      required: true,
                      type: 'number',
                      message: '请输入收集总数',
                      trigger: 'change',
                    },
                  ]"
                >
                  <a-input-number
                    v-model:value="source.imageQuantity"
                    :min="1"
                    :step="100"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </div>
          <a-button type="dashed" block @click="addDataSource" style="margin-top: 8px">
            + 添加数据源
          </a-button>
        </a-card>

        <a-card title="模型下发配置" size="small" class="section-card">
          <a-form-item label="下发方式">
            <a-radio-group v-model:value="formModel.dispatchMode" :disabled="loading">
              <a-radio value="manual">手动下发</a-radio>
              <a-radio value="auto">自动下发</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="推理分析装置地址（可多选）">
            <a-select
              v-model:value="formModel.gpuUrlTargetIds"
              mode="multiple"
              :options="formOptions.gpuUrlTargetOptions"
              :disabled="loading"
              show-search
              option-filter-prop="label"
              placeholder="请绑定接收模型的推理分析装置"
              style="width: 100%"
            />
          </a-form-item>
        </a-card>

        <a-card title="数据集切分配置" size="small" class="section-card">
          <DatasetSplitConfig v-model="formModel.splitSize" />
        </a-card>

        <a-card title="参数配置" size="small" class="section-card">
          <a-row :gutter="12">
            <a-col :span="12">
              <div class="param-group-title">训练参数列表</div>
              <a-form-item
                v-for="config in trainParamConfigList"
                :key="`train-${config.id}`"
                :name="['trainParams', config.field]"
                :label="config.label || config.field"
                :rules="buildDynamicRules(config)"
              >
                <template v-if="config.range?.options && config.range.options.length > 0">
                  <a-select
                    v-model:value="formModel.trainParams[config.field]"
                    :placeholder="config.msg || '请选择'"
                    style="width: 100%"
                  >
                    <a-select-option v-for="opt in config.range.options" :key="opt" :value="opt">
                      {{ opt }}
                    </a-select-option>
                  </a-select>
                </template>
                <template v-else-if="config.type === 'numeric'">
                  <a-input-number
                    v-model:value="formModel.trainParams[config.field]"
                    style="width: 100%"
                    :placeholder="config.msg || '请输入数值'"
                  />
                </template>
                <template v-else-if="config.type === 'boolean'">
                  <a-select
                    v-model:value="formModel.trainParams[config.field]"
                    :placeholder="config.msg || '请选择'"
                    style="width: 100%"
                  >
                    <a-select-option value="true">true</a-select-option>
                    <a-select-option value="false">false</a-select-option>
                  </a-select>
                </template>
                <template v-else>
                  <a-input
                    v-model:value="formModel.trainParams[config.field]"
                    :placeholder="config.msg"
                  />
                </template>
                <div class="field-hint">{{ config.inputDescription }}</div>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <div class="param-group-title">转换参数列表</div>
              <a-form-item
                v-for="config in convertParamConfigList"
                :key="`convert-${config.id}`"
                :name="['convertParams', config.field]"
                :label="config.label || config.field"
                :rules="buildDynamicRules(config)"
              >
                <template v-if="config.range?.options && config.range.options.length > 0">
                  <a-select
                    v-model:value="formModel.convertParams[config.field]"
                    :placeholder="config.msg || '请选择'"
                    style="width: 100%"
                  >
                    <a-select-option v-for="opt in config.range.options" :key="opt" :value="opt">
                      {{ opt }}
                    </a-select-option>
                  </a-select>
                </template>
                <template v-else-if="config.type === 'numeric'">
                  <a-input-number
                    v-model:value="formModel.convertParams[config.field]"
                    style="width: 100%"
                    :placeholder="config.msg || '请输入数值'"
                  />
                </template>
                <template v-else-if="config.type === 'boolean'">
                  <a-select
                    v-model:value="formModel.convertParams[config.field]"
                    :placeholder="config.msg || '请选择'"
                    style="width: 100%"
                  >
                    <a-select-option value="true">true</a-select-option>
                    <a-select-option value="false">false</a-select-option>
                  </a-select>
                </template>
                <template v-else>
                  <a-input
                    v-model:value="formModel.convertParams[config.field]"
                    :placeholder="config.msg"
                  />
                </template>
                <div class="field-hint">{{ config.inputDescription }}</div>
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>

        <a-card title="资源配置" size="small" class="section-card">
          <ResourceCards
            :selected-resource-id="formModel.hardwareParamsId || undefined"
            :initial-gpu-mode="formModel.gpuMode"
            :initial-gpu-count="formModel.gpuCount"
            @select-resource="selectResource"
            @gpu-mode-change="handleGpuModeChange"
            @gpu-count-change="handleGpuCountChange"
          />
        </a-card>
      </a-form>
    </AConfigProvider>
  </a-modal>
</template>

<script setup lang="ts">
  import { computed, nextTick, reactive, ref, watch } from 'vue';
  import {
    Alert as AAlert,
    Button as AButton,
    Card as ACard,
    Col as ACol,
    ConfigProvider as AConfigProvider,
    Form as AForm,
    FormItem as AFormItem,
    Input as AInput,
    InputNumber as AInputNumber,
    Modal as AModal,
    Row as ARow,
    Select as ASelect,
    SelectOption as ASelectOption,
    Tag as ATag,
    Radio as ARadio,
    RadioGroup as ARadioGroup,
    message,
  } from 'ant-design-vue';
  import ResourceCards from '/@/views/mineai/train/generationCreate/ResourceCards.vue';
  import type { ValidationRule } from 'ant-design-vue/lib/form/Form';
  import {
    DynamicParamConfig,
    SelfIterationFormOptions,
    SelfIterationParentTaskForm,
  } from '../types/selfIteration';
  import {
    getDeviceFirmwareByBoundApplicationName,
    getApplicationNameByBoundDeviceFirmware,
    getUniqueModelApplication,
    getTrainConvertAndOtherHyperParams,
  } from '../../generationGuideList/generationGuideList.data';
  import {
    listAllHttpCameras,
    listHttpCameraServer,
    listHttpCameras,
  } from '/@/views/mineai/data/data-backflow/api';
  import type {
    HttpCameraItem,
    HttpCameraServerItem,
  } from '/@/views/mineai/data/data-backflow/api';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { getLabelsByImageUrlApi } from '/@/views/mineai/data/dataset-details2/api';
  import CardSelector from '/@/views/mineai/data/dataset-details2/auto-label/CardSelector.vue';
  import DatasetSplitConfig from './DatasetSplitConfig.vue';

  const LABEL_COLORS = ['pink', 'red', 'orange', 'green', 'cyan', 'blue', 'purple'];

  const props = defineProps<{
    visible: boolean;
    mode: 'create' | 'edit';
    loading: boolean;
    initialValue: SelfIterationParentTaskForm | null;
    formOptions: SelfIterationFormOptions;
  }>();

  const emit = defineEmits<{
    (e: 'close'): void;
    (e: 'submit', value: SelfIterationParentTaskForm): void;
  }>();

  type FormRef = InstanceType<typeof AForm>;

  const formRef = ref<FormRef>();
  const applicationOptions = ref<Array<{ label: string; value: string }>>([]);
  const deviceFirmwareOptions = ref<Array<{ label: string; value: string }>>([]);
  const imageOptions = ref<any[]>([]);
  const imageOptionsLoading = ref(false);
  const trainedGenerationOptions = ref<any[]>([]);
  const trainedGenerationLoading = ref(false);
  const selectedImageLabels = ref<any[]>([]);
  const applicationBoundLabels = ref<any[]>([]);
  const applicationContextRequestId = ref(0);
  const standardCardSelectorRef = ref<InstanceType<typeof CardSelector> | null>(null);
  const guidedCardSelectorRef = ref<InstanceType<typeof CardSelector> | null>(null);
  const httpServerList = ref<HttpCameraServerItem[]>([]);
  const httpCameraList = ref<HttpCameraItem[]>([]);
  const httpCameraMap = reactive<Record<number, HttpCameraItem[]>>({});
  const trainParamConfigList = ref<DynamicParamConfig[]>([
    ...props.formOptions.trainParamConfigList,
  ]);
  const convertParamConfigList = ref<DynamicParamConfig[]>([
    ...props.formOptions.convertParamConfigList,
  ]);

  function resetDynamicParamConfigs() {
    trainParamConfigList.value = [...props.formOptions.trainParamConfigList];
    convertParamConfigList.value = [...props.formOptions.convertParamConfigList];
  }

  // 为每个数据源计算可用的摄像机选项（排除已选择的）
  const httpServerOptions = computed(() =>
    (httpServerList.value || [])
      .filter((server) => Number(server?.isDelete ?? 1) === 0)
      .map((server) => ({
        label: server.name,
        value: server.id,
      })),
  );

  function cameraStatusText(status?: string) {
    const normalizedStatus = (status || '').toUpperCase();
    if (normalizedStatus === 'ONLINE') return '在线';
    if (normalizedStatus === 'INACTIVE') return '未工作';
    if (normalizedStatus === 'DELETED') return '已删除';
    if (normalizedStatus === 'OFFLINE') return '离线';
    return '未知';
  }

  const getAvailableCameraOptions = (currentIndex: number) => {
    const currentSource = formModel.dataSources[currentIndex];
    const serverId = currentSource?.httpCameraServerId;
    if (!serverId) {
      return [];
    }

    const selectedIds = formModel.dataSources
      .map((ds, idx) => (idx !== currentIndex ? ds.httpCameraId : null))
      .filter((id) => id !== null);
    const currentId = currentSource?.httpCameraId;
    const options = (httpCameraMap[serverId] || [])
      .filter(
        (camera) =>
          camera.isDelete === 0 &&
          String(camera.status || '').toUpperCase() !== 'DELETED' &&
          (!selectedIds.includes(camera.id) || camera.id === currentId),
      )
      .map((camera) => ({
        label: `${camera.name} (流ID:${camera.cameraId}) - ${cameraStatusText(camera.status)}`,
        value: camera.id,
      }));

    if (
      currentId &&
      !options.some((option) => option.value === currentId) &&
      Array.isArray(httpCameraMap[serverId])
    ) {
      const currentCamera = httpCameraMap[serverId].find((camera) => camera.id === currentId);
      if (currentCamera) {
        options.unshift({
          label: `${currentCamera.name} (流ID:${currentCamera.cameraId}) - ${cameraStatusText(
            currentCamera.status,
          )}`,
          value: currentCamera.id,
        });
      }
    }

    return options;
  };

  const createDefaultForm = (): SelfIterationParentTaskForm => {
    const trainParams: Record<string, number | string | boolean> = {};
    const convertParams: Record<string, number | string | boolean> = {};

    trainParamConfigList.value.forEach((item) => {
      trainParams[item.field] = getConfigDefaultValue(item);
    });
    convertParamConfigList.value.forEach((item) => {
      convertParams[item.field] = getConfigDefaultValue(item);
    });

    return {
      taskName: '',
      applicationName: null,
      deviceFirmware: null,
      datasetGroupId: null,
      datasetGroupName: '',
      dataSources: [
        { httpCameraServerId: null, httpCameraId: null, captureInterval: 5, imageQuantity: 200 },
      ],
      deleteRawAfterCollect: true,
      confidenceThreshold: 0.5,
      requireManualReview: false,
      autoLabelImageUrl: null,
      initialAutoLabelSource: 'BOUND_DEFAULT',
      initialAutoLabelTrainSource: 'STANDARD',
      initialAutoLabelModelGenerationId: null,
      initialAutoLabelStandardJobName: null,
      splitSize: '8-1-1',
      trainParams,
      convertParams,
      authCode: '',
      inferenceDeviceIds: [],
      gpuUrlTargetIds: [],
      hardwareParamsId: null,
      gpuMode: 'single-exclusive',
      gpuCount: 1,
      maxRounds: 10,
      iterationStartImageQuantity: 150,
      targetAccuracy: 0.8,
      targetRecall: 0.8,
      reuseAnnotationModel: 'ALWAYS',
      reuseTrainModel: 'ALWAYS',
      dispatchMode: 'manual',
    };
  };

  const formModel = reactive<SelfIterationParentTaskForm>(createDefaultForm());

  const modalTitle = computed(() =>
    props.mode === 'create' ? '新增自迭代训练任务' : '编辑自迭代训练任务',
  );

  function getSelectPopupContainer(triggerNode?: HTMLElement): HTMLElement {
    return (
      triggerNode?.closest<HTMLElement>('.self-iteration-modal .ant-modal-body') ||
      document.querySelector<HTMLElement>('.self-iteration-modal .ant-modal-body') ||
      document.body
    );
  }

  const toSelectOptions = (options: Array<{ label: string; value: number }>) => options;
  const applicationBoundLabelNames = computed(() =>
    applicationBoundLabels.value.map((label) => label.name).filter(Boolean),
  );
  const guidedCardRequestParam = computed(() =>
    formModel.applicationName && formModel.deviceFirmware
      ? `${formModel.applicationName}__${formModel.deviceFirmware}`
      : null,
  );

  const rules: Record<string, ValidationRule[]> = {
    taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
    applicationName: [{ required: true, message: '请选择应用', trigger: 'change' }],
    deviceFirmware: [{ required: true, message: '请选择设备固件', trigger: 'change' }],
    maxRounds: [{ required: true, type: 'number', message: '请输入最大轮次', trigger: 'change' }],
    iterationStartImageQuantity: [
      { required: true, type: 'number', message: '请输入迭代开始图片数量', trigger: 'change' },
    ],
    targetAccuracy: [
      { required: true, type: 'number', message: '请输入最低准确率', trigger: 'change' },
    ],
    targetRecall: [
      { required: true, type: 'number', message: '请输入最低召回率', trigger: 'change' },
    ],
  };

  function getConfigDefaultValue(config: DynamicParamConfig): number | string | boolean {
    // 如果有枚举选项，使用第一个选项或 defaultValue
    if (config.range?.options && config.range.options.length > 0) {
      return config.defaultValue || config.range.options[0];
    }

    if (config.type === 'numeric') {
      return Number(config.defaultValue ?? config.defaultNum ?? 0);
    }

    if (config.type === 'boolean') {
      const raw = String(config.defaultValue ?? 'false').toLowerCase();
      return raw === 'true' || raw === '1';
    }

    return config.defaultValue ?? '';
  }

  function buildDynamicRules(config: DynamicParamConfig): ValidationRule[] {
    const dynamicRules: ValidationRule[] = [];

    // 如果有枚举选项，验证值是否在选项中
    if (config.range?.options && config.range.options.length > 0) {
      dynamicRules.push({
        validator: async (_rule, value: unknown) => {
          if (value === null || value === undefined || value === '') {
            if (config.required) {
              return Promise.reject(`${config.label || config.field}不能为空`);
            }
            return Promise.resolve();
          }

          const strValue = String(value);
          if (!config.range!.options!.includes(strValue)) {
            return Promise.reject(`请选择有效的选项`);
          }

          return Promise.resolve();
        },
        trigger: 'change',
      });
      return dynamicRules;
    }

    const range = config.range || { min: null, max: null, options: null };

    if (config.type === 'numeric') {
      dynamicRules.push({
        validator: async (_rule, value: unknown) => {
          if (value === null || value === undefined || value === '') {
            if (config.required) {
              return Promise.reject(`${config.label || config.field}不能为空`);
            }
            return Promise.resolve();
          }

          const numericValue = typeof value === 'number' ? value : Number(value);
          if (!Number.isFinite(numericValue)) {
            return Promise.reject('请输入数字');
          }

          if (range.min !== null && numericValue < range.min) {
            return Promise.reject(`数值需大于等于 ${range.min}`);
          }
          if (range.max !== null && numericValue > range.max) {
            return Promise.reject(`数值需小于等于 ${range.max}`);
          }
          return Promise.resolve();
        },
        trigger: 'change',
      });
    } else if (config.type === 'boolean') {
      dynamicRules.push({
        validator: async (_rule, value: unknown) => {
          if (value === null || value === undefined || value === '') {
            if (config.required) {
              return Promise.reject(`${config.label || config.field}不能为空`);
            }
            return Promise.resolve();
          }

          if (typeof value !== 'boolean' && value !== 'true' && value !== 'false') {
            return Promise.reject('请输入布尔值');
          }

          return Promise.resolve();
        },
        trigger: 'change',
      });
    } else {
      dynamicRules.push({
        validator: async (_rule, value: unknown) => {
          const text = value === null || value === undefined ? '' : String(value).trim();
          if (!text && config.required) {
            return Promise.reject(`${config.label || config.field}不能为空`);
          }

          return Promise.resolve();
        },
        trigger: 'change',
      });
    }

    return dynamicRules;
  }

  function parseOptionalNumber(value: unknown): number | null {
    if (value === null || value === undefined || value === '') {
      return null;
    }
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }

  function sumDataSourceImageQuantity(
    dataSources: SelfIterationParentTaskForm['dataSources'] | undefined,
  ): number | null {
    if (!Array.isArray(dataSources) || dataSources.length === 0) {
      return null;
    }
    const total = dataSources.reduce((sum, source) => {
      const imageQuantity = Number(source?.imageQuantity ?? 0);
      return sum + (Number.isFinite(imageQuantity) && imageQuantity > 0 ? imageQuantity : 0);
    }, 0);
    return total > 0 ? total : null;
  }

  function getDefaultIterationStartImageQuantity(
    dataSources: SelfIterationParentTaskForm['dataSources'] | undefined,
  ): number | null {
    const total = sumDataSourceImageQuantity(dataSources);
    return total == null ? null : Math.max(1, Math.floor(total * 0.75));
  }

  function getDefaultIterationStartImageQuantityByValues(values: unknown[]): number | null {
    const total = values.reduce((sum, value) => {
      const imageQuantity = Number(value ?? 0);
      return sum + (Number.isFinite(imageQuantity) && imageQuantity > 0 ? imageQuantity : 0);
    }, 0);
    return total > 0 ? Math.max(1, Math.floor(total * 0.75)) : null;
  }

  function syncIterationStartImageQuantity(oldDefault: number | null) {
    const nextDefault = getDefaultIterationStartImageQuantity(formModel.dataSources);
    if (
      formModel.iterationStartImageQuantity == null ||
      formModel.iterationStartImageQuantity === oldDefault
    ) {
      formModel.iterationStartImageQuantity = nextDefault;
    }
  }

  function normalizeDynamicParams(
    configList: DynamicParamConfig[],
    source: Record<string, unknown> | undefined,
    fallback: Record<string, number | string | boolean>,
  ): Record<string, number | string | boolean> {
    const normalized: Record<string, number | string | boolean> = {};

    configList.forEach((config) => {
      const rawValue = source?.[config.field];
      if (rawValue === null || rawValue === undefined || rawValue === '') {
        normalized[config.field] = fallback[config.field];
        return;
      }

      if (config.type === 'numeric') {
        const parsed = Number(rawValue);
        normalized[config.field] = Number.isFinite(parsed) ? parsed : fallback[config.field];
        return;
      }

      if (config.type === 'boolean') {
        if (config.range?.options && config.range.options.length > 0) {
          const parsed = String(rawValue).toLowerCase();
          normalized[config.field] = parsed === 'true' || parsed === '1' ? 'true' : 'false';
          return;
        }
        if (typeof rawValue === 'boolean') {
          normalized[config.field] = rawValue ? 'true' : 'false';
          return;
        }
        const parsed = String(rawValue).toLowerCase();
        normalized[config.field] = parsed === 'true' || parsed === '1' ? 'true' : 'false';
        return;
      }

      normalized[config.field] = String(rawValue);
    });

    return normalized;
  }

  function inferParamType(paramRange: string | null): 'numeric' | 'boolean' | 'string' {
    if (!paramRange) return 'string';
    if (paramRange === '{true,false}' || paramRange === '{false,true}') return 'boolean';
    if (paramRange.startsWith('(') || paramRange.startsWith('[')) return 'numeric';
    return 'string';
  }

  function parseParamRange(paramRange: string | null): {
    min: number | null;
    max: number | null;
    options: string[] | null;
  } {
    if (!paramRange) return { min: null, max: null, options: null };

    // 枚举类型 {option1,option2,...}
    if (paramRange.startsWith('{') && paramRange.endsWith('}')) {
      const options = paramRange
        .slice(1, -1)
        .split(',')
        .map((s) => s.trim())
        .filter((s) => s);
      return { min: null, max: null, options };
    }

    // 数值范围 (min,max) 或 [min,max]
    const rangeMatch = paramRange.match(/^[\[\(]([^,]*),([^\]\)]*)[\]\)]$/);
    if (rangeMatch) {
      const minStr = rangeMatch[1].trim();
      const maxStr = rangeMatch[2].trim();
      return {
        min: minStr ? Number(minStr) : null,
        max: maxStr ? Number(maxStr) : null,
        options: null,
      };
    }

    return { min: null, max: null, options: null };
  }

  function applyInitialFormValue(initialValue: SelfIterationParentTaskForm | null) {
    const defaults = createDefaultForm();
    if (!initialValue) {
      Object.assign(formModel, defaults);
      return;
    }

    Object.assign(formModel, {
      ...defaults,
      ...initialValue,
      taskName: initialValue.taskName ?? '',
      applicationName: initialValue.applicationName ?? null,
      deviceFirmware: initialValue.deviceFirmware ?? null,
      datasetGroupId: parseOptionalNumber(initialValue.datasetGroupId),
      datasetGroupName: initialValue.datasetGroupName ?? '',
      confidenceThreshold: Number(initialValue.confidenceThreshold ?? 0.5),
      requireManualReview: Boolean(initialValue.requireManualReview),
      deleteRawAfterCollect: initialValue.deleteRawAfterCollect !== false,
      autoLabelImageUrl: initialValue.autoLabelImageUrl ?? null,
      initialAutoLabelSource:
        initialValue.initialAutoLabelSource ??
        (initialValue.autoLabelImageUrl ? 'DEFAULT_IMAGE' : 'BOUND_DEFAULT'),
      initialAutoLabelTrainSource: initialValue.initialAutoLabelTrainSource ?? 'STANDARD',
      initialAutoLabelModelGenerationId: parseOptionalNumber(
        initialValue.initialAutoLabelModelGenerationId,
      ),
      initialAutoLabelStandardJobName: initialValue.initialAutoLabelStandardJobName ?? null,
      splitSize: initialValue.splitSize || '8-1-1',
      dataSources:
        Array.isArray(initialValue.dataSources) && initialValue.dataSources.length > 0
          ? initialValue.dataSources.map((ds) => ({
              httpCameraServerId: parseOptionalNumber(ds.httpCameraServerId),
              httpCameraId: parseOptionalNumber(ds.httpCameraId),
              captureInterval: Number(ds.captureInterval ?? 5),
              imageQuantity: Number(ds.imageQuantity ?? 200),
            }))
          : defaults.dataSources,
      trainParams: normalizeDynamicParams(
        trainParamConfigList.value,
        initialValue.trainParams,
        defaults.trainParams,
      ),
      convertParams: normalizeDynamicParams(
        convertParamConfigList.value,
        initialValue.convertParams,
        defaults.convertParams,
      ),
      hardwareParamsId: parseOptionalNumber(initialValue.hardwareParamsId),
      gpuMode: initialValue.gpuMode || 'single-exclusive',
      gpuCount: Number(initialValue.gpuCount ?? 1),
      maxRounds: initialValue.maxRounds ?? 10,
      iterationStartImageQuantity:
        initialValue.iterationStartImageQuantity ??
        getDefaultIterationStartImageQuantity(initialValue.dataSources),
      targetAccuracy: initialValue.targetAccuracy ?? null,
      targetRecall: initialValue.targetRecall ?? null,
      reuseAnnotationModel: initialValue.reuseAnnotationModel ?? 'ALWAYS',
      reuseTrainModel: initialValue.reuseTrainModel ?? 'ALWAYS',
      inferenceDeviceIds: Array.isArray(initialValue.inferenceDeviceIds)
        ? initialValue.inferenceDeviceIds
            .map((id) => Number(id))
            .filter((id) => Number.isFinite(id) && id > 0)
        : [],
      gpuUrlTargetIds: Array.isArray(initialValue.gpuUrlTargetIds)
        ? initialValue.gpuUrlTargetIds
            .map((id) => Number(id))
            .filter((id) => Number.isFinite(id) && id > 0)
        : [],
      dispatchMode: initialValue.dispatchMode ?? 'manual',
    });
    if (formModel.iterationStartImageQuantity == null) {
      formModel.iterationStartImageQuantity = getDefaultIterationStartImageQuantity(
        formModel.dataSources,
      );
    }
  }

  function resetFormState() {
    applicationContextRequestId.value += 1;
    resetDynamicParamConfigs();
    Object.assign(formModel, createDefaultForm());
    imageOptions.value = [];
    trainedGenerationOptions.value = [];
    selectedImageLabels.value = [];
    applicationBoundLabels.value = [];
    httpServerList.value = [];
    httpCameraList.value = [];
    Object.keys(httpCameraMap).forEach((key) => delete httpCameraMap[Number(key)]);
    formRef.value?.clearValidate();
  }

  watch(
    () => [props.visible, props.initialValue],
    async () => {
      if (!props.visible) {
        resetFormState();
        return;
      }
      applyInitialFormValue(props.initialValue);
      await Promise.all([
        loadHttpCameraData(),
        loadApplicationOptions(),
        loadDeviceFirmwareOptions(),
      ]);
      await refreshApplicationContext();
      nextTick(() => {
        formRef.value?.clearValidate();
      });
    },
    { immediate: true },
  );

  watch(
    () => formModel.dataSources.map((source) => source.imageQuantity),
    (_newValues, oldValues = []) => {
      syncIterationStartImageQuantity(getDefaultIterationStartImageQuantityByValues(oldValues));
    },
  );

  function handleClose() {
    resetFormState();
    emit('close');
  }

  async function loadHttpCameraData() {
    try {
      httpCameraList.value = [];
      Object.keys(httpCameraMap).forEach((key) => delete httpCameraMap[Number(key)]);
      const servers = await listHttpCameraServer();
      httpServerList.value = servers || [];
      await initializeSelectedCameraServers();
    } catch (error) {
      console.error('Failed to load HTTP camera data:', error);
    }
  }

  async function ensureServerCamerasLoaded(serverId: number) {
    if (!serverId || httpCameraMap[serverId]) {
      return;
    }
    const cameras = await listHttpCameras(serverId);
    httpCameraMap[serverId] = cameras || [];
  }

  async function initializeSelectedCameraServers() {
    const selectedSources = formModel.dataSources.filter((source) => source.httpCameraId);
    if (!selectedSources.length) {
      return;
    }

    const unresolvedSources = selectedSources.filter(
      (source) => !source.httpCameraServerId && source.httpCameraId,
    );
    if (unresolvedSources.length) {
      httpCameraList.value = (await listAllHttpCameras()) || [];
    }

    for (const source of selectedSources) {
      if (!source.httpCameraServerId && source.httpCameraId) {
        const matchedCamera = httpCameraList.value.find(
          (camera) => camera.id === source.httpCameraId,
        );
        source.httpCameraServerId = matchedCamera?.httpCameraServerId ?? null;
      }
      if (source.httpCameraServerId) {
        await ensureServerCamerasLoaded(source.httpCameraServerId);
      }
    }
  }

  async function loadApplicationOptions() {
    try {
      const { getDrawerReleasedApplicationName } = await import(
        '../../generationGuideList/generationGuideList.data'
      );
      applicationOptions.value = await getDrawerReleasedApplicationName();
    } catch (error) {
      console.error('Failed to load applications:', error);
    }
  }

  async function loadDeviceFirmwareOptions() {
    try {
      const { getDrawerReleasedDeviceAndFirmWare } = await import(
        '../../generationGuideList/generationGuideList.data'
      );
      deviceFirmwareOptions.value = await getDrawerReleasedDeviceAndFirmWare();
    } catch (error) {
      console.error('Failed to load device firmwares:', error);
    }
  }

  async function handleSubmit() {
    if (
      formModel.maxRounds == null ||
      formModel.iterationStartImageQuantity == null ||
      formModel.targetAccuracy == null ||
      formModel.targetRecall == null
    ) {
      message.warning('最大轮次、迭代开始图片数量、最低准确率和最低召回率必须填写');
      return;
    }

    const totalImageQuantity = sumDataSourceImageQuantity(formModel.dataSources);
    if (
      totalImageQuantity != null &&
      Number(formModel.iterationStartImageQuantity) > totalImageQuantity
    ) {
      message.warning(`迭代开始图片数量不能大于摄像机采集数量总和 ${totalImageQuantity}`);
      return;
    }

    const initialSource = formModel.initialAutoLabelSource ?? 'BOUND_DEFAULT';
    if (initialSource === 'DEFAULT_IMAGE' && !formModel.autoLabelImageUrl) {
      message.warning('请选择首轮自动标注默认镜像');
      return;
    }
    if (initialSource === 'TRAINED_MODEL') {
      if (!formModel.initialAutoLabelTrainSource) {
        message.warning('请选择首轮自动标注训练类型');
        return;
      }
      if (!formModel.initialAutoLabelModelGenerationId) {
        message.warning('请选择首轮自动标注训练任务');
        return;
      }
      if (
        formModel.initialAutoLabelTrainSource === 'STANDARD' &&
        !formModel.initialAutoLabelStandardJobName
      ) {
        message.warning('请选择标准化训练任务');
        return;
      }
    }

    const defaults = createDefaultForm();
    formModel.trainParams = normalizeDynamicParams(
      trainParamConfigList.value,
      formModel.trainParams,
      defaults.trainParams,
    );
    formModel.convertParams = normalizeDynamicParams(
      convertParamConfigList.value,
      formModel.convertParams,
      defaults.convertParams,
    );
    await nextTick();
    try {
      await formRef.value?.validate();
    } catch (error) {
      console.warn('SelfIteration dynamic form validate failed:', {
        error,
        trainParams: formModel.trainParams,
        convertParams: formModel.convertParams,
      });
      throw error;
    }
    const payload: SelfIterationParentTaskForm = {
      ...formModel,
      autoLabelImageUrl: initialSource === 'DEFAULT_IMAGE' ? formModel.autoLabelImageUrl : null,
      initialAutoLabelSource: initialSource,
      initialAutoLabelTrainSource:
        initialSource === 'TRAINED_MODEL' ? formModel.initialAutoLabelTrainSource : null,
      initialAutoLabelModelGenerationId:
        initialSource === 'TRAINED_MODEL' ? formModel.initialAutoLabelModelGenerationId : null,
      initialAutoLabelStandardJobName:
        initialSource === 'TRAINED_MODEL' && formModel.initialAutoLabelTrainSource === 'STANDARD'
          ? formModel.initialAutoLabelStandardJobName
          : null,
      maxRounds: formModel.maxRounds,
      iterationStartImageQuantity: formModel.iterationStartImageQuantity,
      targetAccuracy: formModel.targetAccuracy,
      targetRecall: formModel.targetRecall,
      trainParams: { ...formModel.trainParams },
      convertParams: { ...formModel.convertParams },
      dataSources: formModel.dataSources.map((ds) => ({ ...ds })),
    };
    console.log('SelfIterationParentTaskForm payload:', payload);
    console.log('trainParams:', payload.trainParams);
    console.log('convertParams:', payload.convertParams);
    console.log('autoLabelImageUrl:', payload.autoLabelImageUrl);
    emit('submit', payload);
  }

  async function handleApplicationChange() {
    clearApplicationPreview();
    if (!formModel.applicationName) return;
    const requestId = applicationContextRequestId.value;
    const nextDeviceFirmwareOptions = await getDeviceFirmwareByBoundApplicationName(
      formModel.applicationName,
    );
    if (requestId !== applicationContextRequestId.value) return;
    deviceFirmwareOptions.value = nextDeviceFirmwareOptions;
    if (
      formModel.deviceFirmware &&
      !deviceFirmwareOptions.value.some((option) => option.value === formModel.deviceFirmware)
    ) {
      formModel.deviceFirmware = null;
    }
    await refreshApplicationContext();
  }

  async function handleDeviceFirmwareChange() {
    clearApplicationPreview();
    if (!formModel.deviceFirmware) return;
    const requestId = applicationContextRequestId.value;
    if (!formModel.applicationName) {
      const nextApplicationOptions = await getApplicationNameByBoundDeviceFirmware(
        formModel.deviceFirmware,
      );
      if (requestId !== applicationContextRequestId.value) return;
      applicationOptions.value = nextApplicationOptions;
    }
    await refreshApplicationContext();
  }

  function clearApplicationPreview() {
    applicationContextRequestId.value += 1;
    applicationBoundLabels.value = [];
    imageOptions.value = [];
    trainedGenerationOptions.value = [];
    selectedImageLabels.value = [];
    imageOptionsLoading.value = false;
    trainedGenerationLoading.value = false;
    formModel.initialAutoLabelModelGenerationId = null;
    formModel.initialAutoLabelStandardJobName = null;
  }

  function handleInitialAutoLabelSourceChange() {
    if (formModel.initialAutoLabelSource === 'DEFAULT_IMAGE') {
      formModel.initialAutoLabelTrainSource = null;
      formModel.initialAutoLabelModelGenerationId = null;
      formModel.initialAutoLabelStandardJobName = null;
      return;
    }

    if (formModel.initialAutoLabelSource === 'TRAINED_MODEL') {
      formModel.autoLabelImageUrl = null;
      selectedImageLabels.value = [];
      formModel.initialAutoLabelTrainSource = formModel.initialAutoLabelTrainSource || 'STANDARD';
      if (formModel.initialAutoLabelTrainSource === 'GUIDED') {
        formModel.initialAutoLabelModelGenerationId = null;
        formModel.initialAutoLabelStandardJobName = null;
      }
      return;
    }

    formModel.autoLabelImageUrl = null;
    selectedImageLabels.value = [];
    formModel.initialAutoLabelTrainSource = null;
    formModel.initialAutoLabelModelGenerationId = null;
    formModel.initialAutoLabelStandardJobName = null;
  }

  function handleInitialAutoLabelTrainSourceChange() {
    formModel.autoLabelImageUrl = null;
    selectedImageLabels.value = [];
    formModel.initialAutoLabelModelGenerationId = null;
    formModel.initialAutoLabelStandardJobName = null;
  }

  function handleStandardGenerationChange() {
    formModel.initialAutoLabelStandardJobName = null;
  }

  function handleStandardTaskSelect(payload: any) {
    formModel.initialAutoLabelStandardJobName = payload?.key ?? null;
  }

  function handleGuidedTaskSelect(payload: any) {
    formModel.initialAutoLabelModelGenerationId = payload?.key ?? null;
  }

  async function refreshApplicationContext() {
    if (!formModel.applicationName || !formModel.deviceFirmware) {
      clearApplicationPreview();
      return;
    }

    const requestId = applicationContextRequestId.value + 1;
    applicationContextRequestId.value = requestId;
    applicationBoundLabels.value = [];
    selectedImageLabels.value = [];
    imageOptions.value = [];

    await Promise.all([
      loadTrainingParameters(requestId),
      loadImageOptions(requestId),
      loadTrainedGenerationOptions(requestId),
      loadApplicationBoundLabels(requestId),
    ]);
  }

  async function loadImageOptions(requestId = applicationContextRequestId.value) {
    if (!formModel.applicationName || !formModel.deviceFirmware) return;

    try {
      imageOptionsLoading.value = true;
      const modelApp = await getUniqueModelApplication(
        formModel.applicationName,
        formModel.deviceFirmware,
      );
      const annotationType = modelApp?.model?.trainModelVersion?.annotationType;

      const res = await maHttp.get(
        {
          url: '/modelVersion/findMatchedAutoLabelImages',
          params: {
            applicationName: formModel.applicationName,
            deviceFirmware: formModel.deviceFirmware,
            ...(annotationType ? { annotationType } : {}),
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      // 手动映射为 { label, value } 格式
      if (requestId !== applicationContextRequestId.value) return;
      imageOptions.value = Array.isArray(res)
        ? res.map((item) => ({ label: item.name, value: item.url }))
        : [];
      if (
        formModel.autoLabelImageUrl &&
        !imageOptions.value.some((option) => option.value === formModel.autoLabelImageUrl)
      ) {
        formModel.autoLabelImageUrl = null;
        selectedImageLabels.value = [];
      }
    } catch (error) {
      console.error('加载镜像列表失败:', error);
    } finally {
      if (requestId === applicationContextRequestId.value) {
        imageOptionsLoading.value = false;
      }
    }
  }

  async function loadTrainedGenerationOptions(requestId = applicationContextRequestId.value) {
    trainedGenerationOptions.value = [];
    if (!formModel.applicationName || !formModel.deviceFirmware) return;

    try {
      trainedGenerationLoading.value = true;
      const res = await maHttp.get(
        {
          url: '/modelGeneration/findModelGenerationSupportAutoLabelByApplication',
          params: {
            applicationName: formModel.applicationName,
            deviceFirmware: formModel.deviceFirmware,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      if (requestId !== applicationContextRequestId.value) return;
      trainedGenerationOptions.value = Array.isArray(res)
        ? res.map((item: any) => ({
            label: item.name,
            value: item.id,
          }))
        : [];
      if (
        formModel.initialAutoLabelTrainSource === 'STANDARD' &&
        formModel.initialAutoLabelModelGenerationId &&
        !trainedGenerationOptions.value.some(
          (option) => option.value === formModel.initialAutoLabelModelGenerationId,
        )
      ) {
        formModel.initialAutoLabelModelGenerationId = null;
        formModel.initialAutoLabelStandardJobName = null;
      }
    } catch (error) {
      console.error('加载训练模型列表失败:', error);
    } finally {
      if (requestId === applicationContextRequestId.value) {
        trainedGenerationLoading.value = false;
      }
    }
  }

  async function handleImageChange(url: string) {
    const requestId = applicationContextRequestId.value;
    selectedImageLabels.value = [];
    if (!url) return;

    try {
      const labels = await getLabelsByImageUrlApi(url);
      if (requestId !== applicationContextRequestId.value) return;
      selectedImageLabels.value = labels || [];
    } catch (error) {
      console.error('获取镜像标签失败:', error);
    }
  }

  async function loadApplicationBoundLabels(requestId = applicationContextRequestId.value) {
    applicationBoundLabels.value = [];
    if (!formModel.applicationName || !formModel.deviceFirmware) return;

    try {
      const modelApp = await getUniqueModelApplication(
        formModel.applicationName,
        formModel.deviceFirmware,
      );
      if (requestId !== applicationContextRequestId.value) return;
      if (!modelApp?.id) return;

      const labels = await maHttp.get(
        {
          url: `/modelApplication/getBoundLabels/${modelApp.id}`,
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      if (requestId !== applicationContextRequestId.value) return;
      applicationBoundLabels.value = Array.isArray(labels)
        ? labels.map((item: any) => ({ id: item.id, name: item.name }))
        : [];
    } catch (error) {
      console.error('获取应用绑定标签失败:', error);
    }
  }

  async function loadTrainingParameters(requestId = applicationContextRequestId.value) {
    if (!formModel.applicationName || !formModel.deviceFirmware) return;
    try {
      const modelApp = await getUniqueModelApplication(
        formModel.applicationName,
        formModel.deviceFirmware,
      );
      if (requestId !== applicationContextRequestId.value) return;
      if (!modelApp?.model?.id) return;

      const params = await getTrainConvertAndOtherHyperParams(modelApp.model.id);
      if (requestId !== applicationContextRequestId.value) return;
      if (!params) return;

      if (params.trainHyperParamsWithValue && Array.isArray(params.trainHyperParamsWithValue)) {
        const trainConfigs = params.trainHyperParamsWithValue.map((p: any) => ({
          field: p.paramName,
          label: p.paramName,
          type: inferParamType(p.paramRange),
          required: true,
          defaultValue: p.trainDefaultValue || p.imageDefaultValue || '',
          description: p.paramDescription || '',
          range: parseParamRange(p.paramRange),
        }));
        trainParamConfigList.value = trainConfigs;
        trainConfigs.forEach((config: DynamicParamConfig) => {
          formModel.trainParams[config.field] = getConfigDefaultValue(config);
        });
      }

      if (params.convertHyperParamsWithValue && Array.isArray(params.convertHyperParamsWithValue)) {
        const convertConfigs = params.convertHyperParamsWithValue.map((p: any) => ({
          field: p.paramName,
          label: p.paramName,
          type: inferParamType(p.paramRange),
          required: true,
          defaultValue: p.trainDefaultValue || p.imageDefaultValue || '',
          description: p.paramDescription || '',
          range: parseParamRange(p.paramRange),
        }));
        convertParamConfigList.value = convertConfigs;
        convertConfigs.forEach((config: DynamicParamConfig) => {
          formModel.convertParams[config.field] = getConfigDefaultValue(config);
        });
      }
    } catch (error) {
      console.error('Failed to load training parameters:', error);
    }
  }

  function handleDatasetGroupSelect() {
    if (formModel.datasetGroupId) {
      formModel.datasetGroupName = '';
    }
  }

  function handleDatasetGroupNameInput() {
    if (formModel.datasetGroupName) {
      formModel.datasetGroupId = null;
    }
  }

  function addDataSource() {
    const oldDefault = getDefaultIterationStartImageQuantity(formModel.dataSources);
    formModel.dataSources.push({
      httpCameraServerId: null,
      httpCameraId: null,
      captureInterval: 5,
      imageQuantity: 200,
    });
    syncIterationStartImageQuantity(oldDefault);
  }

  function removeDataSource(index: number) {
    if (formModel.dataSources.length > 1) {
      const oldDefault = getDefaultIterationStartImageQuantity(formModel.dataSources);
      formModel.dataSources.splice(index, 1);
      syncIterationStartImageQuantity(oldDefault);
    }
  }

  async function handleHttpCameraServerChange(index: number, serverId: number) {
    const source = formModel.dataSources[index];
    if (!source) {
      return;
    }
    source.httpCameraServerId = parseOptionalNumber(serverId);
    source.httpCameraId = null;
    if (source.httpCameraServerId) {
      await ensureServerCamerasLoaded(source.httpCameraServerId);
    }
  }

  // 资源配置相关方法
  function selectResource(id: number) {
    formModel.hardwareParamsId = id;
  }

  function handleGpuModeChange(data: { tabType: string; mode: string; gpuCount: number }) {
    formModel.gpuMode = data.mode;
    if (data.mode === 'multi-exclusive' || data.mode === 'multi-shared') {
      formModel.gpuCount = data.gpuCount;
    } else {
      formModel.gpuCount = 1;
    }
  }

  function handleGpuCountChange(data: { tabType: string; mode: string; gpuCount: number }) {
    if (data.mode === 'multi-exclusive' || data.mode === 'multi-shared') {
      formModel.gpuCount = data.gpuCount;
      formModel.gpuMode = data.mode;
    }
  }
</script>

<style scoped>
  .field-hint {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.6);
    line-height: 1.4;
    margin-top: 2px;
  }

  .param-group-title {
    margin-bottom: 12px;
    font-weight: 600;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.92);
  }

  .section-card {
    margin-bottom: 16px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  }

  .section-card:hover {
    transform: translateY(-2px);
    border-color: rgba(24, 144, 255, 0.32);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.25);
  }

  .data-source-item {
    padding: 12px;
    margin-bottom: 12px;
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 4px;
    background: rgba(255, 255, 255, 0.02);
  }

  .data-source-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  .data-source-title {
    font-weight: 500;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.85);
  }

  .section-card :deep(.ant-card-head) {
    min-height: 40px;
    padding: 0 16px;
    background: #1f1f1f;
    border-bottom-color: rgba(255, 255, 255, 0.1);
  }

  .section-card :deep(.ant-card-head-title) {
    padding: 10px 0;
    font-weight: 600;
  }

  .section-card :deep(.ant-card-body) {
    padding: 16px 16px 6px;
    background: #1f1f1f;
  }

  :deep(.ant-modal-body) {
    max-height: 72vh;
    overflow-y: auto;
  }

  :deep(.self-iteration-modal .ant-modal-header) {
    background: #1f1f1f;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    padding: 14px 26px;
  }

  :deep(.self-iteration-modal .ant-modal-content) {
    background: #1f1f1f;
  }

  :deep(.self-iteration-modal .ant-select-dropdown) {
    max-height: min(320px, 48vh);
    overflow: hidden;
  }

  :deep(.self-iteration-modal .ant-select-dropdown .rc-virtual-list-holder) {
    max-height: min(300px, 44vh) !important;
  }

  :deep(.self-iteration-modal .ant-modal-footer) {
    background: #1f1f1f;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    padding: 12px 26px;
  }

  :deep(.self-iteration-modal .ant-form-item-label > label) {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.86);
  }

  :deep(.self-iteration-modal .ant-input-number),
  :deep(.self-iteration-modal .ant-select-selector),
  :deep(.self-iteration-modal .ant-input) {
    border-color: rgba(255, 255, 255, 0.14);
  }

  :deep(.self-iteration-modal .ant-input:hover),
  :deep(.self-iteration-modal .ant-input-number:hover),
  :deep(.self-iteration-modal .ant-select-selector:hover) {
    border-color: rgba(24, 144, 255, 0.42) !important;
  }
</style>

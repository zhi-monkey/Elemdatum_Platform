<template>
  <PageWrapper
    @back="goBack"
    contentClass="page-content"
    :contentStyle="{ margin: '0', padding: '12px', background: '#181d31' }"
  >
    <template #title>自迭代任务详情</template>

    <a-spin :spinning="loading">
      <template v-if="isParentView && parentTask && parentFormModel">
        <a-card size="small" class="summary-card">
          <a-descriptions :column="4" size="small">
            <a-descriptions-item label="父任务ID">{{ parentTask.id }}</a-descriptions-item>
            <a-descriptions-item label="任务名称">{{ parentTask.taskName }}</a-descriptions-item>
            <a-descriptions-item label="当前迭代轮次"
              >第 {{ parentTask.currentIteration }} 轮</a-descriptions-item
            >
            <a-descriptions-item label="状态"
              >{{ parentStatusText(parentTask.status) }}
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-row :gutter="14">
          <a-col :xs="24" :xl="14">
            <a-card title="参数配置" size="small" class="section-card">
              <a-form layout="vertical">
                <a-row :gutter="12">
                  <a-col :span="12">
                    <a-form-item label="任务名称">
                      <a-input v-model:value="parentFormModel.taskName" :disabled="!isEditing" />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="应用">
                      <a-select
                        v-model:value="parentFormModel.applicationName"
                        :options="applicationOptions"
                        show-search
                        :disabled="!isEditing"
                        @change="handleApplicationChange"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="设备固件">
                      <a-select
                        v-model:value="parentFormModel.deviceFirmware"
                        :options="deviceFirmwareOptions"
                        show-search
                        :disabled="!isEditing"
                        @change="handleDeviceFirmwareChange"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="24">
                    <a-form-item label="数据集组">
                      <div style="display: flex; gap: 8px">
                        <a-select
                          v-model:value="parentFormModel.datasetGroupId"
                          :options="datasetGroupOptionsForForm"
                          :field-names="{ label: 'label', value: 'value' }"
                          allow-clear
                          style="flex: 1"
                          :disabled="!isEditing"
                          @change="handleDatasetGroupSelect"
                        />
                        <span style="line-height: 32px; white-space: nowrap">或新建：</span>
                        <a-input
                          v-model:value="parentFormModel.datasetGroupName"
                          style="flex: 1"
                          :disabled="!isEditing"
                          @input="handleDatasetGroupNameInput"
                        />
                      </div>
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="结果置信度">
                      <a-input-number
                        v-model:value="parentFormModel.confidenceThreshold"
                        :min="0"
                        :max="1"
                        :step="0.01"
                        :disabled="!isEditing"
                        style="width: 100%"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="人工审核">
                      <a-switch
                        v-model:checked="parentFormModel.requireManualReview"
                        :disabled="!isEditing"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="首轮自动标注镜像">
                      <a-input v-model:value="parentFormModel.autoLabelImageUrl" disabled />
                    </a-form-item>
                  </a-col>
                  <a-col :span="24">
                    <div class="config-group-title">迭代结束条件</div>
                  </a-col>
                  <a-col :span="8">
                    <a-form-item label="最大轮次">
                      <a-input-number
                        v-model:value="parentFormModel.maxRounds"
                        :min="1"
                        :max="100"
                        placeholder="请输入最大轮次"
                        style="width: 100%"
                        :disabled="!isEditing"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="8">
                    <a-form-item label="最低准确率">
                      <a-input-number
                        v-model:value="parentFormModel.targetAccuracy"
                        :min="0"
                        :max="1"
                        :step="0.01"
                        placeholder="0-1，小数形式"
                        style="width: 100%"
                        :disabled="!isEditing"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="8">
                    <a-form-item label="最低召回率">
                      <a-input-number
                        v-model:value="parentFormModel.targetRecall"
                        :min="0"
                        :max="1"
                        :step="0.01"
                        placeholder="0-1，小数形式"
                        style="width: 100%"
                        :disabled="!isEditing"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="标注模型复用策略">
                      <a-select
                        v-model:value="parentFormModel.reuseAnnotationModel"
                        style="width: 100%"
                        :disabled="!isEditing"
                      >
                        <a-select-option value="ALWAYS">始终复用上一轮模型</a-select-option>
                        <a-select-option value="NEVER">始终使用默认模型</a-select-option>
                        <a-select-option value="METRIC_COMPARE">基于指标对比决定</a-select-option>
                      </a-select>
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="训练模型复用策略">
                      <a-select
                        v-model:value="parentFormModel.reuseTrainModel"
                        style="width: 100%"
                        :disabled="!isEditing"
                      >
                        <a-select-option value="ALWAYS">始终复用上一轮模型</a-select-option>
                        <a-select-option value="NEVER">始终从头训练</a-select-option>
                        <a-select-option value="METRIC_COMPARE">基于指标对比决定</a-select-option>
                      </a-select>
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-divider orientation="left">数据源配置</a-divider>
                <a-row :gutter="12">
                  <a-col :span="8">
                    <a-form-item label="迭代开始图片数量">
                      <a-input-number
                        v-model:value="parentFormModel.iterationStartImageQuantity"
                        :min="1"
                        :step="100"
                        placeholder="请输入迭代开始图片数量"
                        style="width: 100%"
                        :disabled="!isEditing"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>
                <div
                  v-for="(source, index) in parentFormModel.dataSources"
                  :key="`ds-${index}`"
                  style="
                    padding: 10px;
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    margin-bottom: 10px;
                    border-radius: 6px;
                  "
                >
                  <a-row :gutter="12">
                    <a-col :span="12">
                      <a-form-item :label="`摄像机 ${index + 1}`">
                        <a-select
                          v-model:value="source.httpCameraId"
                          :options="getCameraOptions(index)"
                          show-search
                          :disabled="!isEditing"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="6">
                      <a-form-item label="采样间隔(秒)">
                        <a-input-number
                          v-model:value="source.captureInterval"
                          :min="1"
                          style="width: 100%"
                          :disabled="!isEditing"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="6">
                      <a-form-item label="采样数量">
                        <a-input-number
                          v-model:value="source.imageQuantity"
                          :min="1"
                          style="width: 100%"
                          :disabled="!isEditing"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </div>

                <a-form-item label="数据集切分配置">
                  <DatasetSplitConfig v-model="parentFormModel.splitSize" :disabled="!isEditing" />
                </a-form-item>

                <a-divider orientation="left">训练参数</a-divider>
                <a-row :gutter="12" v-if="trainRenderConfigs.length">
                  <a-col
                    v-for="config in trainRenderConfigs"
                    :key="`train-${config.field}`"
                    :xs="24"
                    :md="12"
                  >
                    <a-form-item :label="config.label || config.field">
                      <a-select
                        v-if="config.range?.options && config.range.options.length"
                        v-model:value="parentFormModel.trainParams[config.field]"
                        style="width: 100%"
                        :disabled="!isEditing"
                      >
                        <a-select-option
                          v-for="opt in config.range.options"
                          :key="opt"
                          :value="opt"
                          >{{ opt }}</a-select-option
                        >
                      </a-select>
                      <a-input-number
                        v-else-if="config.type === 'numeric'"
                        v-model:value="parentFormModel.trainParams[config.field]"
                        style="width: 100%"
                        :disabled="!isEditing"
                      />
                      <a-select
                        v-else-if="config.type === 'boolean'"
                        v-model:value="parentFormModel.trainParams[config.field]"
                        style="width: 100%"
                        :disabled="!isEditing"
                      >
                        <a-select-option value="true">true</a-select-option>
                        <a-select-option value="false">false</a-select-option>
                      </a-select>
                      <a-input
                        v-else
                        v-model:value="parentFormModel.trainParams[config.field]"
                        :disabled="!isEditing"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>
                <a-row :gutter="12" v-else>
                  <a-col
                    v-for="(_value, key) in parentFormModel.trainParams"
                    :key="`train-fallback-${key}`"
                    :xs="24"
                    :md="12"
                  >
                    <a-form-item :label="String(key)">
                      <a-input
                        v-model:value="parentFormModel.trainParams[key]"
                        :disabled="!isEditing"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-divider orientation="left">转换参数</a-divider>
                <a-row :gutter="12" v-if="convertRenderConfigs.length">
                  <a-col
                    v-for="config in convertRenderConfigs"
                    :key="`convert-${config.field}`"
                    :xs="24"
                    :md="12"
                  >
                    <a-form-item :label="config.label || config.field">
                      <a-select
                        v-if="config.range?.options && config.range.options.length"
                        v-model:value="parentFormModel.convertParams[config.field]"
                        style="width: 100%"
                        :disabled="!isEditing"
                      >
                        <a-select-option
                          v-for="opt in config.range.options"
                          :key="opt"
                          :value="opt"
                          >{{ opt }}</a-select-option
                        >
                      </a-select>
                      <a-input-number
                        v-else-if="config.type === 'numeric'"
                        v-model:value="parentFormModel.convertParams[config.field]"
                        style="width: 100%"
                        :disabled="!isEditing"
                      />
                      <a-select
                        v-else-if="config.type === 'boolean'"
                        v-model:value="parentFormModel.convertParams[config.field]"
                        style="width: 100%"
                        :disabled="!isEditing"
                      >
                        <a-select-option value="true">true</a-select-option>
                        <a-select-option value="false">false</a-select-option>
                      </a-select>
                      <a-input
                        v-else
                        v-model:value="parentFormModel.convertParams[config.field]"
                        :disabled="!isEditing"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>
                <a-row :gutter="12" v-else>
                  <a-col
                    v-for="(_value, key) in parentFormModel.convertParams"
                    :key="`convert-fallback-${key}`"
                    :xs="24"
                    :md="12"
                  >
                    <a-form-item :label="String(key)">
                      <a-input
                        v-model:value="parentFormModel.convertParams[key]"
                        :disabled="!isEditing"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-form-item label="下发方式">
                  <a-radio-group
                    v-model:value="parentFormModel.dispatchMode"
                    :disabled="!isEditing"
                  >
                    <a-radio value="manual">手动下发</a-radio>
                    <a-radio value="auto">自动下发</a-radio>
                  </a-radio-group>
                </a-form-item>

                <a-form-item label="推理分析装置地址（可多选）">
                  <a-select
                    v-model:value="parentFormModel.gpuUrlTargetIds"
                    mode="multiple"
                    :options="formOptions.gpuUrlTargetOptions"
                    :disabled="!isEditing"
                    show-search
                    option-filter-prop="label"
                    placeholder="请绑定接收模型的推理分析装置"
                    style="width: 100%"
                  />
                </a-form-item>

                <div class="form-actions">
                  <a-space>
                    <a-button v-if="isEditing" @click="cancelEdit">取消</a-button>
                    <a-button
                      v-if="isEditing"
                      type="primary"
                      :loading="saving"
                      @click="saveParentConfig"
                      >保存配置</a-button
                    >
                    <a-button
                      v-if="
                        !isEditing &&
                        parentTask.currentIteration === 0 &&
                        parentTask.status === 'pending'
                      "
                      type="primary"
                      ghost
                      :loading="starting"
                      @click="startIteration"
                    >
                      开始迭代
                    </a-button>
                  </a-space>
                </div>
              </a-form>
            </a-card>

            <a-card title="资源配置" size="small" class="section-card">
              <ResourceCards
                :selected-resource-id="parentFormModel.hardwareParamsId || undefined"
                :initial-gpu-mode="parentFormModel.gpuMode"
                :initial-gpu-count="parentFormModel.gpuCount"
                @select-resource="
                  (id) => {
                    if (isEditing) parentFormModel.hardwareParamsId = id;
                  }
                "
                @gpu-mode-change="
                  (data) => {
                    if (isEditing) {
                      parentFormModel.gpuMode = data.mode;
                      parentFormModel.gpuCount =
                        data.mode === 'multi-exclusive' || data.mode === 'multi-shared'
                          ? data.gpuCount
                          : 1;
                    }
                  }
                "
                @gpu-count-change="
                  (data) => {
                    if (
                      isEditing &&
                      (data.mode === 'multi-exclusive' || data.mode === 'multi-shared')
                    ) {
                      parentFormModel.gpuCount = data.gpuCount;
                      parentFormModel.gpuMode = data.mode;
                    }
                  }
                "
              />
            </a-card>
          </a-col>

          <a-col :xs="24" :xl="10">
            <a-card title="每轮迭代效果" size="small" class="section-card">
              <a-empty
                v-if="parentTask.currentIteration === 0"
                description="暂无结果，开始首轮迭代后展示"
              />
              <div v-else class="chart-stack">
                <ParentLossBar :data="curveData" />
                <ParentAccBar :data="curveData" />
              </div>
            </a-card>

            <a-card title="迭代轮次列表" size="small" class="section-card table-card">
              <a-table
                :columns="roundColumns"
                :data-source="sortedChildTasks"
                :pagination="false"
                size="small"
                row-key="id"
              />
            </a-card>
          </a-col>
        </a-row>
      </template>

      <template v-else-if="detail">
        <a-card size="small" class="summary-card">
          <a-descriptions :column="4" size="small">
            <a-descriptions-item label="父任务ID">{{ detail.parentTaskId }}</a-descriptions-item>
            <a-descriptions-item label="自迭代任务ID">{{ detail.id }}</a-descriptions-item>
            <a-descriptions-item label="迭代轮次">第 {{ detail.round }} 轮</a-descriptions-item>
            <a-descriptions-item label="状态"
              >{{ childStatusText(detail.status) }}
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-steps
          v-model:current="currentStep"
          type="navigation"
          size="small"
          class="step-navigation"
        >
          <a-step title="数据采集" :status="detail.stepStatus.dataCollection" />
          <a-step title="数据标注" :status="detail.stepStatus.dataAnnotation" />
          <a-step title="模型迭代" :status="detail.stepStatus.modelIteration" />
          <a-step title="模型下发" :status="detail.stepStatus.modelDispatch" />
        </a-steps>

        <div class="step-panel">
          <DataCollectionStep
            v-if="currentStep === 0"
            :detail="detail"
            @retry-collect="retryCollect"
          />
          <DataAnnotationStep
            v-if="currentStep === 1"
            :detail="detail"
            :confirm-loading="confirmingReview"
            @go-review="goReview"
            @confirm-review="confirmReview"
          />
          <ModelIterationStep v-if="currentStep === 2" :detail="detail" />
          <ModelDispatchStep
            v-if="currentStep === 3"
            :detail="detail"
            @refresh="loadChildTaskDetail"
          />
        </div>
      </template>

      <a-empty v-else description="未找到任务详情" />
    </a-spin>
    <SelfIterationStartModal @register="registerStartModal" @success="handleStartSuccess" />
  </PageWrapper>
</template>

<script setup lang="ts">
  import { computed, h, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
  import {
    Button as AButton,
    Card as ACard,
    Col as ACol,
    Descriptions as ADescriptions,
    DescriptionsItem as ADescriptionsItem,
    Divider as ADivider,
    Empty as AEmpty,
    Form as AForm,
    FormItem as AFormItem,
    Input as AInput,
    InputNumber as AInputNumber,
    Row as ARow,
    Select as ASelect,
    SelectOption as ASelectOption,
    Space as ASpace,
    Spin as ASpin,
    Radio as ARadio,
    RadioGroup as ARadioGroup,
    Step as AStep,
    Steps as ASteps,
    Table as ATable,
    Tag as ATag,
  } from 'ant-design-vue';
  import { useRoute, useRouter } from 'vue-router';
  import { PageWrapper } from '/@/components/Page';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useModal } from '/@/components/Modal';
  import DataCollectionStep from '../components/steps/DataCollectionStep.vue';
  import DataAnnotationStep from '../components/steps/DataAnnotationStep.vue';
  import ModelIterationStep from '../components/steps/ModelIterationStep.vue';
  import ModelDispatchStep from '../components/steps/ModelDispatchStep.vue';
  import ParentLossBar from './components/ParentLossBar.vue';
  import ParentAccBar from './components/ParentAccBar.vue';
  import ResourceCards from '/@/views/mineai/train/generationCreate/ResourceCards.vue';
  import SelfIterationStartModal from '../components/SelfIterationStartModal.vue';
  import {
    confirmReviewApi,
    fetchSelfIterationChildTaskDetail,
    fetchSelfIterationFormOptions,
    getSelfIterationParentTaskById,
    retrySelfIterationCollect,
    updateSelfIterationParentTask,
  } from '../api/selfIteration';
  import { listAllHttpCameras } from '/@/views/mineai/data/data-backflow/api';
  import { getDatasetById, queryFirstImg } from '/@/views/mineai/data/dataset-details2/api';
  import {
    getApplicationNameByBoundDeviceFirmware,
    getDeviceFirmwareByBoundApplicationName,
    getTrainConvertAndOtherHyperParams,
    getUniqueModelApplication,
  } from '../../generationGuideList/generationGuideList.data';
  import DatasetSplitConfig from '../components/DatasetSplitConfig.vue';
  import {
    DynamicParamConfig,
    SelfIterationChildTaskDetail,
    SelfIterationFormOptions,
    SelfIterationParentTask,
    SelfIterationParentTaskForm,
  } from '../types/selfIteration';

  const route = useRoute();
  const router = useRouter();
  const { createMessage } = useMessage();

  const loading = ref(false);
  const saving = ref(false);
  const starting = ref(false);
  const confirmingReview = ref(false);
  const isEditing = ref(false);

  const detail = ref<SelfIterationChildTaskDetail>();
  const parentTask = ref<SelfIterationParentTask>();
  const parentFormModel = ref<SelfIterationParentTaskForm>();
  const currentStep = ref(0);
  const stepNavChildId = ref<number | null>(null);
  const stepNavRound = ref<number | null>(null);
  const hasAutoJumpedToIteration = ref(false);
  const hasAutoJumpedToDispatch = ref(false);
  const applicationOptions = ref<Array<{ label: string; value: string }>>([]);
  const deviceFirmwareOptions = ref<Array<{ label: string; value: string }>>([]);
  const cameraOptions = ref<Array<{ label: string; value: number }>>([]);
  const httpCameraMap = reactive<
    Record<
      number,
      Array<{ id: number; cameraId: string; name: string; status?: string; isDelete: number }>
    >
  >({});
  const childDetailTimer = ref<number | null>(null);
  const [registerStartModal, { openModal: openStartModal }] = useModal();

  const formOptions = ref<SelfIterationFormOptions>({
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
  });

  const isParentView = computed(() => Boolean(route.query.parentId) && !resolveChildTaskId());
  type AnnotationDatasetItem =
    SelfIterationChildTaskDetail['dataAnnotation']['datasetStatuses'][number];

  const datasetGroupOptionsForForm = computed(() => {
    const base = [...(formOptions.value.datasetGroupOptions || [])];
    const currentId = parentFormModel.value?.datasetGroupId;
    if (currentId && !base.some((item) => item.value === currentId)) {
      base.unshift({ label: `数据集组(${currentId})`, value: currentId });
    }
    return base;
  });

  function getCameraOptions(index: number) {
    const serverId = parentFormModel.value?.dataSources?.[index]?.httpCameraServerId;
    if (!serverId) {
      return [];
    }
    const base = (httpCameraMap[serverId] || [])
      .filter(
        (item) =>
          Number(item?.isDelete ?? 1) === 0 &&
          String(item?.status || '').toUpperCase() === 'ONLINE',
      )
      .map((item) => ({
        label: item.name || `摄像头${item.id}`,
        value: item.id,
      }));
    const selectedIds = (parentFormModel.value?.dataSources || [])
      .map((source, sourceIndex) => (sourceIndex === index ? null : source.httpCameraId || null))
      .filter((id): id is number => typeof id === 'number');
    const filtered = base.filter((item) => !selectedIds.includes(item.value));
    const currentId = parentFormModel.value?.dataSources?.[index]?.httpCameraId;
    if (currentId && !filtered.some((item) => item.value === currentId)) {
      filtered.unshift({ label: `摄像机(${currentId})`, value: currentId });
    }
    return filtered;
  }

  function resolveChildTaskId(): number | null {
    const rawChildId = route.query.childId ?? route.params.id;
    const childTaskId = Number(rawChildId || 0);
    return childTaskId > 0 ? childTaskId : null;
  }

  const sortedChildTasks = computed(() =>
    [...(parentTask.value?.childTasks ?? [])].sort((a, b) => b.round - a.round),
  );

  const curveData = computed(() => {
    if ((parentTask.value?.currentIteration || 0) === 0) {
      return { epoch: [], recall: [], accuracy: [] };
    }
    const ordered = [...sortedChildTasks.value].reverse();
    const epoch: string[] = [];
    const recall: number[] = [];
    const accuracy: number[] = [];

    ordered.forEach((task) => {
      epoch.push(String(task.round));

      // 使用真实数据
      const acc = task.trainAccuracy != null ? Number(task.trainAccuracy) : null;
      const rec = task.trainRecall != null ? Number(task.trainRecall) : null;

      if (acc !== null) accuracy.push(acc);
      if (rec !== null) recall.push(rec);
    });

    return {
      epoch: epoch.length ? epoch : ['1'],
      recall: recall.length ? recall : [0.7],
      accuracy: accuracy.length ? accuracy : [0.72],
    };
  });

  const roundColumns = [
    { title: '轮次', dataIndex: 'round', width: 70 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      customRender: ({ record }) =>
        h(ATag, { color: childStatusColor(record.status) }, () => childStatusText(record.status)),
    },
    {
      title: '当前步骤',
      dataIndex: 'currentStep',
      width: 130,
      customRender: ({ record }) => stepText(record.currentStep),
    },
    { title: '更新时间', dataIndex: 'updateTime', width: 170 },
    {
      title: '操作',
      dataIndex: 'action',
      width: 110,
      customRender: ({ record }) =>
        h(
          AButton,
          {
            type: 'link',
            size: 'small',
            onClick: () => openChildTaskDetail(record.id),
          },
          () => '查看轮次详情',
        ),
    },
  ];

  const parentStatusTextMap: Record<string, string> = {
    pending: '待启动',
    running: '运行中',
    paused: '已暂停',
    processing: '处理中',
    completed: '已完成',
    failed: '失败',
  };

  const childStatusTextMap: Record<string, string> = {
    pending: '待开始',
    collecting: '采集中',
    annotating: '标注中',
    training: '训练中',
    converting: '转换中',
    packaging: '打包中',
    dispatching: '下发中',
    processing: '处理中',
    completed: '已完成',
    failed: '失败',
    waiting_review: '待审核',
  };

  const childStatusColorMap: Record<string, string> = {
    pending: 'default',
    collecting: 'processing',
    annotating: 'processing',
    training: 'processing',
    converting: 'processing',
    packaging: 'processing',
    dispatching: 'processing',
    processing: 'processing',
    completed: 'success',
    failed: 'error',
    waiting_review: 'warning',
  };

  function parentStatusText(status: string) {
    return parentStatusTextMap[status] || '未知';
  }

  function childStatusText(status: string) {
    return childStatusTextMap[status] || '未知';
  }

  function childStatusColor(status: string) {
    return childStatusColorMap[status] || 'default';
  }

  function stepText(step: string) {
    const map: Record<string, string> = {
      dataCollection: '数据采集',
      dataAnnotation: '数据标注',
      modelIteration: '模型迭代',
      modelDispatch: '模型下发',
    };
    return map[step] || '未知';
  }

  function normalizeDynamicParams(
    configs: DynamicParamConfig[],
    source: Record<string, number | string | boolean> | undefined,
    fallback: Record<string, number | string | boolean>,
  ) {
    const normalized: Record<string, number | string | boolean> = {
      ...(source || {}),
    };
    configs.forEach((config) => {
      const value = source?.[config.field];
      if (value === null || value === undefined || value === '') {
        if (fallback[config.field] !== undefined) {
          normalized[config.field] = fallback[config.field];
        }
        return;
      }
      if (config.type === 'numeric') {
        const parsed = Number(value);
        if (Number.isFinite(parsed)) {
          normalized[config.field] = parsed;
        }
        return;
      }
      if (config.type === 'boolean') {
        const parsed = String(value).toLowerCase();
        normalized[config.field] = parsed === 'true' || parsed === '1' ? 'true' : 'false';
        return;
      }
      normalized[config.field] = String(value);
    });
    return normalized;
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

  function buildDefaults(configs: DynamicParamConfig[]) {
    const result: Record<string, number | string | boolean> = {};
    configs.forEach((config) => {
      if (config.type === 'numeric') {
        result[config.field] = Number(config.defaultNum ?? 0);
      } else if (config.type === 'boolean') {
        const raw = String(config.defaultValue ?? 'false').toLowerCase();
        result[config.field] = raw === 'true' || raw === '1' ? 'true' : 'false';
      } else {
        result[config.field] = config.defaultValue ?? '';
      }
    });
    return result;
  }

  function createParentEditModel(task: SelfIterationParentTask): SelfIterationParentTaskForm {
    const trainDefaults = buildDefaults(formOptions.value.trainParamConfigList);
    const convertDefaults = buildDefaults(formOptions.value.convertParamConfigList);
    const sourceForm =
      task.form ||
      ({
        taskName: task.taskName,
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
        splitSize: '8-1-1',
        trainParams: {},
        convertParams: {},
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
        dispatchMode: 'manual',
      } as SelfIterationParentTaskForm);
    const normalizedTrain = formOptions.value.trainParamConfigList.length
      ? normalizeDynamicParams(
          formOptions.value.trainParamConfigList,
          sourceForm.trainParams,
          trainDefaults,
        )
      : { ...(sourceForm.trainParams || {}) };
    const normalizedConvert = formOptions.value.convertParamConfigList.length
      ? normalizeDynamicParams(
          formOptions.value.convertParamConfigList,
          sourceForm.convertParams,
          convertDefaults,
        )
      : { ...(sourceForm.convertParams || {}) };

    return {
      ...JSON.parse(JSON.stringify(sourceForm)),
      trainParams: normalizedTrain,
      convertParams: normalizedConvert,
      authCode: sourceForm.authCode || '',
      inferenceDeviceIds: sourceForm.inferenceDeviceIds || [],
      gpuUrlTargetIds: sourceForm.gpuUrlTargetIds || [],
      dispatchMode: sourceForm.dispatchMode ?? 'manual',
      maxRounds: sourceForm.maxRounds ?? 10,
      iterationStartImageQuantity:
        sourceForm.iterationStartImageQuantity ??
        getDefaultIterationStartImageQuantity(sourceForm.dataSources),
      targetAccuracy: sourceForm.targetAccuracy ?? null,
      targetRecall: sourceForm.targetRecall ?? null,
    };
  }

  function buildRenderConfigs(
    configs: DynamicParamConfig[],
    params: Record<string, number | string | boolean> | undefined,
  ): any[] {
    const map = new Map<string, any>();
    (configs || []).forEach((cfg) => map.set(cfg.field, cfg));
    Object.keys(params || {}).forEach((key) => {
      if (map.has(key)) return;
      const value = params?.[key];
      const type =
        typeof value === 'number'
          ? 'numeric'
          : typeof value === 'boolean' || value === 'true' || value === 'false'
          ? 'boolean'
          : 'string';
      map.set(
        key,
        type === 'boolean'
          ? { field: key, label: key, type, range: { options: ['true', 'false'] } }
          : { field: key, label: key, type, range: null },
      );
    });
    return Array.from(map.values());
  }

  const trainRenderConfigs = computed(() =>
    buildRenderConfigs(formOptions.value.trainParamConfigList, parentFormModel.value?.trainParams),
  );

  const convertRenderConfigs = computed(() =>
    buildRenderConfigs(
      formOptions.value.convertParamConfigList,
      parentFormModel.value?.convertParams,
    ),
  );

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
    if (paramRange.startsWith('{') && paramRange.endsWith('}')) {
      const options = paramRange
        .slice(1, -1)
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
      return { min: null, max: null, options };
    }
    const match = paramRange.match(/^[\[\(]([^,]*),([^\]\)]*)[\]\)]$/);
    if (!match) return { min: null, max: null, options: null };
    const minStr = match[1].trim();
    const maxStr = match[2].trim();
    return {
      min: minStr ? Number(minStr) : null,
      max: maxStr ? Number(maxStr) : null,
      options: null,
    };
  }

  async function loadParentTaskDetail() {
    const parentId = Number(route.query.parentId || 0);
    if (!parentId) {
      parentTask.value = undefined;
      parentFormModel.value = undefined;
      return;
    }

    const [task, options] = await Promise.all([
      getSelfIterationParentTaskById(parentId),
      fetchSelfIterationFormOptions(),
    ]);
    formOptions.value = options;
    parentTask.value = task;
    parentFormModel.value = task ? createParentEditModel(task) : undefined;
    isEditing.value = false;

    const cameras = await listAllHttpCameras();
    Object.keys(httpCameraMap).forEach((key) => delete httpCameraMap[Number(key)]);
    (cameras || []).forEach((item: any) => {
      const serverId = Number(item?.httpCameraServerId || 0);
      if (!serverId) return;
      if (!httpCameraMap[serverId]) {
        httpCameraMap[serverId] = [];
      }
      httpCameraMap[serverId].push(item);
    });
    (parentFormModel.value?.dataSources || []).forEach((source) => {
      if (source.httpCameraServerId || !source.httpCameraId) return;
      const currentCamera = (cameras || []).find((item: any) => item.id === source.httpCameraId);
      source.httpCameraServerId = currentCamera?.httpCameraServerId ?? null;
    });
    cameraOptions.value = (cameras || [])
      .filter(
        (item: any) =>
          Number(item?.isDelete ?? 1) === 0 &&
          String(item?.status || '').toUpperCase() === 'ONLINE',
      )
      .map((item: any) => ({
        label: item.name || `摄像机${item.id}`,
        value: item.id,
      }));

    if (parentFormModel.value?.applicationName) {
      applicationOptions.value = [
        {
          label: parentFormModel.value.applicationName,
          value: parentFormModel.value.applicationName,
        },
      ];
      deviceFirmwareOptions.value = await getDeviceFirmwareByBoundApplicationName(
        parentFormModel.value.applicationName,
      );
    }
    if (parentFormModel.value?.deviceFirmware && !deviceFirmwareOptions.value.length) {
      deviceFirmwareOptions.value = [
        {
          label: parentFormModel.value.deviceFirmware,
          value: parentFormModel.value.deviceFirmware,
        },
      ];
    }

    if (parentFormModel.value?.applicationName && parentFormModel.value?.deviceFirmware) {
      await loadTrainingParameters();
    }
  }

  async function loadChildTaskDetail() {
    const childTaskId = resolveChildTaskId();
    if (!childTaskId) {
      detail.value = undefined;
      currentStep.value = 0;
      stepNavChildId.value = null;
      stepNavRound.value = null;
      hasAutoJumpedToIteration.value = false;
      hasAutoJumpedToDispatch.value = false;
      return;
    }
    const result = await fetchSelfIterationChildTaskDetail(childTaskId);
    detail.value = result;
    const map: Record<string, number> = {
      dataCollection: 0,
      dataAnnotation: 1,
      modelIteration: 2,
      modelDispatch: 3,
    };
    if (!result) {
      currentStep.value = 0;
      stepNavChildId.value = null;
      stepNavRound.value = null;
      hasAutoJumpedToIteration.value = false;
      hasAutoJumpedToDispatch.value = false;
      return;
    }

    const isNewRoundContext =
      stepNavChildId.value !== childTaskId || stepNavRound.value !== result.round;
    if (isNewRoundContext) {
      stepNavChildId.value = childTaskId;
      stepNavRound.value = result.round;
      hasAutoJumpedToIteration.value = false;
      hasAutoJumpedToDispatch.value = false;
    }

    let nextStep = map[result.currentStep] ?? 0;
    const hasIterationJobs =
      !!result.modelIteration?.trainJobId ||
      !!result.modelIteration?.convertJobId ||
      !!result.modelIteration?.packageTaskId;
    const isIterationStatus = [
      'training',
      'converting',
      'packaging',
      'dispatching',
      'completed',
    ].includes(result.status);

    if (nextStep < 2 && (hasIterationJobs || isIterationStatus)) {
      nextStep = 2;
    }

    if (nextStep < 3 && ['dispatching', 'completed'].includes(result.status)) {
      nextStep = 3;
    }

    // 首次进入本轮时根据后端状态自动定位；之后仅在阶段首次推进时自动跳一次，避免轮询反复抢占用户视角
    if (!hasAutoJumpedToIteration.value && nextStep >= 2) {
      currentStep.value = 2;
      hasAutoJumpedToIteration.value = true;
    }

    if (!hasAutoJumpedToDispatch.value && nextStep >= 3) {
      currentStep.value = 3;
      hasAutoJumpedToDispatch.value = true;
    }

    if (!hasAutoJumpedToIteration.value && !hasAutoJumpedToDispatch.value) {
      currentStep.value = nextStep;
      if (nextStep >= 2) {
        hasAutoJumpedToIteration.value = true;
      }
      if (nextStep >= 3) {
        hasAutoJumpedToDispatch.value = true;
      }
    }
  }

  function stopChildDetailPolling() {
    if (childDetailTimer.value) {
      window.clearInterval(childDetailTimer.value);
      childDetailTimer.value = null;
    }
  }

  function startChildDetailPolling() {
    stopChildDetailPolling();
    if (isParentView.value) return;
    if (!resolveChildTaskId()) return;
    childDetailTimer.value = window.setInterval(() => {
      loadChildTaskDetail();
    }, 5000);
  }

  async function loadData() {
    loading.value = true;
    try {
      if (isParentView.value) {
        stopChildDetailPolling();
        detail.value = undefined;
        await loadParentTaskDetail();
      } else {
        parentTask.value = undefined;
        parentFormModel.value = undefined;
        await loadChildTaskDetail();
        startChildDetailPolling();
      }
    } finally {
      loading.value = false;
    }
  }

  async function saveParentConfig() {
    if (!parentTask.value || !parentFormModel.value) {
      return;
    }

    saving.value = true;
    try {
      if (
        parentFormModel.value.maxRounds == null ||
        parentFormModel.value.iterationStartImageQuantity == null ||
        parentFormModel.value.targetAccuracy == null ||
        parentFormModel.value.targetRecall == null
      ) {
        createMessage.warning('最大轮次、迭代开始图片数量、最低准确率和最低召回率必须填写');
        return;
      }

      const totalImageQuantity = sumDataSourceImageQuantity(parentFormModel.value.dataSources);
      if (
        totalImageQuantity != null &&
        Number(parentFormModel.value.iterationStartImageQuantity) > totalImageQuantity
      ) {
        createMessage.warning(`迭代开始图片数量不能大于摄像机采集数量总和 ${totalImageQuantity}`);
        return;
      }

      const payload: SelfIterationParentTaskForm = {
        ...parentFormModel.value,
        maxRounds: parentFormModel.value.maxRounds,
        iterationStartImageQuantity: parentFormModel.value.iterationStartImageQuantity,
        targetAccuracy: parentFormModel.value.targetAccuracy,
        targetRecall: parentFormModel.value.targetRecall,
        trainParams: { ...parentFormModel.value.trainParams },
        convertParams: { ...parentFormModel.value.convertParams },
        dataSources: (parentFormModel.value.dataSources || []).map((item) => ({ ...item })),
      };
      const updated = await updateSelfIterationParentTask(parentTask.value.id, payload);
      if (updated) {
        parentTask.value = updated;
        parentFormModel.value = createParentEditModel(updated);
        isEditing.value = false;
        createMessage.success('参数配置已保存');
      } else {
        createMessage.warning('保存失败，任务不存在');
      }
    } finally {
      saving.value = false;
    }
  }

  async function retryCollect() {
    const childTaskId = resolveChildTaskId();
    if (!childTaskId) {
      createMessage.warning('未找到子任务ID');
      return;
    }
    const ok = await retrySelfIterationCollect(childTaskId);
    if (!ok) {
      createMessage.error('重新采集触发失败');
      return;
    }
    createMessage.success('已触发重新采集');
    await loadChildTaskDetail();
  }

  function resolveReviewDatasets(): Array<{ datasetId: number; datasetName: string }> {
    if (!detail.value) return [];

    const results: Array<{ datasetId: number; datasetName: string }> = [];
    const seen = new Set<number>();
    const pushDataset = (datasetId?: number, datasetName?: string) => {
      if (typeof datasetId !== 'number' || Number.isNaN(datasetId) || seen.has(datasetId)) return;
      seen.add(datasetId);
      results.push({
        datasetId,
        datasetName: datasetName || `dataset-${datasetId}`,
      });
    };

    const cascadeMap = new Map<string, number>();
    (detail.value.dataCollection?.datasetCascade || []).forEach((group) => {
      (group.datasets || []).forEach((dataset) => {
        if (typeof dataset.datasetId === 'number') {
          pushDataset(dataset.datasetId, dataset.datasetName);
          if (dataset.datasetName) {
            cascadeMap.set(dataset.datasetName, dataset.datasetId);
          }
        }
      });
    });

    (detail.value.dataAnnotation?.datasetStatuses || []).forEach((item) => {
      const statusDatasetId =
        typeof item.datasetId === 'number' ? item.datasetId : cascadeMap.get(item.datasetName);
      pushDataset(statusDatasetId, item.datasetName);
    });

    return results;
  }

  async function jumpToDatasetReview(datasetId: number, datasetName: string) {
    const safeName = datasetName || `dataset-${datasetId}`;
    let prefix = 'annotate';
    try {
      const datasetInfo = await getDatasetById(datasetId);
      const annotateType = Number(datasetInfo?.annotateType ?? datasetInfo?.annotationType ?? 0);
      if (annotateType === 103) {
        prefix = 'segmentation';
      }
    } catch (error) {
      console.warn('load dataset detail failed before review jump', error);
    }

    let firstImgId: unknown = undefined;
    try {
      firstImgId = await queryFirstImg(datasetId);
    } catch (error) {
      console.warn('query first image failed before review jump', error);
    }

    await router.push({
      path: `/maData/${prefix}/${datasetId}/${safeName}`,
      state: { imgId: firstImgId },
    });
  }
  async function goReview(dataset?: AnnotationDatasetItem) {
    if (!detail.value) {
      createMessage.warning('未加载到当前子任务详情');
      return;
    }
    if (detail.value.status !== 'waiting_review') {
      createMessage.warning('当前尚未进入人工审核阶段');
      return;
    }

    const datasets = resolveReviewDatasets();
    if (!datasets.length) {
      createMessage.warning('未找到可审核的数据集');
      return;
    }

    let target = datasets[0];
    if (dataset) {
      if (typeof dataset.datasetId === 'number') {
        target = datasets.find((item) => item.datasetId === dataset.datasetId) || target;
      } else if (dataset.datasetName) {
        target = datasets.find((item) => item.datasetName === dataset.datasetName) || target;
      }
    }

    try {
      await jumpToDatasetReview(target.datasetId, target.datasetName);
    } catch (error) {
      console.error('jump to review page failed', error);
      createMessage.error('跳转标注页失败，请重试');
    }
  }

  async function confirmReview() {
    if (!detail.value) {
      createMessage.warning('未加载到当前子任务详情');
      return;
    }
    if (detail.value.status !== 'waiting_review') {
      createMessage.warning('当前不在待审核状态');
      return;
    }

    const taskId =
      parentTask.value?.id || detail.value.parentTaskId || Number(route.query.parentId || 0);
    if (!taskId) {
      createMessage.warning('未找到父任务ID');
      return;
    }

    if (confirmingReview.value) return;
    confirmingReview.value = true;
    try {
      await confirmReviewApi(taskId);
      createMessage.success('审核完成，任务已继续');
      if (isParentView.value) {
        await loadData();
      } else {
        await loadChildTaskDetail();
      }
    } catch (error) {
      console.error('完成审核失败:', error);
      createMessage.error('操作失败，请重试');
    } finally {
      confirmingReview.value = false;
    }
  }

  function cancelEdit() {
    if (!parentTask.value) return;
    parentFormModel.value = createParentEditModel(parentTask.value);
    isEditing.value = false;
  }

  function startIteration() {
    if (!parentTask.value) return;
    openStartModal(true, { record: parentTask.value });
  }

  async function handleStartSuccess() {
    await loadData();
  }

  function handleDatasetGroupSelect() {
    if (!parentFormModel.value) return;
    if (parentFormModel.value.datasetGroupId) {
      parentFormModel.value.datasetGroupName = '';
    }
  }

  function handleDatasetGroupNameInput() {
    if (!parentFormModel.value) return;
    if (parentFormModel.value.datasetGroupName) {
      parentFormModel.value.datasetGroupId = null;
    }
  }

  async function handleApplicationChange() {
    if (!parentFormModel.value?.applicationName) return;
    deviceFirmwareOptions.value = await getDeviceFirmwareByBoundApplicationName(
      parentFormModel.value.applicationName,
    );
    if (parentFormModel.value.deviceFirmware) {
      await loadTrainingParameters();
    }
  }

  async function handleDeviceFirmwareChange() {
    if (!parentFormModel.value?.deviceFirmware) return;
    if (!parentFormModel.value.applicationName) {
      applicationOptions.value = await getApplicationNameByBoundDeviceFirmware(
        parentFormModel.value.deviceFirmware,
      );
    }
    if (parentFormModel.value.applicationName && parentFormModel.value.deviceFirmware) {
      await loadTrainingParameters();
    }
  }

  async function loadTrainingParameters() {
    if (!parentFormModel.value?.applicationName || !parentFormModel.value?.deviceFirmware) return;
    try {
      const modelApp = await getUniqueModelApplication(
        parentFormModel.value.applicationName,
        parentFormModel.value.deviceFirmware,
      );
      if (!modelApp?.model?.id) return;
      const params = await getTrainConvertAndOtherHyperParams(modelApp.model.id);
      if (!params) return;

      if (Array.isArray(params.trainHyperParamsWithValue)) {
        const trainConfigs = params.trainHyperParamsWithValue.map((p: any, index: number) => ({
          id: index + 1,
          field: p.paramName,
          label: p.paramName,
          type: inferParamType(p.paramRange),
          required: true,
          defaultValue: p.trainDefaultValue || p.imageDefaultValue || '',
          defaultNum: p.trainDefaultValue || p.imageDefaultValue || '',
          msg: p.paramDescription || '',
          inputDescription: p.paramDescription || '',
          ...parseParamRange(p.paramRange),
          range: parseParamRange(p.paramRange),
        }));
        formOptions.value.trainParamConfigList = trainConfigs as any;
      }

      if (Array.isArray(params.convertHyperParamsWithValue)) {
        const convertConfigs = params.convertHyperParamsWithValue.map((p: any, index: number) => ({
          id: index + 1000,
          field: p.paramName,
          label: p.paramName,
          type: inferParamType(p.paramRange),
          required: true,
          defaultValue: p.trainDefaultValue || p.imageDefaultValue || '',
          defaultNum: p.trainDefaultValue || p.imageDefaultValue || '',
          msg: p.paramDescription || '',
          inputDescription: p.paramDescription || '',
          ...parseParamRange(p.paramRange),
          range: parseParamRange(p.paramRange),
        }));
        formOptions.value.convertParamConfigList = convertConfigs as any;
      }

      if (parentFormModel.value) {
        const trainDefaults = buildDefaults(formOptions.value.trainParamConfigList);
        const convertDefaults = buildDefaults(formOptions.value.convertParamConfigList);
        parentFormModel.value.trainParams = normalizeDynamicParams(
          formOptions.value.trainParamConfigList,
          parentFormModel.value.trainParams,
          trainDefaults,
        );
        parentFormModel.value.convertParams = normalizeDynamicParams(
          formOptions.value.convertParamConfigList,
          parentFormModel.value.convertParams,
          convertDefaults,
        );
      }
    } catch (error) {
      console.error('loadTrainingParameters failed', error);
    }
  }

  function openChildTaskDetail(childId: number) {
    if (!parentTask.value) {
      return;
    }
    router.push(
      `/maTrainingCenter/selfIterationDetail?childId=${childId}&parentId=${parentTask.value.id}`,
    );
  }

  function goBack() {
    router.go(-1);
  }

  onMounted(() => {
    loadData();
  });

  onBeforeUnmount(() => {
    stopChildDetailPolling();
  });
</script>

<style scoped>
  .page-content {
    margin: 0;
    background: #181d31;
    min-height: calc(100vh - 64px);
  }

  .summary-card,
  .section-card {
    border: 1px solid rgba(255, 255, 255, 0.1);
    margin-bottom: 14px;
  }

  .summary-card :deep(.ant-card-body),
  .summary-card :deep(.ant-descriptions-view),
  .section-card :deep(.ant-card-head),
  .section-card :deep(.ant-card-body) {
    background: #181d31;
    border-bottom-color: rgba(255, 255, 255, 0.08);
  }

  .chart-stack {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .table-card :deep(.ant-table) {
    background: transparent;
  }

  .table-card :deep(.ant-table-thead > tr > th) {
    background: rgba(255, 255, 255, 0.04);
    border-bottom-color: rgba(255, 255, 255, 0.1);
  }

  .table-card :deep(.ant-table-tbody > tr > td) {
    border-bottom-color: rgba(255, 255, 255, 0.08);
  }

  .form-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 8px;
  }

  .config-group-title {
    margin: 2px 0 10px;
    font-weight: 600;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.92);
  }

  .step-navigation {
    margin-bottom: 14px;
    background: #181d31;
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 8px;
  }

  .step-panel {
    background: #181d31;
    border-radius: 8px;
    min-height: clamp(300px, calc(100vh - 340px), 720px);
  }

  :deep(.ant-spin-nested-loading),
  :deep(.ant-spin-container),
  :deep(.vben-page-wrapper-content),
  :deep(.vben-page-wrapper-content-main),
  :deep(.vben-layout-content),
  :deep(.vben-page-wrapper),
  :deep(.vben-page-wrapper-content-body) {
    background: #181d31 !important;
  }

  @media (max-width: 992px) {
    .page-content {
      min-height: calc(100vh - 56px);
    }
  }
</style>

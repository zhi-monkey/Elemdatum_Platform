<template>
  <div style="display: flex; justify-content: center; align-items: center; height: 100%">
    <a-modal
      :title="drawerTitle"
      v-model:visible="visible"
      centered
      width="60%"
      :maskClosable="false"
      @cancel="close"
      :bodyStyle="{ padding: '24px' }"
    >
      <a-row :gutter="16" style="width: 100%; display: flex; justify-content: center">
        <a-col :span="24">
          <a-card title="基础信息填写" style="margin-bottom: 16px; padding: 16px">
            <BasicForm
              style="width: 100%; padding: 0 16px"
              :schemas="generationSchemas"
              @register="registerInfoData"
              @submit="handleSubmit"
            >
              <template #splitSizeSlot>
                <div style="display: flex; align-items: center">
                  <a-slider
                    v-model:value="splitSize"
                    range
                    :min="1"
                    :max="99"
                    :step="1"
                    style="flex: 1"
                    @update:value="handleSliderChange"
                  />
                </div>
                <div class="slider-labels">
                  <span>训练集: {{ splitSize[0] }}%</span>
                  <span>测试集: {{ splitSize[1] - splitSize[0] }}%</span>
                  <span>验证集: {{ 100 - splitSize[1] }}%</span>
                </div>
              </template>
            </BasicForm>
          </a-card>
        </a-col>

        <a-col :span="24">
          <DatasetSelectCard
            v-if="reflash"
            ref="datasetSelectCardRef"
            :annotate-type="annotateType"
            :annotation-format="annotationFormat"
          />
        </a-col>

        <a-col :span="24">
          <a-card title="训练参数" bordered style="margin-bottom: 16px; padding: 16px">
            <a-form
              v-if="showTrainForm"
              :model="trainParams"
              :rules="trainRules"
              ref="trainFormRef"
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 15 }"
              style="padding: 0 16px"
              @validate="handleValidate"
            >
              <a-form-item
                v-for="(schema, index) in trainInputs"
                :key="index"
                :label="schema.label"
                :name="schema.field"
                :rules="[
                  {
                    required: true,
                    message: `${index}不能为空`,
                  },
                  {
                    validator: (_, val) => validateRange(val, schema.range, String(index)),
                  },
                ]"
                style="margin-bottom: 12px"
              >
                <a-input v-model:value="trainParams[schema.field]">
                  <template #suffix>
                    <a-tooltip>
                      <template #title>{{ schema.description }}</template>
                      <InfoCircleOutlined />
                    </a-tooltip>
                  </template>
                </a-input>
                <template #help>
                  <span v-if="errors[index]" style="color: red">
                    {{ errors[index] }}
                  </span>
                </template>
              </a-form-item>
            </a-form>
          </a-card>

          <a-card title="资源配置" bordered style="padding: 16px" v-if="reflash">
            <template #extra>
              <div style="display: flex; gap: 16px">
                <a-button type="primary" @click="openConfigurationModal">添加配置</a-button>
                <a-button type="primary" @click="openManagementModal">删除配置</a-button>
              </div>
            </template>
            <a-row :gutter="16" align="middle">
              <a-col :span="24">
                <ResourceCards
                  :selected-resource-id="selectedResourceId || undefined"
                  :initial-gpu-mode="gpuMode"
                  :initial-gpu-count="gpuCount"
                  @select-resource="selectResource"
                  @gpu-mode-change="handleGpuModeChange"
                  @gpu-count-change="handleGpuCountChange"
                />
              </a-col>
            </a-row>
            <ConfigurationModal @register="registerConfigurationModal" />
            <ManagementModal
              @register="registerManagementModal"
              @delete-success="refreshConfigurations"
            />
          </a-card>
        </a-col>
      </a-row>
      <template #footer>
        <a-button key="back" @click="close">取消</a-button>
        <a-button key="submit" type="primary" @click="handleSubmit" :loading="isSubmitting"
          >提交
        </a-button>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
  import { BasicForm, useForm } from '/@/components/Form';
  import { generationSchemas, MODEL_SOURCE_BASE } from './data';
  import {
    Card as ACard,
    Col as ACol,
    Form as AForm,
    FormItem as AFormItem,
    Input as AInput,
    Modal as AModal,
    Row as ARow,
    Tooltip as ATooltip,
    Slider as ASlider,
  } from 'ant-design-vue';
  import { computed, defineEmits, defineExpose, nextTick, onMounted, ref, watch } from 'vue';
  import { on } from '/@/views/mineai/model/generationCreate/modelCreateEventBus';
  import { useModal } from '/@/components/Modal';
  import ConfigurationModal from '/@/views/mineai/train/generationCreate/ConfigurationModal.vue';
  import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { maHttp } from '/@/utils/http/axios';
  import { useUserStore } from '/@/store/modules/user';
  import { useMessage } from '/@/hooks/web/useMessage';
  import AButton from '/@/components/Button/src/BasicButton.vue';
  import { InfoCircleOutlined } from '@ant-design/icons-vue';
  import { getHardwareParamsById } from '/@/views/mineai/model/generationGuide/api/modelApi';
  import ManagementModal from './ManagementModal.vue';
  import DatasetSelectCard from '/@/views/mineai/train/generationCreate/DatasetSelectCard.vue';
  import ResourceCards from './ResourceCards.vue';
  import {
    normalizeSplitRange,
    splitRangeToPercentString,
  } from '/@/views/mineai/train/utils/datasetSplit';

  const [registerManagementModal, { openModal: openManagementModal }] = useModal();

  const refreshConfigurations = async () => {
    try {
      // 清空资源配置表单中当前选择的值
      selectedResourceId.value = null;
    } catch (error) {
      console.error('刷新配置失败:', error);
    }
  };

  onMounted(() => {
    // 监听算法选择结果，更新算法选择对应的form
    on('modelChanged', handleModelChange);
    // 展开所有面板
    expandAll();
  });

  const splitSize = ref([70, 80]);

  // 处理滑块变化，确保每个集合至少1%
  const handleSliderChange = (value) => {
    splitSize.value = normalizeSplitRange(value);
  };

  const _props = defineProps({
    visible: {
      type: Boolean,
      required: true,
    },
  });
  watch(
    () => _props.visible,
    (newVal) => {
      if (newVal) {
        // 打开模态框时挂载组件
        reflash.value = true;
      } else {
        // 关闭模态框时卸载组件
        reflash.value = false;
      }
    },
  );
  const reflash = ref(false);
  const currentRowRecord = ref(null);
  const isEditing = ref(false);
  const drawerTitle = computed(() => {
    return isEditing.value ? '编辑任务' : '新增任务';
  });
  defineExpose({
    isEditing,
    currentRowRecord,
  });
  const emit = defineEmits(['close', 'submitSuccess']);

  const showTrainForm = ref(false);
  const trainFormRef = ref<any>(null);
  let trainInputs = ref<any[]>([]);
  const trainParams = ref({});
  const trainRules = computed(() => {
    if (!Array.isArray(trainInputs.value)) {
      return {};
    }
    return trainInputs.value.reduce((rules: any, schema: any) => {
      if (schema.required) {
        rules[schema.field] = [{ required: true, message: `${schema.label} is required` }];
      }
      return rules;
    }, {});
  });

  // GPU模式和数量相关变量
  const gpuMode = ref('single-exclusive');
  const gpuCount = ref(1);

  // 处理GPU模式变化
  const handleGpuModeChange = (data: { tabType: string; mode: string; gpuCount: number }) => {
    gpuMode.value = data.mode;
    // 如果是多卡模式，更新GPU数量
    if (data.mode === 'multi-exclusive' || data.mode === 'multi-shared') {
      gpuCount.value = data.gpuCount;
    } else {
      gpuCount.value = 1;
    }
  };

  // 处理GPU数量变化
  const handleGpuCountChange = (data: { tabType: string; mode: string; gpuCount: number }) => {
    // 只有在多卡模式下才更新GPU数量
    if (data.mode === 'multi-exclusive' || data.mode === 'multi-shared') {
      gpuCount.value = data.gpuCount;
      gpuMode.value = data.mode;
    }
  };

  function close() {
    emit('close');
    // closeInfoModal();
    // closeResourceModal();
  }

  const { createMessage } = useMessage();

  const trainModelVersion = ref();
  let hyperParamTrainList: any[] = [];

  // 存储当前选择的model信息
  const currentModel = ref<any>(null);

  // 计算属性：将Model的trainModelVersion的annotationType转换为Dataset的annotateType
  // Detection -> 102 (目标检测), Segmentation -> 103 (语义分割)
  // 新建时：从选择的算法(Model.trainModelVersion)获取格式
  const annotateType = computed(() => {
    const annotationType = currentModel.value?.trainModelVersion?.annotationType;
    if (annotationType === 'Detection') {
      return 102;
    } else if (annotationType === 'Segmentation') {
      return 103;
    }
    return undefined;
  });

  // 计算属性：根据annotationType和annotationFormat确定DatasetVersion的format
  // Detection + YOLO -> 'YOLO'
  // Detection + COCO -> 'COCO'
  // Segmentation + YOLO -> 'Segment-YOLO'
  // Segmentation + COCO -> 'COCO'
  // 新建时：从选择的算法(Model.trainModelVersion)获取格式
  const annotationFormat = computed(() => {
    const annotationTypeValue = currentModel.value?.trainModelVersion?.annotationType;
    const formatValue = currentModel.value?.trainModelVersion?.annotationFormat;

    if (annotationTypeValue === 'Detection' && formatValue === 'YOLO') {
      return 'YOLO';
    } else if (annotationTypeValue === 'Detection' && formatValue === 'COCO') {
      return 'COCO';
    } else if (annotationTypeValue === 'Segmentation' && formatValue === 'YOLO') {
      return 'Segment-YOLO';
    } else if (annotationTypeValue === 'Segmentation' && formatValue === 'COCO') {
      return 'COCO';
    }
    return undefined;
  });

  // 定义一个 ref 来存储当前展开的面板 key
  const activeKeys = ref<string[]>([]);

  // 展开所有面板
  async function expandAll() {
    activeKeys.value = ['1', '2', '3'];
  }

  const userStore = useUserStore();
  const userData = userStore.getUserInfo;

  const [registerConfigurationModal, { openModal: openConfigurationModal }] = useModal();

  // 资源配置相关变量
  const selectedResourceId = ref<number | null>(null);

  // 选择资源配置
  const selectResource = (id: number) => {
    selectedResourceId.value = id;
  };

  //绑定基础信息
  const [registerInfoData, { resetFields: resetInfoFields, validate: validateInfo }] = useForm({
    labelWidth: 200,
    schemas: generationSchemas,
    showActionButtonGroup: true,
    showResetButton: false,
    showSubmitButton: false,
  });

  //选择算法时，调整右侧超参
  const handleModelChange = async (model) => {
    await expandAll();
    if (!model) {
      currentModel.value = null;
      trainInputs.value = [];
      trainParams.value = {};
      showTrainForm.value = false;
      return;
    }
    // 保存当前选择的model信息，用于获取annotationType和annotationFormat
    currentModel.value = model;

    if (model.modelSource === MODEL_SOURCE_BASE) {
      const dedupeTrainInputs = (items) => {
        const seen = new Set();
        return (items || []).filter((item) => {
          const key = item?.field;
          if (!key || seen.has(key)) {
            return false;
          }
          seen.add(key);
          return true;
        });
      };

      const assignTrainInputs = (items) => {
        trainInputs.value = dedupeTrainInputs(items);
        trainParams.value = trainInputs.value.reduce((acc, item) => {
          acc[item.field] = item.defaultNum;
          return acc;
        }, {});
        showTrainForm.value = trainInputs.value.length > 0;
      };

      const modelConfigList = model?.trainModelVersion?.modelConfigList || [];
      if (modelConfigList.length > 0) {
        const baseTrainInputs = modelConfigList
          .filter((e) => e?.field)
          .map((e) => ({
            label: e.field,
            field: e.field,
            required: true,
            description: e.inputDescription || '',
            defaultNum: e.defaultNum,
            range: e.msg || null,
          }));
        assignTrainInputs(baseTrainInputs);
        return;
      }

      try {
        const allParams = await maHttp.get(
          {
            url: `modelJob/getHyperParams/${model.id}`,
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );
        const baseHyperParams = (allParams?.trainHyperParamsWithValue || [])
          .filter((item) => {
            const paramName = item?.paramName || '';
            return !!paramName && paramName !== 'MODE_WORKING_MODE' && !paramName.startsWith('CONVERT_');
          })
          .map((item) => {
            const defaultNum =
              item.trainDefaultValue !== null && item.trainDefaultValue !== undefined
                ? item.trainDefaultValue
                : item.imageDefaultValue;
            return {
              label: item.paramName,
              field: item.paramName,
              required: true,
              description: item.paramDescription || '',
              defaultNum,
              range: item.paramRange || null,
            };
          });
        assignTrainInputs(baseHyperParams);
      } catch (error) {
        trainInputs.value = [];
        trainParams.value = {};
        showTrainForm.value = false;
        createMessage.error('获取基础算法训练参数失败');
      }
      return;
    }

    if (model.trainModelVersion != null) {
      trainModelVersion.value = model.trainModelVersion;
      hyperParamTrainList = [];
      if (trainModelVersion.value.modelConfigList != null) {
        trainModelVersion.value.modelConfigList.forEach((e) => {
          if (e.field != null && e.field != '') {
            hyperParamTrainList.push({
              field: e.field,
              description: e.inputDescription || '',
              defaultNum: e.defaultNum,
              msg: e.msg || null,
            });
          }
        });
      }
      trainInputs.value = hyperParamTrainList.map((item) => {
        return {
          label: item.field,
          field: item.field,
          required: true,
          description: item.description,
          defaultNum: item.defaultNum,
          range: item.msg,
        };
      });
      trainParams.value = trainInputs.value.reduce((acc, item) => {
        acc[item.field] = item.defaultNum;
        return acc;
      }, {});
      showTrainForm.value = true;
    }
  };
  const errors = ref<Record<string, string>>({});

  const validateRange = (value: number, rangeStr: string, key: string) => {
    const { min, max } = getMinMaxValue(rangeStr);
    let isValid = true;

    if (min !== null && value < min) {
      errors.value[key] = `值不能小于${min}`;
      isValid = false;
    }

    if (max !== null && value > max) {
      errors.value[key] = `值不能大于${max}`;
      isValid = false;
    }

    if (isValid) {
      delete errors.value[key];
      return Promise.resolve();
    }
    return Promise.reject(new Error(errors.value[key]));
  };

  const handleValidate = (name: string, status: boolean) => {
    if (!status) return;
    delete errors.value[name];
  };

  const getMinMaxValue = (rangeStr: string) => {
    // 如果 rangeStr 为空或无效，直接返回无任何限制
    if (!rangeStr) {
      return { min: null, max: null };
    }

    // 正则表达式匹配范围：处理如 "[1, 3)"，"[1,)" 或 "[, 3)"
    const regex = /^\[?(\d+)?\s*,\s*(\d+)?\)?$/;
    const matches = rangeStr.match(regex);

    // 如果正则匹配失败，返回默认的无限制
    if (!matches) {
      return { min: null, max: null };
    }

    // 解析最小值和最大值
    const min = matches[1] ? parseInt(matches[1], 10) : null;
    const max = matches[2] ? parseInt(matches[2], 10) : null;

    // 如果没有最大值，表示正无穷
    if (!max) {
      return { min, max: null };
    }

    return { min, max };
  };

  //提交基础信息之后返回的generationId
  const newGenerationId = ref(0);

  const isSubmitting = ref(false);

  // 添加对 DatasetSelectCard 组件的引用
  const datasetSelectCardRef = ref<any>(null);

  // 提交
  async function handleSubmit() {
    isSubmitting.value = true;
    // 校验表单
    let infoParams;
    let resourceParams;
    try {
      infoParams = await validateInfo();
      if (trainFormRef.value) {
        await trainFormRef.value.validateFields();
      }
      // 验证是否选择了资源配置
      if (!selectedResourceId.value) {
        createMessage.error('请选择资源配置');
        isSubmitting.value = false;
        return;
      }
      resourceParams = { resource: selectedResourceId.value };
    } catch (e) {
      isSubmitting.value = false;
      return;
    }
    try {
      const res = await checkResource(resourceParams);
      if (!res) {
        isSubmitting.value = false;
        return;
      }
      infoParams = {
        ...infoParams,
        gpuMode: gpuMode.value,
        gpuCount: gpuCount.value,
      };

      // 从 DatasetSelectCard 组件获取选中的版本ID和标签
      let selectedVersionIds = [];
      let selectedLabelsMap = {}; // 改为 Map 对象
      if (datasetSelectCardRef.value) {
        selectedVersionIds = datasetSelectCardRef.value.getSelectedVersionIds();

        // 获取标签列表并转换为 Map
        const selectedLabelsList = datasetSelectCardRef.value.getSelectedLabels();
        const tempMap = new Map();
        selectedLabelsList.forEach((label, index) => {
          tempMap.set(index, label); // 使用 Map 对象，key 是真正的数字
        });
        selectedLabelsMap = Object.fromEntries(tempMap); // 转回普通对象
      }
      // 验证数据集版本和标签是否选择
      if (!selectedVersionIds.length) {
        createMessage.error('请选择数据集版本');
        isSubmitting.value = false;
        return;
      }

      // 检查 selectedLabelsMap 是否为空对象
      if (Object.keys(selectedLabelsMap).length === 0) {
        createMessage.error('请选择标签');
        isSubmitting.value = false;
        return;
      }

      // 将选中的版本ID和标签Map添加到 infoParams 中
      infoParams = {
        ...infoParams,
        selectedVersionIds: selectedVersionIds,
        selectedLabels: selectedLabelsMap, // 现在是 Map 对象
      };

      await submitBasicInfo(infoParams);
      await submitTrainHyperParam();
      await submitResourceParam(resourceParams);
      emit('submitSuccess', true);
      createMessage.success('所有信息提交成功！');
      isSubmitting.value = false;
      // 调用resetFields清空表单
      resetFormData();
      close();
    } catch (error: any) {
      isSubmitting.value = false;
      console.error(error.message);
      if (error.message === '部门不存在') return;
      if (error.message === '生产任务名称重复') return;
      createMessage.error(`提交失败：${error.message ? error.message : ''}`);
    }
  }

  // 校验用户资源所剩还够不够开这一次任务
  const checkResource = async (resourceParams) => {
    const hardWareParamsId = resourceParams.resource;
    let hardWareParams = await getHardwareParamsById(hardWareParamsId);
    const cpuNeed = Number.parseInt(hardWareParams.cpus);
    const memoryNeed = Number.parseInt(hardWareParams.memory.split('G')[0]);
    const gpuMemoryNeed = Number.parseInt(hardWareParams.gpuMemory.split('G')[0]);
    const userCpuMemoryUsed = await getUserResourceUsed((userData as any).id);
    if (userCpuMemoryUsed.cpuUsed + cpuNeed > userCpuMemoryUsed.cpuTotal) {
      createMessage.error(
        `您所在部门剩余的CPU资源不足(已用${userCpuMemoryUsed.cpuUsed}/共${
          userCpuMemoryUsed.cpuTotal
        }/可用${userCpuMemoryUsed.cpuTotal - userCpuMemoryUsed.cpuUsed})，请调整参数或联系管理员`,
        10,
      );
      return false;
    }
    if (userCpuMemoryUsed.memoryUsed + memoryNeed > userCpuMemoryUsed.memoryTotal) {
      createMessage.error(
        `您所在部门剩余的MEMORY资源不足(已用${userCpuMemoryUsed.memoryUsed}/共${
          userCpuMemoryUsed.memoryTotal
        }/可用${
          userCpuMemoryUsed.memoryTotal - userCpuMemoryUsed.memoryUsed
        })，请调整参数或联系管理员`,
        10,
      );
      return false;
    }
    if (userCpuMemoryUsed.gpuMemoryUsed + gpuMemoryNeed > userCpuMemoryUsed.gpuMemoryTotal) {
      createMessage.error(
        `您所在部门剩余的显存资源不足(已用${userCpuMemoryUsed.gpuMemoryUsed}/共${
          userCpuMemoryUsed.gpuMemoryTotal
        }/可用${
          userCpuMemoryUsed.gpuMemoryTotal - userCpuMemoryUsed.gpuMemoryUsed
        })，请调整参数或联系管理员`,
        10,
      );
      return false;
    }
    return true;
  };

  /**
   * 获取用户当前所在部门一共用了的CPU和内存
   * @param userId 用户ID
   */
  async function getUserResourceUsed(userId: any) {
    return maHttp.get(
      {
        url: 'users/getUserResourceUsed',
        params: {
          userId: userId,
        },
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
    );
  }

  // 使用 resetFields 清空表单数据
  function resetFormData() {
    resetInfoFields(); // 重置基础信息表单
    selectedResourceId.value = null; // 清空选择的资源配置
    trainParams.value = {}; // 清空训练参数
    showTrainForm.value = false; // 隐藏训练参数表单
    // 重置GPU模式和数量
    gpuMode.value = 'single-exclusive';
    gpuCount.value = 1;

    reflash.value = false;
    nextTick(() => {
      reflash.value = true;
    });
  }

  // 绑定基础信息方法
  async function submitBasicInfo(infoParams) {
    let infoValues = {};
    infoValues['name'] = infoParams.name;
    infoValues['description'] = infoParams.description;
    infoValues['gpuModel'] = infoParams.gpuModel;
    infoValues['gpuCount'] = infoParams.gpuCount;
    // 从 DatasetSelectCard 组件获取实际的 splitSize 值
    let actualSplitSize = splitSize.value; // 默认值
    if (datasetSelectCardRef.value && datasetSelectCardRef.value.getSplitSize) {
      actualSplitSize = datasetSelectCardRef.value.getSplitSize();
    }

    // 处理 splitSize，将数组转换为符合要求的字符串格式
    if (Array.isArray(actualSplitSize)) {
      infoValues['splitSize'] = splitRangeToPercentString(actualSplitSize);
    }
    infoValues['datasetSource'] = infoParams.datasetSource;

    if (datasetSelectCardRef.value) {
      infoValues['trainDatasetVersions'] = infoParams.selectedVersionIds;
      infoValues['selectedLabels'] = infoParams.selectedLabels;
    }
    //
    // infoValues['trainDataset'] = 267;
    // if (infoParams.testDataset != null) {
    //   infoValues['testDataset'] = 267;
    // } else {
    //   infoValues['testDataset'] = 267;
    // }
    // if (infoParams.valDataset != null) {
    //   infoValues['valDataset'] = 267;
    // } else {
    //   infoValues['valDataset'] = 267;
    // }
    infoValues['isuse'] = true;
    if (infoParams.modelSource === MODEL_SOURCE_BASE) {
      infoValues['model'] = { id: infoParams.model };
    } else {
      infoValues['modelExplore'] = { id: infoParams.model };
    }
    if (selectedResourceId.value != null) {
      infoValues['hardwareParams'] = { id: selectedResourceId.value };
    }
    infoValues['isGuided'] = false;
    infoValues['userId'] = (userData as any)?.id;

    // 添加GPU模式和数量信息
    infoValues['gpuMode'] = gpuMode.value;
    infoValues['gpuCount'] = gpuCount.value;

    try {
      const v = await maHttp.post(
        {
          url: 'modelGeneration/addModelGeneration',
          params: infoValues,
          headers: {
            ignoreCancelToken: 'true',
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );
      newGenerationId.value = v;
    } catch (e: any) {
      throw new Error(e.message);
    }
  }

  // 提交超参
  async function submitTrainHyperParam() {
    let trainValues = {};
    let params = {};

    params = Object.assign(params, trainParams.value);
    params['MODE_WORKING_MODE'] = '1';
    trainValues['modelGenerationId'] = Number(newGenerationId.value);
    trainValues['params'] = params;

    try {
      await maHttp.post(
        {
          url: 'modelGeneration/setTrainParams',
          params: trainValues,
          headers: {
            ignoreCancelToken: 'true',
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );
    } catch {
      throw new Error('训练参数提交失败！');
    }
  }

  // 保存资源配置参数
  async function submitResourceParam(resourceParams) {
    try {
      await maHttp.get(
        {
          url: 'modelGeneration/bindHardwareParams',
          params: {
            modelGenerationId: newGenerationId.value,
            hardwareParamsId: resourceParams.resource,
          },
          headers: {
            ignoreCancelToken: 'true',
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );
    } catch {
      throw new Error('资源参数提交失败！');
    }
  }
</script>

<style scoped>
  /* 添加分页样式 */
  .pagination-container {
    display: flex;
    justify-content: center;
    margin-top: 16px;
  }
  .ant-modal-body {
    overflow-y: auto;
  }

  .ant-card-head-title {
    font-weight: bold;
    font-size: 16px;
  }

  .ant-form-item {
    margin-bottom: 16px;
  }

  .slider-labels {
    display: flex;
    justify-content: space-between;
    margin-top: 5px;
  }

  /* 新增的卡片选择器样式 */
  .resource-cards-container {
    width: 100%;
  }

  .resource-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 6px;
    margin-bottom: 16px;
  }

  .resource-card {
    cursor: pointer;
    transition: all 0.3s ease;
    border: 0.4px solid #0f52c8;
    background-color: #031c49;
    min-height: 100px;
    height: auto;
    font-size: 12px;
  }

  .resource-card :deep(.ant-card-body) {
    padding: 8px;
  }

  :deep(.ant-divider) {
    margin: 8px 0;
  }

  .resource-card:hover {
    border-color: #1890ff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }

  .resource-card.selected {
    border-color: #1890ff;
    background-color: #093788;
  }

  .resource-details p {
    font-size: 11px;
    color: #d1d0d0;
  }

  .no-resources {
    text-align: center;
    padding: 24px;
    color: #8c8c8c;
    font-style: italic;
  }
</style>

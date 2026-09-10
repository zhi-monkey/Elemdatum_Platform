<template>
  <div style="height: 100%; width: 100%">
    <BasicModal
      v-bind="$attrs"
      title="编辑信息"
      width="60%"
      @register="modalRegister"
      @close="closeModal"
      @cancel="closeModal"
      @ok="handleSubmit"
    >
      <div style="display: flex; flex-direction: column; height: 100%">
        <a-card title="基础信息编辑" style="flex: 1; margin-bottom: 10px">
          <BasicForm
            style="width: 100%"
            :schemas="generationSchemas"
            @register="registerInfoData"
            @submit="handleBindInfoSubmit"
          />
        </a-card>

        <a-card title="数据集绑定" style="flex: 1; margin-bottom: 10px">
          <PublicDatasetCascade
            ref="publicDatasetCascadeRef"
            :model-generation-id="modelGenerationId"
            :bound-versions="boundVersions"
            :initial-selected-labels="initialSelectedLabels"
            :annotate-type="annotateType"
            :annotation-format="annotationFormat"
          />
        </a-card>

        <DatasetSplitConfig v-model="splitSize" />
        <a-card title="参数配置" style="flex: 1; margin-bottom: 10px">
          <a-form
            :model="formState"
            ref="formRef"
            layout="horizontal"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 15 }"
            style="width: 100%"
            @validate="handleValidate"
          >
            <a-form-item
              v-for="(value, key) in formInputs"
              :key="key"
              :label="`${key}：`"
              :name="key"
              :rules="[
                {
                  required: true,
                  message: `${key}不能为空`,
                },
                {
                  validator: (_, val) => validateRange(val, value.range, key),
                },
              ]"
              style="width: 100%; margin-bottom: 12px"
            >
              <a-input v-model:value="formState[key]" style="width: 100%">
                <template #suffix>
                  <a-tooltip>
                    <template #title>{{ value.inputDescription }}</template>
                    <InfoCircleOutlined />
                  </a-tooltip>
                </template>
              </a-input>
              <!-- 添加错误提示插槽 -->
              <template #help>
                <span v-if="errors[key]" style="color: red">
                  {{ errors[key] }}
                </span>
              </template>
            </a-form-item>
          </a-form>
        </a-card>

        <a-card title="资源配置" style="width: 100%">
          <template #extra>
            <div style="display: flex; gap: 16px">
              <a-button type="primary" @click="openConfigurationModal()">添加配置</a-button>
              <a-button type="primary" @click="openManagementModal">删除配置</a-button>
            </div>
          </template>
          <a-row>
            <a-col :span="24">
              <ResourceCards
                :preset-configs="[]"
                :user-defined-resources="resourceOptions"
                :selected-resource-id="selectedResourceId"
                :total-resources="totalResources"
                :current-page="currentPage"
                :page-size="pageSize"
                :initial-gpu-mode="gpuMode"
                :initial-gpu-count="gpuCount"
                @select-resource="selectResource"
                @page-change="handlePageChange"
                @gpu-mode-change="handleGpuModeChange"
                @gpu-count-change="handleGpuCountChange"
              />
            </a-col>
          </a-row>
        </a-card>
      </div>

      <ConfigurationModal @register="registerConfigurationModal" />
      <ManagementModal
        @register="registerManagementModal"
        @delete-success="refreshConfigurations"
      />
    </BasicModal>
  </div>
</template>
<script setup lang="ts">
  import {
    Button as AButton,
    Card as ACard,
    Col as ACol,
    Form as AForm,
    FormItem as AFormItem,
    Input as AInput,
    Row as ARow,
    Slider as ASlider,
    Tooltip as ATooltip,
  } from 'ant-design-vue';
  import { computed, nextTick, onMounted, ref, watch } from 'vue';
  import {
    generationSchemas,
    getDatasetList,
    getModelGenerationBoundVersionsWithoutLabelFilter,
    ResourceSchemas,
  } from './data';
  import 'vue-simple-uploader/dist/style.css';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useModal, useModalInner } from '/@/components/Modal';
  import ConfigurationModal from './ConfigurationModal.vue';
  import { InfoCircleOutlined } from '@ant-design/icons-vue';
  import BasicModal from '/@/components/Modal/src/BasicModal.vue';
  import ManagementModal from '/@/views/mineai/train/generationCreate/ManagementModal.vue';
  import ResourceCards from '/@/views/mineai/train/generationCreate/ResourceCards.vue';
  import PublicDatasetCascade from '/@/views/mineai/train/generationEdit/PublicDatasetCascade.vue';
  import DatasetSplitConfig from '/@/views/mineai/train/generationEdit/DatasetSplitConfig.vue';

  const publicDatasetCascadeRef = ref();
  const [modalRegister, { closeModal }] = useModalInner(async (data) => {
    if (data.splitSize !== null) {
      // 拆分字符串并取前两个数字
      const [first, second] = data.splitSize.split('-').map((item) => parseInt(item)); // 取得第一个和第二个数字
      // 计算前两个数字对应的占比
      const splitSizeArr = [
        first * 10, // 第一个数字转换为训练集占比 20
        (first + second) * 10, // 第二个数字加上第一个数字，得到测试集占比 50
      ];
      splitSize.value = splitSizeArr;
    }
    setBasicInfo(data);
    setResourceInfo(data);

    initialSelectedLabels.value = data.selectedLabels;
  });

  // 添加一个响应式变量来存储初始标签
  const initialSelectedLabels = ref({});

  //定义一个接口，防止下面访问内部元素时提示错误
  interface ModelGenerationParams {
    hyperParams?: Object; // 可选属性，类型为 HyperParams
  }

  const boundVersions = ref([]);

  const splitSize = ref([60, 80]);

  // 定义 props
  const props = defineProps<{
    modelGenerationParams: ModelGenerationParams;
  }>();

  // 定义 emits
  const emit = defineEmits(['next', 'close', 'register']);

  // 定义表单状态
  const formState = ref({});
  const modelGenerationId = ref(0);

  // 计算属性，获取 hyperParams 对象
  const formInputs = computed(() => {
    const transformedParams = {};
    const selectedModel =
      props.modelGenerationParams?.model || props.modelGenerationParams?.modelStore || null;
    const modelConfigList = selectedModel?.trainModelVersion?.modelConfigList || [];
    // 遍历原始的hyperParams字典
    for (const key in props.modelGenerationParams?.hyperParams || {}) {
      if (
        props.modelGenerationParams?.hyperParams?.hasOwnProperty(key) &&
        key !== 'MODE_WORKING_MODE'
      ) {
        const value = props.modelGenerationParams?.hyperParams[key];

        // 查找匹配的modelConfigList项
        const modelConfig = modelConfigList.find((config) => config.field === key);

        // 获取inputDescription，如果找到的话
        const inputDescription = modelConfig ? modelConfig.inputDescription : '无描述';
        const range = modelConfig ? modelConfig.msg : null;
        // 将新的对象项添加到结果对象中
        transformedParams[key] = {
          field: key,
          value: value,
          inputDescription: inputDescription,
          range: range,
        };
      }
    }
    return transformedParams;
    // return props.modelGenerationParams?.hyperParams || {};
  });

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

  // 计算属性：将ModelGeneration的annotationType转换为Dataset的annotateType
  // Detection -> 102 (目标检测), Segmentation -> 103 (语义分割)
  // 编辑时：从ModelGeneration获取格式
  const annotateType = computed(() => {
    const annotationType = props.modelGenerationParams?.annotationType;
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
  // 编辑时：从ModelGeneration获取格式
  const annotationFormat = computed(() => {
    const annotationTypeValue = props.modelGenerationParams?.annotationType;
    const formatValue = props.modelGenerationParams?.annotationFormat;

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

  // 监视 modelGenerationParams 的变化
  watch(
    () => props.modelGenerationParams,
    async (newValue) => {
      formState.value = { ...newValue.hyperParams };
      modelGenerationId.value = newValue.id;
      boundVersions.value = await getModelGenerationBoundVersionsWithoutLabelFilter(newValue.id);
    },
    { deep: true },
  );

  const { createMessage } = useMessage();

  // 提交
  async function handleSubmit() {
    try {
      if (!selectedResourceId.value) {
        createMessage.error('请选择资源配置');
        return;
      }

      // 调用数据集绑定功能
      await handleDatasetBind();

      // 获取标签信息 - 新增逻辑
      let selectedLabelsMap = {};
      if (publicDatasetCascadeRef.value && publicDatasetCascadeRef.value.getSelectedLabels) {
        const selectedLabelsList = publicDatasetCascadeRef.value.getSelectedLabels();

        // 将标签列表转换为 Map 对象，与第一个组件保持一致
        const tempMap = new Map();
        selectedLabelsList.forEach((label, index) => {
          tempMap.set(index, label);
        });
        selectedLabelsMap = Object.fromEntries(tempMap);
      }

      await handleBindInfoSubmit(selectedLabelsMap); // 传递标签信息
      await handleBindTrainParams();
      await handleBindResource();
      createMessage.success('所有信息提交成功！');
      closeModal();
      emit('close');
    } catch (error) {
      // 如果报错为这个会在拦截器抛异常
      if (error.message === '生产任务名称重复') return;
      createMessage.error(`提交失败：${error.message ? error.message : ''}`);
    }
  }

  const [registerConfigurationModal, { openModal: openConfigurationModal }] = useModal();
  const [registerManagementModal, { openModal: openManagementModal }] = useModal();

  //绑定基础信息
  const [registerInfoData, { setFieldsValue: setBasicInfoFields, validate: validateInfo }] =
    useForm({
      labelWidth: 200,
      schemas: generationSchemas,
      showActionButtonGroup: false,
      showResetButton: false,
    });

  //绑定资源参数
  const [registerResourceData, { setFieldsValue: setResource, validate: validateResource }] =
    useForm({
      labelWidth: 150,
      schemas: ResourceSchemas,
      actionColOptions: { span: 24 },
      showActionButtonGroup: false,
      showResetButton: false,
      // submitButtonOptions: {
      //   text: '提交 ',
      // },
    });

  // 将 splitSize 从 [20, 50] 转换为 "2-3-5" 格式
  const convertToBackendFormat = (splitSize: [number, number]): string => {
    const trainSize = splitSize[0] / 10;
    const testSize = (splitSize[1] - splitSize[0]) / 10;
    const valSize = (100 - splitSize[1]) / 10;
    // 拼接成字符串 "2-3-5"
    return `${trainSize}-${testSize}-${valSize}`;
  };

  // 绑定基础信息方法
  async function handleBindInfoSubmit(selectedLabelsMap = {}) {
    const infoParams = await validateInfo();
    const splitSizeStr = convertToBackendFormat(splitSize.value);
    infoParams['splitSize'] = splitSizeStr;

    // 添加GPU模式和数量信息
    infoParams['gpuMode'] = gpuMode.value;
    infoParams['gpuCount'] = gpuCount.value;

    // 添加标签信息 - 新增逻辑
    if (Object.keys(selectedLabelsMap).length > 0) {
      infoParams['selectedLabels'] = selectedLabelsMap;
    }

    await maHttp.post(
      {
        url: 'modelGeneration/updateModelGeneration',
        params: infoParams,
        headers: {
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
  }

  // //绑定训练参数对应方法
  async function handleBindTrainParams() {
    const trainParams = formState.value;
    let trainValues = {};
    let params = {};
    Object.keys(trainParams).forEach((key) => {
      params[key] = trainParams[key];
    });
    params['MODE_WORKING_MODE'] = '1';
    trainValues['modelGenerationId'] = modelGenerationId.value;
    trainValues['params'] = params;
    await maHttp.post(
      {
        url: 'modelGeneration/setTrainParams',
        params: trainValues,
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
  }

  //保存资源配置参数
  async function handleBindResource() {
    // const resourceParams = await validateResource();
    await maHttp.get(
      {
        url: 'modelGeneration/bindHardwareParams',
        params: {
          modelGenerationId: modelGenerationId.value,
          hardwareParamsId: selectedResourceId.value,
        },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
  }

  // 新增：处理数据集绑定
  async function handleDatasetBind() {
    // 调用子组件的提交方法
    if (
      publicDatasetCascadeRef.value &&
      typeof publicDatasetCascadeRef.value.handleSubmit === 'function'
    ) {
      await publicDatasetCascadeRef.value.handleSubmit();
    }
  }

  interface item {
    value: number;
    label: string;
  }

  const modelList = ref([] as item[]);
  const modelVersionList = ref([] as item[]);

  onMounted(async () => {
    await getModelData();
    await getDatasetList();
    await fetchResourceOptions();
  });

  //复用镜像上传时，getModelData获取其他算法的函数
  async function getModelData() {
    maHttp
      .get(
        {
          url: 'model/getModelNameList',
          params: {},
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then((v) => {
        modelList.value = [];
        for (let key in v) {
          modelList.value.push({ value: Number(key), label: v[key] });
        }
      });
  }

  function setBasicInfo(mg) {
    setBasicInfoFields({ id: mg.id });
    setBasicInfoFields({ name: mg.name });
    setBasicInfoFields({ description: mg.description });
    if (mg.model) {
      setBasicInfoFields({ model: mg.model.id });
    } else if (mg.modelStore) {
      setBasicInfoFields({ model: mg.modelStore.id });
    }
  }

  function setResourceInfo(mg) {
    if (mg.hardwareParams != null) {
      selectedResourceId.value = mg.hardwareParams.id;
    }

    // 添加GPU模式和数量的回显
    if (mg.gpuMode) {
      gpuMode.value = mg.gpuMode;
    }
    if (mg.gpuCount) {
      gpuCount.value = mg.gpuCount;
    }
  }

  // 资源配置相关变量
  const resourceOptions = ref<any[]>([]);
  const selectedResourceId = ref<number | null>(null);
  // 添加分页相关变量
  const currentPage = ref(1);
  const pageSize = ref(6);
  const totalResources = ref(0);
  // 处理 GPU 模式和 GPU 数量变化
  const gpuMode = ref('single-exclusive');
  const gpuCount = ref(1);
  // 处理GPU模式变化
  const handleGpuModeChange = (data: { tabType: string; mode: string; gpuCount: number }) => {
    gpuMode.value = data.mode;
    // 根据模式类型设置GPU数量
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

  // 处理页码变化
  const handlePageChange = async (page: number) => {
    currentPage.value = page;
    await fetchResourceOptions();
  };

  // 获取资源配置列表
  const fetchResourceOptions = async () => {
    try {
      const response = await maHttp.get(
        {
          url: 'modelGeneration/findAllHardwareParams',
          params: {
            page: currentPage.value - 1,
            size: pageSize.value,
          },
          headers: {
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      // 假设返回的数据结构为 { content: [], totalElements: number }
      if (response && response.content) {
        resourceOptions.value = response.content || [];
        totalResources.value = response.totalElements;
      } else {
        // 兼容旧版API
        resourceOptions.value = response || [];
        totalResources.value = response.length || 0;
      }
    } catch (error) {
      console.error('获取资源配置失败:', error);
      resourceOptions.value = [];
      totalResources.value = 0;
    }
  };

  // 选择资源配置
  const selectResource = (id: number) => {
    selectedResourceId.value = id;
  };

  // 刷新配置
  const refreshConfigurations = async () => {
    try {
      // 重置页码
      currentPage.value = 1;
      // 重新获取资源配置列表
      await fetchResourceOptions();
    } catch (error) {
      console.error('刷新配置失败:', error);
    }
  };
</script>

<style scoped>
  .p {
    width: 100%;
  }

  .p1 {
    margin-top: 1%;
    margin-bottom: 2%;
  }

  .p2 {
    margin-bottom: 3%;
  }

  .p3 {
    margin-top: 3%;
    margin-bottom: 3%;
  }

  .uploader-example .uploader-list {
    max-height: 440px;
    margin-top: 4px;
    border-radius: 2px;
    overflow: auto;
    overflow-x: hidden;
  }

  /*上传文件的样式*/
  .uploader-list :deep(.uploader-file) {
    background-color: #434960;
    border-bottom: 1px solid #181d31;
  }

  .uploader-list :deep(.uploader-file-progress) {
    background-color: #0960bd;
  }

  .flex {
    display: flex;
  }

  .flex-col {
    flex-direction: column;
  }

  .uploader-drop {
    position: relative;
    padding: 10px;
    overflow: hidden;
    border-radius: 4px;
    border: 2px solid #0960bd;
    background-color: #181d31;
  }

  .uploader-btn-prg {
    margin-top: 10px;
    background-color: #181d31;
    height: 50px;
  }

  .vben-collapse-container :deep(.app-iconify svg) {
    font-size: 35px;
    display: inline-flex;
  }

  .slider-labels {
    display: flex;
    justify-content: space-between;
    margin-top: 5px;
  }

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

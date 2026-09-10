<template>
  <a-form :rules="formRules" :model="formModel" ref="hyperParamsFormRef">
    <div class="top-section-container">
      <a-card
        title="资源配置"
        bordered
        style="margin-bottom: 12px; padding: 12px"
        class="resource-card"
      >
        <a-row :gutter="16" align="middle">
          <a-col :span="24">
            <ResourceCards
              :preset-configs="[]"
              :user-defined-resources="resourceOptions"
              :selected-resource-id="selectedResourceId"
              :total-resources="totalResources"
              :current-page="currentPage"
              :page-size="pageSize"
              @select-resource="selectResource"
              @page-change="handlePageChange"
              @gpu-mode-change="handleGpuModeChange"
              @gpu-count-change="handleGpuCountChange"
            />
          </a-col>
        </a-row>
      </a-card>
    </div>

    <div class="top-section-container">
      <div class="dataset-split-wrapper">
        <DatasetSplitConfig v-model="splitSize" />
      </div>
    </div>

    <a-divider style="font-size: 14px" v-if="hasQuantizationParams"
      >模型转换量化数据集参数
    </a-divider>

    <div class="quantization-section" v-if="hasQuantizationParams">
      <a-row :gutter="16">
        <a-col :span="24">
          <a-form-item
            label="是否量化"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 14 }"
            name="MODE_QUANTIZATION_ON"
            :rules="formRules.quantizationOn"
          >
            <div style="display: flex; align-items: center">
              <a-radio-group v-model:value="quantizationOn.value">
                <a-radio :value="true">是</a-radio>
                <a-radio :value="false">否</a-radio>
              </a-radio-group>
              <a-tooltip title="是否需要向转换容器内传入用于模型量化的数据集">
                <InfoCircleOutlined style="margin-left: 8px" />
              </a-tooltip>
            </div>
          </a-form-item>
        </a-col>
        <a-col :span="24" v-if="quantizationOn.value">
          <a-form-item
            label="图片数量"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 14 }"
            name="quantizationImageNum"
            :rules="formRules.quantizationImageNum"
          >
            <div style="display: flex; align-items: center">
              <a-input
                v-model:value="quantizationImageNum.value"
                placeholder="请输入图片数量"
                style="flex: 1"
              />
              <a-tooltip title="需要用于量化的图片数量（不能超过训练数据集最大的图片数量）">
                <InfoCircleOutlined style="margin-left: 8px" />
              </a-tooltip>
            </div>
          </a-form-item>
        </a-col>
      </a-row>
    </div>

    <!-- 新增高级选项开关 -->
    <div class="advanced-toggle-section">
      <a-form-item label="启用高级选项" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <div style="display: flex; align-items: center">
          <a-switch
            v-model:checked="showAdvancedOptions"
            checked-children="开"
            un-checked-children="关"
          />
          <a-tooltip title="显示/隐藏高级参数配置">
            <InfoCircleOutlined style="margin-left: 8px; color: #666" />
          </a-tooltip>
        </div>
      </a-form-item>
    </div>

    <div class="custom-divider-wrapper">
      <a-divider class="custom-dashed-divider" :dashed="true" />
    </div>

    <!-- 高级选项内容 -->
    <div v-show="showAdvancedOptions" class="advanced-content">
      <!-- 训练超参数 -->
      <a-divider style="font-size: 14px">训练超参数</a-divider>
      <div class="params-section">
        <a-row :gutter="24">
          <a-col :span="24" v-for="(param, index) in trainHyperParameters" :key="index">
            <a-form-item
              :label="param.paramName"
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 14 }"
              :name="param.paramName"
              :rules="formRules[param.paramName]"
            >
              <div style="display: flex; align-items: center">
                <a-input
                  v-model:value="param.value"
                  placeholder="请输入超参数"
                  style="flex: 1"
                  class="param-input"
                />
                <a-tooltip>
                  <template #title>
                    {{ param.paramDescription || '该参数暂时没有说明' }}
                  </template>
                  <InfoCircleOutlined style="margin-left: 8px; color: #666" />
                </a-tooltip>
              </div>
            </a-form-item>
          </a-col>
        </a-row>
      </div>

      <!-- 转换超参数 -->
      <a-divider style="font-size: 14px" v-if="hasConvertHyperParams">转换超参数</a-divider>
      <div class="params-section" v-if="hasConvertHyperParams">
        <a-row :gutter="24">
          <a-col :span="24" v-for="(param, index) in convertHyperParameters" :key="index">
            <a-form-item
              :label="param.paramName"
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 14 }"
              :name="param.paramName"
              :rules="formRules[param.paramName]"
            >
              <div style="display: flex; align-items: center">
                <a-input
                  v-model:value="param.value"
                  placeholder="请输入超参数"
                  style="flex: 1"
                  class="param-input"
                />
                <a-tooltip>
                  <template #title>
                    {{ param.paramDescription || '该参数暂时没有说明' }}
                  </template>
                  <InfoCircleOutlined style="margin-left: 8px; color: #666" />
                </a-tooltip>
              </div>
            </a-form-item>
          </a-col>
        </a-row>
      </div>

      <!-- 其他超参数 -->
      <a-divider style="font-size: 14px" v-if="hasOtherHyperParams">其他超参数</a-divider>
      <div class="params-section" v-if="hasOtherHyperParams">
        <a-row :gutter="24">
          <a-col :span="24" v-for="(param, index) in otherHyperParameters" :key="'other-' + index">
            <a-form-item
              :label="param.paramName"
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 14 }"
              :name="param.paramName"
              :rules="formRules[param.paramName]"
            >
              <div style="display: flex; align-items: center">
                <a-input
                  v-model:value="param.value"
                  placeholder="请输入超参数"
                  style="flex: 1"
                  class="param-input"
                />
                <a-tooltip>
                  <template #title>
                    {{ param.paramDescription || '该参数暂时没有说明' }}
                  </template>
                  <InfoCircleOutlined style="margin-left: 8px; color: #666" />
                </a-tooltip>
              </div>
            </a-form-item>
          </a-col>
        </a-row>
      </div>
    </div>
  </a-form>
</template>

<script setup lang="ts">
  import {
    Card as ACard,
    Col as ACol,
    Divider as ADivider,
    Form as AForm,
    FormItem as AFormItem,
    Input as AInput,
    Radio as ARadio,
    RadioGroup as ARadioGroup,
    Row as ARow,
    Tooltip as ATooltip,
    Switch as ASwitch,
  } from 'ant-design-vue';
  import { InfoCircleOutlined } from '@ant-design/icons-vue';
  import { ref, watch, onMounted } from 'vue';
  import {
    getModelExploreByModelId,
    getTrainConvertAndOtherHyperParams,
    getUniqueModelApplication,
  } from '/@/views/mineai/train/generationGuideList/generationGuideList.data';
  import { getMaxPicCountByDatasetVersionId } from '/@/views/mineai/train/modelGeneration/modal/api/api';
  import { getHardwareParamsById } from '/@/views/mineai/model/generationGuide/api/modelApi';
  import { useUserStore } from '/@/store/modules/user';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ResourceCards from './ResourceCards.vue';
  import DatasetSplitConfig from './DatasetSplitConfig.vue';

  const userStore = useUserStore();
  const userData = userStore.getUserInfo;
  const { createMessage } = useMessage();

  const showAdvancedOptions = ref(false);
  const hyperParamsFormRef = ref();
  let hasConvertHyperParams = ref(false);
  let hasOtherHyperParams = ref(false);
  let hasQuantizationParams = ref(false);
  const trainHyperParameters = ref([]);
  const convertHyperParameters = ref([]);
  const otherHyperParameters = ref([]);
  const splitSize = ref([70, 80]);

  // 资源配置相关变量
  const resourceOptions = ref<any[]>([]);
  const selectedResourceId = ref<number | null>(null);
  const currentPage = ref(1);
  const pageSize = ref(6);
  const totalResources = ref(0);

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

  const quantizationOn = ref({
    paramName: 'MODE_QUANTIZATION_ON',
    value: false,
  });
  const quantizationImageNum = ref({
    paramName: 'MODE_QUANTIZATION_IMAGE_NUM',
    value: '',
  });

  const formModel = ref({
    selectedResource: selectedResourceId.value,
    quantizationOn: quantizationOn.value,
    quantizationImageNum: quantizationImageNum.value,
    splitSize: '8-1-1',
  });

  const formRules = ref({
    selectedResource: [
      {
        required: true,
        message: '请选择资源配置',
        validator: () => {
          return new Promise((resolve, reject) => {
            if (!selectedResourceId.value) {
              reject(new Error('资源配置是必填项'));
            }
            resolve();
          });
        },
        trigger: 'change',
      },
    ],
    quantizationImageNum: [
      {
        required: true,
        message: '请输入图片数量',
        validator: () => {
          return new Promise((resolve, reject) => {
            if (!quantizationImageNum.value.value) {
              reject(new Error('图片数量是必填项'));
            }
            resolve();
          });
        },
        trigger: 'change',
      },
    ],
  });

  const addDynamicParamRules = (dynamicParams) => {
    dynamicParams.forEach((param) => {
      formRules.value[param.paramName] = [
        {
          required: true,
          message: `请输入${param.paramName}`,
          validator: () => {
            return new Promise((resolve, reject) => {
              if (!param.value) {
                reject(new Error(`${param.paramName}是必填项`));
              }
              resolve();
            });
          },
          trigger: 'change',
        },
      ];
    });
  };

  const getHyperParams = async (applicationName, deviceAndFirmware) => {
    const modelApplication = await getUniqueModelApplication(applicationName, deviceAndFirmware);
    const modelId = modelApplication.model.id;
    const allParams = await getTrainConvertAndOtherHyperParams(modelId);

    trainHyperParameters.value = allParams.trainHyperParamsWithValue.map((item) => {
      return {
        ...item,
        value: item.trainDefaultValue ? item.trainDefaultValue : item.imageDefaultValue,
      };
    });

    if (allParams.convertHyperParamsWithValue.length > 0) {
      hasConvertHyperParams.value = true;
      convertHyperParameters.value = allParams.convertHyperParamsWithValue.map((item) => {
        return {
          ...item,
          value: item.trainDefaultValue ? item.trainDefaultValue : item.imageDefaultValue,
        };
      });
    }

    // 获取量化参数和其他参数
    if (allParams.otherParamsWithValue.length > 0) {
      const otherParams = [];

      allParams.otherParamsWithValue.forEach((item) => {
        if (item.paramName === 'MODE_QUANTIZATION_ON') {
          hasQuantizationParams.value = true;
          quantizationOn.value = {
            ...item,
            value: item.trainDefaultValue === 'true' || item.imageDefaultValue === 'true',
          };
        } else if (item.paramName === 'MODE_QUANTIZATION_IMAGE_NUM') {
          quantizationImageNum.value = {
            ...item,
            value: item.trainDefaultValue
              ? item.imageDefaultValue
                ? item.trainDefaultValue > item.imageDefaultValue
                  ? item.imageDefaultValue
                  : item.trainDefaultValue
                : item.trainDefaultValue
              : item.imageDefaultValue
              ? item.imageDefaultValue
              : '',
          };
        } else {
          otherParams.push({
            ...item,
            value: item.trainDefaultValue ? item.trainDefaultValue : item.imageDefaultValue,
          });
        }
      });

      if (otherParams.length > 0) {
        hasOtherHyperParams.value = true;
        otherHyperParameters.value = otherParams;
      }
    }
    // 添加训练和转换参数校验规则
    addDynamicParamRules(trainHyperParameters.value);
    addDynamicParamRules(convertHyperParameters.value);
    return await getModelExploreByModelId(modelId);
  };
  // 监听选中的资源配置变化
  watch(selectedResourceId, (newValue) => {
    formModel.value.selectedResource = newValue;
  });

  const getQuantizationImageNum = async (datasetId) => {
    const maxPicCount = await getMaxPicCountByDatasetVersionId(datasetId);
    quantizationImageNum.value = {
      paramName: 'MODE_QUANTIZATION_IMAGE_NUM',
      value: maxPicCount,
    };
  };

  const validateForm = () => {
    return hyperParamsFormRef.value.validate();
  };

  const resetForm = () => {
    hyperParamsFormRef.value.resetFields();
    selectedResourceId.value = null;
    gpuMode.value = 'single-exclusive';
    gpuCount.value = 1;
  };

  // 校验用户资源所剩还够不够开这一次任务
  const checkResource = async () => {
    if (!selectedResourceId.value) {
      createMessage.error('请选择资源配置');
      return false;
    }

    const hardWareParamsId = selectedResourceId.value;
    let hardWareParams = await getHardwareParamsById(hardWareParamsId);
    const cpuNeed = Number.parseInt(hardWareParams.cpus);
    const memoryNeed = Number.parseInt(hardWareParams.memory.split('G')[0]);
    const gpuMemoryNeed = Number.parseInt(hardWareParams.gpuMemory.split('G')[0]);
    const userCpuMemoryUsed = await getUserResourceUsed(userData.id);

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

  // 获取当前选中的资源参数
  const getSelectedResourceParams = () => {
    return {
      resource: selectedResourceId.value,
      gpuMode: gpuMode.value,
      gpuCount: gpuCount.value,
    };
  };

  // 获取当前的分割比例参数
  const getSplitParams = () => {
    return {
      splitSize: formModel.value.splitSize,
    };
  };

  onMounted(() => {
    // 获取资源配置列表
    fetchResourceOptions();
  });

  defineExpose({
    trainHyperParameters,
    convertHyperParameters,
    getHyperParams,
    getQuantizationImageNum,
    quantizationOn,
    quantizationImageNum,
    selectedResourceId,
    validateForm,
    formModel,
    resetForm,
    checkResource,
    getSelectedResourceParams,
    getSplitParams,
    gpuMode,
    gpuCount,
    getSplitSize: () => {
      return splitSize.value;
    },
  });
</script>

<style scoped lang="less">
  .slider-labels {
    display: flex;
    justify-content: space-between;
    margin-top: 8px;
    color: #595959;
    font-size: 12px;
  }

  .resource-card :deep(.ant-card-body) {
    padding: 0 24px;
  }

  /* 上部组件容器 - 不贴边，居中显示 */
  .top-section-container {
    padding: 0 60px;
    margin: 0 auto;
    max-width: 1200px;
  }

  /* 优化数据集划分组件的包装器 */
  .dataset-split-wrapper {
    margin-bottom: 12px;

    // 让DatasetSplitConfig组件更紧凑扁平
    :deep(.ant-card) {
      margin-bottom: 0;

      .ant-card-body {
        padding: 8px 14px; // 进一步从10px 16px减少到8px 14px
      }
    }

    :deep(.split-config-section .unified-container) {
      padding: 10px 14px; // 进一步从12px 16px减少到10px 14px
      margin: 0;
    }

    :deep(.config-header) {
      margin-bottom: 10px; // 进一步从12px减少到10px

      .section-title {
        font-size: 14px;

        .section-icon {
          font-size: 16px;
        }
      }

      .config-info .split-stats {
        gap: 10px; // 进一步从12px减少到10px

        .stat-item {
          padding: 6px 10px; // 进一步从8px 12px减少到6px 10px
          min-width: 70px; // 进一步从80px减少到70px

          .stat-label {
            font-size: 10px; // 进一步从11px减少到10px
            margin-bottom: 1px; // 进一步从2px减少到1px
          }

          .stat-value {
            font-size: 14px; // 进一步从16px减少到14px
          }
        }
      }
    }

    :deep(.slider-section) {
      .split-slider {
        margin-bottom: 6px; // 进一步从8px减少到6px
      }

      .slider-marks {
        margin-top: 4px; // 进一步从6px减少到4px
        font-size: 10px; // 进一步从11px减少到10px
      }
    }
  }

  /* 量化参数区域优化 */
  .quantization-section {
    margin: 16px 0;
    padding: 16px 24px;
    border-radius: 8px;
  }

  /* 高级选项开关区域 */
  .advanced-toggle-section {
    margin: 16px 0;
    padding: 12px 24px;
    border-radius: 6px;
  }

  /* 参数区域优化 - 更宽的布局 */
  .params-section {
    margin: 0 12px 24px 12px;
    padding: 20px;
    border-radius: 8px;
  }

  .advanced-content {
    .param-input {
      border-radius: 4px;
      transition: all 0.3s;

      &:hover {
        border-color: #40a9ff;
      }
    }
  }

  .ant-form-item {
    margin-bottom: 16px;
  }

  /* 自定义分割线样式保持不变 */
  .custom-divider-wrapper {
    width: 60%;
    margin: 24px 13% 24px 25%;

    :deep(.custom-dashed-divider) {
      border-color: #444444;
      margin: 0;

      &::before {
        content: '';
        display: block;
        border-top: 2px dashed #5d5d5d;
        background-image: linear-gradient(
          to right,
          #808080 0%,
          #808080 50%,
          transparent 50%,
          transparent 100%
        );
        background-size: 20px 2px;
        height: 2px;
      }

      .ant-divider-inner-text {
        display: none;
      }

      &.ant-divider-horizontal {
        margin: 0;
        padding: 0;
      }
    }
  }

  /* 资源配置卡片优化 */
  .ant-card {
    background: #1f1f1f !important;
  }

  .ant-card-head-title {
    font-weight: bold;
    font-size: 16px;
  }

  /* ResourceCards组件容器优化 */
  :deep(.resource-cards-container) {
    // 减少ResourceCards组件的内部间距
    .tab-content-wrapper {
      min-height: 160px; // 从180px减少到160px
      max-height: 180px; // 从200px减少到180px
    }
  }

  /* 响应式优化 */
  @media (max-width: 1200px) {
    .top-section-container {
      padding: 0 40px;
      max-width: 1000px;
    }

    .params-section {
      margin: 0 8px 20px 8px;
      padding: 16px;
    }

    .dataset-split-wrapper :deep(.split-config-section .unified-container) {
      padding: 16px;
    }
  }

  @media (max-width: 768px) {
    .top-section-container {
      padding: 0 20px;
      max-width: 100%;
    }

    .params-section {
      margin: 0 4px 16px 4px;
      padding: 12px;
    }

    .quantization-section,
    .advanced-toggle-section {
      margin: 12px 0;
      padding: 12px 16px;
    }

    .dataset-split-wrapper :deep(.split-config-section .unified-container) {
      padding: 12px;
    }

    // 移动端label和wrapper比例调整
    .ant-form-item {
      :deep(.ant-form-item-label) {
        flex: 0 0 25%;
      }

      :deep(.ant-form-item-control) {
        flex: 1 1 70%;
      }
    }
  }
</style>

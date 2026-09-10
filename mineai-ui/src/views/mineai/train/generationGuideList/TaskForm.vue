<template>
  <a-form ref="createFormRef" :model="formState" :rules="rules">
    <a-form-item
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 14 }"
      label="选择应用"
      name="applicationName"
    >
      <div style="display: flex; align-items: center">
        <!-- a-select 下拉框 -->
        <a-select
          v-model:value="formState.applicationName"
          style="flex: 1"
          @change="handleApplicationNameChange"
        >
          <a-select-option v-for="app in applications" :key="app.value" :value="app.value">
            {{ app.label }}
          </a-select-option>
        </a-select>

        <!-- InfoCircle 图标与 Tooltip -->
        <a-tooltip>
          <template #title>{{ APPLICATION_NAME_DESCRIPTION }}</template>
          <InfoCircleOutlined style="margin-left: 8px" />
        </a-tooltip>
      </div>
    </a-form-item>

    <a-form-item
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 14 }"
      label="选择设备-固件"
      name="deviceFirmware"
    >
      <div style="display: flex; align-items: center">
        <a-select
          v-model:value="formState.deviceFirmware"
          style="flex: 1"
          @change="handleDeviceChange"
        >
          <a-select-option
            v-for="device in deviceFirmwares"
            :key="device.value"
            :value="device.value"
          >
            {{ device.label }}
          </a-select-option>
        </a-select>

        <a-tooltip>
          <template #title>{{ DEVICE_FIRMWARE_DESCRIPTION }}</template>
          <InfoCircleOutlined style="margin-left: 8px" />
        </a-tooltip>
      </div>
    </a-form-item>

    <a-form-item
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 14 }"
      label="训练任务名称"
      name="name"
    >
      <div style="display: flex; align-items: center">
        <a-input v-model:value="formState.name" placeholder="请输入训练任务名称" style="flex: 1" />
        <a-tooltip>
          <template #title>{{ TASK_NAME_DESCRIPTION }}</template>
          <InfoCircleOutlined style="margin-left: 8px" />
        </a-tooltip>
      </div>
    </a-form-item>

    <!-- 数据集名称 -->
    <a-form-item
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 14 }"
      label="数据集名称"
      name="datasetName"
    >
      <div style="display: flex; align-items: center">
        <a-input
          v-model:value="formState.datasetName"
          placeholder="请输入数据集名称"
          style="flex: 1"
        />
        <a-tooltip>
          <template #title>{{ DATASET_NAME_DESCRIPTION }}</template>
          <InfoCircleOutlined style="margin-left: 8px" />
        </a-tooltip>
      </div>
    </a-form-item>

    <!-- 数据集组 -->
    <a-form-item
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 14 }"
      label="数据集组"
      name="datasetGroup"
    >
      <div style="display: flex; align-items: center">
        <a-input
          v-model:value="formState.datasetGroup"
          placeholder="请输入数据集组名称"
          style="flex: 1"
        />
        <a-tooltip>
          <template #title>{{ DATASET_GROUP_DESCRIPTION }}</template>
          <InfoCircleOutlined style="margin-left: 8px" />
        </a-tooltip>
      </div>
    </a-form-item>
  </a-form>
</template>

<script lang="ts" setup>
  import {
    Form as AForm,
    FormItem as AFormItem,
    Input as AInput,
    Select as ASelect,
    SelectOption as ASelectOption,
    Tooltip as ATooltip,
  } from 'ant-design-vue';
  import { InfoCircleOutlined } from '@ant-design/icons-vue';
  import { onMounted, reactive, ref, watch } from 'vue';
  import {
    findPrivateAndPublicDatasets,
    generateGuidedModelGenerationNameByApplicationName,
    getApplicationNameByBoundDeviceFirmware,
    getDeviceFirmwareByBoundApplicationName,
    getDrawerReleasedApplicationName,
    getDrawerReleasedDeviceAndFirmWare,
  } from '/@/views/mineai/train/generationGuideList/generationGuideList.data';
  import { useMessage } from '/@/hooks/web/useMessage';

  const TASK_NAME_DESCRIPTION = '本次训练任务的名称';
  const APPLICATION_NAME_DESCRIPTION = '仅可选已发布到算法应用商城，并与设备固件绑定的应用名称';
  const DEVICE_FIRMWARE_DESCRIPTION = '仅可选已发布到算法应用商城，并与应用绑定的设备固件';
  const DATASET_NAME_DESCRIPTION = '数据集名称由训练任务名称自动生成，格式为：任务名称-dataset';
  const DATASET_GROUP_DESCRIPTION = '数据集组名称由训练任务名称自动生成，格式为：任务名称-group';

  // 表单校验规则
  const rules = reactive({
    name: [
      {
        required: true,
        message: '训练任务名称是必填项',
        validator: () => {
          return new Promise((resolve, reject) => {
            if (!formState.name) {
              reject(new Error('训练任务名称是必填项'));
            }
            resolve();
          });
        },
        trigger: 'blur',
      },
    ],
    applicationName: [
      {
        required: true,
        message: '请选择一个应用',
        validator: () => {
          return new Promise((resolve, reject) => {
            if (!formState.applicationName) {
              reject(new Error('请选择一个应用'));
            }
            resolve();
          });
        },
        trigger: 'change',
      },
    ],
    deviceFirmware: [
      {
        required: true,
        message: '请选择一个设备固件',
        validator: () => {
          return new Promise((resolve, reject) => {
            if (!formState.deviceFirmware) {
              reject(new Error('请选择一个设备固件'));
            }
            resolve();
          });
        },
        trigger: 'change',
      },
    ],
    datasetName: [
      {
        required: true,
        message: '请输入数据集名称',
        validator: () => {
          return new Promise((resolve, reject) => {
            if (!formState.datasetName) {
              reject(new Error('请输入数据集名称'));
            }
            resolve();
          });
        },
        trigger: 'blur',
      },
    ],
    datasetGroup: [
      {
        required: true,
        message: '请输入数据集组名称',
        validator: () => {
          return new Promise((resolve, reject) => {
            if (!formState.datasetGroup) {
              reject(new Error('请输入数据集组名称'));
            }
            resolve();
          });
        },
        trigger: 'blur',
      },
    ],
  });

  const emit = defineEmits(['showHyperParameters', 'trainDatasetSelectChange', 'canBeCommit']);
  const createFormRef = ref();
  const { createMessage } = useMessage();

  const splitRatio = ref([80, 90]);

  const formState = reactive({
    name: '',
    applicationName: null,
    deviceFirmware: null,
    algorithm: null,
    datasetSplit: 'yes',
    splitSize: '8-1-1',
    trainDataset: null,
    testDataset: null,
    valDataset: null,
    datasetName: '',
    datasetGroup: '', // 新增数据集组字段
  });
  let applications = ref([]);

  let deviceFirmwares = ref([]);

  const datasets = ref([]);

  const handleApplicationNameChange = async () => {
    if (!formState.deviceFirmware) {
      deviceFirmwares.value = await getDeviceFirmwareByBoundApplicationName(
        formState.applicationName,
      );
    }

    // 使用 generateGuidedModelGenerationNameByApplicationName 生成任务名称并填充
    try {
      formState.name = await generateGuidedModelGenerationNameByApplicationName(
        formState.applicationName,
      ); // 将生成的名称填充到表单中的训练任务名称
    } catch (error) {
      console.error('生成训练任务名称失败:', error);
    }

    canBeCommit();
    // showHyperParameters();
  };

  // 将 splitRatio 转换为 splitSize 格式
  const convertToSplitSize = (ratio: number[]): string => {
    const testSize = ratio[0] / 10;
    const trainSize = (ratio[1] - ratio[0]) / 10;
    const valSize = (100 - ratio[1]) / 10;
    return `${testSize}-${trainSize}-${valSize}`;
  };

  // 监听 splitRatio 的变化，实时更新 splitSize
  watch(splitRatio, (newValue) => {
    console.log('splitRatio changed:', newValue); // 输出变化的值
    formState.splitSize = convertToSplitSize(newValue); // 更新 splitSize
    console.log('splitSize updated:', formState.splitSize); // 输出更新后的 splitSize
  });

  // 更新数据集名称，当任务名称更新时自动更新
  watch(
    () => formState.name,
    (newName) => {
      if (newName) {
        formState.datasetName = `${newName}-dataset`; // 自动生成数据集名称
        formState.datasetGroup = `${newName}-group`; // 自动生成数据集组名称
      }
    },
  );

  const handleDeviceChange = async () => {
    if (!formState.applicationName) {
      applications.value = await getApplicationNameByBoundDeviceFirmware(formState.deviceFirmware);
    }
    canBeCommit();
    // showHyperParameters();
  };
  // 是否已填了必填
  const canBeCommit = () => {
    if (!formState.applicationName || !formState.deviceFirmware) {
      return;
    }
    emit('canBeCommit');
  };
  const resetFields = () => {
    createFormRef.value.resetFields();
  };
  const getApplicationName = async () => {
    applications.value = await getDrawerReleasedApplicationName();
  };
  const getDeviceFirmware = async () => {
    deviceFirmwares.value = await getDrawerReleasedDeviceAndFirmWare();
  };

  const validateForm = () => {
    return createFormRef.value.validate();
    // .then(() => {
    //   return true;
    // })
    // .catch(() => {
    //   return false;
    // });
  };
  onMounted(async () => {
    await getApplicationName();
    await getDeviceFirmware();
    datasets.value = await findPrivateAndPublicDatasets();
  });

  defineExpose({
    resetFields,
    formState,
    getApplicationName,
    getDeviceFirmware,
    validateForm,
  });
</script>

<style scoped lang="less">
  .slider-labels {
    display: flex;
    justify-content: space-between;
    margin-top: 5px;
  }
</style>

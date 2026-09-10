<template>
  <a-modal
    v-model:visible="visible"
    title="模型转换"
    @ok="handleSubmit"
    :confirmLoading="confirmLoading"
    :width="800"
    :maskClosable="false"
    @cancel="handleCancel"
    :destroyOnClose="true"
  >
    <a-form
      ref="formRef"
      :model="form"
      :labelCol="{ span: 9 }"
      :wrapperCol="{ span: 15 }"
      class="convert-form"
    >
      <a-form-item label="是否量化" :label-col="{ span: 6 }" :wrapper-col="{ span: 12 }">
        <a-radio-group v-model:value="form.isQuantized" @change="handleQuantizedChange">
          <a-radio :value="false">否</a-radio>
          <a-radio :value="true">是</a-radio>
        </a-radio-group>
      </a-form-item>

      <a-form-item
        label="图片数量"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 6 }"
        v-if="form.isQuantized"
      >
        <a-input-number
          v-model:value="form.count"
          min="1"
          :max="form.maxCount"
          style="width: 100%"
          @blur="validateCount"
        />
      </a-form-item>

      <div v-if="!isBaseModelSource">
        <a-form-item
          v-for="aConfig in dynamicFormSchema"
          :key="aConfig.label"
          :label="aConfig.label"
          :required="aConfig.required"
        >
          <div class="input-wrapper">
            <a-input
              v-if="aConfig.component === 'Input'"
              v-model:value="aConfig.componentProps.placeHolder"
              :placeholder="aConfig.componentProps.placeHolder"
            />
            <a-select
              v-else-if="aConfig.component === 'Select'"
              v-model:value="aConfig.componentProps.placeHolder"
              :placeholder="aConfig.componentProps.placeHolder"
            >
              <a-select-option
                v-for="option in aConfig.componentProps.options"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </a-select-option>
            </a-select>
            <a-tooltip class="info-icon">
              <template #title>
                {{ aConfig.inputDescription ? aConfig.inputDescription : '无' }}
              </template>
              <InfoCircleOutlined />
            </a-tooltip>
          </div>
        </a-form-item>
      </div>
    </a-form>
  </a-modal>
</template>

<script lang="ts" setup>
  import {
    Form as AForm,
    FormItem as AFormItem,
    Input as AInput,
    InputNumber as AInputNumber,
    Modal as AModal,
    Radio as ARadio,
    RadioGroup as ARadioGroup,
    Select as ASelect,
    SelectOption as ASelectOption,
    Tooltip as ATooltip,
  } from 'ant-design-vue';
  import { InfoCircleOutlined } from '@ant-design/icons-vue';
  import { FormSchema } from '/@/components/Form';
  import { maHttp } from '/@/utils/http/axios';
  import { onMounted, ref, watch } from 'vue';
  import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { on } from '/@/views/mineai/train/modelGeneration/modal/convertModalEventBus';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getMaxPicCount } from '/@/views/mineai/train/modelGeneration/modal/api/api';

  // 状态定义
  const visible = ref(false);
  const confirmLoading = ref(false);
  const formRef = ref();
  const form = ref({
    isQuantized: false,
    count: null,
    maxCount: null,
  });
  const device = ref(null);
  const data = ref(null);
  const datas = ref([]);
  const activeKey = ref(['1']);
  const modelGenerationId = ref(-1);
  const image = ref('');
  const weightPath = ref('');
  const gpuNum = ref(0);
  const jobId = ref(0);
  const dynamicFormSchema = ref([]);
  const loading = ref(false);
  const isBaseModelSource = ref(false);

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();

  // 显示模态框方法
  const showModal = async (data: any) => {
    visible.value = true;
    jobId.value = data.jobId;
    modelGenerationId.value = data.modelGenerationId;
    image.value = data.image;
    weightPath.value = data.weightPath;
    gpuNum.value = data.gpuNum;
    isBaseModelSource.value = !!data.isBaseModelSource;

    if (formRef.value) {
      formRef.value.resetFields();
    }

    await fetchDataSet();
    await fetchModelConfigData(jobId.value);
  };

  // 取消处理
  const handleCancel = () => {
    visible.value = false;
    form.value = {
      isQuantized: false,
      count: null,
      maxCount: null,
    };
    isBaseModelSource.value = false;
  };

  // 获取模型配置数据
  async function fetchModelConfigData(jobId: number) {
    try {
      const response = await maHttp.get(
        {
          url: `modelJob/${jobId}/configs`,
          headers: {
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );
      const filteredData = response.filter((item) => item.field !== 'CONVERT_TARGET_PLATFORM');
      console.log(filteredData);
      generateDynamicFormSchema(filteredData);
    } catch (error) {
      console.error('Failed to fetch model config data:', error);
    }
  }

  // 获取数据集
  async function fetchDataSet() {
    try {
      const response = await maHttp.get(
        {
          url: `datarepos/queryAllDataRepos`,
          headers: {
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      datas.value = response.map((dataset) => ({
        value: dataset.uri,
        label: dataset.name,
      }));
    } catch (error) {
      console.error('Failed to fetch dataset:', error);
    }
  }

  // 生成动态表单模式
  function generateDynamicFormSchema(configs: any[]) {
    if (!Array.isArray(configs)) {
      console.error('Invalid configs format:', configs);
      return;
    }

    const schema = configs.map((config) => {
      const fieldSchema: FormSchema = {
        defaultNum: config.defaultNum,
        inputDescription: config.inputDescription,
        field: config.field,
        label: config.field,
        component: 'Input',
        required: config.required,
        componentProps: { placeHolder: config.defaultNum || '' },
      };

      switch (config.type) {
        case 'string':
          fieldSchema.component = 'Input';
          break;

        case 'numeric':
          fieldSchema.component = 'Input';
          // const numericRange = config.msg.match(/([(\[])?\s*([\d.-]*)\s*,\s*([\d.-]*)\s*([)\]])?/);
          //
          // if (numericRange) {
          //   const min = numericRange[2] ? parseFloat(numericRange[2]) : undefined;
          //   const max = numericRange[3] ? parseFloat(numericRange[3]) : undefined;
          //
          //   fieldSchema.componentProps = {
          //     onInput: (event) => {
          //       const value = event.target.value;
          //       const isValid = /^-?\d*(\.\d*)?$/.test(value);
          //       if (!isValid) {
          //         event.target.value = value.slice(0, -1);
          //       }
          //     },
          //     placeholder:
          //       min !== undefined && max !== undefined
          //         ? `Enter a number between ${min} and ${max}`
          //         : min !== undefined
          //         ? `Enter a number greater than ${min}`
          //         : max !== undefined
          //         ? `Enter a number less than ${max}`
          //         : 'Enter a number',
          //   };
          // } else {
          //   fieldSchema.componentProps = {
          //     onInput: (event) => {
          //       const value = event.target.value;
          //       const isValid = /^-?\d*(\.\d*)?$/.test(value);
          //       if (!isValid) {
          //         event.target.value = value.slice(0, -1);
          //       }
          //     },
          //     placeholder: 'Enter a number',
          //   };
          // }
          break;

        case 'enum':
          fieldSchema.component = 'Select';
          const enumMatch = config.msg.match(/\{(.*)\}/);

          if (enumMatch) {
            const enumOptions = enumMatch[1].split(',').map((value) => ({
              label: value.trim(),
              value: value.trim(),
            }));

            fieldSchema.componentProps = {
              placeHolder: config.defaultNum || '',
              options: enumOptions,
            };
          }
          break;

        default:
          fieldSchema.component = 'Input';
      }

      return fieldSchema;
    });
    dynamicFormSchema.value = schema.map((data) => ({
      defaultNum: data.defaultNum,
      component: data.component,
      field: data.field,
      label: data.label,
      componentProps: data.componentProps,
      required: data.required,
      inputDescription: data.inputDescription,
    }));
    console.log(dynamicFormSchema);
  }

  // 处理数据集变化
  function handleDatasetChange() {
    fetchModelConfigData(jobId.value);
  }

  // 提交处理
  async function handleSubmit() {
    try {
      // 校验图片数量
      if (form.value.isQuantized && (form.value.count === null || form.value.count < 1)) {
        createMessage.error('请填写有效的图片数量，且不得少于1');
        return;
      }
      confirmLoading.value = true;
      const formattedPlaceholders = {};
      dynamicFormSchema.value.forEach((item) => {
        formattedPlaceholders[item.field] = item.componentProps.placeHolder || ''; // 使用 field 作为键，placeHolder 作为值
      });
      Object.keys(form.value).forEach((key) => {
        formattedPlaceholders[key] = form.value[key]; // 使用 form 的字段和值
      });
      const values = formattedPlaceholders;
      const curTime = new Date();
      const jobName =
        weightPath.value +
        '-convert-' +
        curTime.getFullYear() +
        '-' +
        (curTime.getMonth() + 1).toString() +
        '-' +
        curTime.getDate() +
        '-' +
        curTime.getHours() +
        '-' +
        curTime.getMinutes() +
        '-' +
        curTime.getSeconds();

      const dynamicParams = {
        ...values,
      };

      // console.log(values);
      // console.log(formattedPlaceholders);
      await maHttp.post(
        {
          url: `modelJob/createConvertJob?modelGenerationId=${modelGenerationId.value}`,
          params: {
            jobName: jobName,
            image: image.value,
            datasetPath: values.datasetPath,
            weightPath: weightPath.value,
            outputWeightPath: jobName,
            gpuNum: gpuNum.value,
            params: dynamicParams,
          },
          headers: {
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      visible.value = false;
      createMessage.success('算法转换执行成功');
      emit('success');
    } catch (error) {
      console.error('Failed to submit:', error);
    } finally {
      confirmLoading.value = false;
    }
  }

  // 处理量化数据集变化
  function handleQuantizedChange() {
    if (!form.value.isQuantized) {
      form.value.count = null;
      form.value.datasetPath = null;
    }
  }

  // 验证数量
  function validateCount() {
    if (form.value.count !== null && form.value.maxCount !== null) {
      if (form.value.count > form.value.maxCount) {
        form.value.count = form.value.maxCount;
      }
    }
  }

  // 监听量化状态变化
  watch(
    () => form.value.isQuantized,
    async (newValue) => {
      if (newValue) {
        const picCount = await getMaxPicCount(jobId.value);
        form.value.maxCount = picCount;
        form.value.count = picCount;
      }
    },
  );

  // 生命周期钩子
  onMounted(() => {
    on('datasetChanged', handleDatasetChange);
  });

  // 对外暴露方法
  defineExpose({
    showModal,
  });
</script>

<style scoped>
  .ant-modal-body {
    overflow-y: auto;
    padding: 16px;
  }

  .convert-form {
    margin-left: 30px;
    margin-top: 30px;
    margin-right: 40px;
    margin-bottom: 50px;
  }

  .input-wrapper {
    display: flex;
    align-items: center;
  }

  .info-icon {
    margin-left: 8px; /* 调整图标与输入框之间的间距 */
    cursor: pointer; /* 鼠标悬停时显示为手型 */
  }
</style>

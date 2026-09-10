<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="请填写发布信息" @ok="handleSubmit">
    <header style="margin-left: 35px; margin-bottom: 16px">名称预览：{{ previewModelName }}</header>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { computed, ref, Ref } from 'vue';
  import { error } from '/@/utils/log';

  let modelGenerationId: Ref<number> = ref(0);
  let bestWeightPath: Ref<string> = ref('');
  let modelJobId: Ref<number> = ref(0);
  let modelFrameName: Ref<string> = ref('算法框架');
  let modelName: Ref<string> = ref('算法名称');
  let modelVersion: Ref<string> = ref('算法版本');
  let description = ref(''); // 初始化为响应式的 description

  const { createMessage } = useMessage();
  const previewModelName = computed(() => {
    return `${modelFrameName.value}-${modelName.value}-${modelVersion.value}`;
  });
  const emits = defineEmits(['success']);
  const [registerModal, { setModalProps, closeModal }] = useModalInner((data) => {
    setModalProps({ confirmLoading: true });
    modelGenerationId.value = data.modelGenerationId;
    bestWeightPath.value = data.bestWeightPath;
    modelJobId.value = data.modelJobId;
    description.value = data.description || '无'; // 确保 description 为空时有默认值
    modelFrameName.value = '算法框架';
    modelName.value = '算法名称';
    modelVersion.value = '算法版本';
    // 设置表单的初始值
    setFieldsValue({
      description: description.value,
    });
    //resetFields();
    setModalProps({ confirmLoading: false });
  });
  const [registerForm, { validate, setFieldsValue }] = useForm({
    labelWidth: 100,
    schemas: [
      {
        field: 'description',
        label: '算法描述',
        component: 'Input',
        componentProps: {
          //value: description.value, // 设置默认值
          placeholder: '请输入算法描述',
          onChange: (value) => (description.value = value.target.value),
        },
        required: true,
      },
      {
        field: 'modelFrameName',
        label: '算法框架',
        component: 'Input',
        componentProps: {
          placeholder: '请输入算法框架(eg:pytorch、tensor...) ',
          onChange: (value) => (modelFrameName.value = value.target.value),
        },
        required: true,
      },
      {
        field: 'modelName',
        label: '算法名称',
        component: 'Input',
        componentProps: {
          placeholder: '请输入算法名称(eg:yolov5、ssd...)',
          onChange: (value) => (modelName.value = value.target.value),
        },
        required: true,
      },
      {
        field: 'modelVersion',
        label: '算法版本',
        rules: [
          {
            required: true,
            validator: async (rule, value) => {
              const regex = /^v\d+\.\d+$/;
              if (!value) {
                return Promise.reject('算法版本为必填项');
              }
              if (!regex.test(value)) {
                return Promise.reject('输入格式错误，请输入类似 v1.0 的版本号');
              }
              return Promise.resolve();
            },
            trigger: 'change',
          },
        ],
        component: 'Input',
        componentProps: {
          placeholder: '请输入算法版本(eg:v1.0、v2.0...)',
          onChange: (value) => (modelVersion.value = value.target.value),
        },
      },
    ],
    showActionButtonGroup: false,
  });

  function handleSuccess() {
    createMessage.success('成功发布到基础算法库！！');
  }

  async function handleSubmit() {
    const { modelFrameName, modelName, modelVersion, description } = await validate();
    await maHttp
      .get(
        {
          url: 'modelGeneration/generationRelease',
          params: {
            modelGenerationId: modelGenerationId.value,
            bestWeightPath: bestWeightPath.value,
            modelJobId: modelJobId.value,
            modelName: modelFrameName + '-' + modelName + '-' + modelVersion,
            description: description, // 在提交时发送 description 字段
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        // 成功处理逻辑
        emits('success');
        handleSuccess();
        closeModal();
      })
      .catch((error) => {
        // 捕获并处理异常
        createMessage.error(`算法名必须唯一: ${error.message}`);
      });
  }
</script>

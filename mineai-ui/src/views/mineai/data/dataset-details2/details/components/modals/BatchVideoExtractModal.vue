// Modal.vue
<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="视频抽帧" @ok="handleSubmit">
    <BasicForm
      :labelWidth="100"
      :schemas="schemas"
      :actionColOptions="{ span: 24 }"
      :showResetButton="false"
      :showSubmitButton="false"
      @register="registerForm"
    />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { ref } from 'vue';

  const splitType = ref(1);
  const splitInterval = ref(1);
  const emits = defineEmits(['submit']);
  const schemas: FormSchema[] = [
    {
      field: 'splitType',
      label: '抽帧间隔单位',
      component: 'RadioButtonGroup',
      defaultValue: 1,
      required: true,
      componentProps: {
        options: [
          { label: '帧', value: 1 },
          { label: '秒', value: 25 },
          { label: '分', value: 25 * 60 },
          { label: '小时', value: 25 * 60 * 60 },
        ],
        onChange: (e) => {
          splitType.value = e;
        },
      },
    },
    {
      field: 'frameInterval',
      label: '抽帧间隔',
      component: 'InputNumber',
      componentProps: {
        min: 1,
        onChange: (e) => {
          splitInterval.value = e;
        },
      },
      required: true,
    },
  ];

  const [registerForm, { setFieldsValue, validate }] = useForm({
    labelWidth: 100,
    schemas: schemas,
    showActionButtonGroup: false,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async () => {
    await setFieldsValue({
      frameInterval: 1,
    });
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });
      emits('submit', values.frameInterval * values.splitType);
      closeModal();
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="图片下载" @ok="handleSubmit">
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
<script setup lang="ts">
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { ref } from 'vue';

  const emits = defineEmits(['single', 'batch']);

  const batchDownload = ref(false);
  const ids = ref([]);
  const urls = ref([]);
  const withLabel = ref(false);

  const schemas: FormSchema[] = [
    {
      field: 'withLabel',
      label: '是否下载标注文件',
      component: 'RadioButtonGroup',
      defaultValue: 0,
      required: true,
      dynamicDisabled: () => !withLabel.value,
      componentProps: {
        options: [
          { label: '是', value: 1 },
          { label: '否', value: 0 },
        ],
      },
    },
    {
      field: 'labelType',
      label: '标注文件类型',
      component: 'RadioButtonGroup',
      defaultValue: 'createML',
      componentProps: {
        options: [
          { label: 'VOC', value: 'VOC' },
          { label: 'YOLO', value: 'YOLO' },
          { label: 'createML', value: 'createML' },
        ],
      },
      ifShow: ({ values }) => {
        return values.withLabel === 1;
      },
    },
  ];

  const [registerForm, { resetFields, validate, updateSchema }] = useForm({
    labelWidth: 150,
    schemas: schemas,
    showActionButtonGroup: false,
  });

  const [registerModal, { closeModal }] = useModalInner(async (data) => {
    // 语义分割
    if (data.annotateType == 103) {
      await updateSchema({
        field: 'labelType',
        componentProps: {
          options: [
            { label: 'YOLO', value: 'Segment-YOLO' },
            // { label: 'COCO', value: 'Segment-COCO' },
          ],
        },
      });
    } else {
      await updateSchema({
        field: 'labelType',
        componentProps: {
          options: [
            { label: 'VOC', value: 'VOC' },
            { label: 'YOLO', value: 'YOLO' },
            { label: 'createML', value: 'createML' },
          ],
        },
      });
    }
    await resetFields();
    batchDownload.value = data.batchDownload;
    ids.value = data.ids;
    urls.value = data.urls;
    withLabel.value = data.withLabel;
  });

  async function handleSubmit() {
    const values = await validate();
    batchDownload.value
      ? emits('batch', values.labelType, ids.value, urls.value)
      : emits('single', values.labelType, ids.value[0], urls.value[0]);
    closeModal();
  }
</script>

<style scoped lang="less"></style>

// Modal.vue
<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="选择标注信息格式" @ok="handleSubmit">
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
  import { getImageAnnotationInfo, queryFile } from '/@/views/mineai/data/dataset-details2/api';
  import { parseAnnotationInfo } from '/@/views/mineai/data/dataset-details2/util';
  import { Modal } from 'ant-design-vue';
  import { h } from 'vue';
  import { JsonPreview } from '/@/components/CodeEditor';
  import vkbeautify from 'vkbeautify';

  let datasetId;
  let fileId;
  let labels;

  const schemas: FormSchema[] = [
    {
      field: 'type',
      label: '标注格式',
      component: 'RadioButtonGroup',
      defaultValue: 'createML',
      required: true,
      componentProps: {
        options: [
          { label: 'createML', value: 'createML' },
          { label: 'VOC', value: 'VOC' },
          { label: 'YOLO', value: 'YOLO' },
        ],
      },
    },
  ];

  const [registerForm, { setFieldsValue, validate }] = useForm({
    labelWidth: 100,
    schemas: schemas,
    showActionButtonGroup: false,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    datasetId = data.datasetId;
    fileId = data.fileId;
    labels = data.labels;
    await setFieldsValue({
      type: 'createML',
    });
  });

  async function handleSubmit() {
    try {
      setModalProps({ confirmLoading: true });
      const values = await validate();
      if (values.type === 'createML') {
        const file = await queryFile(datasetId, parseInt(fileId));
        let annotations: any[] = file.annotation
          ? parseAnnotationInfo(file.annotation, labels)
          : [];
        annotations.forEach((e) => {
          delete e.data.score;
        });
        Modal.info({
          title: '标注详情',
          content: h(JsonPreview, { data: annotations }),
          width: 600,
          closable: true,
        });
      } else if (values.type === 'YOLO') {
        const annotations = await getImageAnnotationInfo(datasetId, parseInt(fileId), 'YOLO');
        Modal.info({
          title: '标注详情',
          content: h('textarea', {
            value: annotations,
            style: {
              maxWidth: '100%',
              width: '400px',
              fontSize: '1rem',
              padding: '10px',
              height: '600px',
              overflow: 'scroll',
            },
            readonly: true,
          }),
          width: 600,
          closable: true,
        });
      } else {
        const annotations = await getImageAnnotationInfo(datasetId, parseInt(fileId), 'VOC');
        Modal.info({
          title: '标注详情',
          content: h('xmp', {
            innerHTML: vkbeautify.xml(annotations),
            style: {
              height: '600px',
              overflow: 'scroll',
            },
          }),
          width: 600,
          closable: true,
        });
      }
      closeModal();
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

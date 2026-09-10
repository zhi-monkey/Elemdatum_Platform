<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    showFooter
    :title="title"
    width="500px"
    @ok="handleSubmit"
  >
    <BasicForm @register="registerForm" class="myForm" />
  </BasicModal>
</template>
<script lang="ts" setup>
  import { defineEmits, ref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formSchema } from './saveVersion';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { queryNextVersion, saveVersion } from '/@/views/mineai/data/dataset-details2/api';
  import { useMessage } from '/@/hooks/web/useMessage';

  const emits = defineEmits(['success']);
  const { createMessage } = useMessage();

  const [registerForm, { resetFields, setFieldsValue, validate, updateSchema }] = useForm({
    labelWidth: 90,
    schemas: formSchema,
    showActionButtonGroup: false,
  });
  const currentRecord = ref(null);

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    currentRecord.value = data.record;
    if (data.record.annotateType === 103) {
      await updateSchema({
        field: 'format',
        label: '导出格式',
        component: 'Select',
        componentProps: {
          options: [
            { label: 'YOLO', value: 'Segment-YOLO' },
            // { label: 'COCO', value: 'Segment-COCO' },
          ],
        },
        required: true,
      });
    } else {
      await updateSchema({
        field: 'format',
        label: '导出格式',
        component: 'Select',
        componentProps: {
          options: [
            // { label: 'CreateML', value: 'COCO' },
            { label: 'YOLO', value: 'YOLO' },
            // { label: 'VOC', value: 'VOC' },
          ],
        },
        required: true,
      });
    }
    await resetFields();
    const nextVersionName = await queryNextVersion(data.record.id);
    setModalProps({ confirmLoading: false });
    await setFieldsValue({
      ...data.record,
      nextVersionName: nextVersionName,
    });
  });

  const title = '保存数据集版本';

  // 主提交函数
  async function handleSubmit() {
    try {
      const values = await validate();

      createMessage.info('开始保存数据集版本');
      setModalProps({ confirmLoading: true });

      // 3. 准备数据和创建新标签
      const data = {
        datasetId: parseInt(values.id),
        format: values.format,
        versionNote: values.versionNote,
        // 这里传入的参数无关, 后端会覆盖为自己映射自己
        labelMappings: null,
      };

      await saveVersion(data);

      // 5. 完成流程
      closeModal();
      emits('success');
    } catch (err) {
      console.error('保存数据集版本失败:', err);
      createMessage.error('保存数据集版本失败!');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
<style scoped>
  .myForm >>> .ant-radio-button-wrapper-disabled {
    background-color: #292421;
    color: grey;
  }
</style>

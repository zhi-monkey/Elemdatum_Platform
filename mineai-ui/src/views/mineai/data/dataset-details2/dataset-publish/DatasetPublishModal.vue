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
  <a-modal
    v-model:visible="labelMappingModalVisible"
    title="标签对齐"
    @ok="handleLabelMappingConfirm"
    @cancel="handleLabelMappingCancel"
    :width="700"
    :bodyStyle="{ maxHeight: '70vh', overflow: 'auto', padding: '12px' }"
    :centered="true"
    :destroyOnClose="true"
  >
    <LabelMapping
      ref="labelMappingRef"
      :datasetLabels="datasetLabels"
      :templateLabels="templateLabels"
      v-model:labelMappings="labelMappings"
    />
  </a-modal>
</template>
<script lang="ts" setup>
  import { defineEmits, ref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formSchema } from './publication';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { Modal as AModal } from 'ant-design-vue';
  import {
    createLabel,
    datasetReleaseWithOption,
    generateRandomHexColor,
    getLabels,
    getTemplateLabels,
    queryNextVersion,
  } from '/@/views/mineai/data/dataset-details2/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import LabelMapping from '/@/views/mineai/data/dataset-details2/labelMapping/LabelMapping.vue';

  const emits = defineEmits(['success']);
  const labelMappingModalVisible = ref(false);
  const { createMessage } = useMessage();

  const [registerForm, { resetFields, setFieldsValue, validate, updateSchema }] = useForm({
    labelWidth: 90,
    schemas: formSchema,
    showActionButtonGroup: false,
  });
  const currentRecord = ref<any>(null);
  const labelMappingRef = ref();

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
            //{ label: 'COCO', value: 'Segment-COCO' },
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
          options: [{ label: 'YOLO', value: 'YOLO' }],
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

  const title = '数据集发布';

  const datasetLabels = ref([]);
  const templateLabels = ref([]);
  const labelMappingResolve = ref<Function | null>(null);
  const labelMappingReject = ref<Function | null>(null);
  const labelMappings = ref({});

  // 将映射处理逻辑提取为独立函数
  function convertLabelMappings(mappings: Record<string, any>) {
    return Object.entries(mappings).map(([sourceLabelId, targetLabelId]) => ({
      sourceLabelId: parseInt(sourceLabelId),
      targetLabelId: targetLabelId === 'none' ? null : parseInt(targetLabelId as string),
    }));
  }

  // 处理新标签的创建
  async function createNewLabels(
    datasetId: number,
    datasetLabels: any[],
    templateLabels: any[],
    mappings: Record<string, any>,
  ) {
    // 获取所有被映射的目标标签ID (排除null和"none")
    const mappedTargetLabelIds = new Set(
      Object.values(mappings)
        .filter((targetId) => targetId !== null && targetId !== 'none')
        .map((targetId: any) => targetId.toString()),
    );

    // 只创建那些既不在数据集中存在，又被用户实际映射使用的标签
    const sourceLabelNames = new Set(datasetLabels.map((item) => item.name));
    const newLabels = templateLabels.filter(
      (label) => !sourceLabelNames.has(label.name) && mappedTargetLabelIds.has(label.id.toString()),
    );

    if (newLabels.length === 0) {
      console.log('没有需要创建的新标签');
      return [];
    } else {
      console.log('需要创建的新标签:', newLabels);
    }

    // 返回Promise.all以便等待所有创建操作完成
    return Promise.all(
      newLabels.map((label) =>
        createLabel(datasetId, {
          name: label.name,
          color: generateRandomHexColor(),
        }),
      ),
    );
  }

  // 获取所需标签数据
  async function fetchLabelData(datasetId: number) {
    const [datasetLabelsData, templateLabelsData] = await Promise.all([
      getLabels(datasetId),
      getTemplateLabels(),
    ]);

    return {
      datasetLabels: datasetLabelsData,
      templateLabels: templateLabelsData,
    };
  }

  // 显示标签映射对话框
  async function showLabelMappingModal() {
    return new Promise((resolve, reject) => {
      labelMappingResolve.value = resolve;
      labelMappingReject.value = reject;
      labelMappingModalVisible.value = true;
    });
  }

  // 主提交函数
  async function handleSubmit() {
    try {
      const values = await validate();
      if (!currentRecord.value) {
        return createMessage.error('数据集记录不存在');
      }
      const datasetId = currentRecord.value.id;

      // 获取标签数据
      const { datasetLabels: fetchedDatasetLabels, templateLabels: fetchedTemplateLabels } =
        await fetchLabelData(datasetId);

      datasetLabels.value = fetchedDatasetLabels;
      templateLabels.value = fetchedTemplateLabels;

      // 2. 显示映射对话框等待用户操作
      const mappingConfirmed = await showLabelMappingModal();

      if (!mappingConfirmed) {
        return createMessage.info('操作已取消');
      }

      createMessage.info('开始数据集发布');
      setModalProps({ confirmLoading: true });

      // 3. 准备数据和创建新标签
      const data = {
        datasetId: parseInt(values.id),
        format: values.format,
        versionNote: values.versionNote,
        labelMappings: convertLabelMappings(labelMappings.value),
        needArchive: values.needArchive,
      };

      await createNewLabels(
        datasetId,
        datasetLabels.value,
        templateLabels.value,
        labelMappings.value,
      );
      await datasetReleaseWithOption(data);

      // 5. 完成流程
      closeModal();
      emits('success');
    } catch (err) {
      console.error('数据集发布失败:', err);
      createMessage.error('数据集发布失败!');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }

  async function handleLabelMappingConfirm() {
    if (!(await labelMappingRef.value.validate())) {
      return;
    }
    labelMappingModalVisible.value = false;
    if (labelMappingResolve.value) {
      labelMappingResolve.value(true);
    }
  }

  function handleLabelMappingCancel() {
    labelMappingModalVisible.value = false;
    if (labelMappingResolve.value) {
      labelMappingResolve.value(false);
    }
  }
</script>
<style scoped>
  .myForm >>> .ant-radio-button-wrapper-disabled {
    background-color: #292421;
    color: grey;
  }
</style>

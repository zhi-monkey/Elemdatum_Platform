<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    showFooter
    title="自动标注"
    width="1000px"
    @ok="handleSubmit"
  >
    <!-- BasicForm 结构保持不变，但插槽内容会根据UI逻辑变化 -->
    <BasicForm @register="registerForm" class="myForm">
      <!-- 引导式训练任务卡片选择器插槽 -->
      <template #guidedModelTaskSelectorSlot="{ model, field }">
        <CardSelector
          type="guided"
          :request-param="datasetId"
          :selected-value="model.guidedGenerationId"
          @update:selected="handleGuidedTaskSelect"
        />
      </template>
    </BasicForm>
  </BasicModal>

  <!-- 标签选择弹窗 (用于训练模型) -->
  <a-modal
    :visible="labelModalVisible"
    title="选择保留标签"
    :width="800"
    :centered="true"
    :destroyOnClose="true"
    :maskClosable="false"
    @cancel="handleLabelModalCancel"
  >
    <template #footer>
      <div class="modal-footer">
        <div class="selected-info">
          <a-tag v-if="selectedLabelNames.length > 0" color="processing">
            已选择 {{ selectedLabelNames.length }} 个标签
          </a-tag>
          <a-tag v-else color="default">未选择标签</a-tag>
        </div>
        <div class="footer-buttons">
          <a-button @click="handleLabelModalCancel">取消</a-button>
          <a-button
            type="primary"
            @click="handleLabelModalConfirm"
            :disabled="selectedLabelNames.length === 0"
          >
            确定 ({{ selectedLabelNames.length }})
          </a-button>
        </div>
      </div>
    </template>

    <LabelChoose
      ref="labelSelectorRef"
      :model-generation-id="currentModelGenerationId"
      :auto-label-image-url="currentAutoLabelImageUrl"
      :default-selected="selectedLabelNames"
      :dataset-id="datasetId"
      v-if="labelModalVisible"
      @update:selected="(labels) => (selectedLabelNames = labels)"
    />
  </a-modal>
</template>

<script setup lang="ts">
  import { defineEmits, ref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { Modal as AModal, Button as AButton, Tag as ATag } from 'ant-design-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import moment from 'moment/moment';

  import CardSelector from '/@/views/mineai/data/dataset-details2/auto-label/CardSelector.vue';
  import LabelChoose from './label-choose.vue';
  import { formSchema } from './autoLabel.ts';

  // 状态变量
  const datasetId = ref<number | null>(null);
  const datasetPath = ref('');
  const labelModalVisible = ref(false);
  const labelSelectorRef = ref(null);
  const currentModelGenerationId = ref<number | null>(null);
  const currentAutoLabelImageUrl = ref(''); // 默认模型需要
  const selectedLabelNames = ref<string[]>([]);

  const { createMessage } = useMessage();
  const emits = defineEmits(['success']);

  const [registerForm, { resetFields, validate, updateSchema, setFieldsValue, getFieldsValue }] =
    useForm({
      labelWidth: 100,
      schemas: formSchema,
      showActionButtonGroup: false,
    });

  async function resetFormToInitialState(data) {
    await resetFields();

    await setFieldsValue({
      datasetId: data.id,
      annotateType: data.annotateType,
      applicationName: data.applicationName,
      deviceFirmware: data.deviceFirmware,
    });
    await updateSchema([
      // 显示训练模型路径
      { field: 'isStandardTrain', show: true, required: true },
      { field: 'guidedModelTaskSelector', show: true, required: true },
      { field: 'guidedGenerationId', required: true },

      // 隐藏默认模型路径
      { field: 'image', show: false, required: false },

      { field: 'labelInfo', show: false },
      { field: 'modelInfo', show: false },
    ]);

    await setFieldsValue({
      useDefault: false,
    });
  }

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    datasetId.value = data.id;
    datasetPath.value = `dataset/${data.id}`;

    await resetFormToInitialState(data);

    setModalProps({ confirmLoading: false });
  });

  function handleGuidedTaskSelect(payload: { key: number } | null) {
    const value = payload ? payload.key : null;
    setFieldsValue({ guidedGenerationId: value });
    if (value) {
      validate(['guidedGenerationId']);
    }
  }

  const showLabelSelector = (modelGenId: number | null, imageUrl: string): Promise<string[]> => {
    return new Promise((resolve, reject) => {
      currentModelGenerationId.value = modelGenId;
      currentAutoLabelImageUrl.value = imageUrl;
      // 清空上次的选择
      selectedLabelNames.value = [];
      labelModalVisible.value = true;

      window._labelSelectorResolve = resolve;
      window._labelSelectorReject = reject;
    });
  };

  const handleLabelModalConfirm = async () => {
    if (labelSelectorRef.value && typeof window._labelSelectorResolve === 'function') {
      const selected = (labelSelectorRef.value as any).getSelectedLabels();
      window._labelSelectorResolve(selected);
    }
    labelModalVisible.value = false;
  };

  const handleLabelModalCancel = () => {
    if (typeof window._labelSelectorReject === 'function') {
      window._labelSelectorReject(new Error('用户取消选择'));
    }
    labelModalVisible.value = false;
  };

  async function handleSubmit() {
    try {
      const allValues = getFieldsValue();
      const fieldsToValidate: string[] = ['useDefault', 'confidence'];

      if (allValues.useDefault) {
        fieldsToValidate.push('image');
      } else {
        fieldsToValidate.push('guidedGenerationId');
      }

      const values = await validate(fieldsToValidate);
      setModalProps({ confirmLoading: true });

      let finalLabels: string[] = [];

      if (values.useDefault) {
        const labelData = getFieldsValue().labelData || [];
        if (labelData.length === 0) {
          createMessage.error('该镜像未配置任何可用标签，无法自动标注！');
          setModalProps({ confirmLoading: false });
          return;
        }
        finalLabels = labelData.map((label) => label.name);
      } else {
        const userSelectedLabels = await showLabelSelector(values.guidedGenerationId, '');
        if (!userSelectedLabels || userSelectedLabels.length === 0) {
          createMessage.warning('操作已取消，未选择任何标签');
          setModalProps({ confirmLoading: false });
          return;
        }
        finalLabels = userSelectedLabels;
      }

      const now = moment().format('MM-DD-HH-mm-ss').toString();
      const jobRequest = {
        jobName: 'autolabel-' + now,
        modelGenerationId: values.useDefault ? undefined : values.guidedGenerationId,
        standardJobName: undefined, // 旧组件无此项
        useDefault: values.useDefault,
        datasetPath: datasetPath.value,
        imageUrl: values.useDefault ? values.image : '',
        params: {
          MODE_WORKING_MODE: 5,
          HP_CONFIDENCE: Number(values.confidence),
        },
        labelNames: finalLabels,
      };

      await maHttp.post(
        { url: 'job/createAutoLabelJob', data: jobRequest },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      createMessage.success('自动标注任务创建成功！');
      closeModal();
      emits('success');
    } catch (error: any) {
      if (error && error.message !== '用户取消选择') {
        console.error('提交失败:', error);
      }
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<style scoped lang="less">
  .myForm {
    padding-top: 20px;
  }
  .modal-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    .selected-info {
      flex: 1;
    }
    .footer-buttons {
      display: flex;
      gap: 8px;
    }
  }
  :deep(.ant-modal-body) {
    padding: 16px;
    max-height: 70vh;
    overflow-y: auto;
  }
</style>

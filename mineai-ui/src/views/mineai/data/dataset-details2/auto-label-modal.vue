<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    showFooter
    title="自动标注"
    width="1000px"
    @ok="handleSubmit"
    @cancel="handleModalCancel"
  >
    <BasicForm @register="registerForm" class="myForm">
      <template #modelTaskSelectorSlot="{ model, field }">
        <CardSelector
          ref="standardCardSelectorRef"
          type="standard"
          :request-param="model.standardGenerationId"
          :selected-value="model.standardJobName"
          @update:selected="handleStandardTaskSelect"
        />
      </template>

      <!-- 引导式训练任务卡片选择器插槽 -->
      <template #guidedModelTaskSelectorSlot="{ model, field }">
        <CardSelector
          ref="guidedCardSelectorRef"
          type="guided"
          :request-param="datasetId"
          :selected-value="model.guidedGenerationId"
          @update:selected="handleGuidedTaskSelect"
        />
      </template>
    </BasicForm>
  </BasicModal>

  <!-- 标签选择弹窗 -->
  <a-modal
    :visible="labelModalVisible"
    title="选择保留标签"
    :width="800"
    :centered="true"
    :destroyOnClose="true"
    :maskClosable="false"
    @cancel="handleLabelModalCancel"
    confirmText="开始自动标注"
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
      v-if="labelModalVisible"
      @update:selected="(labels) => (selectedLabelNames = labels)"
    />
  </a-modal>
</template>

<script setup lang="ts">
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { Modal as AModal, Button as AButton, Tag as ATag } from 'ant-design-vue';
  import { fetchModelInfo, formSchema, resetLabelSwitch } from './autoLabel';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { defineEmits, ref } from 'vue';
  import moment from 'moment/moment';
  import LabelChoose from '/@/views/mineai/data/dataset-details2/auto-label/label-choose.vue';
  import CardSelector from '/@/views/mineai/data/dataset-details2/auto-label/CardSelector.vue';

  // 响应式数据
  const datasetId = ref<number | null>(null);
  const datasetPath = ref('');
  const labelSelectorRef = ref(null);
  const labelModalVisible = ref(false);
  const currentModelGenerationId = ref<number | null>(null);
  const currentAutoLabelImageUrl = ref('');
  const selectedLabelNames = ref<string[]>([]);
  const { createMessage } = useMessage();
  const standardCardSelectorRef = ref<InstanceType<typeof CardSelector> | null>(null);
  const guidedCardSelectorRef = ref<InstanceType<typeof CardSelector> | null>(null);

  // 表单和弹窗注册
  const [registerForm, { resetFields, validate, updateSchema, setFieldsValue, getFieldsValue }] =
    useForm({
      labelWidth: 100,
      schemas: formSchema,
      showActionButtonGroup: false,
    });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    datasetId.value = data.id;
    datasetPath.value = `dataset/${data.id}`;

    await resetFields();

    // 设置 datasetId 到表单，用于 ApiSelect 调用接口时使用
    await setFieldsValue({ datasetId: data.id, annotateType: data.annotateType });

    await updateSchema([
      // 确保默认是"训练模型"模式

      { field: 'isStandardTrain', show: true, required: true },
      { field: 'standardGenerationId', show: true, required: true },
      { field: 'standardJobName', required: true },
      { field: 'modelTaskSelector', show: true },

      { field: 'guidedGenerationId', show: false, required: false },
      { field: 'guidedModelTaskSelector', show: false, required: false },
      { field: 'image', show: false, required: false },

      { field: 'labelInfo', show: false },
    ]);

    setModalProps({ confirmLoading: false });
  });
  function handleStandardTaskSelect(payload) {
    if (payload) {
      setFieldsValue({ standardJobName: payload.key });
    } else {
      setFieldsValue({ standardJobName: null });
    }
    // 触发一次校验，让“必填”提示消失
    // validate(['modelTaskSelector']);
  }

  function handleGuidedTaskSelect(payload) {
    if (payload) {
      setFieldsValue({ guidedGenerationId: payload.key });
    } else {
      setFieldsValue({ guidedGenerationId: null });
    }
    // 触发一次校验
    // validate(['guidedModelTaskSelector']);
  }

  // 标签选择弹窗处理
  const showLabelSelector = (
    modelGenerationId: number,
    autoLabelImageUrl: string,
  ): Promise<string[]> => {
    return new Promise((resolve, reject) => {
      currentModelGenerationId.value = modelGenerationId;

      currentAutoLabelImageUrl.value = autoLabelImageUrl;
      labelModalVisible.value = true;

      window._labelSelectorResolve = resolve;
      window._labelSelectorReject = reject;
    });
  };

  const handleLabelModalConfirm = async () => {
    try {
      if (labelSelectorRef.value) {
        const selected = labelSelectorRef.value.getSelectedLabels();
        selectedLabelNames.value = selected;

        labelModalVisible.value = false;

        if (window._labelSelectorResolve) {
          window._labelSelectorResolve(selected);
          delete window._labelSelectorResolve;
          delete window._labelSelectorReject;
        }
        await labelSelectorRef.value.clearSelection();
        selectedLabelNames.value = [];
      }
    } catch (error) {
      console.error('确认选择标签时出错:', error);
      createMessage.error('操作失败，请重试');
    }
  };

  const handleLabelModalCancel = async () => {
    labelModalVisible.value = false;
    // 不要重置selectedLabelNames，保持用户的选择状态

    if (window._labelSelectorReject) {
      window._labelSelectorReject(new Error('用户取消选择'));
      delete window._labelSelectorResolve;
      delete window._labelSelectorReject;
    }
    await labelSelectorRef.value.clearSelection();
    selectedLabelNames.value = [];
  };

  // 事件发射器
  const emits = defineEmits(['success']);

  // 主提交处理
  async function handleSubmit() {
    try {
      const allValues = getFieldsValue();
      setModalProps({ confirmLoading: true });

      const fieldsToValidate: string[] = ['useDefault', 'confidence'];

      if (allValues.useDefault) {
        fieldsToValidate.push('image');
      } else {
        fieldsToValidate.push('isStandardTrain');
        if (allValues.isStandardTrain) {
          fieldsToValidate.push('standardGenerationId');
          fieldsToValidate.push('standardJobName');
        } else {
          fieldsToValidate.push('guidedGenerationId');
        }
      }

      const values = await validate(fieldsToValidate);

      setModalProps({ confirmLoading: true });

      let modelGenerationId: number | null = null;
      let modelJobName: string | undefined = undefined;
      let imageUrl = '';

      if (values.useDefault) {
        // 默认模型模式
        imageUrl = values.image;
        if (!imageUrl) {
          createMessage.error('请选择一个镜像！');
          setModalProps({ confirmLoading: false });
          return;
        }
      } else {
        // 训练模型模式
        if (values.isStandardTrain) {
          modelJobName = values.standardJobName;
          modelGenerationId = values.standardGenerationId;
          if (!modelJobName) {
            createMessage.error('请选择一个训练任务！');
            setModalProps({ confirmLoading: false });
            return;
          }
        } else {
          modelGenerationId = values.guidedGenerationId;
          if (!modelGenerationId) {
            createMessage.error('请选择一个引导式训练任务！');
            setModalProps({ confirmLoading: false });
            return;
          }
        }
      }

      // 1. 弹出标签选择框
      const selectedLabels = await showLabelSelector(modelGenerationId, imageUrl);

      if (!selectedLabels || selectedLabels.length === 0) {
        // 用户取消或未选择标签
        createMessage.warning('操作已取消，未选择任何标签');
        setModalProps({ confirmLoading: false });
        return;
      } else {
        // 2. 创建自动标注任务
        const now = moment().format('MM-DD-HH-mm-ss').toString();
        const jobRequest = {
          jobName: 'autolabel-' + now,
          // 核心参数调整
          modelGenerationId: !values.useDefault ? modelGenerationId : undefined,
          standardJobName: !values.useDefault && values.isStandardTrain ? modelJobName : undefined,

          datasetPath: datasetPath.value,
          useDefault: values.useDefault,
          imageUrl: values.useDefault ? imageUrl : '',

          params: {
            MODE_WORKING_MODE: 5,
            HP_CONFIDENCE: Number(values.confidence),
          },
          labelNames: selectedLabels,
          clearExistingLabels: allValues.clearExistingLabels || false,
          requireManualConfirmation: allValues.requireManualConfirmation !== false, // 默认true
        };

        await maHttp.post(
          { url: 'job/createAutoLabelJob', data: jobRequest },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );

        createMessage.success('自动标注任务创建成功！');
        closeModal();
        emits('success');
      }
    } catch (error: any) {
      if (error && error.message !== '用户取消选择') {
        console.error('提交失败:', error);
      }
    } finally {
      setModalProps({ confirmLoading: false });
      standardCardSelectorRef.value.clearSearch();
      guidedCardSelectorRef.value.clearSearch();
    }
  }

  const handleModalCancel = () => {
    if (standardCardSelectorRef.value?.clearSearch) {
      standardCardSelectorRef.value.clearSearch();
    }
    if (guidedCardSelectorRef.value?.clearSearch) {
      guidedCardSelectorRef.value.clearSearch();
    }
    // 清空标签选择器
    if (labelSelectorRef.value?.clearSelection) {
      labelSelectorRef.value.clearSelection();
    }
    selectedLabelNames.value = [];

    // 关闭弹窗
    closeModal();
  };
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

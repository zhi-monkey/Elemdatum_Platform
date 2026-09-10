<template>
  <a-modal
    :visible="visible"
    :title="title"
    :width="700"
    :confirm-loading="confirmLoading"
    @ok="handleSubmit"
    @cancel="handleCancel"
    :okButtonProps="{ disabled: isGuided }"
    :cancelButtonProps="{ disabled: isGuided }"
  >
    <div v-if="isGuided" style="padding: 20px 16px">
      <div class="guided-alert">
        <a-alert
          message="引导式数据集"
          description="引导式数据集不允许新增或修改标签，只能查看已有标签。"
          type="warning"
          show-icon
        />
      </div>
    </div>

    <div class="label-container">
      <div class="existing-labels">
        <h4>已有标签</h4>
        <a-spin v-if="loading" class="spin-container" />
        <template v-else>
          <div v-if="existingLabels.length" class="tag-list">
            <a-tag
              v-for="label in existingLabels"
              :key="label.id"
              :color="label.color || 'pink'"
              class="label-tag"
            >
              {{ label.name }}
            </a-tag>
          </div>
          <div v-else class="no-labels">暂无标签</div>
        </template>
      </div>

      <div v-if="!isGuided" class="new-labels">
        <h4>新增标签</h4>
        <div class="form-container">
          <div v-for="(item, index) in newLabels" :key="index" class="form-row">
            <a-input
              v-model:value="item.name"
              placeholder="输入标签名称"
              :maxlength="20"
              :disabled="item.saving"
              class="label-input"
              @change="validateLabelName(index)"
            />
            <div class="color-picker-container">
              <a-tooltip title="选择颜色">
                <el-color-picker v-model="item.color" color-format="hex" class="color-picker" />
              </a-tooltip>
            </div>
            <a-button
              type="link"
              danger
              @click="removeNewLabel(index)"
              :disabled="item.saving"
              class="remove-btn"
            >
              <delete-outlined />
            </a-button>
          </div>
        </div>

        <a-button
          type="dashed"
          @click="addNewLabel"
          block
          class="add-btn"
          :disabled="newLabels.length >= 10"
        >
          <plus-outlined />
          添加标签
        </a-button>

        <div v-if="errorMessage" class="error-message">
          <a-alert :message="errorMessage" type="error" show-icon />
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
  import { ref, computed, watch } from 'vue';
  import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue';
  import { queryLabels, createLabel } from '/@/views/mineai/data/dataset-details2/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    Button as AButton,
    Modal as AModal,
    Alert as AAlert,
    Tooltip as ATooltip,
    Input as AInput,
    Tag as ATag,
    Spin as ASpin,
  } from 'ant-design-vue';

  const { createMessage } = useMessage();

  const props = defineProps({
    visible: Boolean,
    datasetId: Number,
    isGuided: Boolean,
  });

  const emit = defineEmits(['update:visible', 'submitted']);

  const title = computed(() => (props.isGuided ? '查看标签' : '管理标签'));

  const existingLabels = ref<any[]>([]);
  const newLabels = ref<Array<{ name: string; color: string; saving?: boolean }>>([]);
  const confirmLoading = ref(false);
  const errorMessage = ref('');
  const loading = ref(false);

  // 获取已有标签
  const fetchLabels = async () => {
    if (!props.datasetId) return;

    loading.value = true;
    try {
      const response = await queryLabels(props.datasetId, {});
      existingLabels.value = response || [];
    } catch (e) {
      createMessage.error('获取标签失败');
      console.error(e);
    } finally {
      loading.value = false;
    }
  };

  // 添加新标签行
  const addNewLabel = () => {
    if (newLabels.value.length >= 10) return;

    newLabels.value.push({
      name: '',
      color: generateRandomColor(),
    });
  };

  // 移除标签行
  const removeNewLabel = (index: number) => {
    newLabels.value.splice(index, 1);
  };

  // 生成随机颜色
  const generateRandomColor = () => {
    const letters = '0123456789ABCDEF';
    let color = '#';
    for (let i = 0; i < 6; i++) {
      color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
  };

  // 验证标签名称
  const validateLabelName = (index: number) => {
    errorMessage.value = '';
    const label = newLabels.value[index];

    if (!label.name.trim()) {
      errorMessage.value = '标签名称不能为空';
      return false;
    }

    if (label.name.length > 20) {
      errorMessage.value = '标签名称不能超过20个字符';
      return false;
    }

    // 检查是否与已有标签重复
    if (existingLabels.value.some((l) => l.name === label.name)) {
      errorMessage.value = `标签名称 "${label.name}" 已存在`;
      return false;
    }

    // 检查是否与新增标签重复
    if (newLabels.value.filter((_, i) => i !== index).some((l) => l.name === label.name)) {
      errorMessage.value = `标签名称 "${label.name}" 已添加`;
      return false;
    }

    return true;
  };

  // 提交处理
  const handleSubmit = async () => {
    if (props.isGuided) return;

    // 验证所有新标签
    for (let i = 0; i < newLabels.value.length; i++) {
      if (!validateLabelName(i)) return;
    }

    if (!newLabels.value.length) {
      createMessage.warning('请添加至少一个标签');
      return;
    }

    confirmLoading.value = true;

    try {
      // 保存所有新标签
      for (const label of newLabels.value) {
        label.saving = true;
        await createLabel(props.datasetId!, {
          name: label.name,
          color: label.color,
        });
      }

      createMessage.success('标签添加成功');
      emit('submitted');
      closeModal();
    } catch (e) {
      console.error(e);
      createMessage.error('添加标签失败');
    } finally {
      confirmLoading.value = false;
    }
  };

  // 关闭模态框
  const handleCancel = () => {
    closeModal();
  };

  // 关闭模态框
  const closeModal = () => {
    emit('update:visible', false);
  };

  // 监听可见性变化
  watch(
    () => props.visible,
    (val) => {
      if (val) {
        fetchLabels();
        newLabels.value = [];
        errorMessage.value = '';
      }
    },
  );
</script>

<style lang="less" scoped>
  .spin-container {
    display: flex;
    justify-content: center;
    padding: 20px 0;
  }

  /* 新增无标签提示样式 */
  .no-labels {
    color: rgba(232, 232, 232, 0.45);
    font-size: 14px;
    padding: 8px 0;
  }

  .label-container {
    padding: 12px 16px;
  }

  /* 美化后的样式 */
  .guided-alert {
    padding: 16px;
    background: linear-gradient(135deg, #fff7e6 0%, #fef5e7 100%);
    border: 1px solid #ffd666;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(255, 193, 7, 0.1);
    position: relative;
    overflow: hidden;
  }

  .guided-alert::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 4px;
    height: 100%;
    background: linear-gradient(180deg, #faad14 0%, #fa8c16 100%);
  }

  .guided-alert :deep(.ant-alert) {
    background: transparent;
    border: none;
    padding: 0;
    margin: 0;
  }

  .guided-alert :deep(.ant-alert-message) {
    font-weight: 600;
    font-size: 15px;
    color: #d48806;
    margin-bottom: 4px;
  }

  .guided-alert :deep(.ant-alert-description) {
    color: #ad6800;
    font-size: 14px;
    line-height: 1.5;
  }

  .guided-alert :deep(.ant-alert-icon) {
    color: #faad14;
    font-size: 16px;
    margin-right: 8px;
  }

  /* 响应式优化 */
  @media (max-width: 768px) {
    .guided-alert {
      padding: 12px;
      margin-bottom: 16px;
    }

    .guided-alert :deep(.ant-alert-message) {
      font-size: 14px;
    }

    .guided-alert :deep(.ant-alert-description) {
      font-size: 13px;
    }
  }

  /* 悬停效果 */
  .guided-alert:hover {
    box-shadow: 0 4px 12px rgba(255, 193, 7, 0.15);
    transform: translateY(-1px);
    transition: all 0.3s ease;
  }

  /* 暗色主题适配 */
  .dark .guided-alert {
    background: linear-gradient(135deg, #2c2416 0%, #332a1a 100%);
    border-color: #594214;

    &::before {
      background: linear-gradient(180deg, #d48806 0%, #ad6800 100%);
    }
  }

  .dark .guided-alert :deep(.ant-alert-message) {
    color: #faad14;
  }

  .dark .guided-alert :deep(.ant-alert-description) {
    color: #d48806;
  }

  /* 动画效果 */
  .guided-alert {
    animation: slideInDown 0.3s ease-out;
  }

  @keyframes slideInDown {
    from {
      opacity: 0;
      transform: translateY(-10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  .existing-labels {
    margin-bottom: 24px;
    min-height: 60px;

    h4 {
      margin-bottom: 12px;
      font-weight: 500;
    }

    .tag-list {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .label-tag {
      padding: 4px 10px;
      border-radius: 4px;
      color: white;
      font-weight: 500;
    }
  }

  .new-labels {
    h4 {
      margin-bottom: 12px;
      font-weight: 500;
    }

    .form-container {
      margin-bottom: 16px;
    }

    .form-row {
      display: flex;
      align-items: center;
      margin-bottom: 12px;

      .label-input {
        flex: 1;
        margin-right: 12px;
      }

      .color-picker-container {
        width: 40px;
        margin-right: 12px;

        .color-picker {
          width: 100%;
        }
      }

      .remove-btn {
        color: #ff4d4f;
      }
    }

    .add-btn {
      margin-top: 8px;
    }

    .error-message {
      margin-top: 16px;
    }
  }
</style>

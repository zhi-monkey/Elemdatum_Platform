<template>
  <BasicModal :visible="visible" @close="handleClose" :title="title" :footer="null">
    <!-- 训练模型参数 -->
    <div class="section">
      <h3 class="section-title">训练模型参数</h3>
      <div class="params-container">
        <div class="params-column" v-for="(value, key) in params.trainParams" :key="key">
          <span class="param-key">{{ key }}:</span>
          <span class="param-value">{{ value }}</span>
        </div>
      </div>
    </div>

    <!-- 转换模型参数 -->
    <div class="section">
      <h3 class="section-title">转换模型参数</h3>
      <template v-if="hasConvertParams">
        <div class="params-container">
          <div class="params-column" v-for="(value, key) in params.convertParams" :key="key">
            <span class="param-key">{{ key }}:</span>
            <span class="param-value">{{ value }}</span>
          </div>
        </div>
      </template>
      <div v-else class="empty-state">
        <span class="empty-text">无</span>
      </div>
    </div>

    <!-- 其他配置参数 -->
    <div class="section">
      <h3 class="section-title">其他配置参数</h3>
      <div class="params-container">
        <div class="params-column" v-for="(value, key) in params.otherParams" :key="key">
          <span class="param-key">{{ key }}:</span>
          <span class="param-value">{{ value }}</span>
        </div>
      </div>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { PropType, defineEmits, defineProps } from 'vue';
  import BasicModal from '/@/components/Modal/src/BasicModal.vue';
  import { computed } from 'vue';

  const hasConvertParams = computed(() => {
    if (!props.params.convertParams || Object.keys(props.params.convertParams).length === 0) {
      return false;
    }

    // 增强空值判断
    const hasValidValue = Object.values(props.params.convertParams).some((value) => {
      return (
        value !== undefined &&
        value !== null &&
        value !== '' &&
        !(typeof value === 'string' && value.trim() === '')
      );
    });

    return hasValidValue;
  });

  const props = defineProps({
    visible: {
      type: Boolean,
      required: true,
    },
    title: {
      type: String,
      default: '参数详情',
    },
    params: {
      type: Object as PropType<{
        trainParams: Record<string, string>;
        convertParams: Record<string, string>;
        otherParams: Record<string, string>;
      }>,
      required: true,
      default: () => ({
        trainParams: {},
        convertParams: {},
        otherParams: {},
      }),
    },
  });

  const emit = defineEmits(['update:visible']);
  function handleClose() {
    emit('update:visible', false);
  }
</script>

<style scoped>
  .section {
    margin-bottom: 20px;
    padding: 15px;
    background-color: #1e1e1e;
    border-radius: 8px;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
  }

  .section-title {
    font-size: 18px;
    font-weight: bold;
    color: #3399ff;
    margin-bottom: 10px;
    border-bottom: 2px solid #e0e0e0;
    padding-bottom: 5px;
  }

  .params-container {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .params-column {
    display: flex;
    justify-content: space-between;
    font-size: 15px;
    padding: 1px 0;
  }

  .param-key {
    font-weight: 600;
    color: #e0e0e0;
  }

  .param-value {
    color: #e0e0e0;
  }
  .empty-state {
    padding: 12px;
    text-align: center;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 4px;
    margin-top: 8px;
  }

  .empty-text {
    color: #666;
    font-style: italic;
    font-size: 14px;
  }
</style>

<template>
  <a-modal
    :visible="visible"
    title="自定义分配比例"
    width="900px"
    @ok="handleOk"
    @cancel="close"
    :maskClosable="false"
    :okButtonProps="{ disabled: totalProportion !== 100 || isDisabled }"
  >
    <div class="proportion-container">
      <!-- 禁用提示 -->
      <div v-if="isDisabled" class="disabled-notice">
        <a-alert
          :message="disabledReason"
          type="warning"
          show-icon
          banner
          style="margin-bottom: 16px"
        />
      </div>

      <div class="proportion-header">
        <div class="header-item">成员</div>
        <div class="header-item">分配比例</div>
        <div class="header-item">图片数量</div>
      </div>

      <div v-for="(member, index) in members" :key="member.id" class="proportion-row">
        <div class="member-info">
          <div class="member-details">
            <span class="member-name">{{ member.nickName }}</span>
            <span class="member-username">{{ member.username }}</span>
          </div>
        </div>

        <div class="slider-container">
          <a-slider
            v-model:value="proportions[index]"
            :min="0"
            :max="100"
            :step="1"
            :disabled="isDisabled"
            class="proportion-slider"
          />
          <a-input-number
            v-model:value="proportions[index]"
            :min="0"
            :max="100"
            :step="1"
            :disabled="isDisabled"
            class="proportion-input"
            @change="validateInput(index)"
          />
          <span class="percent-symbol">%</span>
        </div>

        <div class="image-count"> {{ imageCounts[index] }} 张 </div>
      </div>

      <div class="proportion-footer">
        <div class="footer-stats">
          <div class="stat-item">
            <span class="stat-label">总比例:</span>
            <span :class="['stat-value', totalProportionClass]"> {{ totalProportion }}% </span>
          </div>
          <div class="stat-item">
            <span class="stat-label">总图片数:</span>
            <span class="stat-value">{{ totalImages }} 张</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">团队人数:</span>
            <span class="stat-value">{{ members.length }} 人</span>
          </div>
        </div>
        <a-button type="primary" @click="resetToEqual" :disabled="isDisabled" class="reset-button">
          重置为均分
        </a-button>
      </div>

      <div v-if="totalProportion !== 100 && !isDisabled" class="error-tip">
        <warning-outlined style="color: #ff4d4f" />
        <span>总比例必须为100%</span>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { WarningOutlined } from '@ant-design/icons-vue';
  import {
    Alert as AAlert,
    Button as AButton,
    InputNumber as AInputNumber,
    Modal as AModal,
    Slider as ASlider,
  } from 'ant-design-vue';

  const props = defineProps({
    visible: Boolean,
    members: {
      type: Array,
      default: () => [],
    },
    totalImages: {
      type: Number,
      default: 0,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    disabledReason: {
      type: String,
      default: '',
    },
  });

  const emit = defineEmits(['update:visible', 'submit']);

  // 比例数组 (0-100)
  const proportions = ref<number[]>([]);

  // 计算总比例
  const totalProportion = computed(() => proportions.value.reduce((sum, val) => sum + val, 0));

  // 总比例样式类
  const totalProportionClass = computed(() =>
    totalProportion.value === 100 ? 'success' : 'error',
  );

  // 是否禁用（优先使用外部传入的禁用状态）
  const isDisabled = computed(() => {
    return (
      props.disabled ||
      (props.totalImages > 0 &&
        props.members.length > 0 &&
        props.totalImages < props.members.length)
    );
  });

  // 禁用原因
  const disabledReason = computed(() => {
    if (props.disabledReason) {
      return props.disabledReason;
    }
    if (
      props.totalImages > 0 &&
      props.members.length > 0 &&
      props.totalImages < props.members.length
    ) {
      return `当前数据集仅有 ${props.totalImages} 张图片，少于团队成员数量 ${props.members.length} 人，无法进行自定义分配。建议选择图片数量更多的数据集或减少团队成员。`;
    }
    return '';
  });

  // 计算每个成员的图片数量（处理余数）
  const imageCounts = computed(() => {
    if (props.totalImages === 0 || totalProportion.value !== 100 || isDisabled.value) {
      return props.members.map(() => 0);
    }

    const total = props.totalImages;
    const memberCount = props.members.length;

    // 总图片数必须至少等于成员数量
    if (total < memberCount) {
      return props.members.map(() => 0);
    }

    // 第一阶段：每个成员分配1张基础图片
    const remaining = total - memberCount;

    // 第二阶段：剩余图片按比例精确分配
    const exactValues = proportions.value.map((p) => (p / 100) * remaining);
    const bases = exactValues.map((v) => Math.floor(v));
    let remainingAfterFloor = remaining - bases.reduce((a, b) => a + b, 0);

    // 按小数部分及比例进行余数分配
    const decimalInfo = exactValues
      .map((exact, index) => ({
        index,
        decimal: exact - Math.floor(exact),
        proportion: proportions.value[index],
      }))
      .sort((a, b) => b.decimal - a.decimal || b.proportion - a.proportion);

    // 实施余数分配
    const allocated = [...bases];
    for (let i = 0; i < remainingAfterFloor; i++) {
      allocated[decimalInfo[i].index]++;
    }

    // 合并基础分配和剩余分配
    return allocated.map((count) => count + 1);
  });

  // 验证输入值
  const validateInput = (index: number) => {
    if (proportions.value[index] > 100) {
      proportions.value[index] = 100;
    } else if (proportions.value[index] < 0) {
      proportions.value[index] = 0;
    }
  };

  // 重置为均分
  const resetToEqual = () => {
    if (isDisabled.value) return;

    const memberCount = props.members.length;
    if (memberCount === 0) return;

    const base = Math.floor(100 / memberCount);
    const remainder = 100 % memberCount;

    proportions.value = props.members.map((_, index) =>
      index === memberCount - 1 ? base + remainder : base,
    );
  };

  // 关闭模态框
  const close = () => {
    emit('update:visible', false);
  };

  // 提交比例数据
  const handleOk = () => {
    if (totalProportion.value !== 100 || isDisabled.value) return;

    const userProportions = props.members.map((member, index) => ({
      userId: member.id,
      proportion: proportions.value[index],
    }));

    emit('submit', userProportions);
    close();
  };

  // 当成员变化时重置比例
  watch(
    () => props.members,
    (newMembers) => {
      if (newMembers.length > 0 && !isDisabled.value) {
        resetToEqual();
      }
    },
    { immediate: true },
  );

  // 监听禁用状态变化
  watch(
    () => isDisabled.value,
    (disabled) => {
      if (disabled) {
        // 如果被禁用，将所有比例设为0
        proportions.value = props.members.map(() => 0);
      } else if (props.members.length > 0) {
        // 如果启用且有成员，重置为均分
        resetToEqual();
      }
    },
    { immediate: true },
  );
</script>

<style scoped lang="less">
  @primary-color: #1890ff;
  @error-color: #ff4d4f;
  @success-color: #52c41a;
  @border-color: #e8e8e8;

  .proportion-container {
    padding: 16px;
    max-height: 70vh;
    overflow-y: auto;
  }

  .disabled-notice {
    margin-bottom: 16px;
  }

  .proportion-header,
  .proportion-row {
    display: grid;
    grid-template-columns: 200px 400px 100px;
    gap: 20px;
    align-items: center;
    padding: 12px 16px;
  }

  .proportion-header {
    font-weight: 600;
    border-radius: 4px;
    margin-bottom: 8px;

    .header-item {
      color: #8d8d8d;
      font-size: 14px;
      text-align: center;

      &:first-child {
        text-align: left;
      }
    }
  }

  .member-info {
    display: flex;
    align-items: center;
    gap: 12px;

    .member-avatar {
      flex-shrink: 0;
    }

    .member-details {
      display: flex;
      flex-direction: column;
    }

    .member-name {
      font-weight: 500;
      font-size: 14px;
    }

    .member-username {
      font-size: 12px;
      color: #888;
    }
  }

  .slider-container {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    justify-content: center;
  }

  .proportion-slider {
    flex: 1;
    min-width: 160px;
    max-width: 200px;
  }

  .proportion-input {
    width: 70px;
    flex-shrink: 0;
  }

  .percent-symbol {
    flex-shrink: 0;
  }

  .image-count {
    text-align: center;
    font-weight: 500;
    color: @text-color;
  }

  .proportion-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 24px;
    padding: 16px;
    border-radius: 4px;
  }

  .footer-stats {
    display: flex;
    gap: 24px;
  }

  .stat-item {
    display: flex;
    gap: 8px;
  }

  .stat-label {
    color: #8d8d8d;
  }

  .stat-value {
    font-weight: 600;

    &.success {
      color: @success-color;
    }

    &.error {
      color: @error-color;
    }
  }

  .reset-button {
    color: @text-color;

    &:hover:not(:disabled) {
      border-color: #bfbfbf;
    }
  }

  .error-tip {
    display: flex;
    align-items: center;
    gap: 8px;
    color: @error-color;
    margin-top: 16px;
    padding: 8px 16px;
    border-radius: 4px;
    font-weight: 500;
  }

  .proportion-row:nth-child(odd) {
  }
</style>

<template>
  <a-card title="数据集划分配置">
    <div class="split-config-section">
      <div class="unified-container">
        <div class="config-header">
          <div class="section-title">
            <span class="section-icon">📊</span>
            <span class="section-text">数据集划分占比</span>
          </div>
          <div class="config-info">
            <div class="split-stats">
              <div class="stat-item train">
                <span class="stat-label">训练集</span>
                <span class="stat-value">{{ splitSize[0] }}%</span>
              </div>
              <div class="stat-item test">
                <span class="stat-label">测试集</span>
                <span class="stat-value">{{ splitSize[1] - splitSize[0] }}%</span>
              </div>
              <div class="stat-item val">
                <span class="stat-label">验证集</span>
                <span class="stat-value">{{ 100 - splitSize[1] }}%</span>
              </div>
            </div>
          </div>
        </div>

        <div class="slider-section">
          <a-slider
            v-model:value="splitSize"
            range
            :min="1"
            :max="99"
            :step="1"
            @change="handleSliderChange"
            class="split-slider"
            :tooltip="{ formatter: (value) => `${value}%` }"
          />
          <div class="slider-marks">
            <span class="mark-start">1%</span>
            <span class="mark-middle">50%</span>
            <span class="mark-end">99%</span>
          </div>
        </div>
      </div>
    </div>
  </a-card>
</template>

<script setup lang="ts">
  import { ref, defineProps, defineEmits, watch } from 'vue';
  import { normalizeSplitRange } from '/@/views/mineai/train/utils/datasetSplit';
  import { Card as ACard, Slider as ASlider } from 'ant-design-vue';

  // 定义属性
  const props = defineProps({
    modelValue: {
      type: Array as () => number[],
      default: () => [70, 80],
    },
  });

  // 定义事件
  const emit = defineEmits(['update:modelValue']);

  // 数据集划分配置
  const splitSize = ref(normalizeSplitRange(props.modelValue));

  watch(
    () => props.modelValue,
    (newValue) => {
      splitSize.value = normalizeSplitRange(newValue);
    },
  );

  // 处理滑块变化，确保每个集合至少1%
  const handleSliderChange = (value: number[]) => {
    splitSize.value = normalizeSplitRange(value);
    emit('update:modelValue', splitSize.value);
  };
</script>

<style scoped lang="less">
  .split-config-section {
    .unified-container {
      padding: 16px; // 从12px增加到16px
      background: linear-gradient(135deg, rgba(40, 40, 50, 0.4) 0%, rgba(30, 30, 40, 0.3) 100%);
      border-radius: 12px;
      border: 1px solid rgba(255, 255, 255, 0.1);
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);

      .config-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px; // 从16px增加到20px

        .section-title {
          display: flex;
          align-items: center;
          gap: 10px; // 从8px增加到10px
          color: #ffffff;
          font-size: 15px; // 从14px增加到15px
          font-weight: 600;

          .section-icon {
            font-size: 17px; // 从16px增加到17px
          }

          .section-text {
            text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
          }
        }

        .config-info {
          .split-stats {
            display: flex;
            gap: 12px; // 从8px增加到12px

            .stat-item {
              display: flex;
              flex-direction: row; // 保持横向布局
              align-items: center;
              justify-content: center;
              padding: 6px 12px; // 从4px 8px增加到6px 12px
              border-radius: 8px; // 从6px增加到8px
              border: 2px solid;
              min-width: 95px; // 从85px增加到95px
              gap: 6px; // 从4px增加到6px
              box-shadow: 0 3px 8px rgba(0, 0, 0, 0.1); // 稍微增加阴影

              &.train {
                background: linear-gradient(
                  135deg,
                  rgba(24, 144, 255, 0.15) 0%,
                  rgba(24, 144, 255, 0.1) 100%
                );
                border-color: #1890ff;
              }

              &.test {
                background: linear-gradient(
                  135deg,
                  rgba(250, 173, 20, 0.15) 0%,
                  rgba(250, 173, 20, 0.1) 100%
                );
                border-color: #faad14;
              }

              &.val {
                background: linear-gradient(
                  135deg,
                  rgba(82, 196, 26, 0.15) 0%,
                  rgba(82, 196, 26, 0.1) 100%
                );
                border-color: #52c41a;
              }

              .stat-label {
                font-size: 11px; // 从10px增加到11px
                color: #e0e0e0;
                margin-bottom: 0; // 横向布局不需要下边距
                font-weight: 500;
                white-space: nowrap; // 防止换行
              }

              .stat-value {
                font-size: 15px; // 从13px增加到15px
                color: #ffffff;
                font-weight: 700;
                text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
                line-height: 1; // 减少行高
              }
            }
          }
        }
      }

      .slider-section {
        .split-slider {
          margin-bottom: 12px; // 从8px增加到12px
        }

        .slider-marks {
          display: flex;
          justify-content: space-between;
          font-size: 11px; // 从10px增加到11px
          color: #888;
          margin-top: 6px; // 从4px增加到6px

          .mark-start,
          .mark-end {
            color: #ff4d4f;
          }

          .mark-middle {
            color: #4096ff;
          }
        }
      }
    }
  }

  // 滑块样式优化 - 简洁单色
  :deep(.ant-slider) {
    .ant-slider-rail {
      background: rgba(255, 255, 255, 0.1) !important;
    }

    .ant-slider-track {
      background: #1890ff !important;
    }

    .ant-slider-handle {
      border-color: #1890ff !important;
      box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.3) !important;

      &:hover,
      &:focus {
        border-color: #40a9ff !important;
        box-shadow: 0 0 0 4px rgba(24, 144, 255, 0.2) !important;
      }
    }
  }
</style>

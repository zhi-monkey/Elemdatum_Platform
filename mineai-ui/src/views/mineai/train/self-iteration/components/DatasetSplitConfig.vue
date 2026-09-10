<template>
  <div class="split-config-section">
    <div class="unified-container">
      <div class="config-header">
        <div class="section-title">
          <span class="section-icon">🎯</span>
          <span class="section-text">数据集划分占比</span>
        </div>
        <div class="config-info">
          <div class="split-stats">
            <div class="stat-item train">
              <span class="stat-label">训练集</span>
              <span class="stat-value">{{ splitSize[0] }}%</span>
            </div>
            <div class="stat-item val">
              <span class="stat-label">验证集</span>
              <span class="stat-value">{{ splitSize[1] - splitSize[0] }}%</span>
            </div>
            <div class="stat-item test">
              <span class="stat-label">测试集</span>
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
          class="split-slider"
          :disabled="disabled"
          :tooltip="{ formatter: (value) => `${value}%` }"
          @change="handleSliderChange"
        />
        <div class="slider-marks">
          <span class="mark-start">1%</span>
          <span class="mark-middle">50%</span>
          <span class="mark-end">99%</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue';
  import { Slider as ASlider } from 'ant-design-vue';
  import {
    normalizeSplitRange,
    splitRangeToPercentString,
    splitStringToRange,
  } from '/@/views/mineai/train/utils/datasetSplit';

  const props = defineProps({
    modelValue: {
      type: String,
      default: '8-1-1',
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  });

  const emit = defineEmits(['update:modelValue', 'change']);

  const splitSize = ref<[number, number]>([80, 90]);

  watch(
    () => props.modelValue,
    (newValue) => {
      splitSize.value = splitStringToRange(newValue);
    },
    { immediate: true },
  );

  const handleSliderChange = (value: number[]) => {
    splitSize.value = normalizeSplitRange(value);
    const nextValue = splitRangeToPercentString(splitSize.value);
    emit('update:modelValue', nextValue);
    emit('change', nextValue);
  };
</script>

<style scoped lang="less">
  .split-config-section {
    .unified-container {
      padding: 16px;
      background: linear-gradient(135deg, rgba(40, 40, 50, 0.4) 0%, rgba(30, 30, 40, 0.3) 100%);
      border-radius: 12px;
      border: 1px solid rgba(255, 255, 255, 0.1);
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);

      .config-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;

        .section-title {
          display: flex;
          align-items: center;
          gap: 10px;
          color: #ffffff;
          font-size: 15px;
          font-weight: 600;

          .section-icon {
            font-size: 17px;
          }
        }

        .config-info {
          .split-stats {
            display: flex;
            gap: 10px;

            .stat-item {
              display: flex;
              flex-direction: row;
              align-items: center;
              justify-content: center;
              padding: 6px 12px;
              border-radius: 8px;
              border: 2px solid;
              min-width: 88px;
              gap: 6px;

              &.train {
                background: rgba(24, 144, 255, 0.12);
                border-color: #1890ff;
              }

              &.val {
                background: rgba(82, 196, 26, 0.12);
                border-color: #52c41a;
              }

              &.test {
                background: rgba(250, 173, 20, 0.12);
                border-color: #faad14;
              }

              .stat-label {
                font-size: 11px;
                color: #e0e0e0;
                white-space: nowrap;
              }

              .stat-value {
                font-size: 15px;
                color: #fff;
                font-weight: 700;
              }
            }
          }
        }
      }

      .slider-section {
        .split-slider {
          margin-bottom: 12px;
        }

        .slider-marks {
          display: flex;
          justify-content: space-between;
          font-size: 11px;
          color: #888;
          margin-top: 6px;

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

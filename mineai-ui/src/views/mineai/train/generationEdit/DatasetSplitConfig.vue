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
            :min="10"
            :max="90"
            :step="10"
            @change="handleSliderChange"
            class="split-slider"
            :tooltip="{ formatter: (value) => `${value}%` }"
          />
          <div class="slider-marks">
            <span class="mark-start">10%</span>
            <span class="mark-middle">50%</span>
            <span class="mark-end">90%</span>
          </div>
        </div>
      </div>
    </div>
  </a-card>
</template>

<script setup lang="ts">
  import { ref, defineProps, defineEmits, watch } from 'vue';
  import { Card as ACard, Slider as ASlider } from 'ant-design-vue';

  // 定义属性
  const props = defineProps({
    modelValue: {
      type: Array as () => number[],
      default: () => [70, 80],
    },
  });

  // 定义事件
  const emit = defineEmits(['update:modelValue', 'change']);

  // 数据集划分配置
  const splitSize = ref([...props.modelValue]);

  // 监听modelValue的变化，同步更新内部的splitSize
  watch(
    () => props.modelValue,
    (newVal) => {
      if (newVal && newVal.length === 2) {
        splitSize.value = [...newVal];
      }
    },
    { deep: true },
  );

  // 处理滑块变化，确保每个集合至少10%
  const handleSliderChange = (value: number[]) => {
    console.log('DatasetSplitConfig - 滑块值变化:', value);
    let [first, second] = value;

    // 确保第一个值至少是10，最多是80（这样测试集至少有10%）
    first = Math.max(10, Math.min(80, first));

    // 确保第二个值至少比第一个值大10（测试集至少10%），最多是90（验证集至少10%）
    second = Math.max(first + 10, Math.min(90, second));
    console.log('DatasetSplitConfig - 处理后的值:', [first, second]);

    splitSize.value = [first, second];
    emit('update:modelValue', splitSize.value);
    // 添加 change 事件触发
    emit('change', splitSize.value);
  };
</script>

<style scoped lang="less">
  .split-config-section {
    .unified-container {
      padding: 24px;
      background: linear-gradient(135deg, rgba(40, 40, 50, 0.4) 0%, rgba(30, 30, 40, 0.3) 100%);
      border-radius: 12px;
      border: 1px solid rgba(255, 255, 255, 0.1);
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);

      .config-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 32px;

        .section-title {
          display: flex;
          align-items: center;
          gap: 10px;
          color: #ffffff;
          font-size: 16px;
          font-weight: 600;

          .section-icon {
            font-size: 18px;
          }

          .section-text {
            text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
          }
        }

        .config-info {
          .split-stats {
            display: flex;
            gap: 16px;

            .stat-item {
              display: flex;
              flex-direction: column;
              align-items: center;
              padding: 12px 16px;
              border-radius: 12px;
              border: 2px solid;
              min-width: 100px;
              box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

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
                font-size: 12px;
                color: #b3b3b3;
                margin-bottom: 4px;
                font-weight: 500;
              }

              .stat-value {
                font-size: 18px;
                color: #ffffff;
                font-weight: 700;
                text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
              }
            }
          }
        }
      }

      .slider-section {
        .split-slider {
          margin-bottom: 16px;
        }

        .slider-marks {
          display: flex;
          justify-content: space-between;
          font-size: 12px;
          color: #888;
          margin-top: 8px;

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

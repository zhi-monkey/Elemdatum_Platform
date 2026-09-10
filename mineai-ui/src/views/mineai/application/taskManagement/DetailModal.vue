<template>
  <Modal
    v-model:visible="visible"
    title="详情信息"
    :footer="null"
    width="800px"
    @cancel="handleClose"
    :body-style="{ overflow: 'auto' }"
  >
    <!-- Tab 栏 -->
    <Tabs default-active-key="1" style="margin-bottom: 16px">
      <TabPane key="1" tab="基本信息">
        <!-- 带边框的盒子容器 -->
        <div class="bordered-box">
          <Descriptions :column="1" bordered>
            <DescriptionsItem label="应用任务ID">{{ formattedId }}</DescriptionsItem>
            <DescriptionsItem label="应用任务名称"
              >{{ record?.applicationTaskName || '无' }}
            </DescriptionsItem>
            <DescriptionsItem label="应用名称"
              >{{ record?.applicationName?.applicationName || '无' }}
            </DescriptionsItem>
            <DescriptionsItem label="适用场景"
              >{{ record?.allApplicableSceneName || '无' }}
            </DescriptionsItem>
            <DescriptionsItem label="设备固件"
              >{{ record?.device?.deviceAndFirmwareName || '无' }}
            </DescriptionsItem>
            <DescriptionsItem label="绑定算法"
              >{{ record?.model?.modelName || '无' }}
            </DescriptionsItem>
            <DescriptionsItem label="算法应用包名称"
              >{{ record?.appZipName || '无' }}
            </DescriptionsItem>
            <DescriptionsItem label="发布状态">{{ record?.isReleased }}</DescriptionsItem>
            <DescriptionsItem label="应用描述">{{ record?.description || '无' }}</DescriptionsItem>
            <DescriptionsItem label="算法应用包大小"
              >{{ record?.appZipSizeFormatted || '无' }}
            </DescriptionsItem>
          </Descriptions>
        </div>
      </TabPane>
      <TabPane key="2" tab="标签信息">
        <!-- 后续可以在这里添加第二个 Tab 的内容 -->
        <LabelInfo :model-application-id="record?.id" />
      </TabPane>
    </Tabs>
  </Modal>
</template>

<script lang="ts" setup>
  import { defineProps, computed, defineEmits } from 'vue';
  import { Modal, Descriptions, DescriptionsItem, Tabs, TabPane } from 'ant-design-vue';
  import LabelInfo from '/@/views/mineai/application/taskManagement/labelInfo.vue';

  const props = defineProps<{
    visible: boolean;
    record: {
      type: Object;
      default: () => {};
    };
  }>();

  // 定义 emit 函数
  const emit = defineEmits(['update:visible']);

  // 格式化 ID
  const formattedId = computed(() => {
    return props.record?.id ? `A${props.record.id.toString().padStart(8, '0')}` : '无';
  });

  // 关闭弹窗
  const handleClose = () => {
    emit('update:visible', false); // 使用 emit 函数触发事件
  };
</script>

<style scoped lang="less">
  .bordered-box {
    border-radius: 4px;
    padding: 16px;
    margin: 0 16px; /* 外边距，与 Modal 的边界保持距离 */
  }

  .ant-descriptions-item {
    line-height: 18px; /* 调整行高 */
  }

  .ant-tabs {
    margin-top: 16px; /* Tab 栏与 Modal 标题的距离 */
  }

  :deep(.ant-tabs-nav-scroll) {
    padding-left: 20px;
  }

  :deep(.ant-descriptions-item-content) {
    padding: 8px 16px !important;
    text-align: center;
  }

  :deep(.ant-descriptions-item-label) {
    padding: 8px 16px !important;
    text-align: center;
  }
</style>

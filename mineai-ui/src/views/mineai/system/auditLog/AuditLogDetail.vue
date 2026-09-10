<!-- src/views/auditLog/AuditLogDetail.vue -->
<template>
  <Modal
    v-model:visible="visible"
    title="审计日志详情"
    width="700px"
    :footer="null"
    @cancel="handleCancel"
    :bodyStyle="{ padding: '24px' }"
  >
    <Descriptions bordered :column="2" size="middle" class="audit-log-descriptions">
      <Descriptions.Item label="日志ID" :span="1">{{ data.id || '-' }}</Descriptions.Item>
      <Descriptions.Item label="状态" :span="1">
        <Tag :color="getStatusColor(data.requestStatus)">
          {{ getStatusText(data.requestStatus) }}
        </Tag>
      </Descriptions.Item>
      <Descriptions.Item label="操作类型" :span="1">{{
        getOperationTypeText(data.operationType)
      }}</Descriptions.Item>
      <Descriptions.Item label="用户名" :span="1">{{ data.uname || '-' }}</Descriptions.Item>
      <Descriptions.Item label="IP地址" :span="1">{{ data.ip || '-' }}</Descriptions.Item>
      <Descriptions.Item label="操作时间" :span="1">{{
        formatDate(data.createDate)
      }}</Descriptions.Item>
      <Descriptions.Item label="执行时间" :span="1"
        >{{ data.executionTime || '0' }}ms</Descriptions.Item
      >
      <Descriptions.Item label="操作描述" :span="2">{{
        getAuditDescriptionText(data.description)
      }}</Descriptions.Item>
      <Descriptions.Item label="方法名" :span="2" class="break-all">{{
        data.method || '-'
      }}</Descriptions.Item>
      <Descriptions.Item label="请求URI" :span="2">{{ data.requestUri || '-' }}</Descriptions.Item>
      <Descriptions.Item label="参数" :span="2" class="break-all">{{
        data.params || '无'
      }}</Descriptions.Item>
      <Descriptions.Item label="异常信息" :span="2" v-if="data.exceptionDesc">{{
        data.exceptionDesc
      }}</Descriptions.Item>
    </Descriptions>
  </Modal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { Modal, Descriptions, Tag } from 'ant-design-vue';
  import { formatToDateTime } from '/@/utils/dateUtil';
  import { getAuditDescriptionText } from './auditLog.data';

  const props = defineProps({
    visible: {
      type: Boolean,
      required: true,
    },
    data: {
      type: Object,
      default: () => ({}),
    },
  });

  const emit = defineEmits(['update:visible']);

  const handleCancel = () => {
    emit('update:visible', false);
  };

  const formatDate = (dateString: string) => {
    try {
      return formatToDateTime(dateString);
    } catch (e) {
      return dateString;
    }
  };

  const getStatusColor = (status: number) => {
    return status === 0 ? 'green' : 'red';
  };

  const getStatusText = (status: number) => {
    return status === 0 ? '正常' : '异常';
  };

  const getOperationTypeText = (type: number) => {
    const types = {
      0: '新增',
      1: '删除',
      2: '修改',
      4: '下载',
    };
    return types[type] || `未知类型(${type})`;
  };
</script>

<style scoped>
  .audit-log-descriptions :deep(.ant-descriptions-item-label) {
    width: 100px;
    white-space: nowrap;
    font-weight: 500;
  }

  .audit-log-descriptions :deep(.ant-descriptions-item-content) {
    width: calc(100% - 100px);
  }

  .break-all {
    word-break: break-all;
  }

  .audit-log-descriptions :deep(pre) {
    max-height: 200px;
    overflow-y: auto;
    padding: 8px;
    background: #f5f5f5;
    border-radius: 4px;
  }
</style>

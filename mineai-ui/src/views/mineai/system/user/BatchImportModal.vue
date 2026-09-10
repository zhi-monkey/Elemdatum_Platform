<template>
  <BasicModal
    v-bind="$attrs"
    :width="1200"
    :title="`批量导入用户 (${processedCount}/${dataSource.length})`"
    @register="registerModal"
    :closeable="!loading"
    :keyboard="!loading"
    :maskClosable="!loading"
    wrapClassName="batch-import-modal"
  >
    <div class="batch-import-container">
      <div class="import-info">
        <a-alert :message="alertMessage" :type="alertType" show-icon class="custom-alert" />
        <div class="action-buttons" v-if="!isCompleted">
          <a-button @click="pauseProcessing" v-if="loading && !isPaused" size="large">
            暂停
          </a-button>
          <a-button @click="resumeProcessing" v-if="isPaused" type="primary" size="large">
            继续
          </a-button>
        </div>
      </div>
      <a-table
        ref="tableRef"
        :dataSource="dataSource"
        :columns="columns"
        :pagination="false"
        size="small"
        :scroll="{ x: 1000, y: 400 }"
        rowKey="id"
        class="custom-table"
        :rowClassName="getRowClassName"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)" class="status-tag">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'error'">
            <span class="error-text">{{ record.error }}</span>
          </template>
        </template>
      </a-table>
    </div>

    <template #footer>
      <div class="custom-footer">
        <a-button @click="handleCancel" :loading="loading" :disabled="loading" size="large">
          取消
        </a-button>
        <a-button
          type="primary"
          @click="handleSubmit"
          :loading="loading"
          :disabled="loading"
          size="large"
        >
          确定
        </a-button>
      </div>
    </template>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { ref, computed, h, nextTick, watch } from 'vue';
  import {
    Alert as AAlert,
    Table as ATable,
    Tag as ATag,
    Button as AButton,
    Tag,
    message,
  } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

  const emit = defineEmits(['success', 'register', 'cancel']);

  const loading = ref(false);
  const dataSource = ref<any[]>([]);
  const currentBatch = ref(0);
  const totalBatches = ref(0);
  const isPaused = ref(false);
  const tableRef = ref();
  const BATCH_SIZE = 10;

  const columns = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      width: 120,
    },
    {
      title: '昵称',
      dataIndex: 'nickName',
      key: 'nickName',
      width: 120,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      width: 170,
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
      width: 120,
    },
    {
      title: '性别',
      dataIndex: 'sex',
      key: 'sex',
      width: 80,
    },
    {
      title: '密码',
      dataIndex: 'password',
      key: 'password',
      width: 100,
    },
    {
      title: '启用状态',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 100,
      customRender: ({ record }) => {
        const enabled = record.enabled;
        let color = enabled ? 'green' : 'red';
        return h(Tag, { color: color }, () => (enabled ? '启用' : '禁用'));
      },
    },
    {
      title: '处理状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      customRender: ({ record }) => {
        const status = record.status;
        let color = getStatusColor(status);
        return h(Tag, { color: color }, () => getStatusText(status));
      },
    },
    {
      title: '错误信息',
      dataIndex: 'error',
      key: 'error',
      width: 200,
      customRender: ({ record }) => {
        return h('span', { class: 'error-text' }, record.error || '');
      },
    },
  ];

  const [registerModal, { closeModal, setModalProps }] = useModalInner((data) => {
    // 重置状态
    loading.value = false;
    currentBatch.value = 0;
    isPaused.value = false;

    if (data.importData) {
      dataSource.value = data.importData.map((item, index) => ({
        id: index,
        ...item,
        status: 'waiting',
        error: '',
      }));
      totalBatches.value = Math.ceil(dataSource.value.length / BATCH_SIZE);
    }
  });

  watch(loading, (newVal) => {
    setModalProps({
      closable: !newVal,
      maskClosable: !newVal,
      keyboard: !newVal,
    });
  });

  const processedCount = computed(() => {
    return dataSource.value.filter((item) => item.status === 'success' || item.status === 'error')
      .length;
  });

  const successCount = computed(() => {
    return dataSource.value.filter((item) => item.status === 'success').length;
  });

  const failCount = computed(() => {
    return dataSource.value.filter((item) => item.status === 'error').length;
  });

  const isCompleted = computed(() => {
    return processedCount.value === dataSource.value.length;
  });

  const alertMessage = computed(() => {
    if (isCompleted.value) {
      return `处理完成！成功 ${successCount.value} 条，失败 ${failCount.value} 条`;
    }
    return `共 ${dataSource.value.length} 条数据，已处理 ${processedCount.value} 条，成功 ${successCount.value} 条，失败 ${failCount.value} 条`;
  });

  const alertType = computed(() => {
    if (isCompleted.value) {
      return failCount.value === 0 ? 'success' : 'warning';
    }
    return 'info';
  });

  const getStatusColor = (status) => {
    const colors = {
      waiting: 'blue',
      pending: 'orange',
      success: 'green',
      error: 'red',
    };
    return colors[status] || 'default';
  };

  const getStatusText = (status) => {
    const texts = {
      waiting: '等待中',
      pending: '处理中',
      success: '成功',
      error: '失败',
    };
    return texts[status] || '未知';
  };

  const getRowClassName = (record) => {
    if (record.status === 'pending') {
      return 'processing-row';
    }
    return '';
  };

  // 自动滚动到正在处理的行
  const scrollToProcessing = async () => {
    await nextTick();
    const processingIndex = dataSource.value.findIndex((item) => item.status === 'pending');
    if (processingIndex !== -1) {
      const tableBody = document.querySelector('.custom-table .ant-table-body');
      if (tableBody) {
        const rows = tableBody.querySelectorAll('.ant-table-row');
        const targetRow = rows[processingIndex + 4];

        if (targetRow) {
          targetRow.scrollIntoView({
            behavior: 'smooth',
            block: 'center',
            inline: 'nearest',
          });
        }
      }
    }
  };

  const validateRow = (row) => {
    const errors = [];

    if (!row.username?.trim()) {
      errors.push('用户名不能为空');
    }

    if (!row.email?.trim()) {
      errors.push('邮箱不能为空');
    } else {
      const emailRegex =
        /^([\w-\.]+)@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.)|(([\w-]+\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\]?)$/;
      if (!emailRegex.test(row.email)) {
        errors.push('邮箱格式有误');
      }
    }

    if (!row.phone?.trim()) {
      errors.push('手机号不能为空');
    } else {
      const phoneRegex =
        /^((13[0-9])|(14[5,7,9])|(15([0-3]|[5-9]))|(166)|(17[0,1,3,5,6,7,8])|(18[0-9])|(19[8|9]))\d{8}$/;
      if (!phoneRegex.test(row.phone)) {
        errors.push('手机号格式有误');
      }
    }

    if (
      !row.password ||
      (typeof row.password === 'string' && !row.password.trim()) ||
      (typeof row.password !== 'string' && String(row.password).trim() === '')
    ) {
      errors.push('密码不能为空');
    }

    return errors;
  };

  const startProcessing = async () => {
    loading.value = true;
    isPaused.value = false;
    setModalProps({
      closable: false,
      maskClosable: false,
      keyboard: false,
    });
    await processAllBatches();
  };

  const pauseProcessing = () => {
    isPaused.value = true;
  };

  const resumeProcessing = () => {
    isPaused.value = false;
    processAllBatches();
  };

  const processBatch = async (batch) => {
    const promises = batch.map(async (item) => {
      item.status = 'pending';

      await scrollToProcessing();

      const validationErrors = validateRow(item);
      if (validationErrors.length > 0) {
        item.status = 'error';
        item.error = validationErrors.join('; ');
        return;
      }

      try {
        const params = {
          ...item,
          status: 2,
          role: { id: -1 },
          department: { id: -2 },
        };
        await maHttp.post(
          {
            url: 'auth/register',
            params: params,
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
        );

        item.status = 'success';
        item.error = '';
      } catch (error: any) {
        item.status = 'error';
        item.error = error.message || '注册失败';
      }
    });

    await Promise.all(promises);
  };

  const processAllBatches = async () => {
    let batchIndex = currentBatch.value;

    while (batchIndex < totalBatches.value && !isPaused.value) {
      const startIndex = batchIndex * BATCH_SIZE;
      const endIndex = Math.min(startIndex + BATCH_SIZE, dataSource.value.length);
      const batch = dataSource.value.slice(startIndex, endIndex);

      const waitingBatch = batch.filter((item) => item.status === 'waiting');

      if (waitingBatch.length > 0) {
        await processBatch(waitingBatch);
      }

      batchIndex++;
      currentBatch.value = batchIndex;

      if (batchIndex < totalBatches.value && !isPaused.value) {
        await new Promise((resolve) => setTimeout(resolve, 500));
      }
    }

    if (!isPaused.value) {
      loading.value = false;
    }
    setModalProps({
      closable: true,
      maskClosable: true,
      keyboard: true,
    });
  };

  const handleCancel = () => {
    if (!loading.value) {
      closeModal();
    }
    emit('cancel');
  };

  const handleSubmit = async () => {
    if (!loading.value && !isCompleted.value) {
      // 如果还没有开始处理，则自动开始处理
      await startProcessing();

      // 处理完成后显示相应提示
      if (failCount.value === 0) {
        message.success(`用户导入成功！共导入 ${successCount.value} 条数据`);
        // 成功时关闭模态框
        closeModal();
      } else if (successCount.value > 0) {
        message.warning({
          content: `用户导入完成！成功 ${successCount.value} 条，失败 ${failCount.value} 条。请查看失败原因并修正后再尝试导入`,
          duration: 5,
        });
      } else {
        message.error({
          content: `用户导入失败！共 ${failCount.value} 条数据导入失败。请查看失败原因并修正后再尝试导入`,
          duration: 5,
        });
      }
      emit('success');
    }
  };
  // 添加重置方法，用于重新导入
  const resetState = () => {
    loading.value = false;
    isPaused.value = false;
    currentBatch.value = 0;
    // 重置数据源状态
    dataSource.value = dataSource.value.map((item) => ({
      ...item,
      status: 'waiting',
      error: '',
    }));
  };
  // 暴露重置方法给父组件
  defineExpose({
    resetState,
  });
</script>

<style scoped>
  .batch-import-container {
    padding: 20px;
    background: #1f1f1f;
    border-radius: 8px;
  }

  .import-info {
    margin-bottom: 20px;
  }

  .custom-alert {
    border-radius: 6px;
    font-size: 14px;
    padding: 12px 16px;
  }

  .action-buttons {
    margin-top: 16px;
    display: flex;
    gap: 12px;
  }

  .action-buttons button {
    border-radius: 6px;
    font-weight: 500;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
    transition: all 0.3s ease;
  }

  .action-buttons button:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.4);
  }

  .status-tag {
    font-weight: 500;
    padding: 2px 10px;
    border-radius: 4px;
    font-size: 12px;
  }

  .error-text {
    color: #ff4d4f;
    font-size: 12px;
  }

  /* 自定义 Footer 样式 */
  .custom-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    padding: 10px 16px;
  }

  .custom-footer button {
    border-radius: 6px;
    font-weight: 500;
    min-width: 80px;
  }

  /* 表格暗色主题样式 */
  :deep(.custom-table) {
    background: #2a2a2a;
    border-radius: 8px;
    overflow: hidden;
  }

  :deep(.custom-table .ant-table) {
    background: #2a2a2a;
    color: #e8e8e8;
  }

  :deep(.custom-table .ant-table-thead > tr > th) {
    background: #333333;
    color: #ffffff;
    border-bottom: 2px solid #404040;
    font-weight: 600;
    padding: 12px 8px;
  }

  :deep(.custom-table .ant-table-tbody > tr) {
    background: #2a2a2a;
    transition: all 0.3s ease;
  }

  :deep(.custom-table .ant-table-tbody > tr:hover) {
    background: #353535 !important;
  }

  :deep(.custom-table .ant-table-tbody > tr > td) {
    border-bottom: 1px solid #333333;
    color: #d9d9d9;
    padding: 10px 8px;
  }

  /* 正在处理的行高亮 */
  :deep(.custom-table .ant-table-tbody > tr.processing-row) {
    background: linear-gradient(
      90deg,
      rgba(250, 173, 20, 0.15) 0%,
      rgba(250, 173, 20, 0.05) 100%
    ) !important;
    animation: pulse 2s ease-in-out infinite;
  }

  :deep(.custom-table .ant-table-tbody > tr.processing-row > td) {
    border-left: 3px solid #faad14;
    font-weight: 500;
  }

  @keyframes pulse {
    0%,
    100% {
      background: linear-gradient(
        90deg,
        rgba(250, 173, 20, 0.15) 0%,
        rgba(250, 173, 20, 0.05) 100%
      );
    }
    50% {
      background: linear-gradient(90deg, rgba(250, 173, 20, 0.25) 0%, rgba(250, 173, 20, 0.1) 100%);
    }
  }

  /* 滚动条样式 */
  :deep(.custom-table .ant-table-body::-webkit-scrollbar) {
    width: 8px;
    height: 8px;
  }

  :deep(.custom-table .ant-table-body::-webkit-scrollbar-track) {
    background: #1f1f1f;
    border-radius: 4px;
  }

  :deep(.custom-table .ant-table-body::-webkit-scrollbar-thumb) {
    background: #555555;
    border-radius: 4px;
    transition: background 0.3s ease;
  }

  :deep(.custom-table .ant-table-body::-webkit-scrollbar-thumb:hover) {
    background: #666666;
  }

  /* Modal 样式优化 */
  :deep(.batch-import-modal .ant-modal-content) {
    background: #262626;
    border-radius: 12px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
  }

  :deep(.batch-import-modal .ant-modal-header) {
    background: #2a2a2a;
    border-bottom: 1px solid #404040;
    border-radius: 12px 12px 0 0;
  }

  :deep(.batch-import-modal .ant-modal-title) {
    color: #ffffff;
    font-weight: 600;
    font-size: 16px;
  }

  :deep(.batch-import-modal .ant-modal-body) {
    background: #262626;
    padding: 0;
  }

  :deep(.batch-import-modal .ant-modal-footer) {
    background: #2a2a2a;
    border-top: 1px solid #404040;
    border-radius: 0 0 12px 12px;
    padding: 0;
  }
</style>

<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="数据集事件列表" @ok="handleSubmit">
    <a-table
      :columns="columns"
      :data-source="eventsData"
      row-key="key"
      :pagination="paginationConfig"
      :rowKey="(record) => record.id"
    >
      <template #eventType="{ text }">
        <span>
          <a-tag :key="'event-' + text" :color="getEventTypeColor(text)">
            {{ eventTypeMap[text] || '未知' }}
          </a-tag>
        </span>
      </template>

      <template #operationType="{ text }">
        <span>
          <a-tag :key="'operation-' + text" :color="getOperationTypeColor(text)">
            {{ operationTypeMap[text] || '未知' }}
          </a-tag>
        </span>
      </template>

      <template #eventSubmittedTime="{ text }">
        <span>{{ formatDate(text) }}</span>
      </template>
    </a-table>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, reactive, computed, unref, onMounted } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { getDatasetEventPage } from '/@/views/mineai/data/dataset-details2/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Table as ATable, Tag as ATag } from 'ant-design-vue';
  import dayjs from 'dayjs'; // 导入 dayjs

  const { createMessage } = useMessage();
  const [registerModal, { closeModal, setModalProps }] = useModalInner(async (data) => {
    await handleFetchDatasetEvents(data.datasetId);
    setModalProps({ minHeight: 250, width: '800px' });
  });

  const eventsData = ref([]);
  const selectedRowKeys = ref([]);

  const paginationConfig = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    onChange: (page, pageSize) => {
      paginationConfig.current = page;
      paginationConfig.pageSize = pageSize;
      handleFetchDatasetEvents(currentDatasetId.value); // 重新加载数据
    },
  });

  // 事件类型和操作类型映射
  const eventTypeMap = {
    0: '信息',
    1: '错误',
    2: '警告',
  };

  const operationTypeMap = {
    0: '数据增强',
    1: '视频帧提取',
    2: '自动标注',
    3: '数据导出',
    4: '数据导入',
    5: '数据集发布',
    6: '数据集保存版本',
    7: '数据上传',
  };

  // 获取事件类型的颜色
  function getEventTypeColor(eventType) {
    const colorMap = {
      0: 'green', // 信息
      1: 'red', // 错误
      2: 'orange', // 警告
    };
    return colorMap[eventType] || 'default';
  }

  // 获取操作类型的颜色
  function getOperationTypeColor(operationType) {
    switch (operationType) {
      case 0:
        return 'blue';
      case 1:
        return 'purple';
      case 2:
        return 'magenta';
      case 3:
        return 'orange';
      case 4:
        return 'cyan';
      default:
        return 'default';
    }
  }

  // 时间格式化函数
  function formatDate(date: string): string {
    return dayjs(date).format('YYYY-MM-DD HH:mm:ss'); // 格式化时间为：年-月-日 时:分:秒
  }

  const columns = [
    {
      title: '事件提交时间',
      dataIndex: 'eventSubmittedTime',
      key: 'eventSubmittedTime',
      slots: { customRender: 'eventSubmittedTime' },
    },
    {
      title: '事件详情',
      dataIndex: 'eventDetailInfo',
      key: 'eventDetailInfo',
    },
    {
      title: '操作类型',
      dataIndex: 'operationType',
      key: 'operationType',
      slots: { customRender: 'operationType' },
    },
    {
      title: '事件类型',
      dataIndex: 'eventType',
      key: 'eventType',
      slots: { customRender: 'eventType' },
    },
  ];

  const rowSelection = computed(() => ({
    selectedRowKeys: unref(selectedRowKeys),
    onChange: (newSelectedRowKeys) => {
      selectedRowKeys.value = newSelectedRowKeys;
    },
  }));

  // 当前的数据集ID，用于查询事件数据
  const currentDatasetId = ref(null);

  // 处理分页查询数据
  async function handleFetchDatasetEvents(datasetId) {
    currentDatasetId.value = datasetId;
    const page = {
      current: paginationConfig.current,
      size: paginationConfig.pageSize,
    };

    try {
      const response = await getDatasetEventPage(page, datasetId);
      if (response) {
        eventsData.value = response.records;
        paginationConfig.total = response.total;
      }
    } catch (error) {
      createMessage.error('加载数据失败');
    }
  }

  async function handleSubmit() {
    closeModal();
  }
</script>

<style scoped>
  h3 {
    margin-top: 16px;
  }
</style>

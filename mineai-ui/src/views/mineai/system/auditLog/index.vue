<!-- src/views/auditLog/index.vue -->
<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <a-button type="primary" @click="handleExport">
          <template #icon>
            <Icon icon="ant-design:export-outlined" />
          </template>
          导出
        </a-button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:eye-outlined',
              tooltip: '查看详情',
              onClick: handleViewDetail.bind(null, record),
            },
            {
              icon: 'ant-design:rollback-outlined',
              tooltip: '查看/恢复已删除的数据集',
              ifShow: isDatasetDeleteAudit(record.description),
              onClick: handleRollback.bind(null, record),
            },
          ]"
          style="justify-content: center"
        />
      </template>
    </BasicTable>

    <AuditLogDetail v-model:visible="detailVisible" :data="currentDetail" />

    <a-modal
      v-model:visible="modalVisible"
      title="选择恢复数据集"
      @ok="handleConfirmRecovery"
      @cancel="handleCancel"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      wrap-class-name="custom-modal"
      :ok-button-props="{ disabled: !isAnyDatasetRecoverable }"
    >
      <a-spin :spinning="modalLoading" tip="正在加载数据集信息...">
        <div class="modal-content">
          <p v-if="!modalLoading && datasetsToRecover.length === 0" class="empty-tip">
            未找到与此删除操作相关的数据集信息。
          </p>
          <div v-else>
            <div class="select-all-container">
              <Checkbox
                v-model:checked="checkAll"
                :indeterminate="isIndeterminate"
                :disabled="recoverableIds.length === 0"
              >
                全选 ({{ selectedDatasetIds.length }} / {{ recoverableIds.length }})
              </Checkbox>
            </div>

            <CheckboxGroup v-model:value="selectedDatasetIds" class="dataset-list">
              <div v-for="dataset in datasetsToRecover" :key="dataset.id" class="dataset-item">
                <Checkbox
                  :value="dataset.id"
                  :disabled="!dataset.deleted || !isDatasetRollbackable(dataset)"
                  class="dataset-checkbox"
                >
                  {{ dataset.name }} (ID: {{ dataset.id }})
                </Checkbox>
                <Tag v-if="!dataset.deleted" color="success" class="recovered-tag">已恢复</Tag>
                <Tag v-else-if="!isDatasetRollbackable(dataset)" color="error" class="recovered-tag"
                  >已超时</Tag
                >
              </div>
            </CheckboxGroup>
          </div>
        </div>
      </a-spin>
    </a-modal>

    <!-- 导出弹窗 -->
    <BasicModal
      v-model:visible="exportVisible"
      title="导出审计日志"
      :confirm-loading="exportLoading"
      @ok="handleConfirmExport"
      @cancel="handleCancelExport"
      :width="700"
      :min-height="430"
    >
      <BasicForm @register="registerExportForm" />
    </BasicModal>
  </div>
</template>

<script lang="ts" setup>
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { BasicModal } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { isDatasetDeleteAudit,columns, searchFormSchema, transformSearchParams,exportFormSchema, } from './auditLog.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { Checkbox, CheckboxGroup, Modal as AModal, Spin as ASpin, Tag } from 'ant-design-vue';
  import { computed, ref } from 'vue';
  import AuditLogDetail from './AuditLogDetail.vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { batchGet, batchRecoverDataset } from '/@/views/mineai/data/dataset-details2/api';
  import { exportAuditLogToExcel } from './exportUtil';
  import { Icon } from '/@/components/Icon';

  // 详情相关状态
  const detailVisible = ref(false);
  const currentDetail = ref<Recordable>({});
  const { createMessage } = useMessage();

  const modalVisible = ref(false);
  const modalLoading = ref(false);
  const confirmLoading = ref(false);
  const datasetsToRecover = ref<any[]>([]);
  const selectedDatasetIds = ref<number[]>([]);

  // 导出相关状态
  const exportVisible = ref(false);
  const exportLoading = ref(false);

  // 表格配置
  const [registerTable] = useTable({
    api: async (params) => {
      params.page = params.page - 1;
      // 转换搜索参数
      const searchParams = transformSearchParams(params);
      if (params.timeRange) {
        params.startTime = params.timeRange[0];
        params.endTime = params.timeRange[1];
        delete params.timeRange;
      }

      const response = await maHttp.get(
        {
          url: 'auditLog/query',
          params: {
            current: params.page,
            size: params.pageSize,
            ...searchParams,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      return {
        items: response.content,
        total: response.totalElements || response.content.length,
        pageCount: response.totalPages,
      };
    },

    columns: [
      ...columns,
      {
        title: '操作',
        dataIndex: 'action',
        slots: { customRender: 'action' },
        width: 80,
        fixed: 'right',
      },
    ],

    formConfig: {
      labelWidth: 120,
      schemas: searchFormSchema,
      autoSubmitOnEnter: true,
    },

    useSearchForm: true,
    showTableSetting: false,
    bordered: true,
    showIndexColumn: false,
    pagination: {
      pageSize: 10,
      showQuickJumper: true,
      showSizeChanger: true,
      showTotal: (total) => `共 ${total} 条`,
      pageSizeOptions: ['10', '20', '30', '50'],
    },
  });

  // 导出表单配置
  const [registerExportForm, { validate: validateExportForm, resetFields: resetExportForm }] =
    useForm({
      labelWidth: 100,
      schemas: exportFormSchema,
      showActionButtonGroup: false,
    });

  // 查看详情
  const handleViewDetail = (record: Recordable) => {
    currentDetail.value = record;
    detailVisible.value = true;
  };

  // 打开导出弹窗
  const handleExport = () => {
    exportVisible.value = true;
  };

  // 取消导出
  const handleCancelExport = () => {
    exportVisible.value = false;
    resetExportForm();
  };

  // 确认导出
  const handleConfirmExport = async () => {
    try {
      const values = await validateExportForm();
      exportLoading.value = true;

      // 转换参数
      const params: any = {};

      if (values.operationType && values.operationType.length > 0) {
        params.operationTypes = values.operationType.join(',');
      }

      if (values.timeRange) {
        // 格式化时间为字符串
        params.startTime = values.timeRange[0].format('YYYY-MM-DD HH:mm:ss');
        params.endTime = values.timeRange[1].format('YYYY-MM-DD HH:mm:ss');
      }

      if (values.uname) {
        params.uname = values.uname;
      }

      if (values.ip) {
        params.ip = values.ip;
      }

      if (values.description) {
        params.description = values.description;
      }

      if (values.requestStatus !== undefined && values.requestStatus !== null) {
        params.requestStatus = values.requestStatus;
      }

      // 调用后端导出接口
      const response = await maHttp.get(
        {
          url: 'auditLog/export',
          params,
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      // 处理响应数据
      const data = Array.isArray(response) ? response : response.content || response.data || [];

      // 导出 Excel
      if (data && data.length > 0) {
        exportAuditLogToExcel(data, '审计日志');
        createMessage.success(`成功导出 ${data.length} 条数据`);
      } else {
        createMessage.warning('没有符合条件的数据');
      }

      exportVisible.value = false;
      resetExportForm();
    } catch (error) {
      console.error('导出失败:', error);
      createMessage.error('导出失败，请重试');
    } finally {
      exportLoading.value = false;
    }
  };

  const recoverableIds = computed(() =>
    datasetsToRecover.value.filter((d) => d.deleted && isDatasetRollbackable(d)).map((d) => d.id),
  );

  // 2. 计算属性：判断是否处于“半选”状态
  const isIndeterminate = computed(
    () =>
      selectedDatasetIds.value.length > 0 &&
      selectedDatasetIds.value.length < recoverableIds.value.length,
  );

  // 3. 计算属性：判断是否已“全选”
  const checkAll = computed({
    get() {
      // 如果没有可选项，或已选数量等于可选项数量，则为全选状态
      return (
        recoverableIds.value.length > 0 &&
        selectedDatasetIds.value.length === recoverableIds.value.length
      );
    },
    set(value: boolean) {
      // 当“全选”复选框状态改变时触发
      selectedDatasetIds.value = value ? recoverableIds.value : [];
    },
  });

  const isAnyDatasetRecoverable = computed(() => recoverableIds.value.length > 0);

  const isDatasetRollbackable = (dataset: any): boolean => {
    // 假设数据集对象中有 updateTime 字段
    if (!dataset?.updateTime) {
      return false;
    }
    const updateTime = new Date(dataset.updateTime);
    const now = new Date();
    const diffInMilliseconds = now.getTime() - updateTime.getTime();
    const twentyFourHoursInMilliseconds = 24 * 60 * 60 * 1000;
    return diffInMilliseconds < twentyFourHoursInMilliseconds;
  };

  const parseIdsFromParams = (paramsStr: string): number[] | null => {
    if (!paramsStr) return null;

    const match = paramsStr.match(/ids=\[([^\]]*)]/);
    if (match && match[1]) {
      const idsString = match[1];
      const ids = idsString
        .split(/[,，]/)
        .map((id) => parseInt(id.trim(), 10))
        .filter((id) => !isNaN(id));
      return ids.length > 0 ? ids : null;
    }
    return null;
  };
  const handleRollback = async (record: Recordable) => {
    const ids = parseIdsFromParams(record.params);

    if (!ids) {
      createMessage.error('无法从日志中解析出有效的数据集ID！');
      return;
    }

    modalLoading.value = true;
    modalVisible.value = true;
    datasetsToRecover.value = [];
    selectedDatasetIds.value = [];

    try {
      const fetchedDatasetsMap: Record<number, any> = await batchGet(ids);

      datasetsToRecover.value = Object.values(fetchedDatasetsMap);
    } catch (error) {
      console.error('获取数据集信息失败:', error);
      createMessage.error('获取数据集信息失败，请重试！');
      modalVisible.value = false;
    } finally {
      modalLoading.value = false;
    }
  };

  // Function for modal's confirm button
  const handleConfirmRecovery = async () => {
    if (selectedDatasetIds.value.length === 0) {
      createMessage.warning('请至少选择一个要恢复的数据集。');
      return;
    }

    confirmLoading.value = true;
    try {
      await batchRecoverDataset(selectedDatasetIds.value);
      createMessage.success('选中的数据集已成功恢复！');
      modalVisible.value = false;
      // Optional: you might want to refresh the audit log table here
      // reload();
    } catch (error) {
      console.error('恢复数据集失败:', error);
      createMessage.error('恢复数据集失败！');
    } finally {
      confirmLoading.value = false;
    }
  };

  // Function to handle modal cancellation
  const handleCancel = () => {
    modalVisible.value = false;
  };
</script>
<style scoped>
  /* 添加模态框内容样式 */
  .modal-content {
    padding: 16px 20px;
  }

  .empty-tip {
    padding: 12px 0;
    color: rgba(255, 255, 255, 0.85);
    text-align: center;
  }

  .dataset-list {
    display: block;
    width: 100%;
    max-height: 50vh;
    overflow-y: auto;
    padding-right: 4px;
  }

  .dataset-item {
    display: flex;
    align-items: center;
    padding: 10px 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  }

  .dataset-checkbox {
    flex: 1;
    color: rgba(255, 255, 255, 0.85);
  }

  .recovered-tag {
    margin-left: 12px;
    flex-shrink: 0;
  }
</style>

<style>
  /* 全局覆盖模态框样式 */
  .custom-modal .ant-modal-content {
    background-color: #1a1a1a;
    border: 1px solid #434343;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  }

  .custom-modal .ant-modal-header {
    background-color: #1a1a1a;
    border-bottom: 1px solid #434343;
    border-radius: 8px 8px 0 0;
    padding: 16px 24px;
  }

  .custom-modal .ant-modal-title {
    color: rgba(168, 168, 168, 0.85);
    font-weight: 500;
  }

  .custom-modal .ant-modal-body {
    padding: 0;
  }

  .custom-modal .ant-modal-footer {
    background-color: #1a1a1a;
    border-top: 1px solid #434343;
    padding: 12px 24px;
    border-radius: 0 0 8px 8px;
  }

  .custom-modal .ant-checkbox-wrapper {
    color: rgba(208, 208, 208, 0.85);
  }

  .custom-modal .ant-checkbox-inner {
    background-color: transparent;
    border-color: #595959;
  }

  .custom-modal .ant-checkbox-checked .ant-checkbox-inner {
    background-color: #177ddc;
    border-color: #177ddc;
  }

  /* 滚动条样式 */
  .dataset-list::-webkit-scrollbar {
    width: 6px;
  }

  .dataset-list::-webkit-scrollbar-thumb {
    background-color: #000000;
    border-radius: 3px;
  }

  .dataset-list::-webkit-scrollbar-track {
    background-color: #1a1a1a;
  }
</style>

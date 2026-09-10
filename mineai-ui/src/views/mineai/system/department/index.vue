<template>
  <PageWrapper style="margin: 0 16px 0 16px" :contentStyle="{ margin: '0' }">
    <template #title>资源管理</template>

    <template #footer>
      <Tabs :active-key="activeKey" @change="activeKey = $event">
        <tab-pane key="1" tab="部门管理">
          <div>
            <BasicTable @register="registerTable" @selection-change="selectionChange">
              <template #toolbar>
                <Button type="primary" @click="handleCreate">新增部门</Button>
                <Button type="primary" @click="handleGroupDelete" :disabled="canDelete">
                  批量删除</Button
                >
              </template>
              <template #action="{ record }">
                <TableAction
                  :actions="[
                    {
                      icon: 'ant-design:align-right-outlined',
                      tooltip: '查看个人资源使用量',
                      onClick: handleProcess.bind(null, record),
                    },
                    {
                      icon: 'clarity:note-edit-line',
                      tooltip: '编辑部门',
                      onClick: (e) => {
                        e.stopPropagation(); // 阻止事件冒泡
                        handleEdit(record);
                      },
                    },
                    {
                      icon: 'ant-design:delete-outlined',
                      color: 'error',
                      tooltip: '删除部门',
                      popConfirm: {
                        title: '是否确认删除',
                        confirm: handleDelete.bind(null, record),
                      },
                    },
                  ]"
                />
              </template>
            </BasicTable>
            <DepartmentModal @register="registerModal" @success="handleSuccess" />
          </div>
        </tab-pane>
        <tab-pane key="2" tab="算力资源详情">
          <ComputingResourceManagement />
        </tab-pane>
        <tab-pane key="3" tab="外部模型转换设备管理">
          <ExternalModelConverter />
        </tab-pane>
        <tab-pane v-if="showRtspSourceManagement" key="4" tab="RTSP流管理">
          <RtspSourceManagement />
        </tab-pane>
      </Tabs>
    </template>
  </PageWrapper>
</template>

<script setup lang="ts">
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { PageWrapper } from '/@/components/Page';
  import { useModal } from '/@/components/Modal';
  import { Button, Modal, TabPane, Tabs } from 'ant-design-vue';
  import { columns, searchFormSchema } from './data';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { deleteDepartment } from '../api/department';
  import { ElMessage as Message } from 'element-plus';
  import DepartmentModal from './DepartmentModal.vue';
  import { useGo } from '/@/hooks/web/usePage';
  import { createVNode, Ref, ref } from 'vue';
  import ComputingResourceManagement from './computingResourceManagement/computingResourceManagement.vue';
  import ExternalModelConverter from '/@/views/mineai/train/externalModelConverter/index.vue';
  import RtspSourceManagement from './rtspSourceManagement.vue';
  import { ExclamationCircleOutlined } from '@ant-design/icons-vue';
  import { useMessage } from '/@/hooks/web/useMessage';

  const [registerModal, { openModal }] = useModal();
  const activeKey = ref('1');
  const showRtspSourceManagement = false;
  const { createMessage } = useMessage();
  const [registerTable, { reload, clearSelectedRowKeys }] = useTable({
    title: '部门列表',
    api: async (params) => {
      const v = await maHttp.get(
        {
          url: 'departments',
          params,
          headers: {},
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
      );
      // 完成前端 GET 方法后，将 Spring Page 的字段转换为 VBen 所需字段
      v.items = v.records;
      return v;
    },
    beforeFetch: (v) => {
      // 发出分页查询请求前，将 1-started 页码（VBen）转换为 0-started 页码（Spring Page）
      v = Object.assign(v, {
        current: v.page,
        size: v.pageSize,
      });
      // 主动使用排序功能，则进行参数转换
      if (Object.hasOwn(v, 'field')) {
        Object.assign(v, {
          order: v.order === 'ascend' ? 'asc' : 'desc',
          sort: v.field,
        });
      }
      // 否则进行默认排序，根据 id 降序排序
      else {
        Object.assign(v, {
          order: 'desc',
          sort: 'id',
        });
      }
      return v;
    },
    columns: columns,
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
      schemas: searchFormSchema,
    },
    rowSelection: {
      type: 'checkbox',
    },
    clearSelectOnPageChange: true,
    useSearchForm: true,
    showTableSetting: false,
    bordered: true,
    showIndexColumn: false,
    actionColumn: {
      width: 150,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
    handleSearchInfoFn(info) {
      return info;
    },
  });

  const canDelete: Ref<boolean> = ref(true);
  const selectedKeys: Ref<Array<any>> = ref([]);
  const selectedRows: Ref<Array<any>> = ref([]);
  const selectionChange = ({ keys, rows }) => {
    selectedKeys.value = keys;
    selectedRows.value = rows;
    canDelete.value = keys.length <= 0;
  };

  const handleGroupDelete = () => {
    if (selectedKeys.value.length <= 0) {
      return;
    }
    const modal = Modal.confirm({
      title: () => '确认删除选中的' + selectedKeys.value.length + '条数据?',
      maskClosable: true,
      icon: () => createVNode(ExclamationCircleOutlined),
      okText: () => '确定',
      okType: 'danger',
      cancelText: () => '取消',
      onOk() {
        // 手动关闭弹窗
        modal.destroy();

        // 异步删除操作
        const ids = selectedRows.value.map((row) => row.id);
        maHttp
          .delete(
            {
              url: 'departments',
              data: { ids },
              headers: {},
            },
            { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
          )
          .then(() => {
            createMessage.success('删除成功！');
            // 清空所有选中的keys
            clearSelectedRowKeys();
            // 刷新页面
            reload();
          })
          .catch((e) => {
            console.error(e);
            createMessage.error('删除失败！');
            reload();
            clearSelectedRowKeys();
          });
      },
    });
  };
  function handleCreate() {
    openModal(true, {
      isUpdate: false,
    });
  }

  async function handleDelete(item) {
    try {
      await deleteDepartment(item.id);
      Message.success('部门删除成功！');
    } catch (e) {
      Message.error('部门删除失败！');
    } finally {
      await reload();
    }
  }

  function handleEdit(record) {
    openModal(true, {
      record,
      isUpdate: true,
    });
  }

  function handleSuccess() {
    reload();
  }

  const go = useGo();

  // 查看个人资源使用量
  function handleProcess(item) {
    go(`/maUser/departmentRole/${item.id}`);
  }
</script>

<style scoped lang="less"></style>

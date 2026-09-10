<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <Button type="primary" @click="handleCreate">新增任务</Button>
        <Button type="primary" @click="handleGroupManage">管理多人标注团队</Button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:align-right-outlined',
              tooltip: '查看标注进度',
              onClick: handleProcess.bind(null, record),
            },
            {
              icon: 'ant-design:team-outlined',
              tooltip: '重新分配任务',
              ifShow: record.status === 0 || record.status === 1,
              onClick: handleReassign.bind(null, record),
            },
            {
              icon: 'ant-design:close-outlined',
              tooltip: '终止任务',
              ifShow: record.status === 0 || record.status === 1,
              popConfirm: {
                title: '是否确认终止',
                confirm: handleTerminate.bind(null, record),
              },
            },
            {
              icon: 'ant-design:delete-outlined',
              color: 'error',
              tooltip: '删除任务',
              popConfirm: {
                title: '是否确认删除',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>
    </BasicTable>
    <GroupLabelingModal @register="registerModal" @success="handleSuccess" />
    <TaskReassignModal @register="registerReassignModal" @success="handleReassignSuccess" />
  </div>
</template>
<script setup lang="ts">
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { Button } from 'ant-design-vue';
  import GroupLabelingModal from './GroupLabelingModal.vue';
  import TaskReassignModal from './TaskReassignModal.vue';
  import { columns } from './data';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useModal } from '/@/components/Modal';
  import { useGo } from '/@/hooks/web/usePage';
  import { ElMessage as Message } from 'element-plus';
  import { deleteTask, terminatetask } from '/@/views/mineai/data/dataset-details2/api';

  const go = useGo();
  const [registerModal, { openModal }] = useModal();
  const [registerReassignModal, { openModal: openReassignModal }] = useModal();
  const [registerTable, { reload }] = useTable({
    api: async (params) => {
      const v = await maHttp.get(
        {
          url: 'datasets/team/task',
          params,
          headers: {},
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
      v.items = v.result;
      v.total = v.page.total;
      return v;
    },
    beforeFetch: (v) => {
      //发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
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
      // 否则进行默认排序，根据id降序排序
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
    },
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

  const handleCreate = () => {
    openModal(true, {
      isUpdate: false,
    });
  };

  const handleSuccess = () => {
    reload();
  };

  // 查看进度
  function handleProcess(item) {
    go(`/maData/groupLabelingStatus/${item.id}`);
  }

  // 重新分配任务
  function handleReassign(record) {
    openReassignModal(true, {
      record,
    });
  }

  const handleReassignSuccess = () => {
    reload();
  };

  // 终止任务
  async function handleTerminate(record) {
    try {
      console.log(record.id);
      await terminatetask(record.id);
      Message.success('任务终止成功！');
    } catch (e) {
      Message.error('任务终止失败！');
    } finally {
      await reload();
    }
  }

  async function handleDelete(item) {
    await deleteTask(item)
      .then(() => {
        Message.success('任务删除成功！');
        reload();
      })
      .catch(() => {
        Message.error('任务删除失败！');
        reload();
      });
  }

  const handleGroupManage = () => {
    go('/maData/groupDetails');
  };
</script>
<style scoped lang="less"></style>

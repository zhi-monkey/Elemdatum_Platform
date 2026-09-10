<template>
  <PageWrapper @back="goBack" style="margin: 0 16px 0 16px" :contentStyle="{ margin: '0' }">
    <template #title>管理多人标注团队</template>
    <div>
      <BasicTable @register="registerTable">
        <template #toolbar>
          <Button type="primary" @click="handleCreate">新增团队</Button>
        </template>
        <template #action="{ record }">
          <TableAction
            :actions="[
              {
                icon: 'clarity:note-edit-line',
                tooltip: '编辑团队',
                onClick: handleEdit.bind(null, record),
                disabled: record.canModify === false,
              },
              {
                icon: 'ant-design:delete-outlined',
                color: 'error',
                tooltip: '删除团队',
                popConfirm: {
                  title: '是否确认删除',
                  confirm: handleDelete.bind(null, record),
                },
                disabled: record.canModify === false,
              },
            ]"
          />
        </template>
      </BasicTable>
      <GroupModal @register="registerModal" @success="handleSuccess" />
    </div>
  </PageWrapper>
</template>
<script setup lang="ts">
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { PageWrapper } from '/@/components/Page';
  import { useModal } from '/@/components/Modal';
  import { useRouter } from 'vue-router';
  import { Button } from 'ant-design-vue';
  import { columns, searchFormSchema } from './data';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { deleteTeam } from '/@/views/mineai/data/dataset-details2/api';
  import { ElMessage as Message } from 'element-plus';
  import GroupModal from './GroupModal.vue';

  const router = useRouter();
  const [registerModal, { openModal }] = useModal();

  const [registerTable, { reload }] = useTable({
    title: '团队列表',
    api: async (params) => {
      const v = await maHttp.get(
        {
          url: 'datasets/team',
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
      schemas: searchFormSchema,
    },
    useSearchForm: true,
    showTableSetting: true,
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

  function handleCreate() {
    openModal(true, {
      isUpdate: false,
    });
  }

  async function handleDelete(item) {
    try {
      await deleteTeam(item);
      Message.success('团队删除成功！');
    } catch (e) {
      if (e.message === '您没有权限删除该团队，只有管理员或团队创建者可以删除团队') return;
      Message.error('团队删除失败！');
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

  function goBack() {
    router.go(-1);
  }
</script>

<style scoped lang="less"></style>

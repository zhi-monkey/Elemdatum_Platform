<template>
  <PageWrapper @back="goBack" :contentStyle="{ margin: '0' }">
    <template #title>数据仓库</template>
    <BasicTable
      @register="registerTable"
      @selection-change="selectionChange"
      @change="onPageChange"
      rowKey="id"
    >
      <template #toolbar>
        <a-button type="primary" @click="handleCreate">新增数据仓库</a-button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="
            datasetPermission
              ? [
                  {
                    icon: 'ant-design:info-circle-outlined',
                    tooltip: '详情',
                    onClick: goRepoDetail.bind(null, record),
                    disabled: record.status === 1,
                  },
                  {
                    icon: 'ant-design:file-add-outlined',
                    tooltip: '导入',
                    onClick: handleUpload.bind(null, record, dataTypeCodeMap.IMAGE),
                  },

                  {
                    icon: 'clarity:note-edit-line',
                    tooltip: '编辑',
                    onClick: handleEdit.bind(null, record),
                    disabled: record.status === 1,
                  },
                  {
                    icon: 'ant-design:delete-outlined',
                    tooltip: '删除',
                    color: 'error',
                    disabled: record.status === 1,
                    popConfirm: {
                      title: '是否确认删除',
                      confirm: handleDelete.bind(null, record),
                    },
                  },
                ]
              : [
                  {
                    icon: 'ant-design:info-circle-outlined',
                    tooltip: '详情',
                    onClick: goRepoDetail.bind(null, record),
                    disabled: record.status === 1,
                  },
                  {
                    icon: 'clarity:note-edit-line',
                    tooltip: '编辑',
                    onClick: handleEdit.bind(null, record),
                    disabled: record.status === 1 || record.userId !== getUserId,
                  },
                  {
                    icon: 'ant-design:delete-outlined',
                    tooltip: '删除',
                    color: 'error',
                    disabled: record.status === 1 || record.userId !== getUserId,
                    popConfirm: {
                      title: '是否确认删除',
                      confirm: handleDelete.bind(null, record),
                    },
                  },
                ]
          "
        />
      </template>
    </BasicTable>
    <CreateDatasetRepoModal @register="registerCreateModal" @success="handleSuccess" />
    <EditDatasetRepoModal @register="registerEditModal" @success="handleSuccess" />

    <UploadDataFile
      :row="importRow"
      :visible="uploadDataFileVisible"
      :close-upload-data-file="closeUploadDataFile"
      :hide-upload-data-file="hideUploadDataFile"
    />
  </PageWrapper>
</template>

<script setup lang="ts">
  import BasicTable from '/@/components/Table/src/BasicTable.vue';
  import { computed, ComputedRef, ref, Ref } from 'vue';
  import { Button as AButton } from 'ant-design-vue';
  import { TableAction, useTable } from '/@/components/Table';
  import { columns, searchFormSchema } from '/@/views/mineai/data/data-repo/dataset.repo';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useGo } from '/@/hooks/web/usePage';
  import { useModal } from '/@/components/Modal';
  import CreateDatasetRepoModal from './CreateDatasetRepoModal.vue';
  import EditDatasetRepoModal from './EditDatasetRepoModal.vue';
  import { delDataRepo, getDataRepo } from '/@/views/mineai/data/dataset-details2/api';
  import { ElMessage as Message } from 'element-plus';
  import { useUserStore } from '/@/store/modules/user';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { PermissionEnum } from '/@/enums/PermissionEnum';
  import { dataTypeCodeMap } from '/@/views/mineai/data/dataset-details2/util';
  import UploadDataFile from '/@/views/mineai/data/data-repo/upload-datafile-inline.vue';
  import PageWrapper from '/@/components/Page/src/PageWrapper.vue';
  import { useRouter } from 'vue-router';

  // 获取当前用户id
  const userStore = useUserStore();
  const { createMessage } = useMessage();
  const getUserId = computed(() => {
    return userStore.getUserInfo.id;
  });
  const go = useGo();
  const importRow: Ref = ref(null);
  const uploadDataFileVisible: Ref<boolean> = ref(false);
  const { hasPermission } = usePermission();
  const resumeButtonVisible: Ref<boolean> = ref(false);
  const datasetPermission: ComputedRef<boolean> = computed(() => {
    return hasPermission([PermissionEnum.DATA_MODIFY_DATASET]);
  });
  const activeKey: Ref<string> = ref('1');
  const [registerCreateModal, { openModal: openCreateModal }] = useModal();
  const [registerEditModal, { openModal: openEditModal }] = useModal();
  const [registerDatasetUploadModel, { openModal: openDatasetUploadModel }] = useModal();
  const [registerTable, { reload, clearSelectedRowKeys }] = useTable({
    api: async (params) => {
      params.deleted = false;
      // todo 根据params更换接口
      const v = await getDataRepo(params);
      //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
      v.items = v.records;
      return v;
    },
    beforeFetch: (v) => {
      // 发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
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
    title: '数据仓库列表',
    columns: columns,
    // rowSelection: {
    //   type: 'checkbox',
    // },
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
      schemas: searchFormSchema,
      submitOnReset: true,
    },
    useSearchForm: true,
    showTableSetting: false,
    bordered: true,
    showIndexColumn: false,
    clickToRowSelect: false,
    actionColumn: {
      width: 200,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
    handleSearchInfoFn(info) {
      //请求前检查日期参数是否为时间戳格式，若不是则尝试转换。
      if (info.createTime && Array.isArray(info.createTime)) {
        const timestampArray = info.createTime.map((time) => {
          const timestamp = Date.parse(time);
          return isNaN(timestamp) ? time : timestamp.toString();
        });
        info.createTime = timestampArray;
      }
      return info;
    },
  });

  const router = useRouter();
  const goBack = () => {
    router.go(-1);
  };

  // 处理上传文件模态框
  function handleUploadModal(record: Recordable, dataType) {
    openDatasetUploadModel(true, { record, dataType });
  }

  // 分页变化
  function onPageChange() {
    clearSelectedRowKeys();
  }

  // add dataset
  function handleCreate() {
    openCreateModal(true, {
      isUpdate: false,
    });
  }

  // delete dataset
  async function handleDelete(record) {
    await delDataRepo(record.id)
      .then(() => {
        Message.success('数据仓库删除成功');
        reload();
      })
      .catch((err) => {
        Message.error(err.message || '删除失败');
        reload();
      });
  }

  // 处理本地上传图片和视频
  function handleUpload(record: Recordable, dataType) {
    importRow.value = record;
    importRow.value.dataType = dataType;
    uploadDataFileVisible.value = true;
  }

  // update dataset
  function handleEdit(record: Recordable) {
    openEditModal(true, {
      record,
      isUpdate: true,
    });
  }

  // Detail dataset
  function goRepoDetail(record: Recordable) {
    go(
      `/maData/fileRepoDetail/${record.id}/${record.name}/${
        getUserId.value === record.userId ? 1 : 0
      }`,
    );
  }

  // success fallback
  async function handleSuccess(isEdit) {
    // 清空所有选中的 keys
    clearSelectedRowKeys();

    if (isEdit) {
      createMessage.success('数据仓库修改成功');
    } else {
      createMessage.success('数据仓库新增成功');
    }

    // 刷新页面
    await reload();
  }

  // 处理多选框选中事件
  const canDelete: Ref<boolean> = ref(true);
  const selectedKeys: Ref<Array<any>> = ref([]);
  const selectedRows: Ref<Array<any>> = ref([]);

  const selectionChange = ({ keys, rows }) => {
    selectedKeys.value = keys;
    selectedRows.value = rows;
    canDelete.value = keys.length <= 0;
  };

  const closeUploadDataFile = (flag): void => {
    uploadDataFileVisible.value = false;
    // 关闭恢复按钮
    if (resumeButtonVisible.value) {
      resumeButtonVisible.value = false;
    }
    // 判断是否是取消还是完成
    if (flag) {
      reload();
    }
  };
  const hideUploadDataFile = (): void => {
    uploadDataFileVisible.value = !uploadDataFileVisible.value;
    resumeButtonVisible.value = !resumeButtonVisible.value;
  };
</script>

<style scoped lang="less">
  .vben-page-wrapper {
    padding: 0;
  }

  .vben-basic-table-form-container {
    padding: 0;
  }
</style>

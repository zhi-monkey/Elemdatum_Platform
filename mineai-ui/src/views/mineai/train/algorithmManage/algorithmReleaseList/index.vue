<template>
  <div>
    <BasicTable @register="registerTable">
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:eye-outlined',
              tooltip: '查看',
              onClick: handleView.bind(null, record),
            },
            {
              icon: 'ant-design:check-circle-outlined',
              tooltip: '通过',
              color: 'success',
              ifShow: () => {
                return record.releaseStatus === 1;
              },
              onClick: handlePublish.bind(null, record),
            },
            {
              icon: 'ant-design:close-circle-outlined',
              tooltip: '拒绝',
              color: 'error',
              ifShow: () => {
                return record.releaseStatus === 1;
              },
              onClick: handleReject.bind(null, record),
            },
            {
              icon: 'ant-design:file-exclamation-outlined',
              tooltip: '拒绝原因',
              color: 'warning',
              ifShow: () => {
                return record.releaseStatus === 3;
              },
              onClick: handleReason.bind(null, record),
            },
          ]"
        />
      </template>
    </BasicTable>
    <ReleaseModal @register="registerModal" />
    <ReasonModal @register="registerReasonModal" />
    <ModelTypeModal @register="registerModelTypeModal" />
  </div>
</template>
<script lang="ts" setup>
  import { computed, ComputedRef, onActivated } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { columns, searchFormSchema } from './algorithmRelease.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { RoleEnum } from '/@/enums/roleEnum';
  import ReleaseModal from './ReleaseModal/ReleaseModal.vue';
  import ReasonModal from './ReleaseModal/ReasonModal.vue';
  import ModelTypeModal from './ReleaseModal/ModelTypeModal.vue';
  import { useModal } from '/@/components/Modal';
  import { useGo } from '/@/hooks/web/usePage';

  const go = useGo();
  const { hasPermission } = usePermission();
  const userPermission: ComputedRef<boolean> = computed(() => {
    return hasPermission([RoleEnum.SystemUser]);
  });

  const [registerModal, { openModal: openReleaseModal }] = useModal();
  const [registerReasonModal, { openModal: openReasonModal }] = useModal();
  const [registerModelTypeModal, { openModal: openModelTypeModal }] = useModal();

  const [registerTable, { reload }] = useTable({
    title: '发布算法列表',
    api: async (params) => {
      return await maHttp
        .get(
          {
            url: 'modelExplore/dynamicFindUnderExamineModelPage',
            params,
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        )
        .then((v) => {
          //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
          v.items = v.content;
          v.total = v.totalElements;
          return v;
        });
    },
    beforeFetch: (v) => {
      //发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v.page -= 1;
      return v;
    },
    afterFetch: (itemList) => {
      //GET请求被解析为 VBen表格Table的行后，做一些操作
      //console.log('afterFetch', itemList);
      return itemList;
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
    handleSearchInfoFn(info) {
      return info;
    },
    actionColumn: {
      width: 40,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
      // ifShow: userPermission.value,
    },
  });

  function handleView(record: Recordable) {
    go(`/maTrainingCenter/trainJobDetails/${record.releaseJob.id}_1`);
  }

  function handlePublish(record: Recordable) {
    openModelTypeModal(true, {
      modelExploreId: Number(record.id),
      modelGenerationId: Number(record.modelGenerationDTO.id),
    });
  }

  function handleReject(record: Recordable) {
    openReleaseModal(true, { record });
  }

  function handleReason(record: Recordable) {
    openReasonModal(true, { record });
  }

  onActivated(() => {
    reload();
  });
</script>

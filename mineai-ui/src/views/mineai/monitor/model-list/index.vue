<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <a-button type="primary" @click="goManageModelType"> 算法类型管理</a-button>
        <a-button type="primary" @click="handleCreate"> 新增算法</a-button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'clarity:note-edit-line',
              tooltip: '编辑',
              disabled: record.subsystem !== 'CENTRAL_PLATFORM',
              onClick: handleEdit.bind(null, record),
            },
            {
              icon: 'ant-design:container-outlined',
              tooltip: '部署',
              ifShow: record.subsystem === 'CENTRAL_PLATFORM' && record.deployModelVersion != null,
              onClick: handDeploy.bind(null, record),
            },
            {
              icon: 'ant-design:close-square-outlined',
              tooltip: '取消部署',
              color: 'error',
              ifShow: record.hasDeployments === true,
              onClick: handCancelDeploy.bind(null, record),
            },
            {
              icon: 'ant-design:cloud-download-outlined',
              tooltip: '权重文件下载',
              onClick: handleDownloadWeightFileList.bind(null, record),
              ifShow: record.bestWeightPath != null,
            },
            {
              icon: 'ant-design:delete-outlined',
              tooltip: '删除',
              color: 'error',
              disabled: record.canDelete !== true || record.subsystem !== 'CENTRAL_PLATFORM',
              popConfirm: {
                title: '是否确认删除',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
          :dropDownActions="[
            {
              label: '训练镜像上传',
              onClick: UploadTrainImage.bind(null, record),
              ifShow: record.source === 1 && !record.trainModelVersion,
            },
            {
              label: '转换镜像上传',
              onClick: UploadConvertImage.bind(null, record),
              ifShow: record.source === 1 && !record.convertModelVersion,
            },
            {
              label: '推理镜像上传',
              onClick: UploadDeployImage.bind(null, record),
              ifShow: record.source === 1 && !record.deployModelVersion,
            },
            {
              label: '自动标注镜像上传',
              onClick: UploadAutoLabelImage.bind(null, record),
              ifShow: record.source === 1 && !record.autoLabelModelVersion,
            },
          ]"
        />
      </template>
    </BasicTable>
    <ModelModal @register="registerModal" @success="handleSuccess" />
    <DeployMentModel @register="registerDeployModal" @success="handleSuccess" />
    <ConfigModal @register="registerConfigModal" />
    <CanCelDeployModal @register="registerCancelDeployModal" @success="handleSuccess" />
    <UploadTrainModal @register="trainModal" @success="handleSuccess" />
    <UploadDeployModal @register="deployModal" @success="handleSuccess" />
    <UploadConvertModal @register="convertModal" @success="handleSuccess" />
    <UploadAutoLabelModal @register="autoLabelModal" @success="handleSuccess" />
    <ModelDownloadModal @register="registerModelDownloadModal" />
  </div>
</template>
<script lang="ts" setup>
  import { computed, ComputedRef, onActivated } from 'vue';
  import { Button as AButton } from 'ant-design-vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import ConfigModal from '/@/views/mineai/controller/scene-list/ConfigModal.vue';
  import { columns, searchFormSchema } from './model.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useModal } from '/@/components/Modal';
  import ModelModal from './ModelModal.vue';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { useGo } from '/@/hooks/web/usePage';
  import { RoleEnum } from '/@/enums/roleEnum';
  import DeployMentModel from '/@/views/mineai/monitor/model-list/DeployMentModel.vue';
  import CanCelDeployModal from '/@/views/mineai/monitor/model-list/CancelDeployModel.vue';
  import UploadTrainModal from '/@/views/mineai/monitor/model-list/uploadImage/UploadTrainModal.vue';
  import UploadDeployModal from '/@/views/mineai/monitor/model-list/uploadImage/UploadDeployModal.vue';
  import UploadConvertModal from '/@/views/mineai/monitor/model-list/uploadImage/UploadConvertModal.vue';
  import UploadAutoLabelModal from '/@/views/mineai/monitor/model-list/uploadImage/UploadAutoLabelModal.vue';
  import ModelDownloadModal from '/@/views/mineai/monitor/model-list/modelDownload/ModelDownloadModal.vue';

  const go = useGo();
  const { hasPermission } = usePermission();
  const [registerConfigModal, { openModal: openConfigModal }] = useModal();
  const userPermission: ComputedRef<boolean> = computed(() => {
    return hasPermission([RoleEnum.SystemUser]);
  });
  const { createMessage } = useMessage();
  const [registerModal, { openModal }] = useModal();
  const [trainModal, { openModal: openTrain }] = useModal();
  const [deployModal, { openModal: openDeploy }] = useModal();
  const [convertModal, { openModal: openConvert }] = useModal();
  const [autoLabelModal, { openModal: openAutoLabel }] = useModal();
  const [registerDeployModal, { openModal: openDeployModal }] = useModal();
  const [registerCancelDeployModal, { openModal: openCancelDeployModal }] = useModal();
  const [registerModelDownloadModal, { openModal: openModelDownloadModal }] = useModal();
  const [registerTable, { reload }] = useTable({
    title: '算法商城',
    api: (params) => {
      return maHttp
        .get(
          {
            url: 'model/dynamicFindModelPage',
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
    bordered: false,
    showIndexColumn: false,
    expandRowByClick: true,
    handleSearchInfoFn(info) {
      return info;
    },
    actionColumn: {
      width: 100,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
      //ifShow: userPermission.value,
    },
  });

  // 跳转算法类型管理页面
  function goManageModelType() {
    go('/maTrainingCenter/modelType');
  }

  function UploadTrainImage(record) {
    openTrain(true, {
      record,
    });
  }

  function UploadDeployImage(record) {
    openDeploy(true, {
      record,
    });
  }

  function UploadConvertImage(record) {
    openConvert(true, {
      record,
    });
  }

  function UploadAutoLabelImage(record) {
    openAutoLabel(true, {
      record,
    });
  }

  function handleCreate() {
    openModal(true, {
      isUpdate: false,
    });
  }

  function handleEdit(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
    });
  }

  function handDeploy(record: Recordable) {
    openDeployModal(true, {
      record,
    });
  }

  function handCancelDeploy(record: Recordable) {
    openCancelDeployModal(true, { record });
  }

  function handleDelete(record: Recordable) {
    maHttp
      .post(
        {
          url: 'model/deleteModel',
          params: record,
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('删除成功！');
        reload();
        openConfigModal();
      });
  }

  function handleDownloadWeightFileList(record: Recordable) {
    openModelDownloadModal(true, { record });
  }

  const handleSuccess = () => {
    reload();
  };

  onActivated(() => {
    reload();
  });
</script>

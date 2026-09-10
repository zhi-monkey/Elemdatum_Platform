<template>
  <div>
    <BasicTable @register="registerTable">
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:file-done-outlined',
              tooltip: '查看权重参数',
              onClick: handleHyperParams.bind(null, record),
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
              disabled:
                !isAdmin ||
                (record.canDelete == true && !hasPermission([RoleEnum.BasicAlgorithm_Write])),
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
    <ParamModal
      :visible="isModalVisible"
      title="参数详情"
      :params="params"
      @update:visible="
        (visible) => {
          isModalVisible = visible;
        }
      "
    />
    <ModelModal @register="registerModal" @success="handleSuccess" />
    <UploadTrainModal @register="trainModal" @success="handleSuccess" />
    <UploadDeployModal @register="deployModal" @success="handleSuccess" />
    <UploadConvertModal @register="convertModal" @success="handleSuccess" />
    <UploadAutoLabelModal @register="autoLabelModal" @success="handleSuccess" />
    <ModelDownloadModal @register="registerModelDownloadModal" />
  </div>
</template>
<script lang="ts" setup>
  import { computed, ComputedRef, onActivated, ref } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { columns, searchFormSchema } from './model.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useModal } from '/@/components/Modal';
  import ModelModal from './component/ModelModal.vue';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { useGo } from '/@/hooks/web/usePage';
  import { RoleEnum } from '/@/enums/roleEnum';
  import UploadTrainModal from '/@/views/mineai/monitor/model-list/uploadImage/UploadTrainModal.vue';
  import UploadDeployModal from '/@/views/mineai/monitor/model-list/uploadImage/UploadDeployModal.vue';
  import UploadConvertModal from '/@/views/mineai/monitor/model-list/uploadImage/UploadConvertModal.vue';
  import UploadAutoLabelModal from '/@/views/mineai/monitor/model-list/uploadImage/UploadAutoLabelModal.vue';
  import ModelDownloadModal from '/@/views/mineai/monitor/model-list/modelDownload/ModelDownloadModal.vue';
  import ParamModal from '/@/views/mineai/application/basicAlgorithm/component/ParamModal.vue';
  import { useUserStore } from '/@/store/modules/user';

  const params = ref({
    trainParams: {},
    convertParams: {},
    otherParams: {},
  });
  const go = useGo();
  const isModalVisible = ref(false);
  //const params = ref<Record<string, any> | null>(null);
  const { hasPermission } = usePermission();
  const userPermission: ComputedRef<boolean> = computed(() => {
    return hasPermission([RoleEnum.SystemUser]);
  });
  const userStore = useUserStore();
  let roles = userStore.getUserInfo.roles;
  const isAdmin = computed(() => {
    // id=1 是管理员角色, id=89是管理人员角色
    return roles[0].id === 1 || roles[0].id === 89;
  });
  const { createMessage } = useMessage();
  const [registerModal, { openModal }] = useModal();
  const [trainModal, { openModal: openTrain }] = useModal();
  const [deployModal, { openModal: openDeploy }] = useModal();
  const [convertModal, { openModal: openConvert }] = useModal();
  const [autoLabelModal, { openModal: openAutoLabel }] = useModal();
  const [registerModelDownloadModal, { openModal: openModelDownloadModal }] = useModal();
  const [registerTable, { reload }] = useTable({
    title: '基础算法库',
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
      itemList.forEach((item) => {
        const idStr: string = item.id.toString().padStart(8, '0');
        item.id = `M${idStr}`;
      });
      return itemList;
    },
    columns: columns,
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
      schemas: searchFormSchema,
    },
    useSearchForm: true,
    showTableSetting: false,
    bordered: false,
    showIndexColumn: false,
    expandRowByClick: true,
    handleSearchInfoFn(info) {
      return info;
    },
    actionColumn: {
      width: 90,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
      // ifShow: userPermission.value,
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

  // function handleEdit(record: Recordable) {
  //   openModal(true, {
  //     record,
  //     isUpdate: true,
  //   });
  // }

  // 打开弹框并分类展示参数
  async function handleHyperParams(record) {
    // 手动将模态框设置为不可见，确保状态重置
    isModalVisible.value = false;

    // 清空参数，确保每次点击时数据都是新的
    params.value = {
      trainParams: {},
      convertParams: {},
      otherParams: {},
    };
    // 关闭模态框，以确保再次点击时可以打开
    // isModalVisible.value = false;
    const allParams = record.publishedHyperParamsList || [];
    const trainParams = {};
    const convertParams = {};
    const otherParams = {};

    allParams.forEach((param) => {
      const { paramName, trainDefaultValue } = param;
      if (paramName.startsWith('HP')) {
        trainParams[paramName] = trainDefaultValue;
      } else if (paramName.startsWith('CONVERT')) {
        convertParams[paramName] = trainDefaultValue;
      } else {
        otherParams[paramName] = trainDefaultValue;
      }
    });
    params.value = {
      trainParams,
      convertParams,
      otherParams,
    };
    //isModalVisible.value = true;
    // 使用 setTimeout 确保状态更新后再显示模态框
    setTimeout(() => {
      isModalVisible.value = true;
    }, 100); // 延迟100毫秒
  }

  function handleSubmit() {
    isModalVisible.value = false;
  }

  /**
   * 删除基础算法库中的算法
   *
   * @param record
   */
  function handleDelete(record: Recordable) {
    let param = { ...record };
    param.id = parseStringId(param.id);
    maHttp
      .post(
        {
          url: `model/deleteModel/${param.trainModelJob.id}`,
          params: param,
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
      });
  }

  function handleDownloadWeightFileList(record: Recordable) {
    openModelDownloadModal(true, { record });
  }

  /**
   * 格式化字符串信息
   *
   * @param id
   */
  function parseStringId(id: string): number {
    let match = id.match(/M0*(\d+)/);
    if (match) {
      return Number(match[1]);
    } else {
      return -1;
    }
  }

  /**
   * 成功提交后刷新页面
   */
  function handleSuccess() {
    reload();
  }

  onActivated(() => {
    reload();
  });
</script>

<style lang="less">
  .params-container {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
  }

  .params-column {
    flex: 0 0 48%;
    margin-bottom: 10px;
    font-size: 14px;
  }
</style>

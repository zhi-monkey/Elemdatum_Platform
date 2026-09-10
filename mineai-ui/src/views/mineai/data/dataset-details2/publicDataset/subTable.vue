<!--获取子列表数据-->
<template>
  <a-table
    :columns="subTableColumns"
    size="small"
    row-key="id"
    :pagination="false"
    :loading="loading"
    :data-source="data"
    :showIndexColumn="true"
  >
    <template #action="{ record }">
      <TableAction
        :actions="[
          {
            icon: 'ant-design:info-circle-outlined',
            tooltip: '详情',
            onClick: goDatasetDetail.bind(null, record),
          },
          {
            icon: 'ant-design:download-outlined',
            tooltip: '导出',
            onClick: goExport.bind(null, record),
            //disabled: !(roles[0].name === '管理人员' || roles[0].name === '管理员'),
          },
          {
            icon: 'ant-design:eye-invisible-outlined',
            tooltip: '取消公开',
            popConfirm: {
              title: '是否确认取消公开',
              confirm: cancelPublic.bind(null, record),
            },
            disabled: !hasPermission([RoleEnum.DatasetDetails_Write]) || !isAdmin,
          },
          {
            icon: 'ant-design:bars',
            tooltip: '查看标签',
            onClick: goTags.bind(null, record),
          },
        ]"
      />
    </template>
  </a-table>

  <a-modal
    v-model:visible="exportStatusModelVisible"
    title="导出"
    cancelText="隐藏"
    confirmText="开始导出"
    :okButtonProps="{ disabled: isPolling }"
    :cancelButtonProps="{ disabled: isPolling }"
    :closable="!isPolling"
    :maskClosable="!isPolling"
    @ok="confirmExport"
    @cancel="cancelExport"
  >
    <div class="modal-content">
      <a-spin v-if="isPolling" size="large" tip="正在导出，请稍候..." />
      <div v-else class="center-text">确定导出数据集？</div>
    </div>
  </a-modal>
  <DatasetDetailsModal @register="registerModal" />
  <a-modal
    v-model:visible="labelInfoModalVisible"
    v-if="labelInfoModalVisible"
    title="标签信息"
    :footer="null"
    :maskClosable="true"
    :body-style="{ overflow: 'auto' }"
    width="650px"
  >
    <LabelInfo :dataset-id="currentRow?.datasetId" :version-name="currentRow?.versionName" />
  </a-modal>
</template>

<script setup lang="ts">
  import { Table as ATable } from 'ant-design-vue';
  import { subTableColumns } from './stmData';
  import { computed, onMounted, ref } from 'vue';
  import { TableAction } from '/@/components/Table';
  import {
    cancelPublicDatasetVersion,
    checkExportStatus,
    exportDatasetVersion,
    getDatasetVersion,
    getMinIOAuth,
  } from '/@/views/mineai/data/dataset-details2/api';
  import { minioBaseUrl, minIOConfig } from '/@/utils/dubhe';
  import { decrypt } from '/@/utils/dubhe/rsaEncrypt';
  import { useRoute, useRouter } from 'vue-router';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useModal } from '/@/components/Modal';
  import DatasetDetailsModal from '/@/views/mineai/data/dataset-details2/dataset-details-modal.vue';
  import { Minio } from 'minio-js';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { RoleEnum } from '/@/enums/roleEnum';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { useUserStore } from '/@/store/modules/user';
  import { downloadByUrl } from '/@/utils/file/download';
  import { Modal as AModal, Spin as ASpin } from 'ant-design-vue';
  import LabelInfo from '/@/views/mineai/data/dataset-details2/datasetHistory/labelInfo.vue';

  let userStore = useUserStore();
  const { hasPermission } = usePermission();
  const labelInfoModalVisible = ref(false);
  const loading = ref(true);
  const data = ref([]);
  const url = 'modelJob/findModelJobsByModelGenerationId?modelGenerationId=';
  const emit = defineEmits<{
    (e: 'reload'): void;
  }>();
  const props = defineProps<{
    record: Object;
  }>();
  let roles = userStore.getUserInfo.roles;

  const getSubData = async (record) => {
    loading.value = true;
    data.value = await maHttp.get(
      {
        url: `datasets/versions/getPublicDatasetVersionList/${record.id}`,
        headers: {},
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    );
    // console.log(res);
    loading.value = false;
  };
  const exportStatusModelVisible = ref(false);
  const isPolling = ref(false);
  let versionToExport: Recordable;
  let timer: number = ref(null);

  onMounted(async () => {
    const authInfo = await getMinIOAuth();
    const { accessKey, privateKey, secretKey } = authInfo || {};
    const rawAccessKey = decrypt(accessKey, privateKey);
    const rawSecretKey = decrypt(secretKey, privateKey);
    const config = { ...minIOConfig.config, accessKey: rawAccessKey, secretKey: rawSecretKey };
    minioClient = new Minio.Client(config);
    await getSubData(props.record);
  });

  const isAdmin = computed(() => {
    // id=1 是管理员角色, id=89是管理人员角色
    return roles[0].id === 1 || roles[0].id === 89;
  });

  const currentRow = ref();
  const goTags = async (record: Recordable) => {
    currentRow.value = record;
    labelInfoModalVisible.value = true;
  };

  const route = useRoute();
  const router = useRouter();
  const { createMessage } = useMessage();
  let minioClient;

  // 此处可以得到用户名
  const datasetId = ref(route.params?.id);
  const datasetName = ref(route.params?.name);

  const [registerModal, { openModal }] = useModal();

  function goDatasetDetail(record: Recordable) {
    openModal(true, {
      record,
    });
  }

  const cancelPublic = async (record: Recordable) => {
    const isSuccess = await cancelPublicDatasetVersion(record.datasetId, record.versionName);
    if (isSuccess) {
      createMessage.success('取消发布成功');
    } else {
      createMessage.error(`取消发布失败}`);
    }
    await getSubData(props.record);
    await emit('reload');
  };

  async function goExport(record: Recordable) {
    exportStatusModelVisible.value = true;
    versionToExport = record;
  }

  async function datasetExportNew(record: Recordable) {
    let response = await getDatasetVersion(record.datasetId, record.versionName);
    if (response.dataConversion === 1) {
      return await exportDatasetVersion(record.datasetId, record.versionName);
    } else if (response.dataConversion === 0) {
      createMessage.warning('数据集相关文件还在拷贝中，请稍作等候');
    } else {
      createMessage.warning('数据集发布还未成功，请稍作等候');
    }
  }

  const confirmExport = async () => {
    isPolling.value = true;
    let zipName = `${versionToExport.datasetId}_${versionToExport.format}_${versionToExport.versionName}.zip`;
    const versionPath =
      versionToExport.versionUrl ||
      `dataset/${versionToExport.datasetId}/versionFile/${versionToExport.versionName}`;
    let ZipUrl = `${minioBaseUrl}/${versionPath}/${versionToExport.format}/${zipName}`;
    //console.log(ZipUrl);

    const stopPolling = (message, isSuccess = false) => {
      clearInterval(timer);
      isPolling.value = false;
      versionToExport = {};
      exportStatusModelVisible.value = false;
      if (message) {
        isSuccess ? createMessage.success(message) : createMessage.error(message);
      }
    };

    try {
      let isStarted = datasetExportNew(versionToExport);
      if (!isStarted) {
        stopPolling('导出失败，请重试！');
        return;
      }
      // createMessage.success('导出任务已经发起，请在此页面等待片刻即可自动下载', 8);
      // stopPolling('导出任务已完成，开始下载...', true);
      // downloadByUrl({
      //   url: 'http://210.30.96.101:9000/cz-dev/big.tar',
      //   target: '_self',
      // });
      timer = setInterval(async () => {
        //console.log(versionToExport);
        try {
          if (versionToExport !== null) {
            let response = await getDatasetVersion(
              versionToExport.datasetId,
              versionToExport.versionName,
            );
            //console.log(response);
            const status = await checkExportStatus(response.id);
            //console.log(status);
            if (status === 'success') {
              stopPolling('导出任务已完成，开始下载...', true);
              downloadByUrl({
                url: ZipUrl,
                target: '_self',
              });
            } else if (status === 'failed') {
              stopPolling('导出任务失败，请重试！');
            }
          }
        } catch (error) {
          console.error('检查导出状态时发生错误：', error);
          stopPolling('检查导出状态时发生错误，请重试！');
        }
      }, 2000);
    } catch (e) {
      stopPolling('导出任务发起失败，请重试！');
    }
  };

  const cancelExport = () => {
    exportStatusModelVisible.value = false;
    versionToExport = null;
  };
</script>

<style lang="scss">
  .modal-content {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100px; // 确保内容在垂直方向有一定高度
  }

  .center-text {
    text-align: center;
  }
</style>

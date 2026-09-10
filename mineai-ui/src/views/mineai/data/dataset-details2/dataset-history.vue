<template>
  <PageWrapper
    :title="`数据集${datasetName}的版本管理`"
    @back="goBack"
    style="margin: 0 16px 0 16px"
  >
    <div class="py-8 bg-white flex flex-col justify-center items-center">
      <BasicTable @register="registerTable">
        <template #action="{ record }">
          <TableAction
            :actions="[
              {
                icon: 'ant-design:check-outlined',
                tooltip: '设置为当前版本',
                onClick: setAsCurrentVersion.bind(null, record),
                ifShow: !record.isCurrent,
              },
              {
                icon: 'ant-design:info-circle-outlined',
                tooltip: '详情',
                onClick: goDatasetDetail.bind(null, record),
              },
              // {
              //   icon: 'ant-design:eye-outlined',
              //   tooltip: !record.isPublic ? '公开' : '已公开',
              //   disabled: !!record.isPublic,
              //   popConfirm: {
              //     title: '是否确认公开',
              //     confirm: publish.bind(null, record),
              //   },
              // },
              {
                icon: 'ant-design:highlight-outlined',
                tooltip: '查看标注',
                onClick: goLabel.bind(null, record),
                // ifShow: record.isCurrent,
              },
              {
                icon: 'ant-design:download-outlined',
                tooltip: '导出',
                onClick: goExport.bind(null, record),
              },
              {
                icon: 'ant-design:bars',
                tooltip: '查看标签',
                onClick: goTags.bind(null, record),
                ifShow: !isPublishing,
              },
              {
                icon: 'ant-design:delete-outlined',
                tooltip: '删除版本',
                color: 'error',
                ifShow: !record.isCurrent, // 当前版本不能删除
                popConfirm: {
                  title: '确认删除该版本？',
                  confirm: deleteVersion.bind(null, record),
                  placement: 'top',
                },
              },
            ]"
          />
        </template>
      </BasicTable>
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
    </div>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { h, onMounted, onUnmounted, ref } from 'vue';
  import { PageWrapper } from '/@/components/Page';
  import { useRoute, useRouter } from 'vue-router';
  import { now } from 'lodash-es';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { columns } from '/@/views/mineai/data/dataset-details2/dataset-history.data';
  import { Modal as AModal, notification, Spin as ASpin } from 'ant-design-vue';
  import {
    checkDatasetVersionBindRelationWithGenerationApi,
    checkExportStatus,
    datasetDetails,
    datasetVersionsByPage,
    deleteDatasetVersion,
    exportDatasetVersion,
    getDatasetVersion,
    getMinIOAuth,
    publishDatasetVersion,
    queryFirstImg,
    switchVersion,
  } from '/@/views/mineai/data/dataset-details2/api';
  import DatasetDetailsModal from '/@/views/mineai/data/dataset-details2/dataset-details-modal.vue';
  import { useModal } from '/@/components/Modal';
  import { decrypt } from '/@/utils/dubhe/rsaEncrypt';
  import { minioBaseUrl, minIOConfig } from '/@/utils/dubhe/minIO';
  import { Minio } from 'minio-js';
  import { downloadZipFromObjectPath } from '/@/utils/dubhe/download';
  import { downloadByUrl } from '/@/utils/file/download';
  import LabelInfo from '/@/views/mineai/data/dataset-details2/datasetHistory/labelInfo.vue';

  onMounted(async () => {
    const authInfo = await getMinIOAuth();
    const { accessKey, privateKey, secretKey } = authInfo || {};
    const rawAccessKey = decrypt(accessKey, privateKey);
    const rawSecretKey = decrypt(secretKey, privateKey);
    const config = { ...minIOConfig.config, accessKey: rawAccessKey, secretKey: rawSecretKey };
    minioClient = new Minio.Client(config);

    // 启动轮询
    startPollingDatasetDetails();
  });

  const labelInfoModalVisible = ref(false);
  const route = useRoute();
  const router = useRouter();
  const { createMessage } = useMessage();
  let minioClient;

  const exportStatusModelVisible = ref(false);
  const isPolling = ref(false);
  const pollingInterval = ref<NodeJS.Timeout>();

  // 此处可以得到用户名
  const datasetId = ref(route.params?.id);
  const datasetName = ref(route.params?.name);

  let versionToExport: Recordable;
  let timer: number = ref(null);
  const [registerModal, { openModal }] = useModal();
  const isPublishing = ref(null);

  // 添加轮询函数
  const startPollingDatasetDetails = async () => {
    // 先清理可能存在的旧定时器
    if (pollingInterval.value) {
      clearInterval(pollingInterval.value);
      pollingInterval.value = undefined;
    }
    const poll = async () => {
      try {
        const result = await datasetDetails(datasetId.value);
        isPublishing.value = result.isPublishing;
        console.log(isPublishing.value);
      } catch (error) {
        console.error('轮询数据集详情出错:', error);
      }
    };

    // 立即执行一次
    await poll();

    // 设置定时轮询，每5秒一次
    pollingInterval.value = setInterval(poll, 5000);
  };

  // 卸载定时查询数据集发布状态
  onUnmounted(() => {
    if (pollingInterval.value) {
      clearInterval(pollingInterval.value);
    }
  });

  const [registerTable, { reload }] = useTable({
    title: '数据集版本管理',
    api: async (params) => {
      const v = await datasetVersionsByPage(params);
      //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
      v.items = v.result;
      v.total = v.page.total;
      return v;
    },
    beforeFetch: (v) => {
      // 发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v = Object.assign(v, {
        current: v.page,
        size: v.pageSize,
      });
      // 加入数据集id
      v = Object.assign(v, {
        datasetId: parseInt(datasetId.value as string),
      });
      return v;
    },
    afterFetch: (v) => {
      // 如果数据集处于发布中状态，那么不展示最新的版本（未发布完成）
      if (v.length > 0 && v[0].dataConversion === 4 && v[v.length - 1].isCurrent === true) {
        v.pop();
      }
      // 倒排
      // 找到当前版本及其索引
      const curVersionIndex = v.findIndex((element) => element.isCurrent);
      // 如果当前版本在当前分页
      if (curVersionIndex !== -1) {
        const curVersion = v.find((element) => element.isCurrent);
        // 删除当前版本在数组中，然后插到第一位
        v.splice(curVersionIndex, 1);
        v.unshift(curVersion);
      }
    },
    columns: columns,
    bordered: true,
    showIndexColumn: false,
    showTableSetting: true,
    actionColumn: {
      width: 200,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
    handleSearchInfoFn(info) {
      return info;
    },
  });

  function goBack() {
    router.go(-1);
  }

  async function setAsCurrentVersion(record: Recordable) {
    try {
      AModal.confirm({
        title: () => '您是否确认切换数据集版本？',
        maskClosable: true,
        content: () =>
          '注意！切换版本会导致您当前数据集未保存的内容丢失，若需要保留已修改内容，请在切换前保存数据集版本！',
        onOk: async () => {
          await switchVersion(record.datasetId, record.versionName);
          createMessage.success('切换版本成功！');
          await reload();
        },
      });
    } catch (e) {
      console.error(e);
    }
  }

  function goDatasetDetail(record: Recordable) {
    openModal(true, {
      record,
    });
  }

  const publish = async (record: Recordable) => {
    const isSuccess = await publishDatasetVersion(record.datasetId, record.versionName);
    if (isSuccess) {
      createMessage.success('发布成功！');
    } else {
      createMessage.error(`发布失败}`);
    }
    await reload();
  };

  async function goLabel(record: Recordable) {
    const prefix = record.annotateType === 103 ? 'segmentation' : 'annotate';
    const firstImgId = await queryFirstImg(record.datasetId, record.versionName);
    await router.push({
      path: `/maData/${prefix}/${record.datasetId}/${record.name}`,
      state: {
        imgId: firstImgId,
        fromHistory: true,
        versionName: record.versionName, // 传递版本名称
        isCurrentVersion: record.isCurrent, // 标识是否为当前版本
      },
    });
  }

  async function deleteVersion(record: Recordable) {
    try {
      // 首先检查版本是否被绑定
      const bindingInfoList = await checkDatasetVersionBindRelationWithGenerationApi(record.id);
      console.log(bindingInfoList);
      if (bindingInfoList && bindingInfoList.length > 0) {
        notification.warning({
          key: 'dataset-version-deletion-blocked-detailed',
          message: '数据集版本删除被阻止',
          description: h('div', null, [
            h('p', '该版本因被任务绑定而无法删除，请先处理：'),
            h(
              'ul',
              { style: { marginTop: '8px', paddingLeft: '20px', listStyleType: 'disc' } },
              bindingInfoList.map((info) => {
                const taskType = info.isGuided ? '引导式训练任务' : '标准化训练任务';
                const descriptionText = `版本被${taskType} "${info.modelGenerationName}" 绑定`;
                return h('li', { key: `${info.modelGenerationName}` }, descriptionText);
              }),
            ),
          ]),
          duration: 0,
        });
        return;
      }
      // 如果没有绑定，继续删除
      await deleteDatasetVersion(record.datasetId, record.versionName);
      createMessage.success('删除版本成功！');
      await reload();
    } catch (error) {
      createMessage.error('删除版本失败！');
      console.error('删除版本失败:', error);
    }
  }

  async function datasetExport(record: Recordable) {
    let response = await getDatasetVersion(record.datasetId, record.versionName);
    if (response.dataConversion === 1) {
      const prefixUrl = `dataset/${record.datasetId}/versionFile/${record.versionName}`;
      // COCO替换为CreateML
      const format = record.format === 'COCO' ? 'CreateML' : record.format;
      const zipPrefix = `${datasetName.value}_${format}_${record.versionName}_${now()}`;
      return downloadZipFromObjectPath(minioClient, prefixUrl, `${zipPrefix}.zip`, {
        fileName: (file) => {
          return file.name.replace(`${prefixUrl}/${format}`, zipPrefix);
        },
        filter: (result) => {
          // 导出 COCO/YOLO 等天天枢格式，直接导出
          if (['COCO', 'YOLO', 'VOC', 'Segment-YOLO'].includes(record.format))
            return result.filter((item) => item.name.startsWith(`${prefixUrl}/${format}`));
          return result.filter((item) => {
            return ['annotation', 'origin'].some((str) =>
              item.name.startsWith(`${prefixUrl}/${str}`),
            );
          });
        },
      });
    } else if (response.dataConversion === 0) {
      createMessage.warning('数据集相关文件还在拷贝中，请稍作等候');
    } else {
      createMessage.warning('数据集发布还未成功，请稍作等候');
    }
  }

  const currentRow = ref();
  const goTags = async (record: Recordable) => {
    currentRow.value = record;
    labelInfoModalVisible.value = true;
  };

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
    let zipName = `${datasetId.value}_${versionToExport.format}_${versionToExport.versionName}.zip`;
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

  async function goExport(record: Recordable) {
    // Modal.confirm({
    //   title: () => '确认导出数据集?',
    //   maskClosable: true,
    //   icon: () => createVNode(ExportOutlined),
    //   content: () => '点击确认即可开始进行导出数据集',
    //   onOk: async () => {
    //     await datasetExport(record);
    //     createMessage.success('导出成功! 请耐心等待下载');
    //   },
    // });
    exportStatusModelVisible.value = true;
    versionToExport = record;
    // console.log(versionToExport);
  }
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

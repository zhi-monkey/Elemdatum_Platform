<template>
  <BasicTable
    @register="registerTable"
    @selection-change="selectionChange"
    @change="onPageChange"
    rowKey="id"
  >
    <template #toolbar>
      <a-button type="primary" @click="handleCreate">新增数据集</a-button>
      <a-button type="primary" @click="handleDelete" :disabled="canDelete"> 删除数据集 </a-button>
    </template>

    <template #action="{ record }">
      <div style="display: flex; justify-content: flex-start">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:info-circle-outlined',
              tooltip: '详情',
              onClick: goDetail.bind(null, record),
              disabled: record.status === 1,
            },
            {
              icon: 'clarity:note-edit-line',
              tooltip: '编辑',
              onClick: handleEdit.bind(null, record),
              disabled:
                record.status === 1 ||
                record.module === 1 ||
                record.status === 302 ||
                record.status === 103,
            },
            {
              icon: 'ant-design:file-add-outlined',
              tooltip: '导入',
              onClick: handleUploadModal.bind(null, record, dataTypeCodeMap.IMAGE),
              disabled:
                record.status === 1 ||
                record.module === 1 ||
                record.status === 403 ||
                record.status === 302 ||
                record.status === 103,
            },
            {
              icon: 'ant-design:tool-outlined',
              tooltip: '数据增强',
              onClick: handleEnhance.bind(null, record),
              ifShow: record.status === 105,
              disabled: record.status === 1 || record.module === 1 || record.status === 103,
            },
            {
              icon: 'ant-design:highlight-outlined',
              tooltip: '标注',
              onClick: goLabel.bind(null, record),
              disabled:
                record.status === 1 ||
                record.module === 1 ||
                record.status === 403 ||
                record.status === 103 ||
                record.haveImage,
            },
            {
              icon: 'ant-design:bg-colors-outlined',
              tooltip: '自动标注',
              onClick: goAutoLabel.bind(null, record),
              disabled:
                record.status === 1 ||
                record.module === 1 ||
                record.annotateType === 103 ||
                record.status === 103 ||
                record.status === 403 ||
                record.haveImage,
            },
          ]"
          :dropDownActions="[
            {
              icon: 'ant-design:check-circle-outlined',
              tooltip: '数据集发布',
              label: '数据集发布',
              onClick: datasetPublish.bind(null, record),
              ifShow: record.status === 105,
              disabled: record.status === 1 || record.module === 1 || record.status === 103,
            },
            {
              icon: 'ant-design:history-outlined',
              tooltip: '历史版本',
              label: '历史版本',
              onClick: goHistory.bind(null, record),
              ifShow: record.currentVersionName !== null,
              disabled: record.status === 1 || record.module === 1 || record.status === 103,
            },
            {
              icon: 'ant-design:check-outlined',
              tooltip: '保存数据集版本',
              label: '保存数据集版本',
              onClick: saveVersion.bind(null, record),
              ifShow: record.status === 105,
              disabled: record.status === 1 || record.module === 1 || record.status === 103,
            },
            {
              icon: 'ant-design:calendar-outlined',
              tooltip: '数据集事件列表',
              label: '数据集事件列表',
              onClick: handleDatasetEventModelOpen.bind(null, record),
            },
          ]"
        />
      </div>
    </template>
  </BasicTable>

  <!-- 所有弹窗组件 -->
  <DatasetModal @register="registerModal" @success="handleDatasetModalSuccess" />
  <DatasetDetailsModal @register="registerHistoryModal" />
  <DataEnhanceModel @register="registerEnhanceModel" @success="handleEnhanceSuccess" />
  <SaveVersionModal @register="registerSaveVersionModel" @success="handleSuccess" />
  <DatasetPublishModal
    @register="registerDatasetPublishModel"
    @success="handleDatasetPublishModalSuccess"
  />
  <AutoLabelModal
    @register="registerAutoLabelModel"
    @success="handleAutoLabelModalSuccess"
    v-if="!isGuidedAutoLabel"
  />
  <AutoLabelModalGuided
    @register="registerAutoLabelModelGuided"
    @success="handleAutoLabelModalSuccess"
    v-if="isGuidedAutoLabel"
  />
  <DatasetUploadModal
    @register="registerDatasetUploadModel"
    @success="handleSuccess"
    @local-upload="handleUpload"
    @local-upload-zip="handleUploadZip"
    @cloud-upload="handleCloudUpload"
  />
  <UploadDataFile
    :row="importRow"
    :visible="uploadDataFileVisible"
    :close-upload-data-file="closeUploadDataFile"
    :hide-upload-data-file="hideUploadDataFile"
    :start-zip-import-polling="startZipImportPolling"
  />
  <UploadDatafileZip
    :row="importRow"
    :visible="uploadZipFileVisible"
    :close-upload-zip-file="closeUploadZipFile"
    :hide-upload-zip-file="hideUploadZipFile"
    :start-zip-import-polling="startZipImportPolling"
  />
  <DatasetEventModel @register="registerDatasetEvent" />

  <div id="resume-button" v-if="resumeButtonVisible">
    <Button type="primary" shape="circle" size="large" @click="hideUploadDataFile">
      <template #icon>
        <RollbackOutlined />
      </template>
    </Button>
  </div>
</template>

<script lang="ts">
  export default {
    name: 'PrivateDataset',
  };
</script>

<script lang="ts" setup>
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import DatasetModal from './DatasetModal.vue';
  import DataEnhanceModel from './DataEnhanceModel.vue';
  import SaveVersionModal from './SaveVersionModal.vue';
  import DatasetUploadModal from './dataset-upload.vue';
  import AutoLabelModal from './auto-label-modal.vue';
  import AutoLabelModalGuided from '/@/views/mineai/train/generationGuideList/detail/dataset/component/autoLabel/auto-label-modal.vue';
  import { Button, Button as AButton, Modal, notification } from 'ant-design-vue';
  import { RollbackOutlined, ExclamationCircleOutlined } from '@ant-design/icons-vue';
  import { columns, searchFormSchema } from './dataset.data';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { createVNode, nextTick, onUnmounted, Ref, ref, watch } from 'vue';
  import { useRouter } from 'vue-router';
  import UploadDataFile from './upload-datafile.vue';
  import UploadDatafileZip from './upload-datafile-zip.vue';
  import { useGo } from '/@/hooks/web/usePage';
  import { dataTypeCodeMap } from './util';
  import {
    queryFirstImg,
    queryDatasetsProgress,
    queryDatasetStatus,
    importDatasetFromDataRepo,
    getDatasetById,
    deleteMapRecordById,
    batchGet,
    checkDatasetBindRelationWithGenerationApi,
  } from './api';
  import { useUploadStore } from '/@/store/modules/upload';
  import { Recordable } from 'vite-plugin-mock';
  import { useUserStore } from '/@/store/modules/user';
  import DatasetPublishModal from './dataset-publish/DatasetPublishModal.vue';
  import DatasetEventModel from './DatasetEventModel.vue';
  import DatasetDetailsModal from './dataset-details-modal.vue';

  // 定义 props，用于外部触发刷新
  interface Props {
    refreshTrigger?: number;
  }

  const props = withDefaults(defineProps<Props>(), {
    refreshTrigger: 0,
  });

  const userStore = useUserStore();
  const userData = userStore.getUserInfo;
  const uploadStore = useUploadStore();
  const go = useGo();
  const router = useRouter();

  const { createMessage } = useMessage();
  const [registerModal, { openModal }] = useModal();
  const [registerHistoryModal, { openModal: openHistoryModal }] = useModal();
  const [registerEnhanceModel, { openModal: openEnhanceModel }] = useModal();
  const [registerSaveVersionModel, { openModal: openSaveVersionModel }] = useModal();
  const [registerDatasetPublishModel, { openModal: openDatasetPublishModel }] = useModal();
  const [registerDatasetUploadModel, { openModal: openDatasetUploadModel }] = useModal();
  const [registerAutoLabelModel, { openModal: openAutoLabelModel }] = useModal();
  const [registerAutoLabelModelGuided, { openModal: openAutoLabelModelGuided }] = useModal();
  const [registerDatasetEvent, { openModal: openDatasetEvent }] = useModal();

  // 状态管理变量
  const datasetPollList = ref([]);
  const datasetUploadList: Ref<any[]> = ref([]);
  const publishingList: Ref<any[]> = ref([]);
  const publishTimer = ref({} as NodeJS.Timer);
  const timer: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);
  const uploadTimer: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);
  const zipImportPollingIds = ref<number[]>([]);
  const zipImportTimer: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);
  const isZipImportPolling = ref(false);

  // 数据集表格
  const [registerTable, { reload, clearSelectedRowKeys, updateTableDataRecord }] = useTable({
    title: '私有数据集列表',
    api: async (params) => {
      if (params.createTime && Array.isArray(params.createTime)) {
        params.createTime = params.createTime.map((time) => {
          const timestamp = Date.parse(time);
          return isNaN(timestamp) ? time : timestamp.toString();
        });
      }
      params.creatorID = userData.id;
      const v = await maHttp.get(
        {
          url: 'datasets',
          params,
          headers: {},
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      v.items = v.result;
      v.total = v.page.total;
      return v;
    },
    beforeFetch: (v) => {
      v = Object.assign(v, {
        current: v.page,
        size: v.pageSize,
      });
      if (Object.hasOwn(v, 'field')) {
        Object.assign(v, {
          order: v.order === 'ascend' ? 'asc' : 'desc',
          sort: v.field,
        });
      } else {
        Object.assign(v, {
          order: 'desc',
          sort: 'id',
        });
      }
      return v;
    },
    afterFetch: async (v) => {
      const ids = v.map((d) => d.id);
      if (ids.length > 0) {
        await queryDatasetsProgress({ datasetIds: ids }).then(async (res: { [x: string]: any }) => {
          v = v.map(async (d, index, dataset) => {
            let remainTime = '';
            if (d.isPublishing || (d.status === 105 && d.dataConversion === 4)) {
              publishingList.value.push(d);
            }

            d.haveImage = Object.values(Object.values(res)[dataset.length - index - 1]).every(
              (d) => d === 0,
            );

            if (
              d.status === 302 ||
              d.status === 401 ||
              d.status === 403 ||
              d.status === 103 ||
              d.dataConversion === 4
            ) {
              return {
                ...d,
                progress: 0,
                remainTime: '',
              };
            } else if (d.status === 104) {
              return {
                ...d,
                progress: 100,
                remainTime: '',
              };
            } else if (uploadStore.queryUploadStatus(d.id)) {
              return {
                ...d,
                status: 1,
                progress: uploadStore.queryUploadProgress(d.id),
                remainTime: '',
              };
            } else if (d.isPublishing) {
              let progress;
              let remainTime;
              return await queryDatasetStatus({ datasetIds: [d.id] }).then((res) => {
                progress = res[d.id].progress / 100;
                remainTime = res[d.id].remainTime;
                return {
                  ...d,
                  progress,
                  remainTime,
                };
              });
            } else {
              const rowProgress: any = res[d.id] || null;
              const progress = computeProgress(rowProgress);
              return {
                ...d,
                progress,
                remainTime: '',
              };
            }
          });
          v = await Promise.all(v);
        });

        datasetPollList.value = v.filter(
          (d) =>
            d.status === 302 ||
            d.status === 401 ||
            d.status === 403 ||
            d.status === 103 ||
            d.dataConversion === 4,
        );
        datasetUploadList.value = v.filter((d) => d.status === 1);

        if (datasetPollList.value.length > 0) {
          clearInterval(timer.value);
          timer.value = setInterval(pollDatasetStatus, 3 * 1000);
        }
        if (datasetUploadList.value.length > 0) {
          clearInterval(uploadTimer.value);
          uploadTimer.value = setInterval(pollUploadStatus, 1000);
        }
        if (publishingList.value.length > 0) {
          clearInterval(publishTimer.value);
          publishTimer.value = setInterval(reloadRow, 3 * 1000);
        }
      }
      return v;
    },
    columns: columns,
    rowSelection: {
      type: 'checkbox',
    },
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
      width: 175,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
    handleSearchInfoFn(info) {
      if (info.createTime && Array.isArray(info.createTime)) {
        info.createTime = info.createTime.map((time) => {
          const timestamp = Date.parse(time);
          return isNaN(timestamp) ? time : timestamp.toString();
        });
      }
      return info;
    },
  });

  // 监听外部刷新触发器
  watch(
    () => props.refreshTrigger,
    (newVal) => {
      if (newVal) {
        reload();
      }
    },
  );

  // 计算进度函数
  const computeProgress = (progress: any = {}) => {
    const { finished, autoFinished, finishAutoTrack, annotationNotDistinguishFile, unfinished } =
      progress;
    const allFinished = finished + autoFinished + finishAutoTrack + annotationNotDistinguishFile;
    return allFinished === 0 ? 0 : allFinished / (allFinished + unfinished);
  };

  // 轮询函数
  const reloadRow = async () => {
    if (publishingList.value.length === 0) {
      clearInterval(publishTimer.value);
      return;
    }
    const datasetIds = publishingList.value.map((e) => e.id);
    const result = await queryDatasetStatus({ datasetIds: datasetIds });
    for (const e of Object.keys(result)) {
      const dataset = await getDatasetById(e);
      if (!dataset.isPublishing && !(result[e].status === 105 && result[e].dataConversion === 4)) {
        publishingList.value.pop();
        const record = { ...dataset, ...result[e] };
        record.progress = await queryDatasetsProgress({ datasetIds: [e] }).then(
          (res: { [x: string]: null }) => {
            const rowProgress: any = res[e] || null;
            return computeProgress(rowProgress);
          },
        );
        await updateTableDataRecord(Number.parseInt(e), record);
      }
    }
  };

  const pollDatasetStatus = async () => {
    if (datasetPollList.value.length === 0) {
      clearInterval(timer.value);
      return;
    }
    const result = await queryDatasetStatus({
      datasetIds: datasetPollList.value.map((e) => e.id),
    });
    let updateList: any[] = [];
    for (const e of Object.keys(result)) {
      if (
        result[e].status === 101 ||
        result[e].status === 102 ||
        result[e].status === 105 ||
        result[e].status === 104
      ) {
        if (
          result[e].status === 105 &&
          result[e].dataConversion !== null &&
          result[e].dataConversion === 4
        ) {
          await setDatasetProgress(result);
          return false;
        }
        updateList.push({
          id: Number(e),
          status: Number(result[e].status),
          dataConversion: result[e].dataConversion,
        });
        datasetPollList.value.splice(
          datasetPollList.value.findIndex((d) => d.id === Number(e)),
          1,
        );
        await deleteMapRecordById(Number(e));
      } else {
        await setDatasetProgress(result);
      }
    }
    if (updateList.length > 0) {
      await queryDatasetsProgress({ datasetIds: updateList.map((e) => e.id) }).then(
        (res: { [x: string]: null }) => {
          updateList = updateList.map((d) => {
            const rowProgress = res[d.id] || null;
            let progress = d.status === 104 ? 100 : computeProgress(rowProgress);
            return { ...d, progress: progress, id: d.id };
          });
        },
      );
      for (const e of updateList) {
        let dataset = await getDatasetById(e.id);
        if (dataset.isPublishing) {
          return;
        }
        const record = { ...dataset, ...e, remainTime: '' };
        updateTableDataRecord(e.id, record);
      }
    }
  };

  const pollUploadStatus = async () => {
    if (datasetUploadList.value.length === 0) {
      clearInterval(uploadTimer.value);
      return;
    }
    for (const e of datasetUploadList.value) {
      if (uploadStore.queryUploadStatus(e.id)) {
        updateTableDataRecord(e.id, { id: e.id, progress: uploadStore.queryUploadProgress(e.id) });
      } else {
        datasetUploadList.value.splice(
          datasetUploadList.value.findIndex((d: any) => d.id === e.id),
          1,
        );
        const progress = await queryDatasetsProgress({ datasetIds: [e.id] });
        const status = await queryDatasetStatus({ datasetIds: [e.id] });

        if (progress && progress[e.id] && status && status[e.id]) {
          updateTableDataRecord(e.id, {
            id: e.id,
            progress: computeProgress(progress[e.id]),
            status: status[e.id].status,
            remainTime: status[e.id].remainTime,
            fileCount: progress[e.id].unfinished,
            haveImage: Object.values(progress[e.id]).every((i) => i === 0),
          });
        } else {
          const dataset = await getDatasetById(e.id);
          if (dataset) {
            updateTableDataRecord(e.id, dataset);
          }
        }
      }
    }
  };

  const setDatasetProgress = async (result: any) => {
    Object.keys(result).forEach((id) => {
      const entry = result[id];
      const originalE = datasetPollList.value.find((e: any) => e.id.toString() === id);
      if (originalE) {
        const record = {
          ...originalE,
          progress: (entry.progress * 1.0) / 100,
          remainTime: entry.remainTime,
        };
        updateTableDataRecord(Number.parseInt(id), record);
      }
    });
  };

  // 基础功能函数
  function handleCreate() {
    openModal(true, {
      isUpdate: false,
    });
  }

  function handleEdit(record: Recordable) {
    openModal(true, { record, isUpdate: true });
  }

  async function goLabel(record: Recordable) {
    const prefix = record.annotateType === 103 ? 'segmentation' : 'annotate';
    const firstImgId = await queryFirstImg(record.id);
    await router.push({
      path: `/maData/${prefix}/${record.id}/${record.name}`,
      state: { imgId: firstImgId },
    });
  }

  const isGuidedAutoLabel = ref(null);

  async function goAutoLabel(record: Recordable) {
    isGuidedAutoLabel.value = record.isGuided;
    await nextTick(() => {
      if (record.isGuided) {
        openAutoLabelModelGuided(true, { id: record.id });
      } else {
        openAutoLabelModel(true, { id: record.id });
      }
    });
  }

  function goDetail(record: Recordable) {
    go(`/maData/fileDetail/${record.id}/${record.name}/${record.module}/${record.annotateType}`);
  }

  function handleEnhance(record: Recordable) {
    openEnhanceModel(true, { record });
  }

  function saveVersion(record: Recordable) {
    openSaveVersionModel(true, { record });
  }

  const datasetPublish = async (record: Recordable) => {
    openDatasetPublishModel(true, { record });
  };

  function goHistory(record: Recordable) {
    go(`/maData/datasetHistory/${record.id}/${record.name}`);
  }

  // 删除功能
  const handleDelete = async () => {
    const bindingInfoList = await checkDatasetBindRelationWithGenerationApi(selectedKeys.value);
    if (bindingInfoList && bindingInfoList.length > 0) {
      notification.warning({
        key: 'dataset-deletion-blocked-detailed',
        message: '数据集删除被阻止',
        description: createVNode('div', null, [
          createVNode('p', null, '以下数据集因被任务绑定而无法删除，请先处理：'),
          createVNode(
            'ul',
            { style: { marginTop: '8px', paddingLeft: '20px', listStyleType: 'disc' } },
            bindingInfoList.map((info) => {
              const taskType = info.isGuided ? '引导式训练任务' : '标准化训练任务';
              const descriptionText = `数据集 "${info.datasetName}" 中存在版本被${taskType} "${info.modelGenerationName}" 绑定`;
              return createVNode(
                'li',
                { key: `${info.datasetName}-${info.modelGenerationName}` },
                descriptionText,
              );
            }),
          ),
        ]),
        duration: 0,
      });
      return;
    }

    const response = await batchGet(selectedKeys.value);
    const datasets = Object.values(response);
    const publicDatasets = datasets.filter((dataset) => dataset.isPublic === 1);

    if (publicDatasets.length > 0) {
      const publicDatasetIds = publicDatasets.map((dataset) => dataset.id).join(', ');
      createMessage.warning(
        `数据集(${publicDatasetIds})已有公开版本不能删除，需要先联系管理员取消公开`,
      );

      selectedRows.value = selectedRows.value.filter((row) => {
        return !publicDatasets.some((dataset) => dataset.id === row.id);
      });
      selectedKeys.value = selectedRows.value.map((row) => row.id);
      canDelete.value = selectedKeys.value.length <= 0;

      if (selectedKeys.value.length === 0) {
        clearSelectedRowKeys();
        return;
      }
    }

    const modal = Modal.confirm({
      title: () => '确认删除选中的' + selectedKeys.value.length + '条数据?',
      maskClosable: true,
      icon: () => createVNode(ExclamationCircleOutlined),
      okText: () => '确定',
      okType: 'danger',
      cancelText: () => '取消',
      onOk() {
        modal.destroy();
        const ids = selectedRows.value.map((row) => row.id);
        maHttp
          .delete(
            { url: 'datasets', data: { ids }, headers: {} },
            { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
          )
          .then(() => {
            createMessage.success('删除成功！');
            clearSelectedRowKeys();
            reload();
          })
          .catch((e) => {
            console.error(e);
            createMessage.error('删除失败！');
            reload();
            clearSelectedRowKeys();
          });
      },
      onCancel() {
        clearSelectedRowKeys();
      },
    });
  };

  // 成功回调函数
  const safeCloseSelectedRowKeys = () => {
    try {
      clearSelectedRowKeys();
    } catch (error) {
      console.warn('清理私有数据集表格选中状态失败:', error);
    }
  };

  const handleSuccess = async () => {
    safeCloseSelectedRowKeys();
    createMessage.success('保存数据集版本成功！');
    try {
      await reload();
    } catch (error) {
      console.warn('表格刷新失败:', error);
      setTimeout(() => {
        reload().catch(console.error);
      }, 100);
    }
  };

  const handleEnhanceSuccess = async () => {
    safeCloseSelectedRowKeys();
    createMessage.success('数据增强成功！');
    try {
      await reload();
    } catch (error) {
      console.warn('表格刷新失败:', error);
      setTimeout(() => {
        reload().catch(console.error);
      }, 100);
    }
  };

  const handleAutoLabelModalSuccess = async () => {
    safeCloseSelectedRowKeys();
    try {
      await reload();
    } catch (error) {
      console.warn('表格刷新失败:', error);
      setTimeout(() => {
        reload().catch(console.error);
      }, 100);
    }
  };

  const handleDatasetPublishModalSuccess = async () => {
    safeCloseSelectedRowKeys();
    createMessage.success('数据集发布成功！');
    try {
      await reload();
    } catch (error) {
      console.warn('表格刷新失败:', error);
      setTimeout(() => {
        reload().catch(console.error);
      }, 100);
    }
  };

  const handleDatasetModalSuccess = async (isEdit) => {
    safeCloseSelectedRowKeys();
    if (isEdit) {
      createMessage.success('数据集修改成功');
    } else {
      createMessage.success('数据集新增成功');
    }
    try {
      await reload();
    } catch (error) {
      console.warn('表格刷新失败:', error);
      setTimeout(() => {
        reload().catch(console.error);
      }, 100);
    }
  };

  // 选择逻辑
  const canDelete: Ref<boolean> = ref(true);
  const selectedKeys: Ref<Array<any>> = ref([]);
  const selectedRows: Ref<Array<any>> = ref([]);

  const selectionChange = ({ keys, rows }) => {
    selectedKeys.value = keys;
    selectedRows.value = rows;
    canDelete.value = keys.length <= 0;
  };

  function onPageChange() {
    clearSelectedRowKeys();
  }

  // 上传相关功能
  function handleUpload(record: Recordable, dataType) {
    importRow.value = record;
    importRow.value.dataType = dataType;
    uploadDataFileVisible.value = true;
  }

  function handleUploadZip(record: Recordable, labelType) {
    importRow.value = record;
    importRow.value.labelType = labelType;
    uploadZipFileVisible.value = true;
  }

  async function handleCloudUpload(record, dataType, dataRepo) {
    await importDatasetFromDataRepo(dataRepo, record.id, dataType);
    createMessage.success('导入任务下发成功');
    await reload();
  }

  function handleUploadModal(record: Recordable, dataType) {
    openDatasetUploadModel(true, { record, dataType });
  }

  const importRow: Ref = ref(null);
  const uploadDataFileVisible: Ref<boolean> = ref(false);
  const uploadZipFileVisible: Ref<boolean> = ref(false);
  const resumeButtonVisible: Ref<boolean> = ref(false);

  const closeUploadDataFile = async (flag) => {
    uploadDataFileVisible.value = false;
    if (resumeButtonVisible.value) {
      resumeButtonVisible.value = false;
    }

    if (flag) {
      if (importRow.value && importRow.value.id) {
        const dataset = await getDatasetById(importRow.value.id);
        if (dataset) {
          updateTableDataRecord(importRow.value.id, dataset);
        }
      }
      await reload();
    }
  };

  const startZipImportPolling = (datasetId: number) => {
    if (!zipImportPollingIds.value.includes(datasetId)) {
      zipImportPollingIds.value.push(datasetId);
    }
    clearInterval(zipImportTimer.value);
    zipImportTimer.value = setInterval(pollZipImportStatus, 2 * 1000);
    pollZipImportStatus();
  };

  const pollZipImportStatus = async () => {
    if (isZipImportPolling.value || zipImportPollingIds.value.length === 0) {
      if (zipImportPollingIds.value.length === 0) clearInterval(zipImportTimer.value);
      return;
    }

    isZipImportPolling.value = true;
    try {
      const datasetIds = [...zipImportPollingIds.value];
      const result = await queryDatasetStatus({ datasetIds });
      const completedIds = datasetIds.filter((id) => {
        const status = result?.[id]?.status;
        return status !== undefined && Number(status) !== 403;
      });

      if (completedIds.length === 0) return;

      zipImportPollingIds.value = zipImportPollingIds.value.filter(
        (id) => !completedIds.includes(id),
      );
      await reload();
    } catch (error) {
      console.error('轮询 ZIP 导入状态失败:', error);
    } finally {
      isZipImportPolling.value = false;
      if (zipImportPollingIds.value.length === 0) clearInterval(zipImportTimer.value);
    }
  };

  const hideUploadDataFile = (): void => {
    uploadDataFileVisible.value = !uploadDataFileVisible.value;
    resumeButtonVisible.value = !resumeButtonVisible.value;
  };

  const closeUploadZipFile = async (flag) => {
    uploadZipFileVisible.value = false;
    if (resumeButtonVisible.value) {
      resumeButtonVisible.value = false;
    }

    if (flag) {
      if (importRow.value && importRow.value.id) {
        const dataset = await getDatasetById(importRow.value.id);
        if (dataset) {
          updateTableDataRecord(importRow.value.id, dataset);
        }
      }
      await reload();
    }
  };

  const hideUploadZipFile = (): void => {
    uploadZipFileVisible.value = !uploadZipFileVisible.value;
    resumeButtonVisible.value = !resumeButtonVisible.value;
  };

  const handleDatasetEventModelOpen = (record: Recordable) => {
    openDatasetEvent(true, { datasetId: record.id });
  };

  onUnmounted(() => {
    clearInterval(timer.value);
    clearInterval(uploadTimer.value);
    clearInterval(publishTimer.value);
    clearInterval(zipImportTimer.value);
  });
</script>

<style lang="less" scoped>
  #resume-button {
    position: fixed;
    right: 32px;
    top: 235px;
    z-index: 2147483640;
    display: flex;
    flex-direction: column;
    cursor: pointer;
  }
</style>

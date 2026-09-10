<template>
  <div class="dataset-group-container">
    <PageWrapper contentBackground @back="goBackToGroupList" style="margin: 0 16px 0 16px">
      <template #title>
        <a-typography-link @click="goBackToGroupList" class="group-title-link">
          <FolderOpenOutlined />
          <span class="ml-2">数据集组: {{ activeGroupName }}</span>
        </a-typography-link>
      </template>

      <BasicTable
        @register="registerDatasetTable"
        @expand="handleDatasetExpand"
        @selection-change="groupSelectionChange"
        @change="onGroupPageChange"
        :rowKey="datasetRowKey"
        :row-class-name="getRowClassName"
      >
        <template #toolbar>
          <a-button type="primary" @click="handleCreate">新增数据集</a-button>
          <a-button type="primary" @click="handleGroupDatasetDelete" :disabled="groupCanDelete">
            删除数据集
          </a-button>
        </template>

        <!-- 完整的操作按钮，复用原有逻辑 -->
        <template #action="{ record }">
          <div style="display: flex; justify-content: flex-start">
            <TableAction
              :actions="getTableActions(record)"
              :dropDownActions="getDropdownActions(record)"
            />
          </div>
        </template>

        <!-- 展开行，显示版本列表 -->
        <template #expandedRowRender="{ record }">
          <div class="version-table-wrapper">
            <a-spin :spinning="expandedVersions[record.id]?.loading">
              <a-table
                :columns="versionTableColumns"
                :data-source="expandedVersions[record.id]?.data"
                :pagination="expandedVersions[record.id]?.pagination"
                @change="(p) => handleVersionPageChange(record.id, p)"
                size="small"
                rowKey="id"
              >
                <template #action="{ record }">
                  <TableAction
                    :actions="getVersionActions(record)"
                    :dropDownActions="getVersionDropdownActions(record)"
                  />
                </template>
              </a-table>
            </a-spin>
          </div>
        </template>
      </BasicTable>
      <input
        ref="pcdFileInput"
        type="file"
        accept=".pcd"
        multiple
        style="display: none"
        @change="handlePcdFileChange"
      />
      <input
        ref="videoFileInput"
        type="file"
        accept="video/mp4,video/x-msvideo,video/quicktime,video/x-matroska,video/x-flv,video/x-ms-wmv,video/webm,.m4v"
        multiple
        style="display: none"
        @change="handleVideoFileChange"
      />
      <input
        ref="recordFileInput"
        type="file"
        accept=".record,application/octet-stream"
        style="display: none"
        @change="handleRecordFileChange"
      />
    </PageWrapper>
    <!-- 所有数据集相关弹窗 -->
    <DatasetModal @register="registerDatasetModal" @success="handleDatasetModalSuccess" />
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
    <a-modal
      v-model:visible="recordImportVisible"
      title="导入 Apollo Record"
      :confirm-loading="recordImportUploading"
      ok-text="上传并开始导入"
      cancel-text="取消"
      @ok="submitRecordImport"
      @cancel="closeRecordImport"
    >
      <div class="record-import-modal">
        <div class="record-import-dataset">数据集：{{ recordImportRecord?.name || '-' }}</div>
        <a-button type="primary" @click="chooseRecordFile">选择 Record 文件</a-button>
        <div v-if="recordImportFile" class="record-import-file">
          {{ recordImportFile.name }}（{{ formatFileSize(recordImportFile.size) }}）
        </div>
        <div v-else class="record-import-placeholder">请选择一个 .record 文件</div>
        <div v-if="recordImportUploading" class="record-import-progress">
          <a-progress :percent="recordImportProgress" />
          <div>正在上传 Record，请勿关闭页面（{{ recordImportProgress }}%）</div>
        </div>
      </div>
    </a-modal>
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
    <!-- 从origin导出弹窗 -->
    <a-modal
      v-model:visible="originExportModelVisible"
      title="导出已标注图片"
      cancelText="取消"
      confirmText="开始导出"
      :okButtonProps="{ disabled: isOriginExportPolling }"
      :cancelButtonProps="{ disabled: isOriginExportPolling }"
      :closable="!isOriginExportPolling"
      :maskClosable="!isOriginExportPolling"
      @ok="confirmOriginExport"
      @cancel="cancelOriginExport"
    >
      <div class="modal-content">
        <a-spin v-if="isOriginExportPolling" size="large" tip="正在导出，请稍候..." />
        <div v-else class="center-text">
          确定导出已标注的图片？（导出时请不要修改数据集内容！）
        </div>
      </div>
    </a-modal>
    <!-- 从origin导出未标注图片弹窗 -->
    <a-modal
      v-model:visible="originUnannotatedExportModelVisible"
      title="导出未标注图片"
      cancelText="取消"
      confirmText="开始导出"
      :okButtonProps="{ disabled: isOriginUnannotatedExportPolling }"
      :cancelButtonProps="{ disabled: isOriginUnannotatedExportPolling }"
      :closable="!isOriginUnannotatedExportPolling"
      :maskClosable="!isOriginUnannotatedExportPolling"
      @ok="confirmOriginUnannotatedExport"
      @cancel="cancelOriginUnannotatedExport"
    >
      <div class="modal-content">
        <a-spin v-if="isOriginUnannotatedExportPolling" size="large" tip="正在导出，请稍候..." />
        <div v-else class="center-text">
          确定导出未标注的图片（包括空标注和无标注文件）？（导出时请不要修改数据集内容！）
        </div>
      </div>
    </a-modal>
    <!-- 标签信息弹窗 -->
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
    <!-- 恢复按钮 -->
    <div id="resume-button" v-if="resumeButtonVisible">
      <Button type="primary" shape="circle" size="large" @click="hideUploadDataFile">
        <template #icon>
          <RollbackOutlined />
        </template>
      </Button>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import DatasetModal from './DatasetModal.vue';
  import DataEnhanceModel from './DataEnhanceModel.vue';
  import SaveVersionModal from './SaveVersionModal.vue';
  import DatasetUploadModal from './dataset-upload.vue';
  import AutoLabelModal from './auto-label-modal.vue';
  import AutoLabelModalGuided from '/@/views/mineai/train/generationGuideList/detail/dataset/component/autoLabel/auto-label-modal.vue';
  import {
    Button,
    Modal,
    Modal as AModal,
    notification,
    Popover as APopover,
    Spin as ASpin,
    Table as ATable,
    Tag as ATag,
    TypographyLink as ATypographyLink,
  } from 'ant-design-vue';
  import {
    ExclamationCircleOutlined,
    FolderOpenOutlined,
    RollbackOutlined,
  } from '@ant-design/icons-vue';
  import { columns, searchFormSchema } from './dataset.data';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { computed, createVNode, h, nextTick, onUnmounted, reactive, Ref, ref, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import UploadDataFile from './upload-datafile.vue';
  import UploadDatafileZip from './upload-datafile-zip.vue';
  import { useGo } from '/@/hooks/web/usePage';
  import { dataTypeCodeMap } from './util';
  import {
    batchGet,
    cancelPublicDatasetVersion,
    checkDatasetBindRelationWithGenerationApi,
    checkDatasetVersionBindRelationWithGenerationApi,
    deleteDatasetVersion,
    deleteMapRecordById,
    exportDatasetVersion,
    checkExportStatus,
    exportDatasetFromOrigin,
    checkOriginExportStatus,
    getOriginExportUrl,
    exportUnannotatedDatasetFromOrigin,
    checkOriginUnannotatedExportStatus,
    getOriginUnannotatedExportUrl,
    getDatasetById,
    getDatasetVersion,
    importDatasetFromDataRepo,
    listVideoTasks,
    queryDatasetsProgress,
    queryDatasetStatus,
    queryFirstImg,
    recordDatasetDownload,
    switchVersion,
  } from './api';
  import { useUploadStore } from '/@/store/modules/upload';
  import { Recordable } from 'vite-plugin-mock';
  import { PageWrapper } from '/@/components/Page';
  import { useUserStore } from '/@/store/modules/user';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { minioBaseUrl } from '/@/utils/dubhe';
  import { downloadByUrl } from '/@/utils/file/download';
  import { RoleEnum } from '/@/enums/roleEnum';
  import {
    createImportTransferTask,
    updateImportTransferProgress,
    completeImportTransferTask,
    failImportTransferTask,
  } from '/@/api/mineai/importTransferTask';
  import DatasetPublishModal from './dataset-publish/DatasetPublishModal.vue';
  import DatasetEventModel from './DatasetEventModel.vue';
  import DatasetDetailsModal from './dataset-details-modal.vue';
  import LabelInfo from '/@/views/mineai/data/dataset-details2/datasetHistory/labelInfo.vue';
  import { PageEnum } from '/@/enums/pageEnum';

  const { hasPermission } = usePermission();
  const userStore = useUserStore();
  const roles = userStore.getUserInfo.roles;
  const exportStatusModelVisible = ref(false);
  const isPolling = ref(false);
  let versionToExport: Recordable = {};
  let timer: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);

  // origin 导出相关变量
  let originExportRecord: Recordable = {};
  let originExportTimer: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);
  const originExportModelVisible = ref(false);
  const isOriginExportPolling = ref(false);

  // 导出未标注图片相关变量
  let originUnannotatedExportRecord: Recordable = {};
  let originUnannotatedExportTimer: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);
  const originUnannotatedExportModelVisible = ref(false);
  const isOriginUnannotatedExportPolling = ref(false);

  const recordFileInput = ref<HTMLInputElement | null>(null);
  const recordImportVisible = ref(false);
  const recordImportUploading = ref(false);
  const recordImportProgress = ref(0);
  const recordImportRecord = ref<Recordable | null>(null);
  const recordImportFile = ref<File | null>(null);

  // 判断是否为管理员
  const isAdmin = computed(() => {
    return roles[0].name === '管理员' || roles[0].name === '管理人员';
  });

  const props = defineProps<{
    refreshTrigger?: number;
  }>();

  const uploadStore = useUploadStore();
  const go = useGo();
  const router = useRouter();
  const route = useRoute();

  const { createMessage } = useMessage();

  // Modal注册
  const [registerDatasetModal, { openModal: openDatasetModal }] = useModal();
  const [registerHistoryModal, { openModal: openHistoryModal }] = useModal();
  const [registerEnhanceModel, { openModal: openEnhanceModel }] = useModal();
  const [registerSaveVersionModel, { openModal: openSaveVersionModel }] = useModal();
  const [registerDatasetPublishModel, { openModal: openDatasetPublishModel }] = useModal();
  const [registerDatasetUploadModel, { openModal: openDatasetUploadModel }] = useModal();
  const [registerAutoLabelModel, { openModal: openAutoLabelModel }] = useModal();
  const [registerAutoLabelModelGuided, { openModal: openAutoLabelModelGuided }] = useModal();
  const [registerDatasetEvent, { openModal: openDatasetEvent }] = useModal();

  // 轮询相关状态
  const groupDatasetPollList = ref([]);
  const groupDatasetUploadList: Ref<any[]> = ref([]);
  const groupPublishingList: Ref<any[]> = ref([]);
  const groupPublishTimer = ref({} as NodeJS.Timer);
  const groupTimer: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);
  const groupUploadTimer: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);
  const groupZipImportPollingIds = ref<number[]>([]);
  const groupZipImportTimer: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);
  const isGroupZipImportPolling = ref(false);

  // 视图切换状态
  const activeGroupId = ref<number | null>(null);
  const activeGroupName = ref<string>('');
  const highlightedRowId = ref<number | null>(null);

  // 标签信息弹窗
  const currentRow = ref();
  const labelInfoModalVisible = ref(false);

  // API函数
  const fetchDatasetsAPI = (datasetGroupId: number, page: number, pageSize: number) => {
    return maHttp
      .get(
        {
          url: 'datasets/group/getDatasetsPageByDatasetGroupId',
          params: {
            datasetGroupId,
            current: page,
            size: pageSize,
          },
        },
        {
          urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
        },
      )
      .then((res) => {
        return {
          items: res.records,
          total: res.total,
        };
      });
  };

  const fetchVersionsAPI = (
    datasetId: number,
    current: number,
    size: number,
    isIncludePublishing = false,
  ) => {
    return maHttp
      .get(
        {
          url: 'datasets/versions/versionsDetailList',
          params: {
            datasetId,
            current,
            size,
            isIncludePublishing,
          },
        },
        {
          urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
        },
      )
      .then((res) => {
        return {
          items: res.result,
          total: res.page.total,
        };
      });
  };

  const goBackToGroupList = () => {
    activeGroupId.value = null;
    activeGroupName.value = '';
    clearGroupPolling();
    clearGroupSelectedRowKeys();
    router.push({
      path: PageEnum.DATASET_DETAILS,
      query: { tab: '1' },
    });
  };
  // 创建 ref 存储原始数据,防止afterfetch改动
  const rawDatasetItems = ref<any[]>([]);
  // 数据集表格
  const [
    registerDatasetTable,
    {
      reload: reloadDatasets,
      updateTableDataRecord: updateGroupTableDataRecord,
      clearSelectedRowKeys: clearGroupSelectedRowKeys,
    },
  ] = useTable({
    api: async (params) => {
      activeGroupId.value = Number(route.params.id);
      if (!activeGroupId.value) return { items: [], total: 0 };
      // 从sessionStorage获取GroupName并赋值
      activeGroupName.value = sessionStorage.getItem('GroupName') ?? '';
      const { page, pageSize } = params;
      const { items, total } = await fetchDatasetsAPI(activeGroupId.value, page, pageSize);
      rawDatasetItems.value = [...items];
      return { items, total };
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
      // 视频数据集标注进度聚合（已标注 done/total）
      const videoDatasets = v.filter((d) => d.dataType === 6);
      for (const d of videoDatasets) {
        try {
          const tasks = (await listVideoTasks(d.id)) || [];
          const total = tasks.length;
          const done = tasks.filter(
            (t) => t.taskStatus === 'SUBMITTED' || t.taskStatus === 'APPROVED',
          ).length;
          const annotating = tasks.filter(
            (t) => t.taskStatus === 'ANNOTATING' || t.taskStatus === 'REVIEWING',
          ).length;
          d.annotateProgress = { total, done, annotating };
        } catch (e) {
          d.annotateProgress = null;
        }
      }

      const commonDatasets = v.filter(
        (d) => d.dataType !== 5 && d.dataType !== 6 && d.dataType !== 7,
      );
      const ids = commonDatasets.map((d) => d.id);
      if (ids.length > 0) {
        await queryDatasetsProgress({ datasetIds: ids }).then(async (res: { [x: string]: any }) => {
          const commonDatasetCount = commonDatasets.length;
          const processedCommonDatasets = commonDatasets.map(async (d, index) => {
            if (d.isPublishing || (d.status === 105 && d.dataConversion === 4)) {
              groupPublishingList.value.push(d);
            }

            d.haveImage = Object.values(Object.values(res)[commonDatasetCount - index - 1]).every(
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
              const progress = computeProgress(rowProgress, null);
              return {
                ...d,
                progress,
                remainTime: '',
              };
            }
          });
          const processed = await Promise.all(processedCommonDatasets);
          const processedByCode = new Map(
            processed.map((dataset) => [datasetCode(dataset), dataset]),
          );
          v = v.map(
            (dataset) =>
              processedByCode.get(datasetCode(dataset)) || {
                ...dataset,
                progress: 0,
                remainTime: '',
                haveImage: false,
              },
          );
        });

        groupDatasetPollList.value = v.filter(
          (d) =>
            d.status === 302 ||
            d.status === 401 ||
            d.status === 403 ||
            d.status === 103 ||
            d.dataConversion === 4,
        );
        groupDatasetUploadList.value = v.filter((d) => d.status === 1);

        if (groupDatasetPollList.value.length > 0) {
          clearInterval(groupTimer.value);
          groupTimer.value = setInterval(pollDatasetStatusGroup, 3 * 1000);
        }
        if (groupDatasetUploadList.value.length > 0) {
          clearInterval(groupUploadTimer.value);
          groupUploadTimer.value = setInterval(pollUploadStatusGroup, 1000);
        }
        if (groupPublishingList.value.length > 0) {
          clearInterval(groupPublishTimer.value);
          groupPublishTimer.value = setInterval(reloadRowGroup, 3 * 1000);
        }
      }
      return v;
    },
    columns: columns,
    pagination: true,
    showIndexColumn: false,
    showTableSetting: false,
    bordered: true,
    clickToRowSelect: false,
    rowExpandable: (record) =>
      record.dataType !== 5 && record.dataType !== 6 && record.dataType !== 7,
    rowSelection: {
      type: 'checkbox',
      getCheckboxProps: (record) => ({
        disabled: record.dataType === 5 || record.dataType === 6 || record.dataType === 7,
      }),
    },
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
      schemas: searchFormSchema,
      submitOnReset: true,
    },
    actionColumn: {
      width: 175,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  });

  // 计算 haveImage 的公共函数
  const computeHaveImage = (progressData: any): boolean => {
    if (!progressData) return false;
    return Object.values(progressData).every((value) => {
      return Number(value) === 0;
    });
  };

  // 计算进度函数
  const computeProgress = (progress: any = {}, datasetStatus?: any) => {
    const { finished, autoFinished, finishAutoTrack, annotationNotDistinguishFile, unfinished } =
      progress;
    const allFinished = finished + autoFinished + finishAutoTrack + annotationNotDistinguishFile;
    const progressValue = allFinished === 0 ? 0 : allFinished / (allFinished + unfinished);

    // 添加消息提示逻辑，导入成功已有右侧弹窗消息提示
    if (!datasetStatus) {
      return allFinished === 0 ? 0 : allFinished / (allFinished + unfinished);
    } else if (datasetStatus === 401 && progressValue >= 1) {
      createMessage.success('数据增强成功');
    } else if (datasetStatus === 4 && progressValue >= 1) {
      createMessage.success('数据集版本保存成功');
    } else if (datasetStatus === true && progressValue >= 1) {
      createMessage.success('数据集发布成功');
    }
    return allFinished === 0 ? 0 : allFinished / (allFinished + unfinished);
  };

  // 轮询函数
  // 刷新展开的版本列表（如果该数据集的版本列表是展开状态）
  const refreshExpandedVersionsIfOpen = async (datasetId: number) => {
    if (expandedVersions[datasetId]) {
      console.log(`刷新数据集 ${datasetId} 的版本列表`);
      try {
        await fetchVersions(datasetId);
      } catch (error) {
        console.warn(`刷新数据集 ${datasetId} 版本列表失败:`, error);
      }
    }
  };

  // 批量刷新多个数据集的展开版本列表
  const refreshAllExpandedVersions = async () => {
    const expandedDatasetIds = Object.keys(expandedVersions)
      .map((id) => Number(id))
      .filter((id) => expandedVersions[id]);

    if (expandedDatasetIds.length > 0) {
      console.log(`批量刷新 ${expandedDatasetIds.length} 个展开的版本列表`);
      for (const datasetId of expandedDatasetIds) {
        try {
          await fetchVersions(datasetId);
        } catch (error) {
          console.warn(`批量刷新数据集 ${datasetId} 版本列表失败:`, error);
        }
      }
    }
  };

  const pollDatasetStatusGroup = async () => {
    if (groupDatasetPollList.value.length === 0) {
      clearInterval(groupTimer.value);
      return;
    }

    const result = await queryDatasetStatus({
      datasetIds: groupDatasetPollList.value.map((e) => e.id),
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
          await setDatasetProgressGroup(result);
          return false;
        }
        updateList.push({
          id: Number(e),
          status: Number(result[e].status),
          dataConversion: result[e].dataConversion,
        });
        groupDatasetPollList.value.splice(
          groupDatasetPollList.value.findIndex((d) => d.id === Number(e)),
          1,
        );
        await deleteMapRecordById(Number(e));
      } else {
        await setDatasetProgressGroup(result);
      }
    }

    if (updateList.length > 0) {
      await queryDatasetsProgress({ datasetIds: updateList.map((e) => e.id) }).then(
        (res: { [x: string]: null }) => {
          updateList = updateList.map((d) => {
            const rowProgress = res[d.id] || null;
            // 从 groupDatasetPollList 中查找该数据集的原始前端状态
            const originalDataset = rawDatasetItems.value.find((item) => item.id === d.id);
            // 多分支赋值 frontendStatus
            let frontendStatus;
            if (originalDataset) {
              if ([401].includes(originalDataset.status)) {
                frontendStatus = originalDataset.status; // 如果数据增强中
              } else if (originalDataset.isPublishing === true) {
                frontendStatus = true; // 如果正在发布，使用 true
              } else if (originalDataset.dataConversion === 4) {
                frontendStatus = 4; // 如果保存版本
              } else {
                frontendStatus = originalDataset.dataConversion;
              }
            } else {
              frontendStatus = null;
            }

            if (d.status === 104) {
              createMessage.success('自动标注成功');
            }
            let progress = d.status === 104 ? 100 : computeProgress(rowProgress, frontendStatus);
            // 重新计算 haveImage
            const haveImage = computeHaveImage(rowProgress);
            return { ...d, progress: progress, haveImage: haveImage, id: d.id };
          });
        },
      );

      for (const e of updateList) {
        let dataset = await getDatasetById(e.id);
        if (dataset.isPublishing) {
          return;
        }
        const record = { ...dataset, ...e, remainTime: '' };
        updateGroupTableDataRecord(e.id, record);

        // 刷新展开的版本列表
        await refreshExpandedVersionsIfOpen(e.id);
      }
    }
  };

  const pollUploadStatusGroup = async () => {
    if (groupDatasetUploadList.value.length === 0) {
      clearInterval(groupUploadTimer.value);
      return;
    }

    for (const e of groupDatasetUploadList.value) {
      if (uploadStore.queryUploadStatus(e.id)) {
        updateGroupTableDataRecord(e.id, {
          id: e.id,
          progress: uploadStore.queryUploadProgress(e.id),
        });
      } else {
        groupDatasetUploadList.value.splice(
          groupDatasetUploadList.value.findIndex((d: any) => d.id === e.id),
          1,
        );
        const progress = await queryDatasetsProgress({ datasetIds: [e.id] });
        const status = await queryDatasetStatus({ datasetIds: [e.id] });

        if (progress && progress[e.id] && status && status[e.id]) {
          updateGroupTableDataRecord(e.id, {
            id: e.id,
            progress: computeProgress(progress[e.id], null),
            status: status[e.id].status,
            remainTime: status[e.id].remainTime,
            fileCount: progress[e.id].unfinished,
            // 重新计算 haveImage
            haveImage: computeHaveImage(progress[e.id]),
          });

          // 上传完成后，刷新展开的版本列表（如果有的话）
          await refreshExpandedVersionsIfOpen(e.id);
        } else {
          const dataset = await getDatasetById(e.id);
          if (dataset) {
            updateGroupTableDataRecord(e.id, dataset);
            // 刷新展开的版本列表
            await refreshExpandedVersionsIfOpen(e.id);
          }
        }
      }
    }
  };

  const reloadRowGroup = async () => {
    if (groupPublishingList.value.length === 0) {
      clearInterval(groupPublishTimer.value);
      return;
    }

    const datasetIds = groupPublishingList.value.map((e) => e.id);
    const result = await queryDatasetStatus({ datasetIds: datasetIds });

    for (const e of Object.keys(result)) {
      const dataset = await getDatasetById(e);
      if (!dataset.isPublishing && !(result[e].status === 105 && result[e].dataConversion === 4)) {
        groupPublishingList.value.pop();
        const record = { ...dataset, ...result[e] };
        record.progress = await queryDatasetsProgress({ datasetIds: [e] }).then(
          (res: { [x: string]: null }) => {
            const rowProgress: any = res[e] || null;
            const progress = computeProgress(rowProgress, null);

            // 重新计算 haveImage
            const haveImage = computeHaveImage(rowProgress);

            // 返回包含 haveImage 的对象
            return { progress, haveImage };
          },
        );
        await updateGroupTableDataRecord(Number.parseInt(e), {
          ...record,
          haveImage: record.progress.haveImage,
          progress: record.progress.progress,
        });

        // 刷新展开的版本列表
        await refreshExpandedVersionsIfOpen(Number.parseInt(e));
      }
    }
  };

  const setDatasetProgressGroup = async (result: any) => {
    Object.keys(result).forEach((id) => {
      const entry = result[id];
      const originalE = groupDatasetPollList.value.find((e: any) => e.id.toString() === id);
      if (originalE) {
        const record = {
          ...originalE,
          progress: (entry.progress * 1.0) / 100,
          remainTime: entry.remainTime,
        };
        updateGroupTableDataRecord(Number.parseInt(id), record);
      }
    });
  };

  const clearGroupPolling = () => {
    clearInterval(groupTimer.value);
    clearInterval(groupUploadTimer.value);
    clearInterval(groupPublishTimer.value);
    clearInterval(groupZipImportTimer.value);
    groupDatasetPollList.value = [];
    groupDatasetUploadList.value = [];
    groupPublishingList.value = [];
    groupZipImportPollingIds.value = [];
  };

  // 选择逻辑
  const groupCanDelete: Ref<boolean> = ref(true);
  const groupSelectedKeys: Ref<Array<any>> = ref([]);
  const groupSelectedRows: Ref<Array<any>> = ref([]);

  const groupSelectionChange = ({ keys, rows }) => {
    groupSelectedKeys.value = keys;
    groupSelectedRows.value = rows;
    groupCanDelete.value = keys.length <= 0;
  };

  const onGroupPageChange = () => {
    clearGroupSelectedRowKeys();
  };

  // 表格操作
  const getTableActions = (record: Recordable) => {
    if (record.dataType === 7) {
      return [
        {
          icon: 'ant-design:file-add-outlined',
          tooltip: '导入 Apollo Record',
          onClick: openRecordImport.bind(null, record),
          disabled: record.status === 3002,
        },
      ];
    }
    if (record.dataType === 5) {
      return [
        {
          icon: 'ant-design:info-circle-outlined',
          tooltip: '详情',
          onClick: goPointCloudDetail.bind(null, record),
          disabled: record.status === 1,
        },
        {
          icon: 'ant-design:file-add-outlined',
          tooltip: '导入 PCD',
          // 点云导入：先弹出上传方式弹窗（与图片流程一致），再选择文件
          onClick: handleUploadModal.bind(null, record, dataTypeCodeMap.POINT_CLOUD),
        },
        {
          icon: 'ant-design:highlight-outlined',
          tooltip: '标注',
          // 点云人工标注入口：当前阶段提供 3D Box 前端仿真，不向后端持久化结果。
          onClick: goPointCloudAnnotate.bind(null, record),
          disabled: record.status === 1 || !record.fileCount,
        },
      ];
    }
    if (record.dataType === 6) {
      return [
        {
          icon: 'ant-design:info-circle-outlined',
          tooltip: '详情',
          // 将独立视频数据集的 dataType 明确传给详情页，用于切换到“视频列表”选项卡。
          onClick: goVideoDetail.bind(null, record),
          disabled: record.status === 1,
        },
        {
          icon: 'ant-design:file-add-outlined',
          tooltip: '上传视频',
          // 视频导入：先弹出上传方式弹窗（与图片流程一致），再选择文件
          onClick: handleUploadModal.bind(null, record, dataTypeCodeMap.VIDEO),
        },
        {
          icon: 'ant-design:highlight-outlined',
          tooltip: '标注',
          onClick: goVideoAnnotate.bind(null, record),
          disabled: record.status === 1,
        },
      ];
    }
    return [
      {
        icon: 'ant-design:info-circle-outlined',
        tooltip: '详情',
        onClick: goDetail.bind(null, record),
        disabled: record.status === 1,
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
          // record.annotateType === 103 ||
          record.status === 103 ||
          record.status === 403 ||
          record.haveImage,
      },
      {
        icon: 'ant-design:check-outlined',
        tooltip: '保存数据集版本',
        onClick: saveVersion.bind(null, record),
        ifShow: record.status === 105,
        disabled: record.status === 1 || record.module === 1 || record.status === 103,
      },
      {
        icon: 'ant-design:check-circle-outlined',
        tooltip: '数据集发布',
        onClick: datasetPublish.bind(null, record),
        ifShow: record.status === 105,
        disabled: record.status === 1 || record.module === 1 || record.status === 103,
      },
    ];
  };

  const getDropdownActions = (record: Recordable) => {
    if (record.dataType === 5 || record.dataType === 6 || record.dataType === 7) {
      return [];
    }
    return [
      {
        icon: 'clarity:note-edit-line',
        tooltip: '编辑',
        label: '编辑',
        onClick: handleEdit.bind(null, record),
        disabled:
          record.status === 1 ||
          record.module === 1 ||
          record.status === 302 ||
          record.status === 103,
      },
      {
        icon: 'ant-design:tool-outlined',
        tooltip: '数据增强',
        label: '数据增强',
        onClick: handleEnhance.bind(null, record),
        ifShow: record.status === 105,
        disabled: record.status === 1 || record.module === 1 || record.status === 103,
      },
      // {
      //   icon: 'ant-design:history-outlined',
      //   tooltip: '历史版本',
      //   label: '历史版本',
      //   onClick: goHistory.bind(null, record),
      //   ifShow: record.currentVersionName !== null,
      //   disabled: record.status === 1 || record.module === 1 || record.status === 103,
      // },
      {
        icon: 'ant-design:calendar-outlined',
        tooltip: '数据集事件列表',
        label: '数据集事件列表',
        onClick: handleDatasetEventModelOpen.bind(null, record),
      },
      {
        icon: 'ant-design:file-image-outlined',
        tooltip: '导出已标注图片',
        label: '导出已标注图片',
        onClick: handleOriginExport.bind(null, record),
        disabled: record.status === 1 || record.status === 103,
      },
      {
        icon: 'ant-design:file-excel-outlined',
        tooltip: '导出未标注图片',
        label: '导出未标注图片',
        onClick: handleOriginUnannotatedExport.bind(null, record),
        disabled: record.status === 1 || record.status === 103,
      },
    ];
  };

  // 数据集操作函数
  const handleCreate = () => {
    openDatasetModal(true, {
      isUpdate: false,
      datasetGroupId: activeGroupId.value,
    });
  };

  const pcdFileInput = ref<HTMLInputElement | null>(null);
  const videoFileInput = ref<HTMLInputElement | null>(null);
  const selectedPcDataset = ref<Recordable | null>(null);
  const selectedVideoDataset = ref<Recordable | null>(null);
  const datasetCode = (record: Recordable) =>
    record.datasetCode ||
    `${
      record.dataType === 5
        ? 'pc'
        : record.dataType === 6
        ? 'video'
        : record.dataType === 7
        ? 'multi'
        : record.dataType === 1
        ? 'video'
        : 'image'
    }_${record.id}`;
  const datasetRowKey = (record: Recordable) => datasetCode(record);

  const normalDatasetIdFromKey = (key: unknown): number | undefined => {
    const match = /^(?:image|video)_(\d+)$/.exec(String(key));
    if (!match) {
      return undefined;
    }
    const id = Number(match[1]);
    return Number.isSafeInteger(id) ? id : undefined;
  };

  const selectedNormalDatasetIds = () =>
    Array.from(
      new Set(
        groupSelectedKeys.value
          .map(normalDatasetIdFromKey)
          .filter((id): id is number => id !== undefined),
      ),
    );

  const hasSelectedSpecialDataset = () =>
    groupSelectedRows.value.some(
      (row) => row.dataType === 5 || row.dataType === 6 || row.dataType === 7,
    );

  const selectPcdFiles = (record: Recordable) => {
    selectedPcDataset.value = record;
    pcdFileInput.value?.click();
  };

  const selectVideoFiles = (record: Recordable) => {
    selectedVideoDataset.value = record;
    videoFileInput.value?.click();
  };

  const handlePcdFileChange = async (event: Event) => {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files || []);
    const dataset = selectedPcDataset.value;
    input.value = '';
    if (!dataset || files.length === 0) {
      return;
    }
    let transferTaskId = 0;
    try {
      transferTaskId = await createImportTransferTask({
        taskName: `数据集“${dataset.name || '点云数据集'}”导入任务`,
        datasetType: 'POINT_CLOUD',
        datasetId: dataset.id,
        sourceType: 'LOCAL',
        totalFiles: files.length,
        totalBytes: files.reduce((total, file) => total + file.size, 0),
      });
      for (const file of files) {
        if (!file.name.toLowerCase().endsWith('.pcd')) {
          throw new Error(`${file.name} 不是 PCD 文件`);
        }
        const upload = await maHttp.post(
          {
            url: `pointcloud/datasets/${dataset.id}/files/upload-url`,
            params: { name: file.name },
            headers: {},
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        );
        const response = await fetch(upload.uploadUrl, { method: 'PUT', body: file });
        if (!response.ok) {
          throw new Error(`${file.name} 上传到对象存储失败`);
        }
        await maHttp.post(
          {
            url: `pointcloud/datasets/${dataset.id}/files/commit`,
            params: { name: file.name, objectKey: upload.objectKey },
            headers: {},
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        );
        await updateImportTransferProgress(transferTaskId!, {
          stage: 'UPLOAD',
          progress: Math.round(((files.indexOf(file) + 1) / files.length) * 100),
          successFiles: files.indexOf(file) + 1,
          totalFiles: files.length,
          message: `${file.name} 上传完成`,
        });
      }
      await completeImportTransferTask(transferTaskId!);
      createMessage.success('PCD 文件导入完成');
      await reloadDatasets();
    } catch (error) {
      if (transferTaskId) {
        try {
          await failImportTransferTask(
            transferTaskId!,
            error instanceof Error ? error.message : 'PCD 文件上传失败',
          );
        } catch (_) {
          // 任务状态上报失败不应覆盖原始上传错误。
        }
      }
      createMessage.error(error instanceof Error ? error.message : 'PCD 文件导入失败');
    } finally {
      selectedPcDataset.value = null;
    }
  };

  const handleVideoFileChange = async (event: Event) => {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files || []);
    const dataset = selectedVideoDataset.value;
    input.value = '';
    if (!dataset || files.length === 0) {
      return;
    }
    let transferTaskId = 0;
    try {
      transferTaskId = await createImportTransferTask({
        taskName: `数据集“${dataset.name || '视频数据集'}”导入任务`,
        datasetType: 'VIDEO',
        datasetId: dataset.id,
        sourceType: 'LOCAL',
        totalFiles: files.length,
        totalBytes: files.reduce((total, file) => total + file.size, 0),
      });
      for (const file of files) {
        const upload = await maHttp.post(
          {
            url: `video/datasets/${dataset.id}/files/upload-url`,
            params: { name: file.name },
            headers: {},
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        );
        const response = await fetch(upload.uploadUrl, { method: 'PUT', body: file });
        if (!response.ok) {
          throw new Error(`${file.name} 上传到对象存储失败`);
        }
        await maHttp.post(
          {
            url: `video/datasets/${dataset.id}/files/commit`,
            params: { name: file.name, objectKey: upload.objectKey },
            headers: {},
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        );
        await updateImportTransferProgress(transferTaskId!, {
          stage: 'UPLOAD',
          progress: Math.round(((files.indexOf(file) + 1) / files.length) * 100),
          successFiles: files.indexOf(file) + 1,
          totalFiles: files.length,
          message: `${file.name} 上传完成`,
        });
      }
      await completeImportTransferTask(transferTaskId!);
      createMessage.success('视频上传完成');
      await reloadDatasets();
    } catch (error) {
      if (transferTaskId) {
        try {
          await failImportTransferTask(
            transferTaskId!,
            error instanceof Error ? error.message : '视频上传失败',
          );
        } catch (_) {
          // 任务状态上报失败不应覆盖原始上传错误。
        }
      }
      createMessage.error(error instanceof Error ? error.message : '视频上传失败');
    } finally {
      selectedVideoDataset.value = null;
    }
  };

  const handleEdit = (record: Recordable) => {
    openDatasetModal(true, { record, isUpdate: true });
  };

  const goDetail = (record: Recordable) => {
    go(`/maData/fileDetail/${record.id}/${record.name}/${record.module}/${record.annotateType}`);
  };

  // 视频数据集详情按钮：通过查询参数传递 dataType，避免误用 module 参数判断数据类型。
  const goVideoDetail = (record: Recordable) => {
    go(
      `/maData/fileDetail/${record.id}/${record.name}/${record.module}/${record.annotateType}?dataType=${record.dataType}`,
    );
  };

  // 点云数据集详情按钮：跳转到点云详情页（文件列表 + 信息总览）
  // 路由由数据库 menu 表 id=1226 记录驱动（path=pointCloudDetail/:id/:name）
  const goPointCloudDetail = (record: Recordable) => {
    go(`/maData/pointCloudDetail/${record.id}/${record.name}`);
  };

  // 视频数据集标注按钮：跳转到视频标注工作台（路由由 menu 表驱动 path=videoAnnotate/:id/:name）
  const goVideoAnnotate = (record: Recordable) => {
    go(`/maData/videoAnnotate/${record.id}/${record.name}`);
  };

  // 点云 3D Box 标注页：路由参数只携带数据集身份，文件列表及 MinIO 对象信息由页面按接口加载。
  const goPointCloudAnnotate = (record: Recordable) => {
    go(`/maData/pointCloudAnnotate/${record.id}/${record.name}`);
  };

  const handleEnhance = (record: Recordable) => {
    openEnhanceModel(true, { record });
  };

  const saveVersion = (record: Recordable) => {
    openSaveVersionModel(true, { record });
  };

  const datasetPublish = async (record: Recordable) => {
    openDatasetPublishModel(true, { record });
  };

  const goLabel = async (record: Recordable) => {
    const prefix = record.annotateType === 103 ? 'segmentation' : 'annotate';
    const firstImgId = await queryFirstImg(record.id);
    await router.push({
      path: `/maData/${prefix}/${record.id}/${record.name}`,
      state: { imgId: firstImgId },
    });
  };

  async function deleteVersion(record: Recordable) {
    try {
      // 首先检查版本是否被绑定
      const bindingInfoList = await checkDatasetVersionBindRelationWithGenerationApi(record.id);
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
      await refreshExpandedVersionsIfOpen(record.datasetId);
    } catch (error) {
      createMessage.error('删除版本失败！');
      console.error('删除版本失败:', error);
    }
  }

  const isGuidedAutoLabel = ref(null);

  const goAutoLabel = async (record: Recordable) => {
    isGuidedAutoLabel.value = record.isGuided;
    await nextTick(() => {
      if (record.isGuided) {
        openAutoLabelModelGuided(true, { id: record.id, annotateType: record.annotateType });
      } else {
        openAutoLabelModel(true, { id: record.id, annotateType: record.annotateType });
      }
    });
  };

  const handleDatasetEventModelOpen = (record: Recordable) => {
    openDatasetEvent(true, { datasetId: record.id });
  };

  // 删除数据集
  const handleGroupDatasetDelete = async () => {
    if (hasSelectedSpecialDataset()) {
      createMessage.warning('点云和独立视频数据集的删除功能暂未开放，请仅选择普通数据集。');
      return;
    }

    let selectedDatasetIds = selectedNormalDatasetIds();
    if (selectedDatasetIds.length === 0) {
      createMessage.warning('未获取到可删除的数据集，请重新选择图片或视频数据集。');
      return;
    }

    const bindingInfoList = await checkDatasetBindRelationWithGenerationApi(selectedDatasetIds);
    if (bindingInfoList && bindingInfoList.length > 0) {
      notification.warning({
        key: 'dataset-deletion-blocked-detailed-group',
        message: '数据集删除被阻止',
        description: h('div', null, [
          h('p', '以下数据集因被任务绑定而无法删除，请先处理：'),
          h(
            'ul',
            { style: { marginTop: '8px', paddingLeft: '20px', listStyleType: 'disc' } },
            bindingInfoList.map((info) => {
              const taskType = info.isGuided ? '引导式训练任务' : '标准化训练任务';
              const descriptionText = `数据集 "${info.datasetName}" 中存在版本被${taskType} "${info.modelGenerationName}" 绑定`;
              return h(
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

    const response = await batchGet(selectedDatasetIds);
    const datasets = Object.values(response);
    const publicDatasets = datasets.filter((dataset) => dataset.isPublic === 1);

    if (publicDatasets.length > 0) {
      const publicDatasetIds = publicDatasets.map((dataset) => dataset.id).join(', ');
      createMessage.warning(
        `数据集(${publicDatasetIds})已有公开版本不能删除，需要先联系管理员取消公开`,
      );

      const publicDatasetIdSet = new Set(publicDatasets.map((dataset) => Number(dataset.id)));
      selectedDatasetIds = selectedDatasetIds.filter((id) => !publicDatasetIdSet.has(id));
      groupSelectedRows.value = groupSelectedRows.value.filter(
        (row) => !publicDatasetIdSet.has(Number(row.id)),
      );
      groupSelectedKeys.value = groupSelectedKeys.value.filter((key) => {
        const id = normalDatasetIdFromKey(key);
        return id === undefined || !publicDatasetIdSet.has(id);
      });
      groupCanDelete.value = groupSelectedKeys.value.length <= 0;

      if (selectedDatasetIds.length === 0) {
        clearGroupSelectedRowKeys();
        return;
      }
    }

    const modal = Modal.confirm({
      title: () => '确认删除选中的' + selectedDatasetIds.length + '条数据?',
      maskClosable: true,
      icon: () => createVNode(ExclamationCircleOutlined),
      okText: () => '确定',
      okType: 'danger',
      cancelText: () => '取消',
      onOk() {
        modal.destroy();
        const ids = selectedDatasetIds;
        maHttp
          .delete(
            { url: 'datasets', data: { ids }, headers: {} },
            { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
          )
          .then(() => {
            createMessage.success('删除成功！');
            clearGroupSelectedRowKeys();
            reloadDatasets();
          })
          .catch((e) => {
            console.error(e);
            createMessage.error('删除失败！');
            reloadDatasets();
            clearGroupSelectedRowKeys();
          });
      },
      onCancel() {
        clearGroupSelectedRowKeys();
      },
    });
  };

  // 上传相关
  const handleUpload = (record: Recordable, dataType) => {
    // 点云(5)：直接走 PCD 文件选择；独立视频(6)：直接走视频文件选择
    if (dataType === dataTypeCodeMap.POINT_CLOUD) {
      selectPcdFiles(record);
      return;
    }
    if (dataType === dataTypeCodeMap.VIDEO && record.dataType === 6) {
      selectVideoFiles(record);
      return;
    }
    // 图片等其他类型：走原 upload-datafile.vue 流程
    importRow.value = record;
    importRow.value.dataType = dataType;
    uploadDataFileVisible.value = true;
  };

  const handleUploadZip = (record: Recordable, labelType) => {
    importRow.value = record;
    importRow.value.labelType = labelType;
    uploadZipFileVisible.value = true;
  };

  const handleCloudUpload = async (record, dataType, dataRepo) => {
    await importDatasetFromDataRepo(dataRepo, record.id, dataType);
    createMessage.success('导入任务下发成功');
    await reloadDatasets();
    await refreshAllExpandedVersions();
  };

  const handleUploadModal = (record: Recordable, dataType) => {
    openDatasetUploadModel(true, { record, dataType });
  };

  const openRecordImport = (record: Recordable) => {
    recordImportRecord.value = record;
    recordImportFile.value = null;
    recordImportProgress.value = 0;
    recordImportVisible.value = true;
  };

  const closeRecordImport = () => {
    if (recordImportUploading.value) return;
    recordImportVisible.value = false;
    recordImportRecord.value = null;
    recordImportFile.value = null;
    recordImportProgress.value = 0;
  };

  const chooseRecordFile = () => {
    recordFileInput.value?.click();
  };

  const handleRecordFileChange = (event: Event) => {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (!file.name.toLowerCase().endsWith('.record')) {
      createMessage.error('只能选择 .record 文件');
      return;
    }
    recordImportFile.value = file;
  };

  const formatFileSize = (size: number) => {
    if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
    if (size >= 1024 * 1024 * 1024) return `${(size / 1024 / 1024 / 1024).toFixed(2)} GB`;
    return `${(size / 1024 / 1024).toFixed(1)} MB`;
  };

  const submitRecordImport = async () => {
    const record = recordImportRecord.value;
    const file = recordImportFile.value;
    if (!record || !file) {
      createMessage.warning('请先选择 .record 文件');
      return;
    }
    recordImportUploading.value = true;
    try {
      const upload = await maHttp.post(
        {
          url: `multi/datasets/${record.id}/record-imports/upload-url`,
          params: { name: file.name },
          headers: {},
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      await uploadRecordWithProgress(upload.uploadUrl, file);
      await maHttp.post(
        {
          url: `multi/datasets/${record.id}/record-imports/${upload.taskId}/commit`,
          headers: {},
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      createMessage.success('Record 已提交，后台开始导入');
      recordImportVisible.value = false;
      recordImportRecord.value = null;
      recordImportFile.value = null;
      recordImportProgress.value = 0;
      await reloadDatasets();
    } catch (error) {
      createMessage.error(error instanceof Error ? error.message : 'Record 导入失败');
    } finally {
      recordImportUploading.value = false;
    }
  };

  const uploadRecordWithProgress = (uploadUrl: string, file: File) =>
    new Promise<void>((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open('PUT', uploadUrl, true);
      xhr.setRequestHeader('Content-Type', 'application/octet-stream');
      xhr.timeout = 24 * 60 * 60 * 1000;
      xhr.upload.onprogress = (event) => {
        if (event.lengthComputable) {
          recordImportProgress.value = Math.min(99, Math.round((event.loaded / event.total) * 100));
        }
      };
      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          recordImportProgress.value = 100;
          resolve();
        } else {
          reject(new Error(`Record 文件上传失败（HTTP ${xhr.status}）`));
        }
      };
      xhr.onerror = () =>
        reject(new Error('Record 文件上传到对象存储失败，请检查 MinIO 网络或跨域配置'));
      xhr.ontimeout = () => reject(new Error('Record 文件上传超时，请检查网络后重试'));
      xhr.onabort = () => reject(new Error('Record 文件上传已取消'));
      xhr.send(file);
    });

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
          updateGroupTableDataRecord(importRow.value.id, dataset);
        }
      }
      await reloadDatasets();
      await refreshAllExpandedVersions();
    }
  };

  const startZipImportPolling = (datasetId: number) => {
    if (!groupZipImportPollingIds.value.includes(datasetId)) {
      groupZipImportPollingIds.value.push(datasetId);
    }
    clearInterval(groupZipImportTimer.value);
    groupZipImportTimer.value = setInterval(pollZipImportStatus, 2 * 1000);
    pollZipImportStatus();
  };

  const pollZipImportStatus = async () => {
    if (isGroupZipImportPolling.value || groupZipImportPollingIds.value.length === 0) {
      if (groupZipImportPollingIds.value.length === 0) clearInterval(groupZipImportTimer.value);
      return;
    }

    isGroupZipImportPolling.value = true;
    try {
      const datasetIds = [...groupZipImportPollingIds.value];
      const result = await queryDatasetStatus({ datasetIds });
      const completedIds = datasetIds.filter((id) => {
        const status = result?.[id]?.status;
        return status !== undefined && Number(status) !== 403;
      });

      if (completedIds.length === 0) return;

      groupZipImportPollingIds.value = groupZipImportPollingIds.value.filter(
        (id) => !completedIds.includes(id),
      );
      await reloadDatasets();
      await refreshAllExpandedVersions();
    } catch (error) {
      console.error('轮询 ZIP 导入状态失败:', error);
    } finally {
      isGroupZipImportPolling.value = false;
      if (groupZipImportPollingIds.value.length === 0) clearInterval(groupZipImportTimer.value);
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
          updateGroupTableDataRecord(importRow.value.id, dataset);
        }
      }
      await reloadDatasets();
      await refreshAllExpandedVersions();
    }
  };

  const hideUploadZipFile = (): void => {
    uploadZipFileVisible.value = !uploadZipFileVisible.value;
    resumeButtonVisible.value = !resumeButtonVisible.value;
  };

  // 成功回调函数
  const safeClearGroupSelectedRowKeys = () => {
    try {
      clearGroupSelectedRowKeys();
    } catch (error) {
      console.warn('清理数据集组表格选中状态失败:', error);
    }
  };

  const handleSuccess = async () => {
    safeClearGroupSelectedRowKeys();
    try {
      await reloadDatasets();
      // 刷新所有展开的版本列表
      await refreshAllExpandedVersions();
    } catch (error) {
      console.warn('表格刷新失败:', error);
      setTimeout(async () => {
        try {
          await reloadDatasets();
          await refreshAllExpandedVersions();
        } catch (retryError) {
          console.error('重试刷新失败:', retryError);
        }
      }, 100);
    }
  };

  const handleEnhanceSuccess = async () => {
    safeClearGroupSelectedRowKeys();
    try {
      await reloadDatasets();
      // 刷新所有展开的版本列表
      await refreshAllExpandedVersions();
    } catch (error) {
      console.warn('表格刷新失败:', error);
      setTimeout(async () => {
        try {
          await reloadDatasets();
          await refreshAllExpandedVersions();
        } catch (retryError) {
          console.error('重试刷新失败:', retryError);
        }
      }, 100);
    }
  };

  const handleAutoLabelModalSuccess = async () => {
    safeClearGroupSelectedRowKeys();
    try {
      await reloadDatasets();
      // 刷新所有展开的版本列表
      await refreshAllExpandedVersions();
    } catch (error) {
      console.warn('表格刷新失败:', error);
      setTimeout(async () => {
        try {
          await reloadDatasets();
          await refreshAllExpandedVersions();
        } catch (retryError) {
          console.error('重试刷新失败:', retryError);
        }
      }, 100);
    }
  };

  const handleDatasetPublishModalSuccess = async () => {
    safeClearGroupSelectedRowKeys();
    try {
      await reloadDatasets();
      // 刷新所有展开的版本列表，因为发布会创建新版本
      await refreshAllExpandedVersions();
    } catch (error) {
      console.warn('表格刷新失败:', error);
      setTimeout(async () => {
        try {
          await reloadDatasets();
          await refreshAllExpandedVersions();
        } catch (retryError) {
          console.error('重试刷新失败:', retryError);
        }
      }, 100);
    }
  };

  const handleDatasetModalSuccess = async (isEdit) => {
    safeClearGroupSelectedRowKeys();
    if (isEdit) {
      createMessage.success('数据集修改成功');
    } else {
      createMessage.success('数据集新增成功');
    }
    try {
      await reloadDatasets();
      // 如果是编辑操作，可能会影响版本信息，需要刷新展开的版本列表
      if (isEdit) {
        await refreshAllExpandedVersions();
      }
    } catch (error) {
      console.warn('表格刷新失败:', error);
      setTimeout(async () => {
        try {
          await reloadDatasets();
          if (isEdit) {
            await refreshAllExpandedVersions();
          }
        } catch (retryError) {
          console.error('重试刷新失败:', retryError);
        }
      }, 100);
    }
  };

  // 版本展开逻辑
  const expandedVersions = reactive<
    Record<string, { loading: boolean; data: any[]; pagination: any }>
  >({});

  const versionTableColumns = [
    { title: '版本号', dataIndex: 'versionName', width: 100, align: 'center' },
    {
      title: '标签',
      dataIndex: 'labelCountMap',
      width: 220,
      align: 'center',
      customRender: ({ text: labelCountMap, record }) => {
        if (!labelCountMap || Object.keys(labelCountMap).length === 0) {
          return h('span', { class: 'no-tags' }, '-');
        }

        const tags = Object.keys(labelCountMap);
        const visibleTags = tags.slice(0, 2);
        const hasMoreTags = tags.length > 2;

        return h('div', { class: 'tags-container' }, [
          // 显示前两个标签（不带数量）
          ...visibleTags.map((tag) =>
            h(
              ATag,
              {
                key: tag,
                color: tagColor(tag),
                class: 'tag-item',
              },
              () => tag,
            ),
          ),

          hasMoreTags && h('span', { class: 'tags-ellipsis' }, '...'),

          h(
            APopover,
            {
              placement: 'bottomLeft',
              trigger: 'hover',
              overlayClassName: 'tags-popover-custom',
            },
            {
              content: () =>
                h('div', { class: 'popover-tags-content' }, [
                  h('div', { class: 'popover-title' }, '所有标签及数量：'),
                  h('div', { class: 'popover-stats' }, [
                    h('div', { class: 'stat-item' }, [
                      h('span', { class: 'stat-label' }, '总图片数：'),
                      h('span', { class: 'stat-value' }, `${record.imageCount || 0} 张`),
                    ]),
                    h('div', { class: 'stat-divider' }),
                    h('div', { class: 'stat-item' }, [
                      h('span', { class: 'stat-label' }, '标签种类：'),
                      h('span', { class: 'stat-value' }, `${tags.length} 种`),
                    ]),
                  ]),
                  h(
                    'div',
                    { class: 'popover-tags-list' },
                    tags.map((tag) =>
                      h(
                        'div',
                        {
                          key: tag,
                          class: 'popover-tag-item-wrapper',
                        },
                        [
                          h(
                            ATag,
                            {
                              color: tagColor(tag),
                              class: 'popover-tag-item',
                            },
                            () => tag,
                          ),
                          h('span', { class: 'tag-count' }, `${labelCountMap[tag]} 个标注`),
                        ],
                      ),
                    ),
                  ),
                ]),
              default: () =>
                h(
                  'span',
                  {
                    class: 'show-all-tags-trigger',
                  },
                  '查看详情',
                ),
            },
          ),
        ]);
      },
    },
    {
      title: '是否公开',
      dataIndex: 'isPublic',
      width: 80,
      align: 'center',
      customRender: ({ record }) => {
        return h(
          ATag,
          {
            color: !record.isPublic ? 'gray' : 'green',
          },
          !record.isPublic ? '否' : '是',
        );
      },
    },
    {
      title: '状态',
      dataIndex: 'dataConversion',
      width: 120,
      align: 'center',
      customRender: ({ text }) => {
        return h('div', { class: 'status-cell' }, [
          h('span', {
            class: {
              'status-dot': true,
              'active-dot': text === 1,
              'inactive-dot': text !== 1,
            },
          }),
          h(
            'span',
            {
              class: 'status-text',
            },
            text === 1 ? '可用' : '不可用',
          ),
        ]);
      },
    },
    {
      title: '图片数量',
      dataIndex: 'imageCount',
      customRender: ({ text }) => h('span', {}, `${text || 0} 张`),
      width: 100,
      align: 'center',
    },
    {
      title: '是否当前',
      dataIndex: 'isCurrent',
      customRender: ({ text }) =>
        h(ATag, { color: text ? 'green' : '' }, () => (text ? '是' : '否')),
      width: 80,
      align: 'center',
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      customRender: ({ text }) => parseDate(text),
      width: 150,
      align: 'center',
    },
    { title: '备注', dataIndex: 'versionNote', align: 'center', width: 75 },
    {
      title: '操作',
      key: 'action',
      width: 150,
      align: 'center',
      slots: { customRender: 'action' },
    },
  ];
  const setAsCurrentVersion = async (record: Recordable) => {
    try {
      AModal.confirm({
        title: () => '您是否确认切换数据集版本？',
        maskClosable: true,
        content: () =>
          '注意！切换版本会导致您当前数据集未保存的内容丢失，若需要保留已修改内容,请在切换前保存数据集版本！',
        onOk: async () => {
          await switchVersion(record.datasetId, record.versionName);
          createMessage.success('切换版本成功！');

          // 刷新当前数据集的版本列表
          await fetchVersions(record.datasetId);

          // 刷新主表格
          await reloadDatasets();
        },
      });
    } catch (e) {
      console.error('切换版本失败:', e);
      createMessage.error('切换版本失败！');
    }
  };
  const getVersionActions = (record: Recordable) => [
    {
      icon: 'ant-design:info-circle-outlined',
      tooltip: '详情',
      onClick: goDatasetDetail.bind(null, record),
    },
    {
      icon: 'ant-design:eye-invisible-outlined',
      tooltip: '取消公开',
      popConfirm: {
        title: '是否确认取消公开',
        confirm: cancelPublic.bind(null, record),
      },
      disabled: !hasPermission([RoleEnum.DatasetDetails_Write]) || !isAdmin.value,
      // 只有公开的版本才显示取消公开按钮
      ifShow: !!record.isPublic,
    },
  ];
  const getVersionDropdownActions = (record: Recordable) => [
    {
      icon: 'ant-design:highlight-outlined',
      tooltip: '查看标注',
      label: '查看标注',
      onClick: goVersionLabel.bind(null, record),
    },
    {
      icon: 'ant-design:check-outlined',
      tooltip: '设置为当前版本',
      label: '设置为当前版本',
      onClick: setAsCurrentVersion.bind(null, record),
      ifShow: !record.isCurrent,
    },
    {
      icon: 'ant-design:download-outlined',
      tooltip: '导出',
      label: '导出',
      onClick: goExport.bind(null, record),
      // disabled: !(roles[0].name === '管理人员' || roles[0].name === '管理员'),
    },
    {
      icon: 'ant-design:bars',
      tooltip: '查看标签',
      label: '查看标签',
      onClick: goTags.bind(null, record),
    },
    {
      icon: 'ant-design:delete-outlined',
      tooltip: '删除版本',
      label: '删除版本',
      color: 'error',
      ifShow: !record.isCurrent, // 当前版本不能删除
      popConfirm: {
        title: '确认删除该版本？',
        confirm: deleteVersion.bind(null, record),
        placement: 'top',
      },
    },
  ];

  const goDatasetDetail = (record: Recordable) => {
    openHistoryModal(true, {
      record,
    });
  };

  const goExport = async (record: Recordable) => {
    exportStatusModelVisible.value = true;
    versionToExport = record;
  };

  const confirmExport = async () => {
    isPolling.value = true;

    const stopPolling = (message, isSuccess = false) => {
      clearInterval(timer.value);
      isPolling.value = false;
      versionToExport = {};
      exportStatusModelVisible.value = false;
      if (message) {
        isSuccess ? createMessage.success(message) : createMessage.error(message);
      }
    };

    try {
      let isStarted = await datasetExportNew(versionToExport);
      if (!isStarted) {
        stopPolling('导出失败，请重试！');
        return;
      }

      await recordDatasetDownloadAudit();

      stopPolling('导出任务已提交，可在右上角传输任务中查看进度', true);
    } catch (e) {
      stopPolling('导出任务发起失败，请重试！');
    }
  };

  const cancelExport = () => {
    exportStatusModelVisible.value = false;
    versionToExport = null;
  };

  const datasetExportNew = async (record: Recordable) => {
    let response = await getDatasetVersion(record.datasetId, record.versionName);
    if (response.dataConversion === 1) {
      // 触发导出任务
      const result = await exportDatasetVersion(record.datasetId, record.versionName);
      if (!result) {
        createMessage.error('导出失败，请重试！');
        return false;
      }

      createMessage.info('导出任务已提交，正在生成文件...');

      // 轮询检查导出状态
      const checkInterval = setInterval(async () => {
        try {
          const status = await checkExportStatus(response.id);
          console.log('Export status:', status);

          if (status === 'success') {
            clearInterval(checkInterval);
            // 构建下载URL: versionUrl/format/zipFileName
            const format = response.format || 'YOLO';
            const zipFileName = `${record.datasetId}_${format}_${record.versionName}.zip`;
            const relativePath = `${response.versionUrl}/${format}/${zipFileName}`;
            const zipUrl = `${minioBaseUrl}/${relativePath}`;

            console.log('Download URL:', zipUrl);
            createMessage.success('导出完成，开始下载...');

            downloadByUrl({
              url: zipUrl,
              target: '_self',
            });
          } else if (status === 'failed') {
            clearInterval(checkInterval);
            createMessage.error('导出失败，请重试');
          }
        } catch (error) {
          console.error('Check export status error:', error);
        }
      }, 2000); // 每2秒检查一次

      return true;
    } else if (response.dataConversion === 0) {
      createMessage.warning('数据集相关文件还在拷贝中，请稍作等候');
    } else {
      createMessage.warning('数据集发布还未成功，请稍作等候');
    }
  };

  // 从origin导出已标注图片 - 打开确认弹窗
  const recordDatasetDownloadAudit = async () => {
    try {
      await recordDatasetDownload();
    } catch (error) {
      console.warn('record dataset download audit failed:', error);
    }
  };

  const handleOriginExport = (record: Recordable) => {
    originExportRecord = record;
    originExportModelVisible.value = true;
  };

  // 确认从origin导出
  const confirmOriginExport = async () => {
    isOriginExportPolling.value = true;
    const datasetId = originExportRecord.id;

    const stopOriginExportPolling = (message, isSuccess = false) => {
      clearInterval(originExportTimer.value);
      isOriginExportPolling.value = false;
      originExportRecord = {};
      originExportModelVisible.value = false;
      if (message) {
        isSuccess ? createMessage.success(message) : createMessage.error(message);
      }
    };

    try {
      const result = await exportDatasetFromOrigin(datasetId);
      console.log('exportDatasetFromOrigin result:', result);
      if (!result) {
        stopOriginExportPolling('导出失败，请重试！');
        return;
      }

      originExportTimer.value = setInterval(async () => {
        try {
          const status = await checkOriginExportStatus(datasetId);
          console.log('Export status:', status);
          if (status === 'success') {
            // 获取下载 URL
            const urlData = await getOriginExportUrl(datasetId);
            const relativePath = urlData;
            const zipUrl = `${minioBaseUrl}/${relativePath}`;
            console.log('Download URL:', zipUrl);
            stopOriginExportPolling('导出完成，开始下载...', true);
            downloadByUrl({
              url: zipUrl,
              target: '_self',
            });
          } else if (status === 'failed') {
            stopOriginExportPolling('导出失败，请重试');
          }
        } catch (error) {
          console.error('Check export status error:', error);
        }
      }, 2000);
    } catch (error) {
      console.error('导出失败:', error);
      stopOriginExportPolling('导出失败，请重试');
    }
  };

  // 取消从origin导出
  const cancelOriginExport = () => {
    clearInterval(originExportTimer.value);
    isOriginExportPolling.value = false;
    originExportRecord = {};
    originExportModelVisible.value = false;
  };

  // 从origin导出未标注图片 - 打开确认弹窗
  const handleOriginUnannotatedExport = (record: Recordable) => {
    originUnannotatedExportRecord = record;
    originUnannotatedExportModelVisible.value = true;
  };

  // 确认从origin导出未标注图片
  const confirmOriginUnannotatedExport = async () => {
    isOriginUnannotatedExportPolling.value = true;
    const datasetId = originUnannotatedExportRecord.id;

    const stopOriginUnannotatedExportPolling = (message, isSuccess = false) => {
      clearInterval(originUnannotatedExportTimer.value);
      isOriginUnannotatedExportPolling.value = false;
      originUnannotatedExportRecord = {};
      originUnannotatedExportModelVisible.value = false;
      if (message) {
        isSuccess ? createMessage.success(message) : createMessage.error(message);
      }
    };

    try {
      const result = await exportUnannotatedDatasetFromOrigin(datasetId);
      console.log('exportUnannotatedDatasetFromOrigin result:', result);
      if (!result) {
        stopOriginUnannotatedExportPolling('导出失败，请重试！');
        return;
      }

      originUnannotatedExportTimer.value = setInterval(async () => {
        try {
          const status = await checkOriginUnannotatedExportStatus(datasetId);
          console.log('Unannotated export status:', status);
          if (status === 'success') {
            // 获取下载 URL
            const urlData = await getOriginUnannotatedExportUrl(datasetId);
            const relativePath = urlData;
            const zipUrl = `${minioBaseUrl}/${relativePath}`;
            console.log('Download URL:', zipUrl);
            stopOriginUnannotatedExportPolling('导出完成，开始下载...', true);
            downloadByUrl({
              url: zipUrl,
              target: '_self',
            });
          } else if (status === 'failed') {
            stopOriginUnannotatedExportPolling('导出失败，请重试');
          }
        } catch (error) {
          console.error('Check unannotated export status error:', error);
        }
      }, 2000);
    } catch (error) {
      console.error('导出未标注图片失败:', error);
      stopOriginUnannotatedExportPolling('导出失败，请重试');
    }
  };

  // 取消从origin导出未标注图片
  const cancelOriginUnannotatedExport = () => {
    clearInterval(originUnannotatedExportTimer.value);
    isOriginUnannotatedExportPolling.value = false;
    originUnannotatedExportRecord = {};
    originUnannotatedExportModelVisible.value = false;
  };

  const cancelPublic = async (record: Recordable) => {
    try {
      const isSuccess = await cancelPublicDatasetVersion(record.datasetId, record.versionName);
      if (isSuccess) {
        createMessage.success('取消发布成功');

        // 刷新版本列表
        const datasetId = record.datasetId;
        if (expandedVersions[datasetId]) {
          await fetchVersions(datasetId);
        }

        // 刷新主表格
        await reloadDatasets();
        await refreshAllExpandedVersions();
      } else {
        createMessage.error('取消发布失败');
      }
    } catch (error) {
      console.error('取消发布操作失败:', error);
      createMessage.error('取消发布失败，请重试');
    }
  };

  const goTags = async (record: Recordable) => {
    currentRow.value = record;
    labelInfoModalVisible.value = true;
  };

  // 查看版本标注
  const goVersionLabel = async (record: Recordable) => {
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
  };

  const handleDatasetExpand = async (expanded: boolean, record: any) => {
    if (record.dataType === 5 || record.dataType === 6 || record.dataType === 7) {
      return;
    }
    const datasetId = record.id;
    if (expanded) {
      // 确保展开状态存在
      if (!expandedVersions[datasetId]) {
        expandedVersions[datasetId] = {
          loading: true,
          data: [],
          pagination: { current: 1, pageSize: 5, total: 0 },
        };
      } else {
        // 如果已存在，也设置为loading状态准备刷新
        expandedVersions[datasetId].loading = true;
      }

      // 每次展开都刷新版本列表，确保数据是最新的
      await fetchVersions(datasetId);
    }
  };

  const fetchVersions = async (datasetId: number) => {
    const state = expandedVersions[datasetId];
    state.loading = true;
    try {
      const result = await fetchVersionsAPI(
        datasetId,
        state.pagination.current as number,
        state.pagination.pageSize as number,
        true,
      );
      state.data = result.items;
      state.pagination.total = result.total;
    } catch (e) {
      createMessage.error(`获取数据集 ${datasetId} 的版本列表失败`);
    } finally {
      state.loading = false;
    }
  };

  const handleVersionPageChange = (
    datasetId: number,
    pagination: { current: number; pageSize: number },
  ) => {
    expandedVersions[datasetId].pagination.current = pagination.current;
    expandedVersions[datasetId].pagination.pageSize = pagination.pageSize;
    fetchVersions(datasetId);
  };

  const tagColor = (tag: string) => {
    const colors = ['blue', 'green', 'orange', 'purple', 'cyan', 'magenta'];
    let hash = 0;
    for (let i = 0; i < tag.length; i++) {
      hash = tag.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash % colors.length)];
  };

  const getRowClassName = (record: any) => {
    return highlightedRowId.value === record.id ? 'highlighted-row' : '';
  };

  // 工具函数
  const parseDate = (dateStr) => {
    return new Date(dateStr).toLocaleString();
  };

  // 监听刷新触发器
  watch(
    () => props.refreshTrigger,
    (newVal) => {
      if (newVal && newVal > 0) {
        // fetchDatasetGroups();
      }
    },
  );

  onUnmounted(() => {
    clearGroupPolling();
    clearInterval(timer.value);
  });
</script>

<style lang="less" scoped>
  // 复用原有样式
  #resume-button {
    position: fixed;
    right: 32px;
    top: 235px;
    z-index: 2147483640;
    display: flex;
    flex-direction: column;
    cursor: pointer;
  }

  .group-title-link {
    display: flex;
    align-items: center;
    font-size: 18px !important;
    font-weight: 500;
    color: #adafb6;

    &:hover {
      color: #40a9ff;
      text-decoration: underline;
    }

    .anticon {
      margin-right: 6px;
    }
  }

  .dataset-group-container {
    .card-list-wrapper {
      padding: 16px;
    }
  }

  .version-table-wrapper {
    // padding: 12px;
  }

  :deep(.status-cell) {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
  }

  :deep(.status-dot) {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
    display: inline-block !important;
  }

  :deep(.status-text) {
    width: 56px;
    text-align: left;
    display: inline-block;
  }

  :deep(.active-dot) {
    background-color: #52c41a !important;
    animation: breathing-glow-green 1.5s infinite ease-in-out;
  }

  :deep(.inactive-dot) {
    background-color: #ff4d4f !important;
    animation: breathing-glow-red 1.5s infinite ease-in-out;
  }

  @keyframes breathing-glow-green {
    0% {
      box-shadow: 0 0 3px #52c41a;
    }
    50% {
      box-shadow: 0 0 10px #52c41a, 0 0 5px #73d13d;
    }
    100% {
      box-shadow: 0 0 3px #52c41a;
    }
  }

  @keyframes breathing-glow-red {
    0% {
      box-shadow: 0 0 3px #ff4d4f;
    }
    50% {
      box-shadow: 0 0 10px #ff4d4f, 0 0 5px #ff7875;
    }
    100% {
      box-shadow: 0 0 3px #ff4d4f;
    }
  }

  :deep(.manual-highlight) {
    animation: soft-glow 4s ease-in-out !important;
  }

  @keyframes soft-glow {
    0% {
      background-color: transparent;
      box-shadow: none;
    }
    5% {
      background-color: rgba(24, 144, 255, 0.03);
      box-shadow: 0 0 2px rgba(24, 144, 255, 0.1);
    }
    10% {
      background-color: rgba(24, 144, 255, 0.08);
      box-shadow: 0 0 4px rgba(24, 144, 255, 0.15);
    }
    20% {
      background-color: rgba(24, 144, 255, 0.15);
      box-shadow: 0 0 8px rgba(24, 144, 255, 0.2);
    }
    30% {
      background-color: rgba(24, 144, 255, 0.14);
      box-shadow: 0 0 7px rgba(24, 144, 255, 0.18);
    }
    50% {
      background-color: rgba(24, 144, 255, 0.12);
      box-shadow: 0 0 6px rgba(24, 144, 255, 0.15);
    }
    65% {
      background-color: rgba(24, 144, 255, 0.1);
      box-shadow: 0 0 5px rgba(24, 144, 255, 0.12);
    }
    80% {
      background-color: rgba(24, 144, 255, 0.06);
      box-shadow: 0 0 3px rgba(24, 144, 255, 0.08);
    }
    90% {
      background-color: rgba(24, 144, 255, 0.03);
      box-shadow: 0 0 2px rgba(24, 144, 255, 0.05);
    }
    95% {
      background-color: rgba(24, 144, 255, 0.01);
      box-shadow: 0 0 1px rgba(24, 144, 255, 0.02);
    }
    100% {
      background-color: transparent;
      box-shadow: none;
    }
  }

  :deep(.tags-ellipsis) {
    color: #999;
    font-size: 14px;
    margin: 0 4px;
    font-weight: 500;
  }

  :deep(.show-all-tags-trigger) {
    color: #1890ff;
    cursor: pointer;
    font-size: 12px;
    text-decoration: none;
    margin-left: 4px;
  }

  :deep(.show-all-tags-trigger:hover) {
    color: #40a9ff;
    text-decoration: underline;
  }

  // 导出功能样式
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

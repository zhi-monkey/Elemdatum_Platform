<template>
  <div class="dataset-group-container">
    <!-- 单个公有数据集组内的数据集列表视图 -->
    <div>
      <PageWrapper contentBackground @back="goBackToPublicGroupList" style="margin: 0 16px 0 16px">
        <template #title>
          <a-typography-link @click="goBackToPublicGroupList" class="group-title-link">
            <FolderOpenOutlined />
            <span class="ml-2">数据集组: {{ activePublicGroupName }}</span>
          </a-typography-link>
        </template>

        <BasicTable
          @register="registerPublicDatasetTable"
          @expand="handlePublicDatasetExpand"
          rowKey="id"
          :row-class-name="getRowClassName"
        >
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
      </PageWrapper>
    </div>

    <!-- 导出状态弹窗 -->
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

    <!-- 数据集详情弹窗 -->
    <DatasetDetailsModal @register="registerHistoryModal" />
  </div>
</template>

<script lang="ts" setup>
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import {
    Modal as AModal,
    Popover as APopover,
    Spin as ASpin,
    Table as ATable,
    Tag as ATag,
    TypographyLink as ATypographyLink,
  } from 'ant-design-vue';
  import { FolderOpenOutlined } from '@ant-design/icons-vue';
  import { columns as publicGroupColumns } from './publicDataset/stmData';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { computed, h, nextTick, onMounted, onUnmounted, reactive, ref, Ref, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { Recordable } from 'vite-plugin-mock';
  import { PageWrapper } from '/@/components/Page';
  import { useUserStore } from '/@/store/modules/user';
  import { usePermission } from '/@/hooks/web/usePermission';
  import DatasetDetailsModal from './dataset-details-modal.vue';
  import LabelInfo from './datasetHistory/labelInfo.vue';
  import {
    cancelPublicDatasetVersion,
    checkExportStatus,
    exportDatasetVersion,
    getDatasetVersion,
    queryFirstImg,
    recordDatasetDownload,
  } from './api';
  import { minioBaseUrl } from '/@/utils/dubhe';
  import { downloadByUrl } from '/@/utils/file/download';
  import { RoleEnum } from '/@/enums/roleEnum';
  import { PageEnum } from '/@/enums/pageEnum';

  const emit = defineEmits(['back']);

  const props = defineProps<{
    refreshTrigger?: number;
  }>();

  const userStore = useUserStore();
  const roles = userStore.getUserInfo.roles;
  const { hasPermission } = usePermission();
  const { createMessage } = useMessage();
  const router = useRouter();
  const route = useRoute();

  const isAdmin = computed(() => {
    return roles[0].id === 1 || roles[0].id === 89;
  });

  // Modal注册
  const [registerHistoryModal, { openModal: openHistoryModal }] = useModal();

  // 导出相关状态
  const exportStatusModelVisible = ref(false);
  const isPolling = ref(false);
  let versionToExport: Recordable = {};
  let timer: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);

  // 标签信息弹窗
  const currentRow = ref();
  const labelInfoModalVisible = ref(false);

  // 公有数据集组状态
  const activePublicGroupId = ref<number | null>(null);
  const activePublicGroupName = ref<string>('');
  const highlightedRowId = ref<number | null>(null);
  const targetDatasetId = ref<number | null>(null);

  // API函数
  const fetchPublicDatasetsAPI = (datasetGroupId: number, page: number, pageSize: number) => {
    return maHttp
      .get(
        {
          url: 'datasets/group/getPublicDatasetsPageByDatasetGroupId',
          params: {
            datasetGroupId,
            page,
            pageSize,
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

  // 视图切换逻辑
  const goBackToPublicGroupList = () => {
    activePublicGroupId.value = null;
    activePublicGroupName.value = '';
    clearPublicGroupSelectedRowKeys();
    router.push({
      path: PageEnum.DATASET_DETAILS,
      query: { tab: '4' },
    });
  };

  // 公有数据集组表格注册
  const [
    registerPublicDatasetTable,
    {
      reload: reloadPublicDatasets,
      getRawDataSource: getPublicRawDataSource,
      clearSelectedRowKeys: clearPublicGroupSelectedRowKeys,
    },
  ] = useTable({
    api: async (params) => {
      activePublicGroupId.value = Number(route.params.id);
      if (!activePublicGroupId.value) return { items: [], total: 0 };
      // 从sessionStorage获取GroupName并赋值
      activePublicGroupName.value = sessionStorage.getItem('PublicGroupName') ?? '';
      const { page, pageSize } = params;
      const { items, total } = await fetchPublicDatasetsAPI(
        activePublicGroupId.value,
        page,
        pageSize,
      );
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
    columns: publicGroupColumns,
    pagination: true,
    showIndexColumn: false,
    showTableSetting: false,
    bordered: true,
    clickToRowSelect: false,
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
      submitOnReset: true,
    },
  });

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
    { title: '格式', dataIndex: 'format', width: 80, align: 'center' },
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
      title: '发布时间',
      dataIndex: 'createTime',
      customRender: ({ text }) => parseDate(text),
      width: 150,
      align: 'center',
    },
    { title: '备注', dataIndex: 'versionNote', align: 'center', width: 100 },
    {
      title: '发布者',
      dataIndex: 'createUserName',
      align: 'center',
      width: 100,
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      slots: { customRender: 'action' },
    },
  ];

  const handlePublicDatasetExpand = async (expanded: boolean, record: any) => {
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
      const result = await maHttp
        .get(
          {
            url: `datasets/versions/getPublicDatasetVersionList/${datasetId}`,
            params: {
              current: state.pagination.current,
              size: state.pagination.pageSize,
            },
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        )
        .then((res) => {
          const items = Array.isArray(res) ? res : res.result || [];
          const total = Array.isArray(res) ? res.length : res.page?.total || items.length;

          const processedItems = items.map((item) => ({
            ...item,
            isCurrent: item.isCurrent !== undefined ? item.isCurrent : false,
            imageCount: item.imageCount || item.imageCounts || 0,
            labelCountMap: item.labelCountMap || {},
            dataConversion: item.dataConversion !== undefined ? item.dataConversion : 1,
            format: item.format || 'Unknown',
          }));

          return {
            items: processedItems,
            total: total,
          };
        });

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

  // 版本操作
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
      icon: 'ant-design:download-outlined',
      tooltip: '导出',
      label: '导出',
      onClick: goExport.bind(null, record),
      disabled: !(roles[0].name === '管理人员' || roles[0].name === '管理员'),
    },
    {
      icon: 'ant-design:bars',
      tooltip: '查看标签',
      label: '查看标签',
      onClick: goTags.bind(null, record),
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
    let zipName = `${versionToExport.datasetId}_${versionToExport.format}_${versionToExport.versionName}.zip`;
    const versionPath =
      versionToExport.versionUrl ||
      `dataset/${versionToExport.datasetId}/versionFile/${versionToExport.versionName}`;
    let ZipUrl = `${minioBaseUrl}/${versionPath}/${versionToExport.format}/${zipName}`;

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

      timer.value = setInterval(async () => {
        try {
          if (versionToExport !== null) {
            let response = await getDatasetVersion(
              versionToExport.datasetId,
              versionToExport.versionName,
            );
            const status = await checkExportStatus(response.id);
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

  const recordDatasetDownloadAudit = async () => {
    try {
      await recordDatasetDownload();
    } catch (error) {
      console.warn('record dataset download audit failed:', error);
    }
  };

  const datasetExportNew = async (record: Recordable) => {
    let response = await getDatasetVersion(record.datasetId, record.versionName);
    if (response.dataConversion === 1) {
      return await exportDatasetVersion(record.datasetId, record.versionName);
    } else if (response.dataConversion === 0) {
      createMessage.warning('数据集相关文件还在拷贝中，请稍作等候');
    } else {
      createMessage.warning('数据集发布还未成功，请稍作等候');
    }
  };

  const cancelPublic = async (record: Recordable) => {
    try {
      const isSuccess = await cancelPublicDatasetVersion(record.datasetId, record.versionName);
      if (isSuccess) {
        createMessage.success('取消发布成功');

        const datasetId = record.datasetId;
        if (expandedVersions[datasetId]) {
          await fetchVersions(datasetId);
        }

        if (activePublicGroupId.value) {
          const publicDatasets = await fetchPublicDatasetsInGroup(activePublicGroupId.value);

          if (publicDatasets.length === 0) {
            createMessage.info('该数据集组已无公开数据集，返回主界面');
            goBackToPublicGroupList();
          } else {
            await reloadPublicDatasets();
          }
        }
      } else {
        createMessage.error('取消发布失败');
      }
    } catch (error) {
      console.error('取消发布操作失败:', error);
      createMessage.error('取消发布失败，请重试');
    }
  };

  const fetchPublicDatasetsInGroup = async (groupId: number) => {
    try {
      const result = await fetchPublicDatasetsAPI(groupId, 1, 1000);
      return result.items;
    } catch (error) {
      console.error('获取组内公开数据集失败:', error);
      return [];
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

  // 数据集链接点击和高亮逻辑
  const handlePublicDatasetLinkClick = async (groupId: number, datasetName: string) => {
    setTimeout(async () => {
      const tableData = getPublicRawDataSource().items;
      const targetRow = tableData.find((d) => d.name === datasetName);
      if (targetRow) {
        targetDatasetId.value = targetRow.id;
        await handlePublicDatasetExpand(true, targetRow);
        await scrollToAndHighlightRow(targetRow.id);
      } else {
        createMessage.warning(`数据集 "${datasetName}" 不在当前页，请翻页查找`);
      }
    }, 1200);
  };

  const scrollToAndHighlightRow = async (rowId: number) => {
    await nextTick();
    const rowElement = document.querySelector(`[data-row-key="${rowId}"]`);

    if (rowElement) {
      rowElement.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });

      const targetCell = rowElement.querySelector(`td:nth-child(2)`);
      if (targetCell) {
        targetCell.classList.add('manual-highlight');
      }

      highlightedRowId.value = rowId;

      setTimeout(() => {
        highlightedRowId.value = null;
        if (targetCell) {
          targetCell.classList.remove('manual-highlight');
        }
      }, 3000);
    }
  };

  const getRowClassName = (record: any) => {
    return highlightedRowId.value === record.id ? 'highlighted-row' : '';
  };

  // 工具函数
  const parseDate = (dateStr) => {
    return new Date(dateStr).toLocaleString();
  };

  onMounted(() => {
    // 初始化逻辑
  });

  onUnmounted(() => {
    clearInterval(timer.value);
  });
</script>

<style lang="less" scoped>
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

  .modal-content {
    .center-text {
      text-align: center;
    }
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

<template>
  <a-card size="small" title="模型迭代阶段（训练/转换/打包）" class="iteration-shell">
    <a-tabs v-model:activeKey="activeTab" size="small" :destroy-inactive-tab-pane="true">
      <a-tab-pane key="train" tab="模型训练">
        <TrainInfo
          v-if="detail.modelIteration.trainJobId"
          :key="`si-train-${detail.round}-${detail.modelIteration.trainJobId}`"
          :job-id="detail.modelIteration.trainJobId"
          :show-bar="true"
          :change-tab-title="false"
          :current-pipeline-phase="2"
          :current-step-phase="2"
        />
        <a-empty v-else description="训练任务尚未创建" />
      </a-tab-pane>

      <a-tab-pane key="convert" tab="模型转换">
        <ConvertInfo
          v-if="detail.modelIteration.convertJobId"
          :key="`si-convert-${detail.round}-${detail.modelIteration.convertJobId}`"
          :job-id="detail.modelIteration.convertJobId"
          :show-bar="true"
          :change-tab-title="false"
          :has-convert="true"
        />
        <a-empty v-else description="转换任务尚未创建" />
      </a-tab-pane>

      <a-tab-pane key="package" tab="模型打包">
        <a-row :gutter="12" class="package-overview-row">
          <a-col :span="6">
            <a-card class="package-metric-card" size="small">
              <a-statistic title="打包文件" :value="packageSummary.total" suffix="个" />
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="package-metric-card" size="small">
              <a-statistic title="可下载" :value="packageSummary.ready" suffix="个" />
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="package-metric-card" size="small">
              <a-statistic title="生成中" :value="packageSummary.generating" suffix="个" />
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="package-metric-card" size="small">
              <a-statistic title="失败" :value="packageSummary.failed" suffix="个" />
            </a-card>
          </a-col>
        </a-row>

        <a-alert
          class="package-alert"
          show-icon
          type="info"
          message="打包产物可用于模型下发与离线部署，若状态为失败建议检查转换日志后重试。"
        />

        <a-card class="stage-card" :bordered="false">
          <a-empty v-if="!packageRows.length" description="暂无打包产物" />
          <a-table
            v-else
            :columns="packageColumns"
            :data-source="packageRows"
            :pagination="false"
            row-key="fileName"
            size="small"
            class="package-table"
          />
        </a-card>

        <ModelSave
          v-if="detail.modelIteration.appZipPath"
          :hide-model-download="true"
          :app-zip-path="detail.modelIteration.appZipPath"
          :job-id="detail.modelIteration.trainJobId ?? 0"
          :dataset-id="0"
          :dataset-version-name="''"
        />
      </a-tab-pane>
    </a-tabs>
  </a-card>
</template>

<script setup lang="ts">
  import { computed, h, ref, watch } from 'vue';
  import {
    Alert as AAlert,
    Button as AButton,
    Card as ACard,
    Col as ACol,
    Empty as AEmpty,
    Row as ARow,
    Statistic as AStatistic,
    Table as ATable,
    TabPane as ATabPane,
    Tabs as ATabs,
    Tag as ATag,
  } from 'ant-design-vue';
  import TrainInfo from './train/TrainInfo.vue';
  import ConvertInfo from './convert/ConvertInfo.vue';
  import ModelSave from '/@/views/mineai/train/generationGuideList/detail/saveOrSchedule/ModelSave.vue';
  import { SelfIterationChildTaskDetail } from '../../types/selfIteration';
  import {
    fetchSelfIterationModelArtifacts,
    downloadSelfIterationArtifact,
  } from '../../api/selfIteration';

  const props = defineProps<{
    detail: SelfIterationChildTaskDetail;
  }>();

  const activeTab = ref('train');
  const hasAutoSwitchedToConvert = ref(false);
  const hasAutoSwitchedToPackage = ref(false);
  const packageRows = ref<any[]>([]);
  const lastFetchedArtifactKey = ref<string>('');

  watch(
    () => props.detail.round,
    () => {
      hasAutoSwitchedToConvert.value = false;
      hasAutoSwitchedToPackage.value = false;
      if (props.detail.modelIteration.packageTaskId) {
        activeTab.value = 'package';
      } else if (props.detail.modelIteration.convertJobId) {
        activeTab.value = 'convert';
      } else {
        activeTab.value = 'train';
      }
    },
    { immediate: true },
  );

  watch(
    () => props.detail.modelIteration.convertJobId,
    (nextConvertJobId, prevConvertJobId) => {
      if (nextConvertJobId && !prevConvertJobId && !hasAutoSwitchedToConvert.value) {
        activeTab.value = 'convert';
        hasAutoSwitchedToConvert.value = true;
      }
      if (!nextConvertJobId && !props.detail.modelIteration.trainJobId) {
        activeTab.value = 'train';
      }
    },
  );

  watch(
    () => props.detail.modelIteration.packageTaskId,
    (nextPackageTaskId, prevPackageTaskId) => {
      if (nextPackageTaskId && !prevPackageTaskId && !hasAutoSwitchedToPackage.value) {
        activeTab.value = 'package';
        hasAutoSwitchedToPackage.value = true;
      }
    },
  );

  watch(
    () => props.detail.status,
    (status) => {
      if (status === 'converting' && !hasAutoSwitchedToConvert.value) {
        activeTab.value = 'convert';
        hasAutoSwitchedToConvert.value = true;
      } else if (
        (status === 'packaging' || status === 'dispatching' || status === 'completed') &&
        !hasAutoSwitchedToPackage.value
      ) {
        activeTab.value = 'package';
        hasAutoSwitchedToPackage.value = true;
      }
    },
  );

  const packageSummary = computed(() => {
    const list = packageRows.value;
    return {
      total: list.length,
      ready: list.filter((item) => item.status === 'ready').length,
      generating: list.filter((item) => item.status === 'generating').length,
      failed: list.filter((item) => item.status === 'failed').length,
    };
  });

  const packageTextMap: Record<string, string> = {
    ready: '可下载',
    generating: '生成中',
    failed: '失败',
  };

  const packageColorMap: Record<string, string> = {
    ready: 'success',
    generating: 'processing',
    failed: 'error',
  };

  const packageColumns = [
    { title: '文件名', dataIndex: 'fileName' },
    { title: '大小', dataIndex: 'fileSize', width: 120 },
    { title: '生成时间', dataIndex: 'createTime', width: 180 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      customRender: ({ record }) =>
        h(ATag, { color: packageColorMap[record.status] }, () => packageTextMap[record.status]),
    },
    {
      title: '操作',
      dataIndex: 'action',
      width: 120,
      customRender: ({ record }) =>
        h(
          AButton,
          {
            type: 'link',
            disabled: record.status !== 'ready',
            onClick: () => handleDownload(record),
          },
          () => '下载',
        ),
    },
  ];

  const downloadingKey = ref<string>('');

  async function handleDownload(record: any) {
    if (downloadingKey.value) return;
    downloadingKey.value = record.key;
    try {
      await downloadSelfIterationArtifact(record.downloadUrl);
    } catch (e) {
      console.error('下载失败', e);
    } finally {
      downloadingKey.value = '';
    }
  }

  function mapArtifactsToPackageRows(files: any[]) {
    return files.map((item: any, index: number) => {
      const fullName = item?.fileName || `artifact-${index + 1}`;
      const shortName = fullName.split('/').pop() || fullName;
      return {
        key: `${shortName}-${index}`,
        fileName: shortName,
        fileSize: item?.fileSize || '-',
        createTime: item?.createTime || '-',
        status: item?.status || 'ready',
        downloadUrl: fullName,
      };
    });
  }

  async function loadPackageArtifacts() {
    // 优先用 trainJobId：后端会同时列出训练产物(.pt)和转换产物(.bmodel)
    const sourceJobId =
      props.detail.modelIteration.trainJobId || props.detail.modelIteration.convertJobId;
    if (!sourceJobId) {
      packageRows.value = [];
      lastFetchedArtifactKey.value = '';
      return;
    }

    const currentKey = `job-${sourceJobId}`;
    if (lastFetchedArtifactKey.value === currentKey) {
      return;
    }

    const files = await fetchSelfIterationModelArtifacts(sourceJobId);
    if (files.length > 0) {
      packageRows.value = mapArtifactsToPackageRows(files);
      lastFetchedArtifactKey.value = currentKey;
      return;
    }
    packageRows.value = [];
    lastFetchedArtifactKey.value = currentKey;
  }

  watch(
    () => activeTab.value,
    (tab) => {
      if (tab === 'package') {
        loadPackageArtifacts();
      }
    },
    { immediate: true },
  );

  watch(
    [() => props.detail.modelIteration.trainJobId, () => props.detail.modelIteration.convertJobId],
    () => {
      if (activeTab.value === 'package') {
        loadPackageArtifacts();
      }
    },
  );
</script>

<style scoped>
  .iteration-shell {
    --si-surface: #181d31;
    --si-surface-elevated: #181d31;
    --si-border: rgba(255, 255, 255, 0.1);
    --si-hover-border: rgba(24, 144, 255, 0.26);
  }

  .iteration-shell :deep(.ant-card-head) {
    background: var(--si-surface-elevated);
    border-bottom-color: var(--si-border);
  }

  .iteration-shell :deep(.ant-card-body) {
    background: var(--si-surface-elevated);
    padding: 14px;
  }

  .iteration-shell :deep(.ant-tabs-nav) {
    margin-bottom: 14px;
  }

  .iteration-shell :deep(.ant-tabs-tab:hover) {
    color: #69b1ff;
  }

  .package-overview-row {
    margin-bottom: 10px;
  }

  .package-metric-card {
    border: 1px solid var(--si-border);
    transition: border-color 0.2s ease, transform 0.2s ease;
  }

  .package-metric-card:hover {
    border-color: var(--si-hover-border);
    transform: translateY(-2px);
  }

  .package-metric-card :deep(.ant-card-body) {
    background: var(--si-surface-elevated);
    padding: 12px;
  }

  .package-metric-card :deep(.ant-statistic-title) {
    color: rgba(255, 255, 255, 0.62);
    font-size: 12px;
  }

  .package-metric-card :deep(.ant-statistic-content) {
    color: rgba(255, 255, 255, 0.9);
    font-size: 20px;
  }

  .package-alert {
    margin-bottom: 10px;
  }

  .stage-card {
    background: var(--si-surface-elevated);
    border: 1px solid var(--si-border);
    transition: border-color 0.2s ease, box-shadow 0.2s ease;
  }

  .stage-card:hover {
    border-color: var(--si-hover-border);
    box-shadow: 0 8px 20px rgba(8, 14, 26, 0.24);
  }

  .stage-card :deep(.ant-card-body) {
    background: var(--si-surface-elevated);
    padding: 12px;
  }

  .package-table :deep(.ant-table) {
    background: transparent;
  }

  .package-table :deep(.ant-table-thead > tr > th) {
    background: rgba(255, 255, 255, 0.04);
    border-bottom-color: var(--si-border);
  }

  .package-table :deep(.ant-table-tbody > tr > td) {
    border-bottom-color: rgba(255, 255, 255, 0.08);
  }

  .package-table :deep(.ant-table-tbody > tr:hover > td) {
    background: rgba(24, 144, 255, 0.12);
  }
</style>

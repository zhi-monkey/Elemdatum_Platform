<template>
  <div class="data-search">
    <BasicTable @register="registerTable" rowKey="id">
      <template #toolbar>
        <a-space>
          <a-button type="primary" @click="openExport">批量导出</a-button>
          <a-button type="primary" @click="openCreateDataset">新建数据集</a-button>
          <a-button type="primary" @click="openMerge">合并数据集</a-button>
        </a-space>
      </template>
      <template #img="{ text }">
        <TableImg :size="60" :simpleShow="true" :imgList="getImageUrl(text)" />
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:info-circle-outlined',
              tooltip: '查看详情',
              onClick: showDetail.bind(null, record),
            },
          ]"
        />
      </template>
    </BasicTable>

    <el-dialog v-model="detailVisible" title="图片详情" width="720px" append-to-body>
      <div v-if="detailRecord" class="detail-body">
        <div class="detail-img">
          <img :src="getImageUrl(detailRecord.url)[0]" alt="图片" />
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item
            v-for="(item, index) in detailItems"
            :key="index"
            :label="item.label"
          >
            {{ item.value || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <el-dialog v-model="exportVisible" title="批量导出" width="420px" append-to-body>
      <div class="export-body">
        <p class="export-tip">
          {{ selectedCount > 0 ? `已勾选 ${selectedCount} 张图片，将导出勾选的数据` : '未勾选，将按当前检索条件导出全部结果' }}
        </p>
        <div class="export-format">
          <span class="export-label">导出格式：</span>
          <el-radio-group v-model="exportFormat">
            <el-radio label="csv">CSV</el-radio>
            <el-radio label="json">JSON</el-radio>
            <el-radio label="zip">原格式(ZIP)</el-radio>
          </el-radio-group>
        </div>
      </div>
      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="primary" :loading="exporting" @click="confirmExport">确定导出</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="datasetVisible" title="新建数据集" width="420px" append-to-body>
      <div class="export-body">
        <p class="export-tip">将勾选的 {{ selectedCount }} 张图片（含标注与元信息）复制到新数据集</p>
        <div class="export-format">
          <span class="export-label">数据集名称：</span>
          <el-input v-model="datasetName" placeholder="请输入数据集名称" />
        </div>
        <div class="export-format">
          <span class="export-label">数据集组：</span>
          <el-select v-model="groupMode" style="width: 130px">
            <el-option label="不分组" value="none" />
            <el-option label="选择已有组" value="existing" />
            <el-option label="新建组" value="new" />
          </el-select>
        </div>
        <div v-if="groupMode === 'existing'" class="export-format">
          <span class="export-label">选择组：</span>
          <el-select
            v-model="datasetGroupId"
            placeholder="选择数据集组"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option v-for="g in groupList" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </div>
        <div v-if="groupMode === 'new'" class="export-format">
          <span class="export-label">组名称：</span>
          <el-input v-model="datasetGroupName" placeholder="新数据集组名称" />
        </div>
      </div>
      <template #footer>
        <el-button @click="datasetVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="confirmCreateDataset">确定创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="mergeVisible" title="合并数据集" width="420px" append-to-body>
      <div class="export-body">
        <p class="export-tip">将勾选的 {{ selectedCount }} 张图片（含标注与元信息）合并到目标数据集</p>
        <div class="export-format">
          <span class="export-label">数据集组：</span>
          <el-select
            v-model="mergeGroupId"
            placeholder="选择数据集组"
            clearable
            filterable
            style="width: 100%"
            @change="handleMergeGroupChange"
          >
            <el-option v-for="g in mergeGroupList" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </div>
        <div class="export-format">
          <span class="export-label">数据集：</span>
          <el-select
            v-model="mergeDatasetId"
            placeholder="选择目标数据集"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option v-for="d in mergeDatasetList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </div>
      </div>
      <template #footer>
        <el-button @click="mergeVisible = false">取消</el-button>
        <el-button type="primary" :loading="merging" @click="confirmMerge">确定合并</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
  import { BasicTable, TableAction, TableImg, useTable } from '/@/components/Table';
  import { columns, searchFormSchema } from './columns';
  import {
    searchFiles,
    createExportTask,
    createDatasetFromSearch,
    getAllDatasetGroup,
    getDatasetsByDatasetGroupId,
    mergeToDataset,
  } from './api';
  import { bucketHost } from '/@/utils/dubhe';
  import { computed, ref } from 'vue';
  import { message } from 'ant-design-vue';

  const emit = defineEmits(['switch-tab']);

  const detailVisible = ref(false);
  const detailRecord = ref<any>(null);

  const exportVisible = ref(false);
  const exportFormat = ref('csv');
  const exporting = ref(false);

  const datasetVisible = ref(false);
  const datasetName = ref('');
  const creating = ref(false);
  const groupMode = ref('none');
  const groupList = ref<any[]>([]);
  const datasetGroupId = ref<number | undefined>(undefined);
  const datasetGroupName = ref('');

  const mergeVisible = ref(false);
  const mergeGroupId = ref<number | undefined>(undefined);
  const mergeDatasetId = ref<number | undefined>(undefined);
  const mergeGroupList = ref<any[]>([]);
  const mergeDatasetList = ref<any[]>([]);
  const merging = ref(false);

  let clearSelected: (() => void) | null = null;
  const [registerTable, { getSelectRows, getForm, clearSelectedRowKeys }] = useTable({
    api: async (params) => {
      const v: any = await searchFiles(buildSearchBody(params));
      v.items = v.result;
      v.total = v.page.total;
      return v;
    },
    columns,
    formConfig: {
      labelWidth: 100,
      showAdvancedButton: false,
      schemas: searchFormSchema,
      resetFunc: async () => {
        clearSelected?.();
      },
    },
    useSearchForm: true,
    showTableSetting: true,
    bordered: true,
    showIndexColumn: false,
    rowSelection: { type: 'checkbox' },
    actionColumn: {
      width: 90,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  });
  clearSelected = clearSelectedRowKeys;

  const selectedCount = computed(() => {
    const rows = getSelectRows();
    return rows ? rows.length : 0;
  });

  function buildSearchBody(params: any) {
    const body: any = {
      datasetIds: params.datasetIds,
      name: params.name,
      annotationStatus: params.annotationStatus,
      labelNames: params.labelNames,
      scenario: params.scenario,
      location: params.location,
      sourceType: params.sourceType,
      device: params.device,
      deviceSn: params.deviceSn,
      lighting: params.lighting,
      quality: params.quality,
      current: params.page,
      size: params.pageSize,
      sortField: params.field || 'id',
      sortOrder: params.order === 'ascend' ? 'asc' : 'desc',
    };
    if (Array.isArray(params.captureTime) && params.captureTime.length === 2) {
      body.captureTimeStart = params.captureTime[0];
      body.captureTimeEnd = params.captureTime[1];
    }
    if (Array.isArray(params.updateTime) && params.updateTime.length === 2) {
      body.updateTimeStart = params.updateTime[0];
      body.updateTimeEnd = params.updateTime[1];
    }
    return body;
  }

  // 从搜索表单值构建检索条件（不含分页）
  function buildCondition(fields: any) {
    const body: any = {
      datasetIds: fields.datasetIds,
      name: fields.name,
      annotationStatus: fields.annotationStatus,
      labelNames: fields.labelNames,
      scenario: fields.scenario,
      location: fields.location,
      sourceType: fields.sourceType,
      device: fields.device,
      deviceSn: fields.deviceSn,
      lighting: fields.lighting,
      quality: fields.quality,
    };
    if (Array.isArray(fields.captureTime) && fields.captureTime.length === 2) {
      body.captureTimeStart = fields.captureTime[0];
      body.captureTimeEnd = fields.captureTime[1];
    }
    if (Array.isArray(fields.updateTime) && fields.updateTime.length === 2) {
      body.updateTimeStart = fields.updateTime[0];
      body.updateTimeEnd = fields.updateTime[1];
    }
    return body;
  }

  function getSelectedIds(): number[] {
    const rows = getSelectRows();
    return rows ? rows.map((r) => r.id) : [];
  }

  function openExport() {
    exportVisible.value = true;
  }

  async function confirmExport() {
    const fileIds = getSelectedIds();
    exporting.value = true;
    try {
      // 勾选了则导出勾选，否则按当前检索条件导出全部
      const condition = buildCondition(getForm().getFieldsValue());
      await createExportTask({ condition, fileIds, format: exportFormat.value });
      message.success('导出任务已创建，请到「导出任务」页确认后导出');
      exportVisible.value = false;
      emit('switch-tab', 'export');
    } catch (e) {
      message.error('提交导出任务失败');
    } finally {
      exporting.value = false;
    }
  }

  async function openCreateDataset() {
    if (getSelectedIds().length === 0) {
      message.warning('请先勾选要复制的图片');
      return;
    }
    datasetName.value = '';
    groupMode.value = 'none';
    datasetGroupId.value = undefined;
    datasetGroupName.value = '';
    groupList.value = [];
    datasetVisible.value = true;
    try {
      const res: any = await getAllDatasetGroup();
      groupList.value = Array.isArray(res) ? res : res?.data || [];
    } catch (e) {
      // 加载数据集组失败，忽略
    }
  }

  async function confirmCreateDataset() {
    const fileIds = getSelectedIds();
    if (fileIds.length === 0) {
      message.warning('请先勾选要复制的图片');
      return;
    }
    if (!datasetName.value.trim()) {
      message.warning('请输入数据集名称');
      return;
    }
    creating.value = true;
    try {
      await createDatasetFromSearch({
        name: datasetName.value.trim(),
        fileIds,
        datasetGroupId: groupMode.value === 'existing' ? datasetGroupId.value : null,
        datasetGroupName: groupMode.value === 'new' ? datasetGroupName.value : null,
      });
      message.success('数据集创建成功');
      datasetVisible.value = false;
    } catch (e) {
      message.error('创建数据集失败');
    } finally {
      creating.value = false;
    }
  }

  async function openMerge() {
    if (getSelectedIds().length === 0) {
      message.warning('请先勾选要合并的图片');
      return;
    }
    mergeGroupId.value = undefined;
    mergeDatasetId.value = undefined;
    mergeDatasetList.value = [];
    mergeGroupList.value = [];
    mergeVisible.value = true;
    try {
      const res: any = await getAllDatasetGroup();
      mergeGroupList.value = Array.isArray(res) ? res : res?.data || [];
    } catch (e) {
      // 忽略
    }
  }

  async function handleMergeGroupChange(groupId: number) {
    mergeDatasetId.value = undefined;
    mergeDatasetList.value = [];
    if (!groupId) return;
    try {
      const res: any = await getDatasetsByDatasetGroupId(groupId);
      mergeDatasetList.value = Array.isArray(res) ? res : res?.data || [];
    } catch (e) {
      // 忽略
    }
  }

  async function confirmMerge() {
    const fileIds = getSelectedIds();
    if (fileIds.length === 0) {
      message.warning('请先勾选要合并的图片');
      return;
    }
    if (!mergeDatasetId.value) {
      message.warning('请选择目标数据集');
      return;
    }
    merging.value = true;
    try {
      await mergeToDataset({ targetDatasetId: mergeDatasetId.value, fileIds });
      message.success('合并成功');
      mergeVisible.value = false;
    } catch (e) {
      message.error('合并失败');
    } finally {
      merging.value = false;
    }
  }

  const getImageUrl = (url: string) => [`${bucketHost}/${url}`];

  const detailItems = computed(() => {
    const r = detailRecord.value;
    if (!r) return [];
    return [
      { label: '名称', value: r.name },
      { label: '所属数据集', value: r.datasetName },
      { label: '标注状态', value: formatAnnotationStatus(r.annotationStatus) },
      { label: '标签', value: r.labels || '无标签' },
      { label: '尺寸', value: r.width && r.height ? `${r.width} × ${r.height}` : '-' },
      { label: '数据来源', value: r.sourceType },
      { label: '采集时间', value: r.captureTime },
      { label: '车辆/设备', value: r.device },
      { label: '设备/相机编号', value: r.deviceSn },
      { label: '采集地点', value: r.location },
      { label: '业务场景', value: r.scenario },
      { label: '光照/环境', value: r.lighting },
      { label: '数据质量', value: r.quality },
      { label: '更新时间', value: r.updateTime },
    ];
  });

  function showDetail(record: any) {
    detailRecord.value = record;
    detailVisible.value = true;
  }

  function formatAnnotationStatus(s: number) {
    if (s === 104) return '已标注';
    if (s === 102) return '标注中';
    if (s === 103) return '自动标注完成';
    return '未标注';
  }
</script>

<style scoped lang="less">
  :deep(.el-dialog__title) {
    display: block;
    text-align: center;
  }

  .detail-body {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .detail-img {
    text-align: center;

    img {
      display: block;
      margin: 0 auto;
      max-width: 100%;
      max-height: 360px;
      border-radius: 4px;
    }
  }

  .export-body {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .export-tip {
    margin: 0;
    color: #666;
    font-size: 13px;
  }

  .export-format {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .export-label {
    flex-shrink: 0;
    color: #333;
  }
</style>

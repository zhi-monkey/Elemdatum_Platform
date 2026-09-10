<template>
  <div class="capture-task-page">
    <BasicTable @register="registerTable" @expand="handleExpand" rowKey="id">
      <template #toolbar>
        <Button type="primary" @click="openTaskModal(false)">新增回流任务</Button>
        <Button style="margin-left: 12px" @click="goDataSourceManagement">数据源管理</Button>
      </template>

      <template #sourceType="{ record }">
        <Tag color="blue">数据收集服务器</Tag>
      </template>

      <template #sourceName="{ record }">{{ getSourceName(record) }}</template>
      <template #groupName="{ record }">{{
        datasetGroupNameMap[record.datasetGroupId] || record.datasetGroupId || '-'
      }}</template>

      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:play-circle-outlined',
              tooltip: '开始采集',
              color: 'success',
              onClick: () => handleStartCapture(record),
            },
            {
              icon: 'clarity:note-edit-line',
              tooltip: '编辑',
              onClick: () => openTaskModal(true, record),
            },
            {
              icon: 'ant-design:delete-outlined',
              color: 'error',
              tooltip: '删除',
              popConfirm: { title: '是否确认删除任务', confirm: () => handleDeleteTask(record) },
            },
          ]"
        />
      </template>

      <template #expandedRowRender="{ record }">
        <Table
          size="small"
          rowKey="id"
          :bordered="false"
          :columns="executionColumns"
          :data-source="executionMap[record.id]?.items || []"
          :loading="executionMap[record.id]?.loading"
          :pagination="executionMap[record.id]?.pagination"
          @change="(p) => handleExecutionPageChange(record.id, p)"
        >
          <template #status="{ record: ex }">
            <Tag :color="getExecStatusColor(ex.status)">{{ getExecStatusText(ex.status) }}</Tag>
          </template>
          <template #progress="{ record: ex }">{{ ex.progress ?? 0 }}%</template>
          <template #datasetLink="{ record: ex }">
            <span>{{ ex.datasetId ? datasetNameMap[ex.datasetId] || '加载中...' : '-' }}</span>
          </template>
          <template #subAction="{ record: ex }">
            <TableAction
              :actions="[
                {
                  icon: 'ant-design:pause-circle-outlined',
                  color: 'warning',
                  tooltip: '停止采集',
                  ifShow: () => [0, 1].includes(Number(ex.status)),
                  onClick: () => handleStopExecution(ex),
                },
                {
                  icon: 'ant-design:delete-outlined',
                  color: 'error',
                  tooltip: '删除记录',
                  onClick: () => confirmDeleteExecution(ex),
                },
              ]"
            />
          </template>
        </Table>
      </template>
    </BasicTable>

    <!-- 新增/编辑弹窗 -->
    <Modal
      v-model:visible="taskModalVisible"
      :title="taskModalMode === 'create' ? '新增回流任务' : '编辑回流任务'"
      :bodyStyle="{ paddingTop: '28px', paddingRight: '32px' }"
      @ok="submitTask"
      @cancel="closeTaskModal"
      destroyOnClose
    >
      <Form :model="taskForm" :label-col="{ span: 7 }" :wrapper-col="{ span: 17 }">
        <FormItem label="任务名称" required>
          <Input v-model:value="taskForm.taskName" placeholder="请输入任务名称" />
        </FormItem>
        <FormItem label="数据源类型" required>
          <Select
            v-model:value="taskForm.sourceType"
            :options="sourceTypeOptions"
            placeholder="请选择"
          />
        </FormItem>
        <FormItem v-if="taskForm.sourceType === 'HTTP'" label="摄像机" required>
          <Select
            v-model:value="taskForm.httpCameraId"
            :options="httpCameraOptions"
            :show-search="true"
            :dropdown-match-select-width="false"
            :dropdown-style="{ width: 'max-content' }"
            placeholder="请选择摄像机"
          />
        </FormItem>
        <FormItem label="数据集组" required>
          <div style="display: flex; gap: 8px">
            <Select
              v-model:value="taskForm.datasetGroupId"
              :options="datasetGroupOptions"
              :show-search="true"
              placeholder="选择已有数据集组"
              allow-clear
              style="flex: 1"
              @change="
                () => {
                  if (taskForm.datasetGroupId) taskForm.datasetGroupName = '';
                }
              "
            />
            <span style="line-height: 32px; color: #999; white-space: nowrap">或新建：</span>
            <Input
              v-model:value="taskForm.datasetGroupName"
              placeholder="填写新数据集组名称"
              style="flex: 1"
              :disabled="!!taskForm.datasetGroupId"
            />
          </div>
          <div style="font-size: 12px; color: #999; margin-top: 4px">
            采集图片将存入该数据集组，每次采集自动创建子数据集
          </div>
        </FormItem>
        <FormItem label="捕获间隔(秒)">
          <InputNumber v-model:value="taskForm.captureInterval" :min="1" style="width: 100%" />
        </FormItem>
        <FormItem label="计划数量">
          <InputNumber v-model:value="taskForm.imageQuantity" :min="1" style="width: 100%" />
        </FormItem>
      </Form>
    </Modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, createVNode, h, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
  import {
    Button,
    Checkbox,
    Form,
    Input,
    InputNumber,
    Modal,
    Select,
    Table,
    Tag,
  } from 'ant-design-vue';
  import { ExclamationCircleOutlined } from '@ant-design/icons-vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { cancelCaptureExecution, listAllHttpCameras, listHttpCameraServer } from '../api';
  import type { HttpCameraItem, HttpCameraServerItem } from '../api';
  import { useGo } from '/@/hooks/web/usePage';

  interface CaptureTask {
    id?: number;
    taskName?: string;
    sourceType?: string;
    rtspSourceId?: number;
    httpCameraId?: number;
    httpCameraServerId?: number;
    datasetGroupId?: number;
    captureInterval?: number;
    imageQuantity?: number;
    createTime?: string;
    updateTime?: string;
  }
  interface CaptureExecution {
    id?: number;
    taskId?: number;
    datasetId?: number;
    status?: number;
    progress?: number;
    capturedCount?: number;
    captureInterval?: number;
    imageQuantity?: number;
    message?: string;
    createTime?: string;
    startTime?: string;
    endTime?: string;
  }
  interface DatasetGroupItem {
    id: number;
    name: string;
  }
  interface ChildTableState {
    items: CaptureExecution[];
    loading: boolean;
    pagination: {
      current: number;
      pageSize: number;
      total: number;
      showSizeChanger: boolean;
      showQuickJumper: boolean;
    };
  }

  const { createMessage } = useMessage();
  const go = useGo();
  const FormItem = Form.Item;
  const DATA_SOURCE_MANAGEMENT_PATH = '/maData/dataSource';

  // 现在暂时不用摄像头视频流，只保留数据收集服务器作为数据源类型。
  const sourceTypeOptions = [
    { label: '数据收集服务器', value: 'HTTP' },
  ];
  const httpServerList = ref<HttpCameraServerItem[]>([]);
  const httpCameraList = ref<HttpCameraItem[]>([]);
  const datasetGroupList = ref<DatasetGroupItem[]>([]);
  // 数据集ID -> 名称的映射，展开子列表时批量填充
  const datasetNameMap = reactive<Record<number, string>>({});

  function cameraStatusText(status?: string) {
    const s = (status || '').toUpperCase();
    if (s === 'ONLINE') return '在线';
    if (s === 'INACTIVE') return '未工作';
    if (s === 'DELETED') return '已删除';
    if (s === 'OFFLINE') return '离线';
    return '未知';
  }
  const httpCameraOptions = computed(() => {
    const serverNameMap: Record<number, string> = {};
    httpServerList.value.forEach((s) => {
      serverNameMap[s.id] = s.name;
    });
    const available = httpCameraList.value.filter(
      (c) => c.isDelete === 0 && String(c.status || '').toUpperCase() !== 'DELETED',
    );
    const selected = httpCameraList.value.filter((c) => c.id === taskForm.httpCameraId);
    const source = [...available];
    selected.forEach((item) => {
      if (!source.some((x) => x.id === item.id)) source.push(item);
    });
    return source.map((c) => ({
      label: `[${serverNameMap[c.httpCameraServerId] || c.httpCameraServerId}] ${c.name} (流地址:${
        c.streamUrl || c.cameraId
      }) - ${cameraStatusText(c.status)}`,
      value: c.id,
    }));
  });
  const datasetGroupOptions = computed(() =>
    datasetGroupList.value.map((i) => ({ label: i.name, value: i.id })),
  );

  const httpServerNameMap = computed<Record<number, string>>(() => {
    const m: Record<number, string> = {};
    httpServerList.value.forEach((i) => {
      m[i.id] = i.name;
    });
    return m;
  });
  const httpCameraNameMap = computed<Record<number, string>>(() => {
    const m: Record<number, string> = {};
    httpCameraList.value.forEach((i) => {
      const serverName = httpServerNameMap.value[i.httpCameraServerId] || i.httpCameraServerId;
      const statusText = cameraStatusText(i.status);
      m[i.id] = `${serverName}-${i.name}-${statusText}`;
    });
    return m;
  });
  const datasetGroupNameMap = computed<Record<number, string>>(() => {
    const m: Record<number, string> = {};
    datasetGroupList.value.forEach((i) => {
      m[i.id] = i.name;
    });
    return m;
  });

  function getSourceName(record: CaptureTask) {
    if (record.sourceType === 'HTTP' && record.httpCameraId) {
      const cameraText = httpCameraNameMap.value[record.httpCameraId];
      if (cameraText) {
        return cameraText;
      }
      const serverText = record.httpCameraServerId
        ? httpServerNameMap.value[record.httpCameraServerId] || String(record.httpCameraServerId)
        : '数据收集服务器';
      return `${serverText}-摄像机${record.httpCameraId}-未知`;
    }
    return '-';
  }

  const execStatusOptions = [
    { label: '排队中', value: 0 },
    { label: '运行中', value: 1 },
    { label: '成功', value: 2 },
    { label: '失败', value: 3 },
    { label: '已取消', value: 4 },
  ];
  function getExecStatusText(s?: number) {
    return execStatusOptions.find((i) => i.value === Number(s))?.label || '未知';
  }
  function getExecStatusColor(s?: number) {
    return (
      { 0: 'default', 1: 'processing', 2: 'success', 3: 'error', 4: 'warning' }[Number(s)] ||
      'default'
    );
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '任务名称', dataIndex: 'taskName', width: 180 },
    {
      title: '数据源类型',
      dataIndex: 'sourceType',
      width: 130,
      slots: { customRender: 'sourceType' },
    },
    {
      title: '数据源',
      dataIndex: 'httpCameraId',
      width: 260,
      slots: { customRender: 'sourceName' },
    },
    {
      title: '数据集组',
      dataIndex: 'datasetGroupId',
      width: 180,
      slots: { customRender: 'groupName' },
    },
    { title: '捕获间隔(秒)', dataIndex: 'captureInterval', width: 110 },
    { title: '计划数量', dataIndex: 'imageQuantity', width: 100 },
    { title: '创建时间', dataIndex: 'createTime', width: 180 },
  ];
  const executionColumns = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '状态', dataIndex: 'status', slots: { customRender: 'status' }, width: 90 },
    { title: '进度', dataIndex: 'progress', slots: { customRender: 'progress' }, width: 80 },
    { title: '已捕获', dataIndex: 'capturedCount', width: 80 },
    {
      title: '关联数据集',
      dataIndex: 'datasetId',
      slots: { customRender: 'datasetLink' },
      width: 120,
    },
    { title: '开始时间', dataIndex: 'startTime', width: 170 },
    { title: '结束时间', dataIndex: 'endTime', width: 170 },
    { title: '日志', dataIndex: 'message', width: 200, ellipsis: true },
    { title: '操作', dataIndex: 'subAction', slots: { customRender: 'subAction' }, width: 100 },
  ];

  const executionMap = reactive<Record<number, ChildTableState>>({});
  const expandedTaskIds = new Set<number>();
  const executionRefreshTimerMap: Record<number, number> = {};

  const [registerTable, { reload }] = useTable({
    title: '数据回流任务列表',
    api: async (params) => {
      const result = await maHttp.get(
        { url: 'rtsp-capture-tasks', params: { current: params.page, size: params.pageSize } },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      return { items: result.records || [], total: result.total || 0 };
    },
    columns,
    bordered: true,
    expandRowByClick: true,
    showIndexColumn: false,
    showTableSetting: false,
    actionColumn: {
      width: 120,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  });

  async function loadOptions() {
    const [httpRes, cameraRes, groupRes] = await Promise.allSettled([
      listHttpCameraServer(),
      listAllHttpCameras(),
      maHttp.get(
        { url: 'datasets/group/getAllDatasetGroup' },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      ),
    ]);
    if (httpRes.status === 'fulfilled') httpServerList.value = httpRes.value || [];
    if (cameraRes.status === 'fulfilled') httpCameraList.value = cameraRes.value || [];
    if (groupRes.status === 'fulfilled') datasetGroupList.value = groupRes.value || [];
  }

  onMounted(loadOptions);
  function goDataSourceManagement() {
    go(DATA_SOURCE_MANAGEMENT_PATH);
  }

  function ensureChildState(taskId: number): ChildTableState {
    if (!executionMap[taskId]) {
      executionMap[taskId] = {
        items: [],
        loading: false,
        pagination: {
          current: 1,
          pageSize: 5,
          total: 0,
          showSizeChanger: true,
          showQuickJumper: false,
        },
      };
    }
    return executionMap[taskId];
  }
  async function loadExecutionList(taskId: number) {
    const state = ensureChildState(taskId);
    state.loading = true;
    try {
      const result = await maHttp.get(
        {
          url: 'rtsp-capture-executions',
          params: { taskId, current: state.pagination.current, size: state.pagination.pageSize },
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      state.items = result.records || [];
      state.pagination.total = result.total || 0;
      // 批量查询数据集名称
      const ids: number[] = (state.items as any[])
        .map((ex) => ex.datasetId)
        .filter((id) => id != null && datasetNameMap[id] === undefined);
      if (ids.length > 0) {
        try {
          const nameMap = await maHttp.get(
            { url: `datasets/batchGet/${ids.join(',')}` },
            { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
          );
          Object.entries(nameMap || {}).forEach(([id, vo]: [string, any]) => {
            datasetNameMap[Number(id)] = vo?.name || String(id);
          });
        } catch (e) {
          ids.forEach((id) => {
            datasetNameMap[id] = String(id);
          });
        }
      }
    } finally {
      state.loading = false;
      syncExecutionAutoRefresh(taskId);
    }
  }
  function handleExpand(expanded: boolean, record: CaptureTask) {
    if (!record.id) return;
    if (expanded) {
      expandedTaskIds.add(record.id);
      loadExecutionList(record.id);
    } else {
      expandedTaskIds.delete(record.id);
      stopExecutionAutoRefresh(record.id);
    }
  }
  function handleExecutionPageChange(taskId: number, p: any) {
    const state = ensureChildState(taskId);
    state.pagination.current = p.current || 1;
    state.pagination.pageSize = p.pageSize || 5;
    loadExecutionList(taskId);
  }

  function hasRunningExecution(taskId: number) {
    const state = executionMap[taskId];
    if (!state?.items?.length) return false;
    return state.items.some((item) => [0, 1].includes(Number(item.status)));
  }

  function startExecutionAutoRefresh(taskId: number) {
    if (executionRefreshTimerMap[taskId]) return;
    executionRefreshTimerMap[taskId] = window.setInterval(() => {
      loadExecutionList(taskId);
    }, 20000);
  }

  function stopExecutionAutoRefresh(taskId: number) {
    const timer = executionRefreshTimerMap[taskId];
    if (!timer) return;
    window.clearInterval(timer);
    delete executionRefreshTimerMap[taskId];
  }

  function syncExecutionAutoRefresh(taskId: number) {
    if (expandedTaskIds.has(taskId) && hasRunningExecution(taskId)) {
      startExecutionAutoRefresh(taskId);
      return;
    }
    stopExecutionAutoRefresh(taskId);
  }

  onBeforeUnmount(() => {
    Object.keys(executionRefreshTimerMap).forEach((key) => stopExecutionAutoRefresh(Number(key)));
  });

  // ---- 弹窗 ----
  const taskModalVisible = ref(false);
  const taskModalMode = ref<'create' | 'edit'>('create');
  const taskForm = reactive({
    id: undefined as number | undefined,
    taskName: '',
    sourceType: 'HTTP',
    httpCameraId: undefined as number | undefined,
    datasetGroupId: undefined as number | undefined,
    datasetGroupName: '',
    captureInterval: 5,
    imageQuantity: 100,
  });

  function resetTaskForm() {
    Object.assign(taskForm, {
      id: undefined,
      taskName: '',
      sourceType: 'HTTP',
      httpCameraId: undefined,
      datasetGroupId: undefined,
      datasetGroupName: '',
      captureInterval: 5,
      imageQuantity: 100,
    });
  }
  function findHttpCamera(id?: number) {
    if (!id) return undefined;
    return httpCameraList.value.find((item) => item.id === id);
  }

  function isHttpCameraSelectable(id?: number) {
    const camera = findHttpCamera(id);
    if (!camera) return false;
    return camera.isDelete === 0 && String(camera.status || '').toUpperCase() !== 'DELETED';
  }

  function isHttpCameraAvailable(id?: number) {
    const camera = findHttpCamera(id);
    if (!camera) return false;
    return camera.isDelete === 0 && String(camera.status || '').toUpperCase() === 'ONLINE';
  }

  function openTaskModal(isEdit: boolean, record?: CaptureTask) {
    taskModalMode.value = isEdit ? 'edit' : 'create';
    resetTaskForm();
    if (isEdit && record) {
      Object.assign(taskForm, record);
      taskForm.datasetGroupName = '';
    }
    taskModalVisible.value = true;
  }
  function closeTaskModal() {
    taskModalVisible.value = false;
  }

  async function submitTask() {
    if (!taskForm.taskName) {
      createMessage.warning('请填写任务名称');
      return;
    }
    if (taskForm.sourceType === 'HTTP' && !taskForm.httpCameraId) {
      createMessage.warning('请选择摄像机');
      return;
    }
    if (taskForm.sourceType === 'HTTP' && !isHttpCameraSelectable(taskForm.httpCameraId)) {
      createMessage.warning('当前摄像机不存在或已删除，请重新同步后选择');
      return;
    }
    if (!taskForm.datasetGroupId && !taskForm.datasetGroupName.trim()) {
      createMessage.warning('请选择已有数据集组或填写新数据集组名称');
      return;
    }
    const payload = {
      taskName: taskForm.taskName,
      sourceType: taskForm.sourceType,
      rtspSourceId: null,
      httpCameraServerId: null,
      httpCameraId: taskForm.sourceType === 'HTTP' ? taskForm.httpCameraId : null,
      datasetGroupId: taskForm.datasetGroupId || null,
      datasetGroupName: !taskForm.datasetGroupId ? taskForm.datasetGroupName.trim() : null,
      captureInterval: taskForm.captureInterval,
      imageQuantity: taskForm.imageQuantity,
    };
    if (taskModalMode.value === 'create') {
      await maHttp.post(
        { url: 'rtsp-capture-tasks', data: payload },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      createMessage.success('创建成功');
    } else {
      await maHttp.put(
        { url: `rtsp-capture-tasks/${taskForm.id}`, data: payload },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      createMessage.success('更新成功');
    }
    closeTaskModal();
    await reload();
    await loadOptions(); // 刷新数据集组列表（可能新建了）
  }

  async function handleDeleteTask(record: CaptureTask) {
    await maHttp.delete(
      { url: `rtsp-capture-tasks/${record.id}` },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    );
    createMessage.success('删除成功');
    await reload();
  }

  function handleStartCapture(record: CaptureTask) {
    if (record.sourceType === 'HTTP' && !isHttpCameraAvailable(record.httpCameraId)) {
      createMessage.warning('当前HTTP流不在线/未工作/已删除，无法启动采集');
      return;
    }
    const delRaw = ref(false);
    Modal.confirm({
      title: '确认开始采集？',
      icon: createVNode(ExclamationCircleOutlined),
      content: () =>
        h('div', [
          h(
            'p',
            `将在数据集组「${
              datasetGroupNameMap.value[record.datasetGroupId!] || record.datasetGroupId
            }」下自动创建数据集`,
          ),
          h(
            Checkbox,
            {
              checked: delRaw.value,
              'onUpdate:checked': (checked: boolean) => {
                delRaw.value = checked;
              },
            },
            () => '采集后删除原始图片',
          ),
        ]),
      okText: '开始采集',
      cancelText: '取消',
      async onOk() {
        await maHttp.post(
          { url: `rtsp-capture-tasks/${record.id}/start`, data: { delRaw: delRaw.value } },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        );
        createMessage.success('已触发采集，数据集已自动创建');
        await reload();
        if (record.id) await loadExecutionList(record.id);
      },
    });
  }

  async function handleDeleteExecution(record: CaptureExecution, withDataset: boolean) {
    const url = withDataset
      ? `rtsp-capture-executions/${record.id}/with-dataset`
      : `rtsp-capture-executions/${record.id}`;
    await maHttp.delete({ url }, { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET });
    createMessage.success(withDataset ? '执行记录及数据集已删除' : '执行记录已删除');
    if (record.taskId) await loadExecutionList(record.taskId);
  }

  function confirmDeleteExecution(record: CaptureExecution) {
    let withDataset = false;
    Modal.confirm({
      title: '确认删除执行记录？',
      icon: createVNode(ExclamationCircleOutlined),
      content: createVNode('div', null, [
        createVNode('div', { style: 'margin-bottom: 8px;' }, '删除后不可恢复'),
        createVNode(
          Checkbox,
          {
            onChange: (e: any) => {
              withDataset = !!e?.target?.checked;
            },
          },
          { default: () => '同步删除关联数据集' },
        ),
      ]),
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      async onOk() {
        await handleDeleteExecution(record, withDataset);
      },
    });
  }

  async function handleStopExecution(record: CaptureExecution) {
    if (!record.id) return;
    await cancelCaptureExecution(record.id);
    createMessage.success('已发送停止指令');
    if (record.taskId) await loadExecutionList(record.taskId);
  }
</script>

<style scoped lang="less">
  .capture-task-page {
    padding: 0;
  }
</style>

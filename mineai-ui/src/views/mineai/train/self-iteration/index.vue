<template>
  <PageWrapper contentClass="page-content">
    <template #title>自迭代训练任务</template>

    <BasicTable @register="registerTable" rowKey="id" @expand="handleExpand">
      <template #expandedRowRender="{ record }">
        <ChildTaskTable :tasks="record.childTasks" @detail="openChildTaskDetail" />
      </template>

      <template #toolbar>
        <AButton type="primary" @click="goDataSourceManagement">数据源管理</AButton>
        <AButton type="primary" style="margin-left: 12px" @click="handleCreate"
          >新增自迭代训练任务</AButton
        >
      </template>

      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:play-circle-outlined',
              tooltip: '启动',
              ifShow: record.status === 'pending',
              onClick: handleStart.bind(null, record.id),
            },
            {
              icon: 'ant-design:pause-circle-outlined',
              tooltip: '暂停',
              ifShow: record.status === 'running',
              popConfirm: {
                title: '确认暂停该任务吗？',
                confirm: handlePause.bind(null, record.id),
              },
            },
            {
              icon: 'ant-design:play-circle-outlined',
              tooltip: '恢复',
              ifShow: record.status === 'paused',
              onClick: handleResume.bind(null, record.id),
            },
            {
              icon: 'ant-design:stop-outlined',
              tooltip: '取消',
              color: 'warning',
              ifShow: record.status === 'running' || record.status === 'paused',
              popConfirm: {
                title: '确认取消该任务吗？取消后无法恢复',
                confirm: handleCancel.bind(null, record.id),
              },
            },
            {
              icon: 'ant-design:info-circle-outlined',
              tooltip: '配置详情',
              onClick: openParentTaskConfig.bind(null, record),
            },
            {
              icon: 'ant-design:delete-outlined',
              tooltip: '删除',
              color: 'error',
              ifShow: record.status === 'completed' || record.status === 'failed',
              popConfirm: {
                title: '确认删除该任务吗？',
                confirm: handleDelete.bind(null, record.id),
              },
            },
          ]"
        />
      </template>
    </BasicTable>

    <ParentTaskModal
      :visible="modalVisible"
      :mode="modalMode"
      :loading="submitLoading"
      :initial-value="editingTaskForm"
      :form-options="formOptions"
      @close="closeModal"
      @submit="handleSubmit"
    />
    <SelfIterationStartModal @register="registerStartModal" @success="handleStartSuccess" />
  </PageWrapper>
</template>

<script setup lang="ts">
  import { h, onMounted, onUnmounted, ref } from 'vue';
  import { Button as AButton, Tag } from 'ant-design-vue';
  import { BasicColumn, BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useGo } from '/@/hooks/web/usePage';
  import { PageWrapper } from '/@/components/Page';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useModal } from '/@/components/Modal';
  import { formatToDateTime } from '/@/utils/dateUtil';
  import ChildTaskTable from './components/ChildTaskTable.vue';
  import ParentTaskModal from './components/ParentTaskModal.vue';
  import SelfIterationStartModal from './components/SelfIterationStartModal.vue';
  import {
    createSelfIterationParentTask,
    deleteSelfIterationParentTask,
    fetchSelfIterationChildTasks,
    fetchSelfIterationFormOptions,
    fetchSelfIterationParentTasks,
    updateSelfIterationParentTask,
    pauseSelfIterationParentTask,
    resumeSelfIterationParentTask,
    cancelSelfIterationParentTask,
  } from './api/selfIteration';
  import {
    SelfIterationChildTask,
    SelfIterationFormOptions,
    SelfIterationParentTask,
    SelfIterationParentTaskForm,
  } from './types/selfIteration';

  const go = useGo();
  const { createMessage } = useMessage();

  const submitLoading = ref(false);
  const formOptionsLoading = ref(false);
  const parentTasks = ref<SelfIterationParentTask[]>([]);
  const formOptions = ref<SelfIterationFormOptions>({
    modelOptions: [],
    rtspSourceOptions: [],
    modelApplicationOptions: [],
    deviceFirmwareOptions: [],
    datasetGroupOptions: [],
    labelOptions: [],
    trainParamConfigList: [],
    convertParamConfigList: [],
  });

  const modalVisible = ref(false);
  const modalMode = ref<'create' | 'edit'>('create');
  const editingTaskId = ref<number | null>(null);
  const editingTaskForm = ref<SelfIterationParentTaskForm | null>(null);
  const loadedChildTaskParentIds = ref<number[]>([]);
  const [registerStartModal, { openModal: openStartModal }] = useModal();

  const columns: BasicColumn[] = [
    { title: '任务 ID', dataIndex: 'id', width: 100 },
    { title: '任务名称', dataIndex: 'taskName', width: 260 },
    { title: '当前迭代轮次', dataIndex: 'currentIteration', width: 130 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      customRender: ({ record }) =>
        h(
          Tag,
          { color: parentStatusColorMap[record.status] },
          () => parentStatusTextMap[record.status],
        ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 180,
      customRender: ({ text }) => formatToDateTime(text),
    },
  ];

  const parentStatusTextMap: Record<string, string> = {
    pending: '待启动',
    running: '运行中',
    paused: '已暂停',
    completed: '已完成',
    failed: '失败',
  };

  const parentStatusColorMap: Record<string, string> = {
    running: 'processing',
    paused: 'default',
    completed: 'success',
    failed: 'error',
  };

  async function fetchParentTaskList() {
    const taskData = await fetchSelfIterationParentTasks();
    return [...taskData].sort((a, b) => {
      if (a.id !== b.id) {
        return b.id - a.id;
      }
      return new Date(b.createTime).getTime() - new Date(a.createTime).getTime();
    });
  }

  function applyTableTasks(tasks: SelfIterationParentTask[]) {
    parentTasks.value = tasks;
    setTableData(tasks);
  }

  async function loadData() {
    const sortedTasks = await fetchParentTaskList();
    applyTableTasks(sortedTasks);
    return sortedTasks;
  }

  async function loadFormOptions(silent = true) {
    if (formOptionsLoading.value) {
      return;
    }
    formOptionsLoading.value = true;
    try {
      formOptions.value = await fetchSelfIterationFormOptions();
      if (!silent && formOptions.value.datasetGroupOptions.length === 0) {
        createMessage.warning('未获取到数据集组选项，请确认数据服务状态');
      }
    } catch {
      if (!silent) {
        createMessage.warning('加载表单选项失败，请稍后重试');
      }
    } finally {
      formOptionsLoading.value = false;
    }
  }

  const [registerTable, { reload, setTableData }] = useTable({
    title: '自迭代训练任务列表',
    api: async () => {
      const list = await loadData();
      return {
        items: list,
        total: list.length,
      };
    },
    columns,
    canResize: false,
    useSearchForm: false,
    showTableSetting: false,
    bordered: true,
    showIndexColumn: false,
    expandRowByClick: true,
    pagination: { pageSize: 10 },
    actionColumn: {
      width: 120,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
    },
  });

  async function handleExpand(expanded: boolean, record: SelfIterationParentTask) {
    if (!expanded) {
      return;
    }

    if (!loadedChildTaskParentIds.value.includes(record.id)) {
      loadedChildTaskParentIds.value = [...loadedChildTaskParentIds.value, record.id];
    }

    if (!record.childTasks || record.childTasks.length === 0) {
      // 懒加载子任务
      const childTasks = await fetchSelfIterationChildTasks(record.id);
      const nextTasks = parentTasks.value.map((task) =>
        task.id === record.id ? { ...task, childTasks } : task,
      );
      applyTableTasks(nextTasks);
      // 触发响应式更新
    }
  }

  async function handleCreate() {
    await loadFormOptions(false);
    modalMode.value = 'create';
    editingTaskId.value = null;
    editingTaskForm.value = null;
    modalVisible.value = true;
  }

  function goDataSourceManagement() {
    go('/maData/dataSource');
  }

  function openParentTaskConfig(record: SelfIterationParentTask) {
    go(`/maTrainingCenter/selfIterationDetail?parentId=${record.id}`);
  }

  async function handleDelete(id: number) {
    const ok = await deleteSelfIterationParentTask(id);
    if (ok) {
      createMessage.success('删除成功');
      await reload();
      return;
    }
    createMessage.warning('删除失败，任务不存在');
  }

  function handleStart(id: number) {
    const record = parentTasks.value.find((task) => task.id === id);
    if (!record) {
      createMessage.warning('未找到任务信息');
      return;
    }
    openStartModal(true, { record });
  }

  async function handleStartSuccess() {
    await reload();
  }

  async function handlePause(id: number) {
    const ok = await pauseSelfIterationParentTask(id);
    if (ok) {
      createMessage.success('任务已暂停');
      await reload();
    } else {
      createMessage.error('暂停失败');
    }
  }

  async function handleResume(id: number) {
    const ok = await resumeSelfIterationParentTask(id);
    if (ok) {
      createMessage.success('任务已恢复');
      await reload();
    } else {
      createMessage.error('恢复失败');
    }
  }

  async function handleCancel(id: number) {
    const ok = await cancelSelfIterationParentTask(id);
    if (ok) {
      createMessage.success('任务已取消');
      await reload();
    } else {
      createMessage.error('取消失败');
    }
  }

  function closeModal() {
    modalVisible.value = false;
    modalMode.value = 'create';
    editingTaskId.value = null;
    editingTaskForm.value = null;
  }

  async function handleSubmit(payload: SelfIterationParentTaskForm) {
    submitLoading.value = true;
    try {
      if (modalMode.value === 'create') {
        await createSelfIterationParentTask(payload);
        createMessage.success('自迭代训练任务创建成功');
      } else if (editingTaskId.value !== null) {
        await updateSelfIterationParentTask(editingTaskId.value, payload);
        createMessage.success('自迭代训练任务更新成功');
      }
      modalVisible.value = false;
      modalMode.value = 'create';
      editingTaskId.value = null;
      editingTaskForm.value = null;
      await reload();
    } catch (error) {
      const message =
        (error as any)?.message ||
        (error as any)?.response?.data?.msg ||
        '提交失败，请检查参数配置';
      createMessage.error(message);
    } finally {
      submitLoading.value = false;
    }
  }

  function openChildTaskDetail(record: SelfIterationChildTask) {
    go(
      `/maTrainingCenter/selfIterationDetail?childId=${record.id}&parentId=${record.parentTaskId}`,
    );
  }

  function tasksSnapshot(tasks: SelfIterationParentTask[]): string {
    return tasks.map((t) => `${t.id}:${t.status}:${t.currentIteration}`).join(',');
  }

  function childrenSnapshot(childTasks: SelfIterationParentTask['childTasks']): string {
    return childTasks.map((c) => `${c.id}:${c.status}:${c.currentStep}`).join(',');
  }

  onMounted(() => {
    reload();
    loadFormOptions();
    listTimer = window.setInterval(async () => {
      const prev = parentTasks.value;
      const prevMap = new Map(prev.map((task) => [task.id, task]));
      const loadedParentIds = loadedChildTaskParentIds.value.filter((id) => prevMap.has(id));

      // 静默拉父任务（loadData 同时更新 parentTasks.value 和排序），检测变化
      const freshTasks = await fetchParentTaskList();
      const parentChanged =
        freshTasks.length !== prev.length || tasksSnapshot(freshTasks) !== tasksSnapshot(prev);

      // 检测已展开的子任务变化
      let childChanged = false;
      const latestChildTasksMap = new Map<number, SelfIterationParentTask['childTasks']>();
      for (const parentId of loadedParentIds) {
        const prevTask = prevMap.get(parentId);
        if (!prevTask) continue;
        const freshChildren = await fetchSelfIterationChildTasks(parentId);
        latestChildTasksMap.set(parentId, freshChildren);
        if (childrenSnapshot(freshChildren) !== childrenSnapshot(prevTask.childTasks || [])) {
          childChanged = true;
        }
      }

      if (!parentChanged && !childChanged) return;

      // 有变化，直接用已拉到的数据更新表格，不再重复请求
      const mergedTasks = freshTasks.map((task) => {
        if (!loadedParentIds.includes(task.id)) {
          return task;
        }
        return {
          ...task,
          childTasks: latestChildTasksMap.get(task.id) ?? prevMap.get(task.id)?.childTasks ?? [],
        };
      });

      applyTableTasks(mergedTasks);
    }, 5000);
  });

  let listTimer: number | null = null;

  onUnmounted(() => {
    if (listTimer) window.clearInterval(listTimer);
  });
</script>

<style scoped>
  .page-content {
    margin: 0;
  }
</style>

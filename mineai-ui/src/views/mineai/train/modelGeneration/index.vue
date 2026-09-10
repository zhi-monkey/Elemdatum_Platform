<template>
  <div>
    <BasicTable @register="registerTable" rowKey="id">
      <template #expandedRowRender="{ record }">
        <SubTable
          :ref="(el) => setSubTableRef(el, record.id)"
          :id="record.id"
          :url="jobUrl"
          :train-model-version-image="getTrainModelVersionImage(record)"
          :convert-model-version-image="getConvertModelVersionImage(record)"
          :is-base-model-source="!!record?.modelStore?.generationId"
          :url-prefix="MaBackendUrlEnum.MODEL_MANAGER"
          :columns="jobColumns"
          @conversionSuccess="handleConversionSuccess"
          @success="handleSuccess"
        />
      </template>

      <template #toolbar>
        <Button type="primary" @click="goToAlgManagement">算法管理</Button>
        <Button type="primary" @click="showCreateDrawer">新增任务</Button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:edit-outlined',
              tooltip: '编辑',
              onClick: () => handleEditClick(record),
              ifShow: record.status !== 1,
            },
            {
              icon: 'ant-design:right-circle-outlined',
              tooltip: '开始训练',
              color: 'success',
              onClick: createTrainJob.bind(null, record),
              ifShow: record.status !== 1,
            },
            {
              icon: 'ant-design:down-square-outlined',
              tooltip: '查看训练日志',
              onClick: goTrainJobDetails.bind(null, record),
              ifShow: record.status !== 3,
            },
            {
              icon: 'ant-design:delete-outlined',
              tooltip: '删除标准化任务',
              color: 'error',
              popConfirm: { title: '是否确认删除', confirm: handleDelete.bind(null, record) },
              ifShow: record.status !== 1,
            },
            {
              icon: 'ant-design:stop-outlined',
              tooltip: '停止子任务',
              popConfirm: { title: '是否停止子任务', confirm: handleStop.bind(null, record) },
              ifShow: record.status === 1,
            },
          ]"
          :stopButtonPropagation="true"
        />
      </template>
    </BasicTable>
    <LoadPreTrainWeight
      @register="registerLoadPreTrainWeight"
      @success="handleSuccess"
      :modelGenerationId="modelGenerationId"
    />
    <GenerationCreate
      :visible="createVisible"
      @close="closeCreateDrawer"
      @submit-success="handleSubmitSuccess"
    />
    <GenerationEdit
      @close="closeEditDrawer"
      :modelGenerationParams="modelGenerationParams"
      @register="editModalRegister"
    />
  </div>
</template>
<script lang="ts" setup>
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import SubTable from './SubTable.vue';
  import { Button, Tag } from 'ant-design-vue';
  import { columns, searchFormSchema } from './modelGeneration';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { h, onMounted, onUnmounted, Ref, ref } from 'vue';
  import { useGo } from '/@/hooks/web/usePage';
  import { useUserStore } from '/@/store/modules/user';
  import LoadPreTrainWeight from '/@/views/mineai/train/modelGeneration/trainJobDetail/LoadPreTrainWeight.vue';
  import { useModal } from '/@/components/Modal';
  import GenerationCreate from '/@/views/mineai/train/generationCreate/index.vue';
  import GenerationEdit from '/@/views/mineai/train/generationEdit/index.vue';
  import { Recordable } from 'vite-plugin-mock';

  const [registerLoadPreTrainWeight, { openModal: openLoadPreTrainWeight }] = useModal();
  const sourceGenerationCache = ref<Record<number, Recordable>>({});

  const getModelVersionUrl = (record: Recordable, versionField: string) => {
    const currentModelUrl =
      record?.[versionField]?.url ||
      record?.model?.[versionField]?.url ||
      record?.modelStore?.[versionField]?.url;
    if (currentModelUrl) return currentModelUrl;

    if (versionField === 'trainModelVersion') {
      const trainJobModelVersionUrl = record?.modelStore?.trainModelJob?.modelVersion?.url;
      if (trainJobModelVersionUrl) return trainJobModelVersionUrl;
    }

    const sourceGeneration = record?.sourceGeneration;
    return (
      sourceGeneration?.[versionField]?.url ||
      sourceGeneration?.model?.[versionField]?.url ||
      sourceGeneration?.modelStore?.[versionField]?.url ||
      (versionField === 'trainModelVersion'
        ? sourceGeneration?.modelStore?.trainModelJob?.modelVersion?.url
        : '') ||
      ''
    );
  };

  const getTrainModelVersionImage = (record: Recordable) =>
    getModelVersionUrl(record, 'trainModelVersion');

  const getConvertModelVersionImage = (record: Recordable) =>
    getModelVersionUrl(record, 'convertModelVersion');

  const getSourceGeneration = async (generationId?: number) => {
    if (!generationId) return null;
    if (sourceGenerationCache.value[generationId]) return sourceGenerationCache.value[generationId];

    const sourceGeneration = await maHttp.get(
      {
        url: 'modelGeneration/findModelGenerationById',
        params: { id: generationId },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    sourceGenerationCache.value[generationId] = sourceGeneration;
    return sourceGeneration;
  };

  const enrichModelVersionFromSourceGeneration = async (record: Recordable) => {
    if (getTrainModelVersionImage(record) && getConvertModelVersionImage(record)) return;

    const generationId = record?.modelStore?.generationId;
    if (!generationId) return;

    try {
      const sourceGeneration = await getSourceGeneration(generationId);
      if (!sourceGeneration) return;

      record.sourceGeneration = sourceGeneration;
      if (!record.modelStore.trainModelVersion) {
        record.modelStore.trainModelVersion =
          sourceGeneration.trainModelVersion ||
          sourceGeneration.model?.trainModelVersion ||
          sourceGeneration.modelStore?.trainModelVersion ||
          sourceGeneration.modelStore?.trainModelJob?.modelVersion;
      }
      if (!record.modelStore.convertModelVersion) {
        record.modelStore.convertModelVersion =
          sourceGeneration.convertModelVersion ||
          sourceGeneration.model?.convertModelVersion ||
          sourceGeneration.modelStore?.convertModelVersion;
      }
    } catch (error) {
      console.warn('补齐基础算法库镜像信息失败', error);
    }
  };

  const enrichModelGenerationRows = async (rows: Recordable[]) => {
    await Promise.all((rows || []).map((row) => enrichModelVersionFromSourceGeneration(row)));
  };

  const [editModalRegister, { openModal: openEditModal }] = useModal();

  const jobColumns = [
    {
      title: 'id',
      dataIndex: 'id',
      width: 20,
      align: 'center',
    },
    {
      title: '子任务名称',
      dataIndex: 'name',
      width: 100,
      align: 'center',
    },
    {
      title: '作业状态',
      dataIndex: 'status',
      width: 60,
      align: 'center',
      customRender: ({ record }) => {
        let text;
        let color;

        switch (record.jobType) {
          case 1: // Training
            switch (record.status) {
              case 0: // CREATE_SUCCESS
                text = '创建成功';
                color = 'green';
                break;
              case 1: // TRAINING
                text = '训练中';
                color = 'yellow';
                break;
              case -1: // PAUSED
                text = '训练暂停';
                color = 'grey';
                break;
              case -10: // CANCELED
                text = '作业取消';
                color = 'grey';
                break;
              case -2: // TRAIN_FAILED
                text = '训练失败';
                color = 'red';
                break;
              case 2: // TRAIN_SUCCEEDED
                text = '训练成功';
                color = 'green';
                break;
              case 3: // INSPECTING
                text = '质检中';
                color = 'yellow';
                break;
              case -3: // INSPECT_FAILED
                text = '质检失败';
                color = 'red';
                break;
              case 4: // INSPECT_SUCCEEDED
                text = '质检成功';
                color = 'green';
                break;
              case 5: // CONVERTING
                text = '转换中';
                color = 'yellow';
                break;
              case -5: // CONVERT_FAILED
                text = '转换失败';
                color = 'red';
                break;
              case 6: // CONVERT_SUCCEEDED
                text = '转换成功';
                color = 'green';
                break;
              case 7: // PUBLISHED
                text = '已发布';
                color = 'blue';
                break;
              case 11:
                text = '排队中';
                color = 'yellow';
                break;
              case 12:
                text = '数据切分中';
                color = 'yellow';
                break;
              default:
                text = '未知状态';
                color = 'black';
                break;
            }
            break;

          case 2: // Inspecting
            switch (record.status) {
              case 3: // INSPECTING
                text = '质检中';
                color = 'yellow';
                break;
              case 4: // INSPECT_SUCCEEDED
                text = '质检成功';
                color = 'green';
                break;
              case -3: // INSPECT_FAILED
                text = '质检失败';
                color = 'red';
                break;
              case -10: // CANCELED
                text = '作业取消';
                color = 'grey';
                break;
              case 0: // CREATE_SUCCESS
                text = '创建成功';
                color = 'green';
                break;
            }
            break;

          case 3: // Converting
            switch (record.status) {
              case 5: // CONVERTING
                text = '转换中';
                color = 'yellow';
                break;
              case 6: // CONVERT_SUCCEEDED
                text = '转换成功';
                color = 'green';
                break;
              case -5: // CONVERT_FAILED
                text = '转换失败';
                color = 'red';
                break;
              case -10: // CANCELED
                text = '作业取消';
                color = 'grey';
                break;
              case 0: // CREATE_SUCCESS
                text = '创建成功';
                color = 'green';
                break;
            }
            break;

          default:
            text = '未知状态';
            color = 'red';
        }

        return h(Tag, { color: color }, () => text);
      },
    },
    { title: '任务开始时间', dataIndex: 'createTime', width: 80, align: 'center' },
    {
      title: '任务结束时间',
      dataIndex: 'lastJobTime',
      width: 80,
      align: 'center',
      customRender: ({ record }) => {
        // 执行中的状态不显示结束时间
        // 1:训练中, 3:质检中, 5:转换中, 11:排队中, 12:数据切分中, 0:创建成功
        const runningStatuses = [0, 1, 3, 5, 11, 12];
        console.log(record.status);
        if (runningStatuses.includes(record.status)) {
          return '-';
        }
        return record.lastJobTime || '-';
      },
    },
    {
      title: '操作',
      dataIndex: 'action',
      width: 80,
      align: 'center',
      slots: { customRender: 'action' },
    },
  ];
  // 子表引用
  const subTableRefs = ref<Record<number, InstanceType<typeof SubTable>>>({});
  //const subTableRefs = ref<Record<number, any>>({});
  const userStore = useUserStore();
  const userData = userStore.getUserInfo;
  const userId = ref();
  const go = useGo();
  const mgTrainUpdateList = ref([]);
  const mgTrainWaitList = ref([]);
  const timerTrain: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);
  const timerTrainWait: Ref<NodeJS.Timer> = ref({} as NodeJS.Timer);
  const { createMessage } = useMessage();
  const jobUrl = 'modelJob/findModelJobsByModelGenerationId?modelGenerationId=';
  //刷新状态
  const loading = ref(true);
  const modelGenerationId = ref(0);

  const [registerTable, { reload, updateTableDataRecord }] = useTable({
    title: '标准化训练任务列表',
    canResize: false,
    expandRowByClick: true,
    rowKey: 'id',
    api: async (params) => {
      return await maHttp
        .get(
          {
            url: 'modelGeneration/dynamicFindUnGuidedModelGenerationPage',
            params: { ...params, userId: userId.value },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        )
        .then((v) => {
          //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
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
    afterFetch: async (v) => {
      // GET请求被解析为 VBen表格Table的行后，做一些操作
      await enrichModelGenerationRows(v);
      const ids = v.map((m) => m.id);
      if (ids.length > 0) {
        //获取所有正在训练
        mgTrainUpdateList.value = v.filter((m) => m.status === 1);
        //获取所有延时训练任务等待中
        mgTrainWaitList.value = v.filter((m) => m.status === 12);
        if (mgTrainUpdateList.value.length > 0) {
          clearInterval(timerTrain.value);
          timerTrain.value = setInterval(() => updateMGTrainStatus(), 3 * 1000);
        }
        if (mgTrainWaitList.value.length > 0) {
          clearInterval(timerTrainWait.value);
          timerTrainWait.value = setInterval(() => updateMGTrainWaitStatus(), 3 * 1000);
        }
      }
      return v;
    },
    columns,
    formConfig: {
      labelWidth: 120,
      schemas: searchFormSchema,
      showAdvancedButton: false,
    },
    useSearchForm: true,
    showTableSetting: false,
    bordered: true,
    showIndexColumn: false,
    actionColumn: {
      width: 150,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  });

  function setSubTableRef(el: any, id: number) {
    if (el) {
      subTableRefs.value[id] = el; // 存储子表的引用
    } else {
      delete subTableRefs.value[id]; // 移除引用
    }
  }

  //跳转至详情日志页面
  function goTrainJobDetails(record: Recordable) {
    go(`/maTrainingCenter/trainJobDetails/${record.latestJobId}_1`);
  }

  //训练任务的状态更新
  async function updateMGTrainStatus() {
    // 状态变更完成，关闭定时器
    if (mgTrainUpdateList.value.length === 0) {
      clearInterval(timerTrain.value);
      return;
    }
    //查询状态是否发生变化
    const result = await getMGStatus({
      mgIds: mgTrainUpdateList.value.map((e) => {
        return e.id;
      }),
    });
    let updateList: any[] = [];
    // 用于保存需要更新的行
    Object.keys(result).forEach((e) => {
      const newStatus = result[e];

      const currentRow = mgTrainUpdateList.value.find((m) => m.id === Number(e));

      if (!currentRow) {
        return; // 如果找不到当前行，直接跳过
      }

      const currentStatus = currentRow.status;

      // 判断如果新状态和表格中的状态不同，才加入更新列表
      if (newStatus !== currentStatus) {
        updateList.push({
          id: Number(e),
          status: Number(newStatus),
        });
        // 移除已经更新状态的任务
        const removeIndex = mgTrainUpdateList.value.findIndex((m) => m.id === Number(e));
        if (removeIndex >= 0) {
          mgTrainUpdateList.value.splice(removeIndex, 1);
        }
      }
    });
    // 外层状态未变更时，子表状态仍可能变化；
    Object.keys(subTableRefs.value).forEach((key) => {
      const id = Number(key);
      if (Number.isNaN(id)) return;
      const inUpdateList = mgTrainUpdateList.value.some((m) => m.id === id);
      if (!inUpdateList) return;
      const latestStatus = Number((result as any)[id]);
      if (latestStatus !== 1) return;
      const subTableRef = subTableRefs.value[id];
      subTableRef && subTableRef.getSubData && subTableRef.getSubData();
    });
    // 如果有需要更新的记录
    if (updateList.length > 0) {
      // 更新该行 api
      updateList.forEach((e) => {
        updateTableDataRecord(e.id, e);
        let subTableRef = subTableRefs.value[e.id];
        subTableRef && subTableRef.getSubData(e);
      });
    }
  }

  //等待训练任务状态更新
  async function updateMGTrainWaitStatus() {
    // 状态变更完成，关闭定时器
    if (mgTrainWaitList.value.length === 0) {
      clearInterval(timerTrainWait.value);
      return;
    }

    //查询状态是否发生变化
    const result = await getMGStatus({
      mgIds: mgTrainWaitList.value.map((e) => e.id),
    });
    let updateList: any[] = [];
    // 用于保存需要更新的行
    Object.keys(result).forEach((e) => {
      const status = result[e];
      //判断质检中
      if (status === 1 || status === 2 || status === -2) {
        updateList.push({
          id: Number(e),
          status: Number(status),
        });
        // 从等待列表中移除已完成的任务
        const removeIndex = mgTrainWaitList.value.findIndex((m) => m.id === Number(e));
        if (removeIndex >= 0) {
          mgTrainWaitList.value.splice(removeIndex, 1);
        }
      }
    });
    if (updateList.length > 0) {
      // 更新该行 api
      updateList.forEach((e) => {
        updateTableDataRecord(e.id, e);
        let subTableRef = subTableRefs.value[e.id];
        subTableRef && subTableRef.getSubData(e);
      });
    }
  }

  /**
   * 获取生产任务状态信息
   *
   * @param params
   */
  const getMGStatus = async (params) => {
    return await maHttp.get(
      {
        url: 'modelGeneration/getMGStatus',
        params,
        headers: {},
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
  };

  // 监听成功事件，触发任务列表刷新
  const handleSuccess = (modelGenerationId: number) => {
    //console.log('弹窗确认成功，modelGenerationId:', modelGenerationId);
    handleModalSuccess(modelGenerationId);
    reload(); // 调用刷新和状态更新
  };

  // 创建任务时打开弹窗
  function createTrainJob(record: Recordable) {
    // 总是打开弹窗，将 record 传递过去，让弹窗内部处理逻辑
    openLoadPreTrainWeight(true, { record });
  }

  // 刷新子列表
  function refreshSubTable(modelGenerationId: number) {
    const subTableRef = subTableRefs.value[modelGenerationId]; // 获取对应的子列表引用
    if (subTableRef && subTableRef.getSubData) {
      subTableRef.getSubData(); // 调用子表的刷新方法
    } else {
      console.error('子列表不存在');
    }
  }

  // 弹窗确认后的回调
  function handleModalSuccess(modelGenerationId: number) {
    reload();
    refreshSubTable(modelGenerationId);
  }

  /**
   * 删除生产任务
   *
   * @param record
   */
  function handleDelete(record: Recordable) {
    maHttp
      .get(
        {
          url: 'modelGeneration/deleteModelGeneration',
          params: { modelGenerationId: record.id },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('标准化任务删除成功！');
        reload();
      });
  }

  function handleStop(record: Recordable) {
    maHttp
      .get(
        {
          url: 'modelJob/stopJob',
          params: { modelGenerationId: record.id },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('子任务停止成功！');
        reload();
      });
  }

  onMounted(async () => {
    loading.value = false;
    let isAdmin = false;
    userData.roles.forEach((role) => {
      if (role.id === 1) {
        isAdmin = true;
      }
    });
    if (!isAdmin) {
      userId.value = userData.id;
    }
  });

  onUnmounted(() => {
    //退出时关闭定时器
    clearInterval(timerTrain.value);
    clearInterval(timerTrainWait.value);
  });

  const createVisible = ref(false);
  const currentRowRecord = ref(null);

  function showCreateDrawer() {
    createVisible.value = true;
    currentRowRecord.value = null; // 创建新任务时重置
  }

  // 定义一个响应式对象来存储参数
  const modelGenerationParams = ref({});

  function handleEditClick(record: Recordable) {
    modelGenerationParams.value = record;
    openEditModal(true, record);
  }

  function handleSubmitSuccess(successStatus) {
    if (successStatus) {
      reload(); // 重新加载数据或执行相应操作
    }
  }

  function closeCreateDrawer() {
    createVisible.value = false;
  }

  function closeEditDrawer() {
    reload();
  }

  function handleConversionSuccess() {
    reload();
  }

  function goToAlgManagement() {
    go(`/maTrainingCenter/algorithmManage`);
  }
</script>

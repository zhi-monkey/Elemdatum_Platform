<template>
  <a-table
    size="small"
    row-key="id"
    :columns="columns"
    :pagination="false"
    :loading="loading"
    :data-source="data"
    :showIndexColumn="true"
  >
    <template #action="{ record }">
      <TableAction
        :actions="[
          {
            icon: 'ant-design:down-square-outlined',
            tooltip: '详情和日志',
            color: 'success',
            onClick: goTrainJobDetails.bind(null, record),
            ifShow: record.jobType !== 3,
          },
          {
            icon: 'ant-design:file-done-outlined',
            tooltip: '模型验证',
            color: 'success',
            onClick: goModalValidate.bind(null, record),
            ifShow:
              ![-10, -2, -1, 1, 12].includes(record.status) &&
              record.jobType !== 3 &&
              trainModelVersionImage.length > 0,
          },
          {
            icon: 'ant-design:retweet-outlined',
            tooltip: '模型转换',
            color: 'success',
            onClick: goModelConvert.bind(null, record),
            ifShow:
              ![-10, -2, -1, 1, 5, 7, 12].includes(record.status) &&
              record.jobType !== 3 &&
              convertModelVersionImage.length > 0,
          },
          {
            icon: 'ant-design:interaction-outlined',
            tooltip: '转换记录',
            color: 'success',
            onClick: handleConvertHistory.bind(null, record),
            ifShow: record.jobType !== 3 && record.convertHistory > 0,
          },
          {
            icon: 'ant-design:cloud-download-outlined',
            tooltip: '模型下载',
            color: 'success',
            onClick: handleDownloadWeight.bind(null, record),
            ifShow: [2, 4, 6, -10].includes(record.status) && record.jobType !== 3,
          },
          {
            icon: 'ant-design:send-outlined',
            tooltip: '算法任务模板发布',
            color: 'success',
            onClick: handleModelRelease.bind(null, record),
            ifShow:
              [2, 4, 6].includes(record.status) &&
              record.jobType !== 3 &&
              // 有转换才能发布
              convertModelVersionImage.length > 0,
          },
          {
            icon: 'ant-design:line-outlined',
            color: 'warning',
            ifShow: record.jobType === 3,
          },
        ]"
      />
    </template>
  </a-table>
  <!--  <ConvertModal @register="register" @success="handleModalConvert" />-->
  <ConvertModal ref="convertModalRef" @success="handleModalConvert" />
  <ConvertHistoryModal @register="registerCH" />
  <ModelChip @register="registerModelChip" @success="checkTaskStatusAndUpdate" />
  <ModelDownloadModal @register="registerModelDownload" @success="modelDownloadSuccess" />
</template>

<script lang="ts" setup>
  import { TableAction } from '/@/components/Table';
  import { Table as ATable } from 'ant-design-vue';
  import { defineEmits, onMounted, onUnmounted, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { maHttp } from '/@/utils/http/axios';
  import { useGo } from '/@/hooks/web/usePage';
  import { useUserStore } from '/@/store/modules/user';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ConvertModal from '/@/views/mineai/train/modelGeneration/modal/ConvertModal.vue';
  import ConvertHistoryModal from '/@/views/mineai/train/modelGeneration/modal/ConvertHistoryModal.vue';
  import { useModal } from '/@/components/Modal';
  import ModelChip from '/@/views/mineai/application/basicAlgorithm/component/ModelChip.vue';
  import ModelDownloadModal from '/@/views/mineai/train/modelGeneration/ModelDownloadModal.vue';

  const convertModalRef = ref();
  const emits = defineEmits(['success', 'conversionSuccess']);
  const { createMessage } = useMessage();
  const userStore = useUserStore();
  const router = useRouter();
  const go = useGo();
  const userData = userStore.getUserInfo;
  const props = defineProps<{
    id: number;
    columns: Array<Object>;
    url: string;
    urlPrefix: string;
    trainModelVersionImage: string | '';
    convertModelVersionImage: string | '';
    isBaseModelSource?: boolean;
  }>();

  // 定义一个类型，每个任务对象都有 id 和 status 字段
  type Task = {
    id: number;
    status: number;
  };

  // 定义 data 为 Task 类型数组的响应式变量
  const data = ref<Task[]>([]);
  const loading = ref(true);
  const convertMap = ref<Map<string, object[]>>(new Map());
  const [registerCH, { openModal: openCHModel }] = useModal();
  const [registerModelDownload, { openModal: openModelDownload }] = useModal();
  const [registerModelChip, { openModal: openModelChip }] = useModal();
  // 定义一个定时器变量
  const timer = ref<ReturnType<typeof setInterval> | null>(null);

  function goTrainJobDetails(record: Recordable) {
    go(`/maTrainingCenter/trainJobDetails/${record.id}_${record.jobType}`);
  }

  /**
   * 模型发布成功！！
   */
  // function handleSuccess() {
  //   createMessage.success('发布成功！');
  // }

  /**
   * 模型验证
   *
   * @param record
   */
  async function goModalValidate(record: Recordable) {
    await router.push({
      path: `/maTrainingCenter/modelValidate/${record.id}`,
      state: {
        id: record.id,
        image: props.trainModelVersionImage,
        weightPath: record.name,
        gpuNum: record.gpus,
        params: {
          MODE_WORKING_MODE: 4,
        },
      },
    });
  }

  /**
   * 发布模型
   *
   * @param record
   */
  function handleModelRelease(record: Recordable) {
    openModelChip(true, {
      modelGenerationId: props.id,
      bestWeightPath: record.weightPath,
      modelJobId: record.id,
      description: record.description,
    });
  }

  /**
   * 模型转换
   *
   * @param record
   */
  function goModelConvert(record: Recordable) {
    convertModalRef.value.showModal({
      modelGenerationId: props.id,
      image: props.convertModelVersionImage,
      weightPath: record.name,
      gpuNum: record.gpus,
      jobId: record.id,
      isBaseModelSource: !!props.isBaseModelSource,
    });
  }

  async function handleModalConvert() {
    await getSubData();
    startTimer(); // 开始状态自动更新
    emits('conversionSuccess');
  }

  const handleDownloadWeight = (record: Recordable) => {
    openModelDownload(true, {
      jobId: record.id,
    });
  };

  const modelDownloadSuccess = () => {
    createMessage.success('下载成功！');
  };

  function handleConvertHistory(record: Recordable) {
    openCHModel(true, { history: convertMap.value.get(record.name) });
  }

  async function getSubData() {
    //loading.value = true;
    await maHttp
      .post(
        {
          url: props.url + props.id.toString(),
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: props.urlPrefix },
      )
      .then((resp) => {
        // console.log(resp);
        const responseData: Task[] = resp; // 确保 resp 是 Task 类型数组
        data.value = responseData;
        let filteredData: Task[] = [];
        data.value.forEach((item) => {
          if (item.jobType === 3) {
            const end = item.name.indexOf('-convert-');
            if (end > 0) {
              const key = item.name.substring(0, end);
              if (convertMap.value.has(key)) {
                let values = convertMap.value.get(key);
                if (!values || values.length <= 0) values = [item];
                else {
                  const exists = values.some((existingItem) => existingItem.id === item.id);
                  if (!exists) values.push(item);
                }
                convertMap.value.set(key, values);
              } else {
                const values = [item];
                convertMap.value.set(key, values);
              }
            }
          } else {
            filteredData.push(item);
          }
        });
        filteredData.forEach((item) => {
          if (convertMap.value.has(item.name)) {
            item.convertHistory = convertMap.value.get(item.name)
              ? convertMap.value.get(item.name)?.length
              : 0;
          } else {
            item.convertHistory = 0;
          }
        });
        data.value = filteredData;
        loading.value = false;
      });
  }

  async function checkTaskStatusAndUpdate() {
    await getSubData();
  }

  // 公开 getSubData 方法，使父组件可以通过 ref 调用
  defineExpose({
    getSubData,
    checkTaskStatusAndUpdate,
    startTimer,
  });

  // 添加定时器来定期检查任务状态
  function startTimer() {
    // 检查定时器是否已经存在，防止重复启动
    if (timer.value === null) {
      timer.value = setInterval(async () => {
        await getSubData();
        // 检查是否所有任务都已完成或失败或者已发布，如果是则停止定时器
        const allTasksFinished = data.value.every(
          (task) =>
            task.status === 2 ||
            task.status === -2 ||
            task.status === 6 ||
            task.status === -5 ||
            task.status === 7,
        );
        if (allTasksFinished && timer.value !== null) {
          clearInterval(timer.value); // 停止定时器
          timer.value = null; // 避免多次清除
        }
      }, 10000); // 每10秒检查一次
    }
  }

  onMounted(() => {
    loading.value = true;
    getSubData(); // 初始获取表格数据
  });

  onUnmounted(() => {
    // 清理定时器
    clearInterval(timer.value);
    timer.value = null;
  });

  // onUnmounted(() => {
  //   if (timer.value !== null) {
  //     clearInterval(timer.value);
  //     timer.value = null; // 避免多次清除
  //   }
  // });
</script>

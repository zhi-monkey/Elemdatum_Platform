<template>
  <ACard style="padding: 20px; background: transparent !important">
    <template #title>
      <h2 style="font-size: 18px; margin: 0">数据集：{{ datasetName }}</h2>
    </template>
    <template #extra>
      <AButton type="primary" @click="goDetail">查看详情</AButton>
    </template>

    <a-skeleton :loading="isLoading" active :paragraph="{ rows: 5 }">
      <ARow :gutter="16">
        <ACol :span="12" style="margin-bottom: 16px">
          <AStatistic
            title="数据集已标注图片数量/总数"
            :value="`${labeledImageCount}/${totalImageCount}`"
            :value-style="{ color: '#1890ff', fontSize: '20px' }"
          />
        </ACol>
        <ACol :span="12" style="margin-bottom: 16px">
          <AStatistic
            title="数据集已抽帧视频数量/总数"
            :value="`${extractedVideoCount}/${totalVideoCount}`"
            :value-style="{ color: '#52c41a', fontSize: '20px' }"
          />
        </ACol>
        <ACol :span="12">
          <AStatistic
            title="标注类型"
            :value="annotateTypeText"
            :value-style="{ color: '#722ed1', fontSize: '20px' }"
          />
        </ACol>
        <ACol :span="12">
          <AStatistic
            title="数据集状态"
            :value="statusText"
            :value-style="{ color: statusColor, fontSize: '20px' }"
          />
        </ACol>
        <ACol :span="24" style="margin-top: 16px">
          <ARow :gutter="16">
            <ACol :span="12" v-if="showTaskProgress">
              <AStatistic
                :title="getTaskTitle()"
                :value="`${taskProgress}%`"
                :value-style="{ color: '#fa8c16', fontSize: '20px' }"
              />
            </ACol>
            <ACol :span="12" v-if="showRemainTime">
              <AStatistic
                title="剩余时间"
                :value="remainTimeText"
                :value-style="{ color: '#fa8c16', fontSize: '20px' }"
              />
            </ACol>
          </ARow>
        </ACol>
      </ARow>
    </a-skeleton>
  </ACard>
</template>

<script lang="ts" setup>
  import { computed, defineProps, onMounted, onUnmounted, ref, watch } from 'vue';
  import {
    Button as AButton,
    Card as ACard,
    Col as ACol,
    Row as ARow,
    Skeleton as ASkeleton,
    Statistic as AStatistic,
  } from 'ant-design-vue';
  import { getDatasetCardInfo } from '/@/views/mineai/train/generationGuideList/detail/dataset/component/api';
  import { useGo } from '/@/hooks/web/usePage';
  import { guidedTrainEventEmitter } from '/@/views/mineai/train/generationGuideList/detail/train/intervalBus';

  const go = useGo();
  const emit = defineEmits([
    'update:status',
    'update:total-image-count',
    'update:total-video-count',
  ]);

  const props = defineProps<{
    createdDatasetId: number;
  }>();

  // 新增loading状态
  const isLoading = ref(true);
  const datasetName = ref('');
  const labeledImageCount = ref(0);
  const totalImageCount = ref(0);
  const extractedVideoCount = ref(0);
  const totalVideoCount = ref(0);
  const module = ref(0);
  const annotateType = ref(0);
  const statusText = ref('');
  const statusColor = ref('gray');
  const response = ref<any>(null);
  const taskProgress = ref(0); // 新增：任务进度
  const showTaskProgress = ref(false); // 新增：是否显示任务进度
  const remainTime = ref(''); // 新增：剩余时间
  const showRemainTime = ref(false); // 新增：是否显示剩余时间
  const remainTimeText = computed(() => formatRemainTime(remainTime.value)); // 格式化后的剩余时间

  // 根据annotateType动态显示标注类型
  const annotateTypeText = computed(() => {
    switch (annotateType.value) {
      case 102:
        return '目标检测';
      case 103:
        return '目标分割';
      default:
        return '未知类型';
    }
  });

  // 组件内定义变量
  const intervalId = ref<NodeJS.Timeout | null>(null);
  const previousData = ref<any>(null);
  const noChangeCount = ref(0);

  const statusMap = {
    101: { text: '未标注', color: 'gray' },
    102: { text: '标注中', color: 'yellow' },
    105: { text: '已标注', color: 'green' },
    302: { text: '抽帧中', color: 'blue' },
    401: { text: '数据增强中', color: 'purple' },
    403: { text: '导入中', color: 'brown' },
    103: { text: '自动标注中', color: 'orange' },
    104: { text: '自动标注完成', color: 'green' },
    1: { text: '上传中', color: 'cyan' },
  };

  const formatRemainTime = (timeValue) => {
    if (timeValue === -1) return '--';
    if (timeValue) return `${timeValue}s`;
    return '';
  };

  // 在setup函数中的代码
  let stopWatch: () => void;

  onMounted(() => {
    stopWatch = watch(
      () => props.createdDatasetId,
      async (newId) => {
        if (newId !== 0) {
          try {
            isLoading.value = true;
            response.value = await getDatasetCardInfo(newId);

            // 完整数据赋值
            datasetName.value = response.value.name;
            labeledImageCount.value = response.value.labeledImageCount;
            totalImageCount.value = response.value.totalImageCount;
            extractedVideoCount.value = response.value.extractedVideoCount;
            totalVideoCount.value = response.value.totalVideoCount;
            module.value = response.value.module;
            annotateType.value = response.value.annotateType;
            taskProgress.value = response.value.progress || 0;
            remainTime.value = response.value.remainTime || '';
            showTaskProgress.value = shouldShowTaskProgress(response.value.status);
            showRemainTime.value = shouldShowRemainTime(response.value.status);

            // 触发双向绑定更新
            emit('update:total-image-count', totalImageCount.value);
            emit('update:total-video-count', totalVideoCount.value);

            // 状态处理逻辑
            let statusInfo = statusMap[response.value.status] || {
              text: '未知状态',
              color: 'gray',
            };

            if (response.value.dataConversion === 4) {
              statusInfo = {
                text: '保存版本中',
                color: 'orange',
              };
            }

            statusText.value = statusInfo.text;
            statusColor.value = statusInfo.color;
          } catch (error) {
            console.error('数据加载失败:', error);
          } finally {
            isLoading.value = false;
            // 满足条件后立即停止监听
            stopWatch();
          }
        }
      },
      { immediate: true }, // 立即执行初始检查
    );
    guidedTrainEventEmitter.on('datasetCardStatusRefresh', handleRefresh);

    guidedTrainEventEmitter.on('datasetCardStatusPoll', () => {
      // 重置状态计数器
      noChangeCount.value = 0;

      // 清除已有轮询
      if (intervalId.value) {
        clearInterval(intervalId.value);
      }

      // 立即执行首次请求
      enhancedPoll();
      // 启动间隔轮询
      intervalId.value = setInterval(enhancedPoll, 1000);
    });

    //初始化的时候默认启动一次轮询。防止切换的时候刷新掉了

    // 重置状态计数器
    noChangeCount.value = 0;

    // 清除已有轮询
    if (intervalId.value) {
      clearInterval(intervalId.value);
    }

    // 立即执行首次请求
    enhancedPoll();
    // 启动间隔轮询
    intervalId.value = setInterval(enhancedPoll, 1000);
  });

  // 安全停止的兜底处理
  onUnmounted(() => {
    if (stopWatch) {
      stopWatch();
    }
    guidedTrainEventEmitter.off('datasetCardStatusRefresh');
    if (intervalId.value) {
      clearInterval(intervalId.value);
    }
    guidedTrainEventEmitter.off('datasetCardStatusPoll');
  });

  // 新增：判断是否显示任务进度
  const shouldShowTaskProgress = (status) => {
    return [302, 401, 103, 1].includes(status);
  };

  // 新增：判断是否显示剩余时间
  const shouldShowRemainTime = (status) => {
    return [103].includes(status); // 自动标注中显示剩余时间
  };

  watch(
    () => response.value?.status,
    (newStatus) => {
      emit('update:status', newStatus);
      // 新增：状态变化时更新进度显示
      if (response.value) {
        showTaskProgress.value = shouldShowTaskProgress(newStatus);
        showRemainTime.value = shouldShowRemainTime(newStatus);
        taskProgress.value = response.value.progress || 0;
        remainTime.value = response.value.remainTime || '';
      }
    },
  );

  function goDetail() {
    go(
      `/maData/fileDetail/${props.createdDatasetId}/${datasetName.value}/${module.value}/${annotateType.value}`,
    );
  }

  async function handleRefresh() {
    response.value = await getDatasetCardInfo(props.createdDatasetId);
    // 完整数据赋值
    datasetName.value = response.value.name;
    labeledImageCount.value = response.value.labeledImageCount;
    totalImageCount.value = response.value.totalImageCount;
    extractedVideoCount.value = response.value.extractedVideoCount;
    totalVideoCount.value = response.value.totalVideoCount;
    module.value = response.value.module;
    annotateType.value = response.value.annotateType;
    taskProgress.value = response.value.progress || 0;
    remainTime.value = response.value.remainTime || '';
    showTaskProgress.value = shouldShowTaskProgress(response.value.status);
    showRemainTime.value = shouldShowRemainTime(response.value.status);

    // 触发双向绑定更新
    emit('update:total-image-count', totalImageCount.value);
    emit('update:total-video-count', totalVideoCount.value);

    // 状态处理逻辑
    let statusInfo = statusMap[response.value.status] || {
      text: '未知状态',
      color: 'gray',
    };

    if (response.value.dataConversion === 4) {
      statusInfo = {
        text: '保存版本中',
        color: 'orange',
      };
    }

    statusText.value = statusInfo.text;
    statusColor.value = statusInfo.color;
  }

  // 轮询检查函数
  const checkStopCondition = () => {
    const currentData = {
      labeledImageCount: labeledImageCount.value,
      totalImageCount: totalImageCount.value,
      extractedVideoCount: extractedVideoCount.value,
      totalVideoCount: totalVideoCount.value,
      module: module.value,
    };

    // 数据比对
    if (JSON.stringify(currentData) === JSON.stringify(previousData.value)) {
      noChangeCount.value++;
    } else {
      noChangeCount.value = 0;
      previousData.value = currentData;
    }

    // 状态检查（使用原始状态值）
    const validStatuses = [101, 102, 105];
    return validStatuses.includes(response.value.status) && noChangeCount.value >= 3;
  };

  // 增强版轮询函数
  const enhancedPoll = async () => {
    await handleRefresh();

    // 发出状态变化事件，传递当前状态
    guidedTrainEventEmitter.emit('datasetStatusChanged', {
      status: response.value.status,
      dataConversion: response.value.dataConversion,
      progress: taskProgress.value, // 新增：传递进度值
      remainTime: remainTime.value, // 新增：传递剩余时间
    });

    if (checkStopCondition()) {
      if (intervalId.value) {
        clearInterval(intervalId.value);
        intervalId.value = null;
      }
      console.log('Polling stopped due to stable data and terminal status');
    }
  };

  // 新增：根据状态获取任务标题
  const getTaskTitle = () => {
    switch (response.value?.status) {
      case 103: // 自动标注中
        return '自动标注任务进度';
      case 302: // 抽帧中
        return '抽帧任务进度';
      case 401: // 数据增强中
        return '数据增强任务进度';
      // case 403: // 导入中
      //   return '导入任务进度';
      case 1: // 上传中
        return '上传任务进度';
      default:
        return '当前任务进度'; // 默认标题
    }
  };
</script>

<style scoped lang="scss"></style>

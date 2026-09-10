<template>
  <div id="dataset-detail">
    <Card :bordered="true" class="info-card">
      <p
        >已抽帧视频 <span class="extractedVideos">{{ extractedVideos }}</span></p
      >
      <p
        >未抽帧视频 <span class="unextractedVideos">{{ unextractedVideos }}</span></p
      >
      <p
        >视频总数 <span class="totalVideos">{{ totalVideos }}</span></p
      >
      <p v-if="extractingVideoName" class="extracting-info"
        >正在抽帧: <span class="extractingVideos">{{ extractingVideoName }}</span>
        <Progress
          :percent="extractingProgress"
          :stroke-width="6"
          :show-info="true"
          class="extracting-progress"
          style="display: inline-block; width: 200px; margin-left: 10px; vertical-align: middle"
        />
      </p>
    </Card>
    <BasicTable
      @register="registerTable"
      @selection-change="selectionChange"
      rowKey="id"
      :rowSelection="{ type: 'checkbox' }"
      :clickToRowSelect="false"
    >
      <template #img="{ text }">
        <TableImg :size="60" :simpleShow="true" :imgList="getVideoUrl(text)" />
      </template>
      <template #toolbar>
        <a-button type="primary" @click="handleUpload" :disabled="props.flag === 1"
          >上传视频</a-button
        >
        <!-- 注释掉批量抽帧按钮 -->
        <!-- <a-button type="primary" @click="batchSample" :disabled="canDelete || props.flag === 1"
          >批量抽帧</a-button
        > -->
        <a-button type="primary" @click="batchDownload" :disabled="canDelete">批量下载</a-button>
        <a-button
          type="primary"
          @click="handleMutipleDelete"
          :disabled="canDelete || props.flag === 1"
        >
          批量删除
        </a-button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:play-circle-outlined',
              tooltip: '预览视频',
              onClick: showVideo.bind(null, record),
            },
            {
              icon: 'ant-design:scissor-outlined',
              tooltip: '视频抽帧',
              disabled:
                record.isBroken || sampleVideoStatus || record.status === 102 || props.flag === 1,
              onClick: videoExtract.bind(null, record),
            },
            {
              icon: 'ant-design:download-outlined',
              tooltip: '下载',
              onClick: singleDownload.bind(null, record),
            },
            {
              icon: 'ant-design:delete-outlined',
              color: 'error',
              tooltip: '删除',
              disabled: props.flag === 1,
              popConfirm: {
                title: '是否确认删除',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>
    </BasicTable>
    <VideoExtractModal @register="registerModal" @submit="handleVideoExtractSubmit" />
    <VideoMsg @register="registerVideoMsg" />
    <BatchVideoExtractModal
      @register="registerVideoBatch"
      @submit="handleBatchVideoExtractSubmit"
    />
    <UploadDataFile
      :row="importRow"
      :visible="uploadDataFileVisible"
      :closeUploadDataFile="closeUploadDataFile"
      :hideUploadDataFile="hideUploadDataFile"
    />
    <div id="resume-button" v-if="resumeButtonVisible">
      <Button type="primary" shape="circle" size="large" @click="hideUploadDataFile">
        <template #icon>
          <RollbackOutlined />
        </template>
      </Button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { BasicTable, TableAction, TableImg, useTable } from '/@/components/Table';
  import {
    columns,
    searchFormSchema,
  } from '/@/views/mineai/data/dataset-details2/details/video-file';
  import {
    videoFilesByPage,
    getVideoDatasetFiles,
  } from '/@/views/mineai/data/dataset-details2/api';
  import VideoMsg from './VideoMsg.vue';
  import {
    batchVideoSample,
    delVideo,
    detail,
    PicAndVideoDetails,
    queryDatasetStatus,
  } from '../api/index';
  import { bucketHost, bucketName } from '/@/utils/dubhe';
  import { ElMessage as Message } from 'element-plus';
  import { useModal } from '/@/components/Modal';
  import { computed, createVNode, nextTick, onUnmounted, ref, Ref } from 'vue';
  import UploadDataFile from '/@/views/mineai/data/dataset-details2/upload-datafile-inline.vue';
  import VideoExtractModal from './components/modals/VideoExtractModal.vue';
  import BatchVideoExtractModal from './components/modals/BatchVideoExtractModal.vue';
  import { Button, Card, Modal, Progress } from 'ant-design-vue';
  import { ExclamationCircleOutlined, RollbackOutlined } from '@ant-design/icons-vue';
  import { downloadImage } from '/@/utils/file/download';
  import moment from 'moment';
  import { useVideoStore } from '/@/store/modules/video';

  const videoStore = useVideoStore();
  const props = defineProps({
    id: Number,
    name: String,
    extractedVideos: Number,
    unextractedVideos: Number,
    flag: Number,
  });
  const [registerModal, { openModal }] = useModal();
  const [registerVideoMsg, { openModal: openVideoMsg }] = useModal();
  const [registerVideoBatch, { openModal: openVideoBatch }] = useModal();

  const importRow: Ref = ref(null);
  const uploadDataFileVisible: Ref<boolean> = ref(false);
  const resumeButtonVisible: Ref<boolean> = ref(false);
  const sampleVideoStatus: Ref<boolean> = ref(false);

  // 处理多选框选中事件
  const canDelete: Ref<boolean> = ref(true);
  const selectedKeys: Ref<Array<any>> = ref([]);
  const selectedRows: Ref<Array<any>> = ref([]);

  //获取视频抽帧信息
  let extractedVideos = ref(0);
  let unextractedVideos = ref(0);
  let totalVideos = ref(0);

  // 正在抽帧的视频名称
  const extractingVideoName = computed(() => {
    const name = videoStore.getExtractingVideoName;
    if (name && name.length > 30) {
      return name.substring(0, 30) + '...';
    }
    return name;
  });
  // 正在抽帧的进度
  const extractingProgress = computed(() => videoStore.getExtractingProgress);

  // 定时器相关
  const pollingTimer = ref<NodeJS.Timeout | null>(null);
  const isPolling = ref(false);

  // 开始轮询
  const startPolling = () => {
    if (pollingTimer.value) {
      clearInterval(pollingTimer.value);
    }

    isPolling.value = true;
    pollingTimer.value = setInterval(async () => {
      await checkVideoStatus();
    }, 5000); // 每5秒检查一次
  };

  // 停止轮询
  const stopPolling = () => {
    if (pollingTimer.value) {
      clearInterval(pollingTimer.value);
      pollingTimer.value = null;
    }
    isPolling.value = false;
  };

  // 检查视频状态
  const checkVideoStatus = async () => {
    try {
      // 刷新数据集信息
      await refreshDatasetInfo();

      // 如果还有抽帧中的任务，继续轮询
      if (sampleVideoStatus.value) {
        // 查询进度 - 使用 queryDatasetStatus 获取抽帧进度
        const statusRes = await queryDatasetStatus({ datasetIds: [props.id] });
        if (statusRes && statusRes[props.id]) {
          const progress = statusRes[props.id].progress || 0;
          videoStore.setExtractingProgress(Math.floor(progress));
        }
        // 使用 setTableData 静默更新表格数据，不触发 loading
        const currentData = getDataSource();
        if (currentData && currentData.length > 0) {
          const videoList = videoStore.getVideoList;
          const updatedData = currentData.map((video: any) => {
            const videoInStore = videoList.find((v: any) => v.id === video.id);
            // 如果视频在抽帧列表中且状态未改变，标记为抽帧中(102)
            if (videoInStore && video.status === videoInStore.status) {
              return { ...video, status: 102 };
            }
            return video;
          });
          setTableData(updatedData);
        }
        // 刷新视频统计信息
        await refreshVideoInfo();
      } else {
        // 所有抽帧完成，停止轮询
        stopPolling();
        await reload();
        Message.success('所有视频抽帧任务已完成');
      }
    } catch (error) {
      console.error('检查视频状态失败:', error);
      stopPolling();
    }
  };

  // 组件卸载时清理定时器
  onUnmounted(() => {
    stopPolling();
  });
  const selectionChange = ({ keys, rows }) => {
    selectedKeys.value = keys;
    selectedRows.value = rows;
    canDelete.value = keys.length <= 0;
  };

  const refreshVideoInfo = async () => {
    const data = await PicAndVideoDetails(props.id);
    extractedVideos.value = data.extractedVideos;
    unextractedVideos.value = data.unextractedVideos;
    totalVideos.value = data.extractedVideos + data.unextractedVideos;
  };

  const refreshDatasetInfo = async () => {
    const data = await detail(props.id);
    sampleVideoStatus.value = Number(data.status) === 302;
  };

  //查询视频时长
  async function addVideoDurations(v) {
    // 创建独立的加载函数
    const loadVideoDuration = (video) => {
      return new Promise<void>((resolve) => {
        const videoElement = document.createElement('video');
        let hasResolved = false;
        let timeoutId;

        // 清理函数
        const cleanup = () => {
          clearTimeout(timeoutId);
          videoElement.removeEventListener('loadedmetadata', loadedHandler);
          videoElement.removeEventListener('error', errorHandler);
          videoElement.src = '';
          videoElement.load();
        };

        // 成功处理
        const loadedHandler = () => {
          if (hasResolved) return;
          hasResolved = true;

          try {
            const durationSeconds = Math.round(videoElement.duration);
            video.duration = moment().startOf('day').seconds(durationSeconds).format('HH:mm:ss');
            video.isBroken = false;
          } catch (e) {
            video.duration = '视频损坏, 无法读取时长';
            video.isBroken = true;
          }

          cleanup();
          resolve();
        };

        // 错误处理
        const errorHandler = () => {
          if (hasResolved) return;
          hasResolved = true;

          video.duration = '视频损坏, 无法读取时长';
          video.isBroken = true;

          cleanup();
          resolve();
        };

        // 设置事件监听
        videoElement.addEventListener('loadedmetadata', loadedHandler, { once: true });
        videoElement.addEventListener('error', errorHandler, { once: true });

        // 设置超时
        timeoutId = setTimeout(() => {
          if (hasResolved) return;
          hasResolved = true;

          video.duration = '视频加载超时';
          video.isBroken = true;

          cleanup();
          resolve();
        }, 10000); // 增加超时时间到10秒

        // 开始加载
        try {
          videoElement.src = getFullFileUrl(video.url);
          videoElement.preload = 'metadata';
          videoElement.load();
        } catch (e) {
          errorHandler();
        }
      });
    };

    // 逐个加载而不是Promise.all
    for (const video of v.result) {
      await loadVideoDuration(video);
    }

    return v;
  }

  const [
    registerTable,
    { reload, clearSelectedRowKeys, updateTableDataRecord, setTableData, getDataSource },
  ] = useTable({
    //title: `数据集 ${props.name} 视频列表 已抽帧：${props.extractedVideos} 未抽帧：${props.unextractedVideos}`,
    api: async (params) => {
      // todo 根据params更换接口
      const v = await videoFilesByPage(props.id, params);
      // 完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
      v.items = v.result;
      v.total = v.page.total;
      // 为每个视频对象添加duration字段
      await addVideoDurations(v);
      return v;
    },
    beforeFetch: (v) => {
      // 发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v = Object.assign(v, {
        current: v.page,
        size: v.pageSize,
      });
      // 主动使用排序功能，则进行参数转换
      if (Object.hasOwn(v, 'field')) {
        Object.assign(v, {
          order: v.order === 'ascend' ? 'asc' : 'desc',
          sort: v.field,
        });
      }
      // 否则进行默认排序，根据id降序排序
      else {
        Object.assign(v, {
          order: 'desc',
          sort: 'id',
        });
      }
      return v;
    },
    afterFetch: async (v) => {
      await refreshVideoInfo();
      await refreshDatasetInfo();

      // 获取视频抽帧中状态
      if (sampleVideoStatus.value) {
        const list = videoStore.getVideoList;
        v.forEach((e) => {
          const index = list.findIndex((d) => d.id === e.id);
          if (index !== -1 && e.status === list[index].status) {
            e.status = 102;
          }
        });
        startPolling();
      }
      // 抽帧完成，重置list
      else {
        videoStore.resetVideoList();
      }
      return v;
    },
    columns: columns,
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
      schemas: searchFormSchema,
    },
    useSearchForm: true,
    showTableSetting: true,
    bordered: true,
    showIndexColumn: false,
    actionColumn: {
      width: 100,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
    handleSearchInfoFn(info) {
      return info;
    },
  });

  async function handleDelete(record: Recordable) {
    const params = {
      fileIds: [record.id],
      datasetId: props.id,
    };
    try {
      await delVideo(params);
      Message.success('文件删除成功');
    } catch (e) {
      console.error(e);
      Message.error('文件删除失败');
    } finally {
      await reload();
    }
  }

  const handleMutipleDelete = async () => {
    Modal.confirm({
      title: () => '确认删除选中的' + selectedKeys.value.length + '条数据?',
      maskClosable: true,
      icon: () => createVNode(ExclamationCircleOutlined),
      okText: () => '确定',
      okType: 'danger',
      cancelText: () => '取消',
      async onOk() {
        const ids = selectedRows.value.map((row) => row.id);

        const params = {
          fileIds: ids,
          datasetId: props.id,
        };
        try {
          await delVideo(params);
          Message.success('文件删除成功');
        } catch (e) {
          console.error(e);
          Message.error('文件删除失败');
        } finally {
          clearSelectedRowKeys();
          // 刷新页面
          await reload();
        }
      },
    });
  };

  // 统一拼接 MinIO 文件地址：老 dubhe 数据 url 已含 bucket 前缀，新数据是 objectKey（不含 bucket），
  // MinIO 直链必须带上 bucket 名（例如 /cz-dev/），否则返回 403 无法加载/下载。
  const getFullFileUrl = (filePath?: string) => {
    if (!filePath) {
      return '';
    }
    if (/^https?:\/\//i.test(filePath)) {
      return filePath;
    }
    if (filePath.startsWith(`${bucketName}/`)) {
      return `${bucketHost}/${filePath}`;
    }
    return `${bucketHost}/${bucketName}/${filePath}`;
  };

  const getVideoUrl = (url) => {
    let fileList: string[] = [];
    fileList.push(getFullFileUrl(url));
    return fileList;
  };

  async function showVideo(record) {
    openVideoMsg(true, {
      mediaType: 'video',
      mediaPath: record.url,
    });
  }

  function videoExtract(record: Recordable) {
    openModal(true, {
      id: record.id,
      datasetId: props.id,
      endTime: record.duration,
      status: record.status,
      name: record.name,
      url: record.url,
    });
  }

  // 处理上传视频模态框
  function handleUpload() {
    importRow.value = { id: props.id, dataType: 1 };
    uploadDataFileVisible.value = true;
  }

  const closeUploadDataFile = async (flag): Promise<void> => {
    uploadDataFileVisible.value = false;
    // 关闭恢复按钮
    if (resumeButtonVisible.value) {
      resumeButtonVisible.value = false;
    }
    // 判断是否是取消还是完成
    if (flag) {
      await reload();
    }
  };

  const hideUploadDataFile = (): void => {
    uploadDataFileVisible.value = !uploadDataFileVisible.value;
    resumeButtonVisible.value = !resumeButtonVisible.value;
    reload();
  };

  // 下载视频
  async function singleDownload(record) {
    await downloadImage(getFullFileUrl(record.url));
  }

  // 批量下载视频
  async function batchDownload() {
    for (const row of selectedRows.value) {
      await downloadImage(getFullFileUrl(row.url));
    }
  }

  // 批量抽帧
  async function batchSample() {
    openVideoBatch(true, {});
  }

  // batch submit事件
  // 在批量抽帧和单视频抽帧成功后开始轮询
  async function handleBatchVideoExtractSubmit(frameInterval) {
    try {
      await batchVideoSample({
        datasetId: props.id,
        fileIds: selectedKeys.value,
        frameInterval: frameInterval,
      });

      // 添加当前videoList
      selectedRows.value.forEach((e) => {
        videoStore.addVideo(e.id, e.status);
      });

      Message.success('下达抽帧任务成功');

      // 开始轮询状态
      startPolling();
    } catch (e) {
      console.error(e);
      Message.error('下达抽帧任务失败');
    } finally {
      clearSelectedRowKeys();
      await reload();
    }
  }

  // submit事件
  async function handleVideoExtractSubmit() {
    clearSelectedRowKeys();
    await reload();
  }

  defineExpose({ reload });
</script>

<style scoped lang="less">
  #dataset-detail :deep(.vben-basic-table-form-container) {
    padding: 0;
  }

  .info-card :deep(.ant-card-body) {
    padding: 12px;
  }

  .info-card {
    width: 100%;
    margin-bottom: 10px;
    min-height: 40px;
    height: auto;
    text-align: center;
    font-size: 12px;

    p {
      margin-bottom: 0;
      display: inline-block;
      margin-left: 30px;
      vertical-align: top;
    }

    .extracting-info {
      display: inline-block;
      margin-left: 30px;
      margin-bottom: 0;
      vertical-align: top;
    }

    .extractedVideos {
      color: #66fd66;
    }

    .unextractedVideos {
      color: #ff6363;
    }

    .totalVideos {
      color: #00ffd0;
    }

    .extractingVideos {
      color: #ffa500;
      font-weight: 500;
    }
  }

  #resume-button {
    position: fixed;
    right: 32px;
    top: 335px;
    z-index: 2147483640;
    display: flex;
    flex-direction: column;
    cursor: pointer;
  }
</style>

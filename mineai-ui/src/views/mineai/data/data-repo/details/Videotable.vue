<template>
  <div id="dataset-detail">
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
        <a-button type="primary" @click="handleUpload">上传视频</a-button>
        <a-button type="primary" @click="batchDownload" :disabled="canDelete">批量下载</a-button>
        <a-button type="primary" @click="handleMutipleDelete" :disabled="canDelete">
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
              icon: 'ant-design:download-outlined',
              tooltip: '下载',
              onClick: singleDownload.bind(null, record),
            },
            {
              icon: 'ant-design:delete-outlined',
              color: 'error',
              tooltip: '删除',
              // disabled: props.flag !== 1,
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
  import { columns, searchFormSchema } from '/@/views/mineai/data/data-repo/details/video-file';
  import VideoMsg from '../../dataset-details2/details/VideoMsg.vue';
  import {
    delDataRepoFile,
    delDataRepoFileBatch,
    delVideo,
    detail,
    getDataRepoFiles,
    PicAndVideoDetails,
    videoRepoFilesByPage,
  } from '../../dataset-details2/api/index';
  import { bucketHost } from '/@/utils/dubhe';
  import { ElMessage as Message } from 'element-plus';
  import { useModal } from '/@/components/Modal';
  import { createVNode, onMounted, ref, Ref } from 'vue';
  import UploadDataFile from '/@/views/mineai/data/data-repo/details/upload-datafile-inline.vue';
  import VideoExtractModal from '../../dataset-details2/details/components/modals/VideoExtractModal.vue';
  import { Button, Modal } from 'ant-design-vue';
  import { ExclamationCircleOutlined, RollbackOutlined } from '@ant-design/icons-vue';
  import { downloadImage } from '/@/utils/file/download';
  import moment from 'moment';
  import { useVideoStore } from '/@/store/modules/video';

  const videoStore = useVideoStore();
  const props = defineProps({
    id: Number,
    name: String,
    flag: Number,
    extractedVideos: Number,
    unextractedVideos: Number,
  });
  // eslint-disable-next-line @typescript-eslint/no-unused-vars,no-unused-vars
  const [registerModal, { openModal }] = useModal();
  const [registerVideoMsg, { openModal: openVideoMsg }] = useModal();

  const importRow: Ref = ref(null);
  const uploadDataFileVisible: Ref<boolean> = ref(false);
  const resumeButtonVisible: Ref<boolean> = ref(false);
  const sampleVideoStatus: Ref<boolean> = ref(false);

  // 处理多选框选中事件
  const canDelete: Ref<boolean> = ref(true);
  const selectedKeys: Ref<Array<any>> = ref([]);
  const selectedRows: Ref<Array<any>> = ref([]);

  const selectionChange = ({ keys, rows }) => {
    selectedKeys.value = keys;
    selectedRows.value = rows;
    canDelete.value = keys.length <= 0;
  };

  const refreshVideoInfo = async () => {
    await PicAndVideoDetails(props.id);
  };

  const refreshDatasetInfo = async () => {
    const data = await detail(props.id);
    sampleVideoStatus.value = Number(data.status) === 302;
  };

  // 查询视频时长
  async function addVideoDurations(v) {
    // 创建一个Promise数组，用于存放所有获取视频时长的异步操作
    const durationPromises = v.result.map(
      (video) =>
        new Promise<void>((resolve) => {
          // 创建一个video元素
          const videoElement = document.createElement('video');
          // 设置video的src属性为视频的URL
          videoElement.src = `${bucketHost}/${video.url}`;
          // 监听loadedmetadata事件
          videoElement.addEventListener(
            'loadedmetadata',
            () => {
              // 获取视频时长并解析为整数秒
              const durationSeconds = Math.round(videoElement.duration);
              // 将时长格式化为"xx小时xx分xx秒"的格式
              video.duration = moment()
                .startOf('day')
                .seconds(durationSeconds)
                .format('HH:mm:ss')
                .toString();
              resolve();
            },
            { once: true },
          );
          // 设置preload属性为metadata，这样不会下载整个视频
          videoElement.preload = 'metadata';
          // 触发浏览器去加载视频的元数据
          videoElement.load();
        }),
    );

    // 等待所有视频时长的Promise完成
    await Promise.all(durationPromises);
    // 所有视频时长都已添加到对应的对象中，返回更新后的对象
    return v;
  }

  const [registerTable, { reload, clearSelectedRowKeys, setTableData }] = useTable({
    api: async (params) => {
      params.fileType = 1;
      params.deleted = false;
      params.datasetId = props.id;
      // todo 根据params更换接口
      const v = await getDataRepoFiles(params);
      //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
      v.items = v.records;
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

  // 删除视频的操作
  async function handleDelete(record: Recordable) {
    try {
      await delDataRepoFile(record.id);
      Message.success('视频文件删除成功！！');
    } catch (e) {
      console.error(e);
      Message.error('视频文件删除失败！！');
    } finally {
      await reload();
    }
  }

  onMounted(async () => {
    getTableData();
  });

  let tableData = [];

  async function getTableData() {
    // let response = await getDataRepoFiles({ datasetId: props.id, fileType: 1 });
    // tableData = response.records;
    // setTableData(tableData);
  }

  // 批量删除
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
          await delDataRepoFileBatch(params);
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

  // 获得视频的Url
  const getVideoUrl = (url) => {
    let fileList: string[] = [];
    fileList.push(`${bucketHost}/${url}`);
    return fileList;
  };

  // 展示视频
  async function showVideo(record) {
    openVideoMsg(true, {
      mediaType: 'video',
      mediaPath: record.url,
    });
  }

  // 处理上传视频模态框
  function handleUpload() {
    importRow.value = { id: props.id, dataType: 1 };
    uploadDataFileVisible.value = true;
  }

  // 关闭上传文件
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

  // 隐藏
  const hideUploadDataFile = (): void => {
    uploadDataFileVisible.value = !uploadDataFileVisible.value;
    resumeButtonVisible.value = !resumeButtonVisible.value;
    reload();
  };

  // 下载视频
  async function singleDownload(record) {
    await downloadImage(`${bucketHost}/${record.url}`);
  }

  // 批量下载视频
  async function batchDownload() {
    for (const row of selectedRows.value) {
      await downloadImage(`${bucketHost}/${row.url}`);
    }
  }

  // submit事件
  async function handleVideoExtractSubmit() {
    clearSelectedRowKeys();
    await reload();
  }
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
    height: 40px;
    text-align: center;
    font-size: 12px;

    p {
      height: 100%;
      margin-bottom: 0;
      display: inline-block;
      margin-left: 30px;
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

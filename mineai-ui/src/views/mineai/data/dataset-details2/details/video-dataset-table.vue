<template>
  <div id="video-dataset-file-detail">
    <BasicTable @register="registerTable" rowKey="id">
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
              onClick: downloadFile.bind(null, record),
            },
            {
              icon: 'ant-design:highlight-outlined',
              tooltip: '标注',
              onClick: goAnnotate.bind(null, record),
              disabled: !props.id,
            },
            {
              icon: 'ant-design:delete-outlined',
              color: 'error',
              tooltip: '删除',
              popConfirm: {
                title: '是否确认删除',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>
    </BasicTable>
    <VideoMsg @register="registerVideoMsg" />
  </div>
</template>

<script setup lang="ts">
  import { h } from 'vue';
  import { Tag } from 'ant-design-vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { formatDateTime } from '/@/utils';
  import { getVideoDatasetFiles, delVideo } from '../api/index';
  import VideoMsg from './VideoMsg.vue';
  import { bucketHost, bucketName } from '/@/utils/dubhe';
  import { downloadByUrl } from '/@/utils/file/download';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useRouter } from 'vue-router';

  const { createMessage } = useMessage();
  const router = useRouter();

  const props = defineProps({
    id: Number,
    name: String,
  });

  const [registerVideoMsg, { openModal: openVideoMsg }] = useModal();

  const columns = [
    { title: '视频编号', dataIndex: 'id', width: 100 },
    { title: '名称', dataIndex: 'name', ellipsis: true, width: 260 },
    { title: '文件类型', dataIndex: 'fileType', width: 120 },
    {
      title: '文件大小',
      dataIndex: 'fileSize',
      width: 140,
      customRender: ({ text }) => formatFileSize(text),
    },
    { title: '媒体状态', dataIndex: 'mediaStatus', width: 120 },
    { title: '抽帧状态', dataIndex: 'extractStatus', width: 120 },
    {
      title: '标注状态',
      dataIndex: 'annotationStatus',
      width: 110,
      customRender: ({ text }) => annotationStatusTag(text),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 180,
      customRender: ({ text }) => formatDateTime(text),
    },
  ];

  const [registerTable, { reload }] = useTable({
    api: async (params) => {
      const response = await getVideoDatasetFiles(props.id, {
        current: params.page,
        size: params.pageSize,
      });
      // 后端统一返回 { result, page }，转换为 BasicTable 默认的 { items, total }。
      return {
        ...response,
        items: response.result || [],
        total: response.page?.total || 0,
      };
    },
    columns,
    showTableSetting: false,
    bordered: true,
    showIndexColumn: false,
    actionColumn: {
      width: 160,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  });

  function showVideo(record: Recordable) {
    openVideoMsg(true, {
      mediaType: 'video',
      mediaPath: record.url || record.objectKey,
    });
  }

  /** 拼接 MinIO 完整地址（与 video-table.vue getFullFileUrl 保持一致） */
  const getFullFileUrl = (filePath?: string) => {
    if (!filePath) return '';
    if (/^https?:\/\//i.test(filePath)) return filePath;
    if (filePath.startsWith(`${bucketName}/`)) return `${bucketHost}/${filePath}`;
    return `${bucketHost}/${bucketName}/${filePath}`;
  };

  /** 下载单个视频 */
  async function downloadFile(record: Recordable) {
    const url = getFullFileUrl(record.url || record.objectKey);
    if (!url) {
      createMessage.warning('视频地址为空，无法下载');
      return;
    }
    await downloadByUrl({ url, target: '_self' });
  }

  /** 跳转到标注页（独立视频数据集抽帧后以图片标注页承载） */
  function goAnnotate(record: Recordable) {
    router.push(`/maData/annotate/${props.id}/${props.name}`);
  }

  /** 删除单个视频文件 */
  async function handleDelete(record: Recordable) {
    try {
      await delVideo({ datasetId: props.id, fileIds: [record.id] });
      createMessage.success('视频文件删除成功');
      await reload();
    } catch (e) {
      console.error('[视频数据集] 删除失败', e);
      createMessage.error('视频文件删除失败');
    }
  }

  function formatFileSize(size?: number) {
    if (size === undefined || size === null) {
      return '-';
    }
    if (size < 1024) {
      return `${size} B`;
    }
    if (size < 1024 * 1024) {
      return `${(size / 1024).toFixed(2)} KB`;
    }
    return `${(size / 1024 / 1024).toFixed(2)} MB`;
  }

  function annotationStatusTag(status?: string) {
    const map: Record<string, { text: string; color: string }> = {
      READY: { text: '未标注', color: 'default' },
      ANNOTATING: { text: '标注中', color: 'processing' },
      SUBMITTED: { text: '已提交', color: 'warning' },
      REVIEWING: { text: '审核中', color: 'warning' },
      APPROVED: { text: '已通过', color: 'success' },
    };
    const m = map[status || ''] || { text: status || '-', color: 'default' };
    return h(Tag, { color: m.color }, () => m.text);
  }

  defineExpose({ reload });
</script>

<style scoped lang="less">
  #video-dataset-file-detail :deep(.vben-basic-table-form-container) {
    padding: 0;
  }
</style>

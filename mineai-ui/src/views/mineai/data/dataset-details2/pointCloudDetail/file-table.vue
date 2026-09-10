<template>
  <div id="pc-file-detail">
    <BasicTable
      @register="registerTable"
      @selection-change="selectionChange"
      rowKey="id"
      :rowSelection="{ type: 'checkbox' }"
      :clickToRowSelect="false"
    >
      <template #toolbar>
        <Button type="primary" @click="handleUpload">上传 PCD</Button>
        <Button type="primary" @click="handleBatchDelete" :disabled="canDelete">批量删除</Button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:download-outlined',
              tooltip: '下载',
              onClick: downloadFile.bind(null, record),
            },
            {
              icon: 'ant-design:highlight-outlined',
              tooltip: '标注',
              onClick: goAnnotate,
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
    <input
      ref="pcdInput"
      type="file"
      accept=".pcd"
      multiple
      style="display: none"
      @change="handlePcdChange"
    />
  </div>
</template>
<script setup lang="ts">
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { Ref, ref } from 'vue';
  import { Button } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { deletePcDatasetFile, getPcDatasetFiles } from '../api/index';
  import { downloadByUrl } from '/@/utils/file/download';
  import { useRouter } from 'vue-router';
  import {
    createImportTransferTask,
    updateImportTransferProgress,
    completeImportTransferTask,
    failImportTransferTask,
  } from '/@/api/mineai/importTransferTask';

  const props = defineProps<{
    id: number;
    name?: string;
  }>();

  const { createMessage } = useMessage();
  const router = useRouter();
  const canDelete: Ref<boolean> = ref(true);
  const selectedRows: Ref<Array<any>> = ref([]);
  const pcdInput = ref<HTMLInputElement | null>(null);

  const columns = [
    { title: '文件ID', dataIndex: 'id', width: 120 },
    { title: '文件名', dataIndex: 'name', width: 200 },
    { title: '文件大小(字节)', dataIndex: 'fileSize', width: 140 },
    { title: '点数', dataIndex: 'pointCount', width: 120 },
    { title: '上传时间', dataIndex: 'createTime', width: 180 },
  ];

  const [registerTable, { reload, clearSelectedRowKeys }] = useTable({
    api: async (params) => {
      // 将 VBen 分页参数转换为后端 Spring Page 参数
      const v = await getPcDatasetFiles(props.id, {
        current: params.page,
        size: params.pageSize,
      });
      const payload = v || {};
      // 后端统一返回 { result, page }，转换为 BasicTable 默认的 { items, total }。
      return {
        ...payload,
        items: payload.result || [],
        total: payload.page?.total || 0,
      };
    },
    beforeFetch: (v) => {
      v = Object.assign(v, {
        current: v.page,
        size: v.pageSize,
      });
      if (Object.hasOwn(v, 'field')) {
        Object.assign(v, {
          order: v.order === 'ascend' ? 'asc' : 'desc',
          sort: v.field,
        });
      } else {
        Object.assign(v, {
          order: 'desc',
          sort: 'id',
        });
      }
      return v;
    },
    columns: columns,
    showTableSetting: false,
    bordered: true,
    showIndexColumn: false,
    actionColumn: {
      width: 140,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  });

  const selectionChange = ({ keys, rows }) => {
    selectedRows.value = rows;
    canDelete.value = keys.length <= 0;
  };

  // 上传 PCD 文件：触发隐藏文件选择器
  const handleUpload = () => {
    pcdInput.value?.click();
  };

  const handlePcdChange = async (event: Event) => {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files || []);
    input.value = '';
    if (files.length === 0) {
      return;
    }
    let transferTaskId = 0;
    try {
      transferTaskId = await createImportTransferTask({
        taskName: `数据集“${props.name || '点云数据集'}”导入任务`,
        datasetType: 'POINT_CLOUD',
        datasetId: props.id,
        sourceType: 'LOCAL',
        totalFiles: files.length,
        totalBytes: files.reduce((total, file) => total + file.size, 0),
      });
      for (const file of files) {
        if (!file.name.toLowerCase().endsWith('.pcd')) {
          throw new Error(`${file.name} 不是 PCD 文件`);
        }
        const upload = await maHttp.post(
          {
            url: `pointcloud/datasets/${props.id}/files/upload-url`,
            params: { name: file.name },
            headers: {},
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        );
        const response = await fetch(upload.uploadUrl, { method: 'PUT', body: file });
        if (!response.ok) {
          throw new Error(`${file.name} 上传到对象存储失败`);
        }
        await maHttp.post(
          {
            url: `pointcloud/datasets/${props.id}/files/commit`,
            params: { name: file.name, objectKey: upload.objectKey },
            headers: {},
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        );
        const finished = files.indexOf(file) + 1;
        await updateImportTransferProgress(transferTaskId!, {
          stage: 'UPLOAD',
          progress: Math.round((finished / files.length) * 100),
          successFiles: finished,
          totalFiles: files.length,
          message: `${file.name} 上传完成`,
        });
      }
      await completeImportTransferTask(transferTaskId!);
      createMessage.success('PCD 文件上传完成');
      await reload();
    } catch (error) {
      if (transferTaskId) {
        try {
          await failImportTransferTask(transferTaskId!, error instanceof Error ? error.message : 'PCD 文件上传失败');
        } catch (_) {
          // 任务状态上报失败不应覆盖原始上传错误。
        }
      }
      createMessage.error(error instanceof Error ? error.message : 'PCD 文件上传失败');
    }
  };

  // 下载单个 PCD 文件（先调后端拿 MinIO 预签名 URL，再交给 downloadByUrl 走真实二进制流）
  const downloadFile = async (record) => {
    try {
      const response = await maHttp.get(
        {
          url: `pointcloud/datasets/${props.id}/files/${record.id}/download`,
          headers: {},
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      // maHttp 的 msTransform 在成功时已解包返回 DataResponseBody 的 data 字段（即 URL 字符串），
      // 因此直接取 response；为兼容历史返回结构，同时兜底 response?.result。
      const url = typeof response === 'string' ? response : (response || {}).result;
      if (typeof url !== 'string') {
        createMessage.error('下载链接为空，请稍后重试');
        return;
      }
      downloadByUrl({ url, target: '_self' });
    } catch (error) {
      console.error('[点云] 获取 PCD 下载链接失败', { datasetId: props.id, fileId: record.id, error });
      createMessage.error('获取下载链接失败，请稍后重试');
    }
  };

  // 跳转到点云标注页
  const goAnnotate = () => {
    router.push(`/maData/pointCloudAnnotate/${props.id}/${props.name}`);
  };

  // 删除单个 PCD 文件
  const handleDelete = async (record) => {
    try {
      await deletePcDatasetFile(props.id, record.id);
      createMessage.success('文件删除成功');
      await reload();
    } catch (e) {
      createMessage.error('文件删除失败');
    }
  };

  // 批量删除
  const handleBatchDelete = async () => {
    try {
      for (const row of selectedRows.value) {
        await deletePcDatasetFile(props.id, row.id);
      }
      createMessage.success('批量删除成功');
      clearSelectedRowKeys();
      await reload();
    } catch (e) {
      createMessage.error('批量删除失败');
    }
  };

  defineExpose({ reload });
</script>

<style scoped lang="less">
  #pc-file-detail :deep(.vben-basic-table-form-container) {
    padding: 0;
  }
</style>

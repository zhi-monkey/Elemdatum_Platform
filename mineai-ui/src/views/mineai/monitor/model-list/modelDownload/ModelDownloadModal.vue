<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="模型下载" @ok="handleSubmit">
    <a-table
      :row-selection="rowSelection"
      :columns="columns"
      :data-source="filesData"
      row-key="key"
      :pagination="false"
    />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, reactive, computed, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { downloadByUrl } from '/@/utils/file/download';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Table as ATable } from 'ant-design-vue';

  const { createMessage } = useMessage();
  const [registerModal, { closeModal, setModalProps }] = useModalInner(async (data) => {
    await handleDownloadWeightFileList(data.record);
    setModalProps({ minHeight: 250, width: '800px' });
  });

  const trainingFiles = reactive([]);
  const convertedFiles = reactive([]);
  const unKnowFiles = reactive([]);

  const columns = [
    {
      title: '文件名',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '更新时间',
      dataIndex: 'time',
      key: 'time',
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
    },
  ];

  const filesData = computed(() => {
    return [
      ...trainingFiles.map((file) => ({ ...file, type: '训练模型' })),
      ...convertedFiles.map((file) => ({ ...file, type: '转换模型' })),
    ];
  });

  const selectedRowKeys = ref([]);

  const rowSelection = computed(() => ({
    selectedRowKeys: unref(selectedRowKeys),
    onChange: (newSelectedRowKeys) => {
      selectedRowKeys.value = newSelectedRowKeys;
    },
  }));

  async function handleSubmit() {
    const selectedFiles = filesData.value.filter((file) =>
      selectedRowKeys.value.includes(file.key),
    );
    selectedFiles.forEach((file) => {
      downloadFile(file.url, file.name);
    });
    closeModal();
    if (selectedFiles.length !== 0) {
      createMessage.success('模型下载成功！');
    }
  }

  function getFileDownloadUrl(filePath) {
    return 'raw/' + encodeURIComponent(filePath);
  }

  async function handleDownloadWeightFileList(record) {
    try {
      const response = await maHttp.get<string[]>(
        {
          url: 'model/getWeightPath',
          params: { modelId: parseStringId(record.id) },
          headers: { ignoreCancelToken: true },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );

      trainingFiles.length = 0;
      convertedFiles.length = 0;
      unKnowFiles.length = 0;

      response.forEach((filePathDate) => {
        const filePath = filePathDate.path;
        const time = filePathDate.formattedDate;
        const url = getFileDownloadUrl(filePath);
        const fileName = filePath.split('/').pop() || 'unknown';
        const uniqueKey = `${fileName}-${time}`;

        if (
          filePath.endsWith('.rknn') ||
          filePath.endsWith('.engine') ||
          filePath.endsWith('.wts') ||
          filePath.endsWith('.bmodel')
        ) {
          const match = filePath.match(/\/([^\/]+)\/([^\/]+)\/([^\/]+)\.rknn$/);
          let newFileName = match ? `${match[3]}-${match[2]}.rknn` : fileName;
          const finalFileName = newFileName.replace(/^.*?(convert-.*\.rknn)$/, '$1');
          convertedFiles.push({ key: uniqueKey, name: finalFileName, url, time, type: '转换模型' });
        } else if (filePath.endsWith('.onnx') || filePath.endsWith('.pt')) {
          trainingFiles.push({ key: uniqueKey, name: fileName, url, time, type: '训练模型' });
        } else {
          unKnowFiles.push({ key: uniqueKey, name: fileName, url, time, type: '未知类型' });
        }
      });
    } catch (error) {
      console.error('获取文件列表失败', error);
    }
  }

  function downloadFile(url, fileName) {
    downloadByUrl({
      url: url,
      target: '_self',
      fileName: fileName,
    });
  }

  function parseStringId(id) {
    let match = id.match(/M0*(\d+)/);
    return match ? Number(match[1]) : -1;
  }
</script>

<style scoped>
  h3 {
    margin-top: 16px;
  }
</style>

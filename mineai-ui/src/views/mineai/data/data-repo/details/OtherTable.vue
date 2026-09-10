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
        <TableImg :size="60" :simpleShow="true" :imgList="getImageUrl(text)" />
      </template>
      <template #toolbar>
        <Button type="primary" @click="handleUpload">上传文件</Button>
        <Button type="primary" @click="batchDownload" :disabled="canDelete">批量下载</Button>
        <Button
          type="primary"
          @click="handleMultipleDelete"
          :disabled="canDelete || props.flag !== 1"
        >
          批量删除
        </Button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:delete-outlined',
              color: 'error',
              tooltip: '删除',
              disabled: props.flag !== 1,
              popConfirm: {
                title: '是否确认删除',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>
    </BasicTable>
    <UploadDataFile
      :row="importRow"
      :visible="uploadDataFileVisible"
      :closeUploadDataFile="closeUploadDataFile"
      :hideUploadDataFile="hideUploadDataFile"
    />
    <ImageAnnotationModal @register="registerAnnotationModal" />
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
  import { columns, searchFormSchema } from '/@/views/mineai/data/data-repo/details/others';
  import {
    del,
    queryFile,
    batchQueryFile,
    getImageAnnotationInfo,
    batchGetImageAnnotationInfo,
    getYoloLabelsFile,
    getDataRepoFiles,
    delDataRepoFile,
    delDataRepoFileBatch,
  } from '../../dataset-details2/api/index';
  import { bucketHost } from '/@/utils/dubhe';
  import { onMounted, ref, Ref, createVNode, onActivated } from 'vue';
  import { parseAnnotationInfo } from '../../dataset-details2/util';
  import { Button, Modal } from 'ant-design-vue';
  import { useRouter } from 'vue-router';
  import {
    downloadByData,
    downloadImage,
    downloadImagesAndAnnotationsAsZip,
    downloadImagesAsZip,
  } from '/@/utils/file/download';
  import { ElMessage as Message } from 'element-plus';
  import UploadDataFile from '/@/views/mineai/data/data-repo/details/upload-datafile-inline.vue';
  import { ExclamationCircleOutlined, RollbackOutlined } from '@ant-design/icons-vue';
  import ImageDownloadModal from '../../dataset-details2/details/components/modals/ImageDownloadModal.vue';
  import ImageAnnotationModal from '../../dataset-details2/details/components/modals/ImageAnnotationModal.vue';
  import { useModal } from '/@/components/Modal';
  import moment from 'moment';

  // eslint-disable-next-line @typescript-eslint/no-unused-vars,no-unused-vars
  const router = useRouter();

  const props = defineProps({
    id: Number,
    name: String,
    flag: Number,
    imageHaveAnnotation: Number,
    imageNoAnnotation: Number,
  });

  let labels;

  // 处理多选框选中事件
  const canDelete: Ref<boolean> = ref(true);
  const selectedKeys: Ref<Array<any>> = ref([]);
  const selectedRows: Ref<Array<any>> = ref([]);

  const resumeButtonVisible: Ref<boolean> = ref(false);
  const importRow: Ref = ref(null);
  const uploadDataFileVisible: Ref<boolean> = ref(false);

  //存储标注信息
  // eslint-disable-next-line @typescript-eslint/no-unused-vars,no-unused-vars
  const params = {
    status: '303',
  };

  const [registerModal, { openModal }] = useModal();
  const [registerAnnotationModal] = useModal();

  const [registerTable, { reload, clearSelectedRowKeys, setTableData }] = useTable({
    api: async (params) => {
      params.fileType = 2;
      params.deleted = false;
      params.datasetId = props.id;
      // todo 根据params更换接口
      const v = await getDataRepoFiles(params);
      //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
      v.items = v.records;
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

  let tableData = [];

  onMounted(async () => {
    getTableData();
  });

  async function getTableData() {
    // let response = await getDataRepoFiles({ datasetId: props.id, fileType: 2 });
    // tableData = response.records;
    // setTableData(tableData);
  }

  // 返回缓存页面后刷新表格
  onActivated(() => {
    reload();
  });

  const selectionChange = ({ keys, rows }) => {
    selectedKeys.value = keys;
    selectedRows.value = rows;
    canDelete.value = keys.length <= 0;
  };

  // 删除的逻辑
  async function handleDelete(record: Recordable) {
    await delDataRepoFile(record.id)
      .then(() => {
        Message.success('文件删除成功');
        reload();
      })
      .catch((err) => {
        Message.error(err.message || '删除失败');
        reload();
      });
  }

  // 批量删除逻辑
  const handleMultipleDelete = async () => {
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
        await delDataRepoFileBatch(params)
          .then(() => {
            Message.success('文件删除成功');
            clearSelectedRowKeys();
            // 刷新页面
            reload();
          })
          .catch((err) => {
            Message.error(err.message || '删除失败');
            reload();
          });
      },
    });
  };

  const getImageUrl = (url) => {
    let fileList: string[] = [];
    fileList.push(`${bucketHost}/${url}`);
    return fileList;
  };

  // 处理上传文件模态框
  function handleUpload() {
    importRow.value = { id: props.id, dataType: 2 };
    uploadDataFileVisible.value = true;
  }

  const closeUploadDataFile = (flag): void => {
    uploadDataFileVisible.value = false;
    // 关闭恢复按钮
    if (resumeButtonVisible.value) {
      resumeButtonVisible.value = false;
    }
    // 判断是否是取消还是完成
    if (flag) {
      reload();
    }
  };
  const hideUploadDataFile = (): void => {
    uploadDataFileVisible.value = !uploadDataFileVisible.value;
    resumeButtonVisible.value = !resumeButtonVisible.value;
    reload();
  };

  // 单个下载 emit
  async function singleDownload(labelType, id, url) {
    const file = await queryFile(props.id, parseInt(id));
    // 需要下载标注文件
    if (labelType !== undefined) {
      // createML
      if (labelType === 'createML') {
        let annotations: any[] = file.annotation
          ? parseAnnotationInfo(file.annotation, labels)
          : [];
        annotations.forEach((e) => {
          delete e.data.score;
        });
        downloadByData(JSON.stringify(annotations), `${file.name}.json`);
      }
      // VOC格式
      else if (labelType === 'VOC') {
        const annotations = await getImageAnnotationInfo(props.id, parseInt(id), labelType);
        downloadByData(annotations, `${file.name}.xml`);
      }
      // YOLO格式
      else if (labelType === 'YOLO') {
        const annotations = await getImageAnnotationInfo(props.id, parseInt(id), labelType);
        const labelInfo = await getYoloLabelsFile(props.id);
        downloadByData(annotations, `${file.name}.txt`);
        downloadByData(labelInfo, `classes.txt`);
      }
    }
    await downloadImage(getImageUrl(url));
  }

  // 批量下载emit
  async function batchDownload() {
    for (const row of selectedRows.value) {
      await downloadImage(`${bucketHost}/${row.url}`);
    }
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

    .haveAnnotation {
      color: #66fd66;
    }

    .noAnnotation {
      color: #ff6363;
    }

    .total {
      color: #108ee9;
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

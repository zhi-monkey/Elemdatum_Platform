<template>
  <div id="dataset-detail">
    <Card :bordered="true" class="info-card">
      <p
        >已标注图片 <span class="haveAnnotation">{{ image_haveAnnotation }}</span></p
      >
      <p
        >未标注图片 <span class="noAnnotation">{{ image_noAnnotation }}</span></p
      >
      <p
        >图片总数 <span class="total">{{ image_total }}</span></p
      >
    </Card>
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
        <Button type="primary" @click="goLabel()" :disabled="labelStat || props.flag === 1"
          >标注</Button
        >
        <Button type="primary" @click="handleUpload" :disabled="props.flag === 1">上传图片</Button>
        <Button type="primary" @click="batchdownloadFile" :disabled="canDelete">批量下载</Button>
        <Button
          type="primary"
          @click="handleMultipleDelete"
          :disabled="canDelete || props.flag === 1"
        >
          批量删除
        </Button>
      </template>

      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:highlight-outlined',
              tooltip: '标注',
              onClick: goLabel.bind(null, record.id),
              disabled: props.flag === 1,
            },
            {
              icon: 'ant-design:info-circle-outlined',
              tooltip: '标注详情',
              onClick: showData.bind(null, record),
              // 已标注 104 未标注 101
              ifShow: record.status === 104,
              disabled: props.flag === 1,
            },
            {
              icon: 'ant-design:download-outlined',
              tooltip: '下载标注文件和图片',
              onClick: downloadFile.bind(null, record),
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
    <UploadDataFile
      :row="importRow"
      :visible="uploadDataFileVisible"
      :closeUploadDataFile="closeUploadDataFile"
      :hideUploadDataFile="hideUploadDataFile"
    />
    <ImageDownloadModal @register="registerModal" @batch="batchDownload" @single="singleDownload" />
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
  import {
    columns,
    searchFormSchema,
  } from '/@/views/mineai/data/dataset-details2/details/image-file';
  import {
    del,
    imageFilesByPage,
    queryFile,
    queryLabels,
    count,
    queryFirstImg,
    queryAnnotationInfo,
    batchQueryFile,
    getImageAnnotationInfo,
    batchGetImageAnnotationInfo,
    getYoloLabelsFile,
  } from '../api/index';
  import { bucketHost, bucketName } from '/@/utils/dubhe';
  import { onMounted, ref, Ref, createVNode, onActivated, h } from 'vue';
  import { parseAnnotationInfo } from '../util';
  import { Button, Modal, Card } from 'ant-design-vue';
  import { useRouter } from 'vue-router';
  import {
    downloadByData,
    downloadImage,
    downloadImagesAndAnnotationsAsZip,
    downloadImagesAsZip,
  } from '/@/utils/file/download';
  import { ElMessage as Message } from 'element-plus';
  import UploadDataFile from '/@/views/mineai/data/dataset-details2/upload-datafile-inline.vue';
  import { ExclamationCircleOutlined, RollbackOutlined } from '@ant-design/icons-vue';
  import ImageDownloadModal from './components/modals/ImageDownloadModal.vue';
  import ImageAnnotationModal from './components/modals/ImageAnnotationModal.vue';
  import { useModal } from '/@/components/Modal';
  import moment from 'moment';
  import { JsonPreview } from '/@/components/CodeEditor';

  const router = useRouter();

  const props = defineProps({
    id: Number,
    name: String,
    imageHaveAnnotation: Number,
    imageNoAnnotation: Number,
    flag: Number,
    annotateType: Number,
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
  const params = {
    status: '303',
  };
  let image_haveAnnotation = ref(0);
  let image_noAnnotation = ref(0);
  let image_total = ref(0);
  let labelStat = ref(true);

  const [registerModal, { openModal }] = useModal();
  const [registerAnnotationModal, { openModal: openAnnotationModal }] = useModal();

  const [registerTable, { reload, clearSelectedRowKeys }] = useTable({
    //title: `数据集${props.name} 图片列表 已标注：${props.image_haveAnnotation} 未标注：${props.image_noAnnotation}`,
    api: async (params) => {
      // todo 根据params更换接口
      const v = await imageFilesByPage(props.id, params);
      //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
      v.items = v.result;
      v.total = v.page.total;
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
    afterFetch: (v) => {
      //console.log(v);
      labelStat.value = v.length <= 0;
      refreshInfoCard();
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

  onMounted(async () => {
    labels = await queryLabels(props.id, {});
  });

  // 返回缓存页面后刷新表格
  onActivated(() => {
    reload();
  });

  const selectionChange = ({ keys, rows }) => {
    selectedKeys.value = keys;
    selectedRows.value = rows;
    canDelete.value = keys.length <= 0;
  };

  const refreshInfoCard = () => {
    count(props.id, params).then((data) => {
      image_haveAnnotation.value = data.haveAnnotation;
      image_noAnnotation.value = data.noAnnotation;
      image_total.value = data.haveAnnotation + data.noAnnotation;
    });
  };

  async function handleDelete(record: Recordable) {
    const params = {
      fileIds: [record.id],
      datasetId: props.id,
    };
    await del(params)
      .then(() => {
        Message.success('文件删除成功');
        reload();
      })
      .catch((err) => {
        Message.error(err.message || '删除失败');
        reload();
      });
  }

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
        await del(params)
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

  async function goLabel(imgId: number | null = null) {
    const dataSetId = Number(props.id);
    let annotateType = await queryAnnotationInfo(dataSetId);
    const prefix = annotateType === 103 ? 'segmentation' : 'annotate';
    const goImgId = imgId === null ? await queryFirstImg(dataSetId) : imgId;
    await router.push({
      path: `/maData/${prefix}/${dataSetId}/${props.name}`,
      state: { imgId: goImgId },
    });
  }

  // 统一拼接 MinIO 文件地址：老 dubhe 数据 url 已含 bucket 前缀，新数据是 objectKey（不含 bucket），
  // MinIO 直链必须带上 bucket 名（例如 /cz-dev/），否则返回 403 无法加载/预览。
  const getFullFileUrl = (url?: string) => {
    if (!url) {
      return '';
    }
    if (/^https?:\/\//i.test(url)) {
      return url;
    }
    if (url.startsWith(`${bucketName}/`)) {
      return `${bucketHost}/${url}`;
    }
    return `${bucketHost}/${bucketName}/${url}`;
  };

  const getImageUrl = (url) => {
    let fileList: string[] = [];
    fileList.push(getFullFileUrl(url));
    return fileList;
  };

  async function showData(record) {
    // 语义分割直接展示
    if (props.annotateType == 103) {
      const file = await queryFile(props.id, parseInt(record.id));
      let annotations: any[] = file.annotation ? parseAnnotationInfo(file.annotation, labels) : [];
      annotations.forEach((e) => {
        delete e.data.score;
      });
      Modal.info({
        title: '标注详情',
        content: h(JsonPreview, { data: annotations }),
        width: 600,
        closable: true,
      });
    } else {
      openAnnotationModal(true, {
        datasetId: props.id,
        fileId: record.id,
        labels,
      });
    }
  }

  // 下载文件打开模态框
  function downloadFile(record) {
    if (record.status === 101) {
      singleDownload(undefined, record.id, record.url);
      return;
    }
    openModal(true, {
      batchDownload: false,
      ids: [record.id],
      annotateType: props.annotateType,
      urls: [record.url],
      withLabel: record.status === 104,
    });
  }

  // 批量下载打开模态框
  function batchdownloadFile() {
    const ids = selectedRows.value.map((row) => row.id);
    const urls = selectedRows.value.map((row) => row.url);
    const withLabel = selectedRows.value.every((row) => row.status === 104);
    withLabel
      ? openModal(true, {
          batchDownload: true,
          ids: ids,
          annotateType: props.annotateType,
          urls: urls,
          withLabel: withLabel,
        })
      : batchDownload(undefined, ids, urls);
  }

  // 处理上传文件模态框
  function handleUpload() {
    importRow.value = { id: props.id, dataType: 0 };
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
  async function batchDownload(labelType, ids, urls) {
    // 打包下载
    await zipBatchDownload(labelType, ids);
  }

  // 打包下载图片和标注文件
  async function zipBatchDownload(labelType, ids) {
    // 要下载的图片文件信息
    const infos = await batchQueryFile({ fileIds: ids, datasetId: props.id });
    // 当前时间
    const now = moment().format('YYYY-MM-DD HH:mm:ss').toString();
    if (labelType !== undefined) {
      // createML
      if (labelType === 'createML') {
        await downloadImagesAndAnnotationsAsZip(
          `${props.name}-${labelType}-${now}.zip`,
          infos,
          labelType,
          labels,
        );
      }
      // VOC
      else if (labelType === 'VOC') {
        // 获取voc标注信息
        const annotations = await batchGetImageAnnotationInfo(props.id, ids, labelType);
        infos.forEach((e, index) => {
          e.voc = annotations[index];
        });
        await downloadImagesAndAnnotationsAsZip(
          `${props.name}-${labelType}-${now}.zip`,
          infos,
          labelType,
          labels,
        );
      }
      // YOLO
      else if (labelType === 'YOLO') {
        // 获取 YOLO 的标注信息
        const annotations = await batchGetImageAnnotationInfo(props.id, ids, labelType);
        infos.forEach((e, index) => {
          e.yolo = annotations[index];
        });
        await downloadImagesAndAnnotationsAsZip(
          `${props.name}-${labelType}-${now}.zip`,
          infos,
          labelType,
          labels,
          props.id,
        );
      }
    } else {
      await downloadImagesAsZip(`${props.name}-${now}.zip`, infos);
    }
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

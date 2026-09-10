<template>
  <div>
    <!--    <UploadForm-->
    <!--      action="fakeApi"-->
    <!--      title="导入图片"-->
    <!--      :visible="uploadDialogVisible"-->
    <!--      :transformFile="withDimensionFile"-->
    <!--      :toggleVisible="handleClose"-->
    <!--      :params="uploadParams"-->
    <!--      :hash="true"-->
    <!--      @upload-success="uploadSuccess"-->
    <!--      @upload-error="uploadError"-->
    <!--    />-->
    <!--主界面-->
    <div class="flex">
      <!--文件列表展示-->
      <div class="file-list-container">
        <div v-loading="crudLoading" class="app-container">
          <!--tabs页和工具栏-->
          <div class="classify-tab">
            <el-tabs v-model="lastTabName" @tab-click="handleTabClick" class="ml-3">
              <el-tab-pane :label="countInfoTxt.noAnnotation" name="noAnnotation" />
              <el-tab-pane :label="countInfoTxt.haveAnnotation" name="haveAnnotation" />
            </el-tabs>
            <div class="classify-button flex flex-between flex-vertical-align">
              <div class="row-left">
                <el-button
                  :icon="Right"
                  class="ml-40"
                  plain
                  type="warning"
                  @click="goAnnotateWithoutFile"
                >
                  标注
                </el-button>
                <el-button
                  v-if="!isTrack"
                  :disabled="lastTabName === 'haveAnnotation'"
                  type="primary"
                  plain
                  :icon="Plus"
                  @click="openUploadDialog"
                >
                  添加图片
                </el-button>
                <el-button
                  type="danger"
                  :icon="Delete"
                  :loading="crudDelAllLoading"
                  :disabled="crudSelections.length === 0"
                  @click="toDelete(crudSelections)"
                >
                  删除
                </el-button>
              </div>
              <div class="row-right flex items-center">
                <el-checkbox
                  v-model="checkAll"
                  :indeterminate="isIndeterminate"
                  :disabled="crudData.length === 0"
                  @change="handleCheckAllChange"
                >
                  {{ checkAll ? '取消全选' : '选择全部' }}
                </el-checkbox>
                <span> 已选 {{ selectImgsId.length }} 张 </span>
              </div>
            </div>
          </div>
          <div v-if="crudPage.total === 0 && !crudLoading">
            <InfoCard>
              <template #image>
                <ContainerOutlined />
              </template>
              <template #desc>
                <span> 暂无数据 </span>
              </template>
            </InfoCard>
          </div>
          <!--图片列表组件-->
          <image-gallery
            v-if="!crudLoading"
            ref="imgGallery"
            v-loading="crudLoading"
            :data-images="crudData"
            :is-multiple="true"
            :categoryId2Name="categoryId2Name"
            class="imgs"
            :selectImgsId="selectImgsId"
            @on-select-multiple-image="handleSelectMultipleImg"
            @go-annotate-img="goAnnotateImg"
          />
          <!--            @click-img="clickImg"-->
          <!--分页组件-->

          <el-pagination
            v-if="crudPage.total > 0"
            :total="crudPage.total"
            v-model:current-page="crudPage.current"
            :page-size="crudPage.size"
            :page-sizes="[10, 30, 50]"
            style="justify-content: center; padding: 10px"
            layout="total, prev, pager, next, sizes"
            @size-change="sizeChangeHandler($event)"
            @current-change="pageChangeHandler($event)"
          />
        </div>
      </div>
      <!--Label列表展示-->
      <!--      <div class="label-list-container pt-10">-->
      <!--        <div class="fixed-label-list">-->
      <!--          <div class="mb-10">-->
      <!--            <span class="text-bs font-bold mb-2">数据集名称</span>-->
      <!--            <div class="f14">-->
      <!--              <span class="vm">{{ datasetInfo.name }}</span>-->
      <!--            </div>-->
      <!--          </div>-->
      <!--          <div class="mb-10">-->
      <!--            <span class="text-bs font-bold mb-2">标注类型</span>-->
      <!--            <div class="f14">-->
      <!--              <span class="vm">{{ annotationBy('code')(datasetInfo.annotateType, 'name') }}</span>-->
      <!--            </div>-->
      <!--          </div>-->
      <!--          <div v-if="datasetInfo.labelGroupId" class="mb-10">-->
      <!--            <span class="text-bs font-bold mb-2">标签组</span>-->
      <!--            <div class="f14 flex items-center">-->
      <!--              <span class="vm">{{ datasetInfo.labelGroupName }} &nbsp;</span>-->
      <!--              &lt;!&ndash;              <el-link&ndash;&gt;-->
      <!--              &lt;!&ndash;                target="_blank"&ndash;&gt;-->
      <!--              &lt;!&ndash;                type="primary"&ndash;&gt;-->
      <!--              &lt;!&ndash;                :underline="false"&ndash;&gt;-->
      <!--              &lt;!&ndash;                class="vm"&ndash;&gt;-->
      <!--              &lt;!&ndash;                :href="`/data/labelgroup/detail?id=${datasetInfo.labelGroupId}`"&ndash;&gt;-->
      <!--              &lt;!&ndash;              >&ndash;&gt;-->
      <!--              &lt;!&ndash;                查看详情&ndash;&gt;-->
      <!--              &lt;!&ndash;              </el-link>&ndash;&gt;-->
      <!--            </div>-->
      <!--          </div>-->
      <!--          <div v-if="rawLabelData.length">-->
      <!--            <div class="pb-2 flex flex-between flex-wrap flex-vertical-align">-->
      <!--              <label class="text-bs font-bold" style="max-width: 39.9%; padding: 0"-->
      <!--                >全部标签({{ rawLabelData.length }})</label-->
      <!--              >-->
      <!--            </div>-->
      <!--            <div style="max-height: 200px; overflow-y: auto">-->
      <!--              <el-row :gutter="5" style="clear: both; width: 94%">-->
      <!--                <el-col v-for="data in labelData" :key="data.id" :span="8">-->
      <!--                  <el-tag-->
      <!--                    class="w-full"-->
      <!--                    :title="data.name"-->
      <!--                    :color="data.color"-->
      <!--                    :style="getStyle(data)"-->
      <!--                  >-->
      <!--                    <span :title="data.name">{{ data.name }}</span>-->
      <!--                  </el-tag>-->
      <!--                </el-col>-->
      <!--              </el-row>-->
      <!--            </div>-->
      <!--          </div>-->
      <!--        </div>-->
      <!--      </div>-->
    </div>
    <!--    <PicInfoModal-->
    <!--      :key="modalId"-->
    <!--      :initialIndex="initialIndex"-->
    <!--      :visible="showPicModal"-->
    <!--      :file="curFile"-->
    <!--      :fileList="fileList"-->
    <!--      okText="标注"-->
    <!--      cancelText="关闭"-->
    <!--      :handleOk="handleOk"-->
    <!--      :handleCancel="handlePicModalClose"-->
    <!--    />-->
  </div>
</template>

<script setup>
  import { without } from 'lodash-es';
  import { colorByLuminance } from '/@/utils/dubhe';
  import { computed, createVNode, onMounted, ref } from 'vue';
  import {
    annotateTypeCodeMap,
    annotationBy,
    dataEnhanceMap,
    fileCodeMap,
    getFileFromMinIO,
    labelGroupTypeMap,
    transformFile,
    transformFiles,
  } from '../util';
  import {
    queryDataEnhanceList,
    detail,
    count,
    getLabels,
    getAutoLabels,
    list,
    del,
    submit,
  } from '../api';
  import ImageGallery from '/@/views/mineai/data/dataset-details2/components/ImageGallery/index.vue';
  import InfoCard from '/@/views/mineai/data/dataset-details2/components/Card/info.vue';
  import { getBasename } from '/@/views/mineai/data/dataset-details2/components/UploadForm/util';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Modal } from 'ant-design-vue';
  import { ContainerOutlined, ExclamationCircleOutlined } from '@ant-design/icons-vue';
  import { Right, Plus, Delete } from '@element-plus/icons-vue';
  import { useRoute } from 'vue-router';
  import { useGo } from '/@/hooks/web/usePage';

  const { createMessage } = useMessage();
  const route = useRoute();
  const go = useGo();

  // refs
  const imgGallery = ref();

  // data
  const datasetId = ref(0);
  const datasetInfo = ref({});
  const uploadDialogVisible = ref(false);
  const lastTabName = ref('noAnnotation');
  const crudStatusMap = ref({
    noAnnotation: [fileCodeMap.NO_ANNOTATION],
    haveAnnotation: [fileCodeMap.HAVE_ANNOTATION],
  });
  const checkAll = ref(false);
  const isIndeterminate = ref(false);
  const rawLabelData = ref([]);
  const labelData = ref([]);
  const categoryId2Name = ref({});
  const commit = ref({
    noAnnotation: [],
    haveAnnotation: [],
  });
  const countInfo = ref({
    noAnnotation: 0,
    haveAnnotation: 0,
  });
  const systemLabels = ref([]);
  const showPicModal = ref(false);
  const curFile = ref();
  const fileList = ref([]);
  const modalId = ref(1);
  const initialIndex = ref(0);
  const enhanceLabels = ref([]);

  // modify
  const crudParams = ref({});

  const crudPage = ref({
    // 页码
    current: 1,
    // 每页数据条数
    size: 10,
    // 总数据条数
    total: 0,
  });

  const crudLoading = ref(false);

  const crudDelAllLoading = ref(false);

  const crudSelections = ref([]);

  const crudData = ref([]);

  // created
  const props = defineProps({
    id: Number,
    name: String,
  });
  // 显示加载中
  crudLoading.value = true;
  // 获取传进来的数据集id
  datasetId.value = parseInt(String(props?.id).toString(), 10);
  // 刷新标签？
  refreshLabel();
  // 获取当前数据集未标注/已标注的图片信息
  Promise.all([
    list({ datasetId: datasetId.value, status: [fileCodeMap.NO_ANNOTATION] }),
    list({ datasetId: datasetId.value, status: [fileCodeMap.HAVE_ANNOTATION] }),
  ]).then(([noAnnotation, haveAnnotation]) => {
    if (noAnnotation.result.length === 0 && haveAnnotation.result.length !== 0) {
      // todo 全部都标注过了，显示已标注的
      lastTabName.value = 'haveAnnotation';
      // crudParams.value.status = crudStatusMap.value[lastTabName.value];
      // // 获取数据
      // // this.crud.toQuery();
      // crudPage.value.current = 1;
      // const pageData = list({ ...crudParams.value, ...crudPage.value });
      // 当前页信息
      crudPage.value.current = haveAnnotation.page.current;
      crudPage.value.size = haveAnnotation.page.size;
      crudPage.value.total = haveAnnotation.page.total;
      // 当前页内容
      crudData.value = haveAnnotation.result;
    }
    // 优先显示为标注过的
    else {
      // 当前页信息
      crudPage.value.current = noAnnotation.page.current;
      crudPage.value.size = noAnnotation.page.size;
      crudPage.value.total = noAnnotation.page.total;
      // 当前页内容
      crudData.value = noAnnotation.result;
    }
    // 去掉加载中
    crudLoading.value = false;
  });

  // 获取该数据集详细信息
  detail(datasetId.value).then((res) => {
    datasetInfo.value = res || {};
  });
  // 获取系统标签
  getSystemLabel();

  // computed
  const urlPrefix = computed(() => {
    return annotationBy('code')(datasetInfo.value.annotateType, 'urlPrefix');
  });

  const isTrack = computed(() => {
    return urlPrefix.value === 'track';
  });

  const isClassification = computed(() => {
    return urlPrefix.value === 'classification';
  });

  const formItems = computed(() => {
    const isNoAnnotation = lastTabName.value === 'noAnnotation';
    return [
      {
        label: '标注状态:',
        prop: 'annotateStatus',
        type: 'checkboxGroup',
        options: [
          { label: '不限', value: '' },
          { label: '未标注', value: fileCodeMap.UNANNOTATED, disabled: !isNoAnnotation },
          { label: '未识别', value: fileCodeMap.UNRECOGNIZED, disabled: !isNoAnnotation },
          ...(isClassification.value
            ? []
            : [
                {
                  label: '标注中',
                  value: fileCodeMap.MANUAL_ANNOTATING,
                  disabled: isNoAnnotation,
                },
              ]),
          { label: '已标注', value: fileCodeMap.FINISHED, disabled: isNoAnnotation },
          ...(isTrack.value
            ? [{ label: '已跟踪', value: fileCodeMap.TRACK_SUCCEED, disabled: isNoAnnotation }]
            : []),
        ],
      },
      {
        label: '标注方式:',
        prop: 'annotateType',
        type: 'checkboxGroup',
        options: [
          { label: '不限', value: '' },
          { label: '手动标注', value: annotateTypeCodeMap.MANUAL, disabled: isNoAnnotation },
          { label: '自动标注', value: annotateTypeCodeMap.AUTO, disabled: isNoAnnotation },
        ],
      },
    ];
  });

  // const withDimensionFile = computed(() => {
  //   return withDimensionFile;
  // });

  // const annotationByCode = computed(() => {
  //   return annotationBy('code');
  // });

  const uploadParams = computed(() => {
    return {
      datasetId: datasetId.value,
      objectPath: `dataset/${datasetId.value}/origin`, // 对象存储路径
    };
  });

  const selectImgsId = computed(() => {
    return commit.value[lastTabName.value] || [];
  });
  const countInfoTxt = computed(() => {
    return {
      noAnnotation: `无标注信息（${countInfo.value.noAnnotation}）`,
      haveAnnotation: `有标注信息（${countInfo.value.haveAnnotation}）`,
    };
  });

  // mounted
  onMounted(async () => {
    // enhanceLabels
    const enhanceListResult = await queryDataEnhanceList();
    const { dictDetails = [] } = enhanceListResult || {};
    const labels = dictDetails.map((d) => ({
      label: d.label,
      value: Number(d.value),
    }));
    enhanceLabels.value = labels;
    // 获取已标注信息和未标注信息
    await updateCountInfo();
  });

  // 更新数据集当前搜索条件下文件有无标注信息的统计数量
  async function updateCountInfo() {
    countInfo.value = await count(datasetId.value, crudParams.value);
  }

  function handleFilter(form) {
    Object.assign(crudParams.value, form);
    // todo:刷新页面数据
    // this.crud.refresh();
  }

  function handleSort(command) {
    resetQuery();
    crudParams.value.sort = command === 1 ? 'name' : '';
    // todo:刷新页面数据
    // this.crud.refresh();
  }

  // 根据文件 enhaneType 找到对应的增强标签
  function findEnhanceMatch(item) {
    return enhanceLabels.value.find((d) => d.value === item.enhanceType);
  }

  // 生成增强标签
  function buildEnhanceTag(file) {
    const match = findEnhanceMatch(file);
    if (match) {
      return {
        label: match.label,
        value: match.value,
        tag: dataEnhanceMap[match.value],
      };
    }
    return undefined;
  }

  // 重置所有查询结果
  function resetQuery() {
    checkAll.value = false;
    isIndeterminate.value = false;
    imgGallery.value.resetMultipleSelection();
    // todo
    crudPage.value.current = 1;
  }

  // 封装查询已标注或未标注信息，并更新
  async function queryPage(type) {
    crudLoading.value = true;
    await list({
      datasetId: datasetId.value,
      status: crudStatusMap.value[type],
      ...crudPage.value,
    }).then((data) => {
      // 当前页信息
      crudPage.value.current = data.page.current;
      crudPage.value.size = data.page.size;
      crudPage.value.total = data.page.total;
      // 当前页内容
      crudData.value = data.result;
    });
    await updateCountInfo();
    crudLoading.value = false;
  }

  // 分页器换size
  async function sizeChangeHandler(e) {
    crudPage.value.size = e;
    crudPage.value.current = 1;
    await queryPage(lastTabName.value);
  }

  // 分页器换页
  async function pageChangeHandler(e) {
    crudPage.value.current = e;
    await queryPage(lastTabName.value);
  }

  function getSystemLabel() {
    getAutoLabels(labelGroupTypeMap.VISUAL.value).then((res) => {
      systemLabels.value = res.map((item) => ({
        value: item.id,
        label: item.name,
        color: item.color,
        chosen: false,
      }));
    });
  }

  function toDelete(datas = []) {
    Modal.confirm({
      title: () => `确认删除选中的${datas.length}个文件?`,
      maskClosable: true,
      icon: () => createVNode(ExclamationCircleOutlined),
      okText: () => '确定',
      okType: 'warning',
      cancelText: () => '取消',
      async onOk() {
        crudDelAllLoading.value = true;
        const ids = datas.map((d) => ({ id: d }));
        const params = {
          fileIds: datas,
          datasetId: datasetId.value,
        };
        if (ids.length) {
          del(params)
            .then(async () => {
              createMessage.success('删除文件成功', 5);
              await queryPage(lastTabName.value);
            })
            .finally(() => {
              crudDelAllLoading.value = false;
            });
        }
        handleCheckAllChange(0);
        // 更新 commit 表
        Object.assign(commit.value, {
          [lastTabName.value]: without(commit.value[lastTabName.value], ...datas),
        });
      },
    });
  }

  function handleCheckAllChange(val) {
    if (imgGallery.value) {
      if (val) {
        imgGallery.value.selectAll();
      } else {
        imgGallery.value.resetMultipleSelection();
      }
    }
  }

  function handleSelectMultipleImg(values) {
    // 选中图片的数量
    const checkedCount = values.length;
    const dataImgLen = imgGallery.value.dataImages.length;
    checkAll.value = checkedCount === dataImgLen;
    isIndeterminate.value = checkedCount > 0 && checkedCount < dataImgLen;
    crudSelections.value = values;
    // 更新 commit 表
    Object.assign(commit.value, {
      [lastTabName.value]: values,
    });
  }

  // 点击图片事件
  function clickImg(img, selectedImgList) {
    // 文件扩展
    const extendFile = (d) => ({
      file_name: getBasename(d.url),
      enhanceType: d.enhanceType,
    });

    // 扩展文件增强类型
    const extendFileEnhance = (d) => ({
      file_name: getBasename(d.url),
      enhanceType: d.enhanceType,
      enhanceTag: this.buildEnhanceTag(d),
    });

    // 如果没有选中图片
    if (selectedImgList.length === 0) {
      showPicModal.value = true;
      curFile.value = transformFile(img, extendFile);
      fileList.value = transformFiles(crudData.value, extendFileEnhance);
      const curIndex = crudData.value.findIndex((item) => item.id === curFile.value.id);
      if (curIndex > -1) {
        initialIndex.value = curIndex;
      }
    }
  }

  function goAnnotateImg(img) {
    const extendFile = (d) => ({
      file_name: getBasename(d.url),
      enhanceType: d.enhanceType,
    });
    curFile.value = transformFile(img, extendFile);
    goAnnotate(curFile.value);
  }

  function handlePicModalClose() {
    modalId.value += 1;
    showPicModal.value = false;
    curFile.value = undefined;
    fileList.value = [];
  }

  //切换已标注和未标注
  async function handleTabClick(tab) {
    const tabName = tab.props.name;
    if (lastTabName.value === tabName) {
      return;
    }
    await queryPage(tabName);
    lastTabName.value = tabName;
    checkAll.value = false;
  }

  async function uploadSuccess(res) {
    const files = getFileFromMinIO(res);
    // 提交业务上传
    if (files.length > 0) {
      submit(datasetId.value, files).then(() => {
        createMessage.success('上传文件成功', 5);
        // todo 刷新（页码1）
        // this.crud.toQuery();
      });
    }
  }

  function uploadError(err) {
    createMessage.error(err.message || '上传文件失败', 5);
  }

  function openUploadDialog() {
    uploadDialogVisible.value = true;
  }

  function handleClose() {
    uploadDialogVisible.value = false;
  }

  function refreshLabel() {
    getLabels(datasetId.value).then((res) => {
      rawLabelData.value = res;
      categoryId2Name.value = rawLabelData.value.reduce(
        (acc, item) =>
          Object.assign(acc, {
            [item.id]: {
              name: item.name,
              color: item.color,
            },
          }),
        {},
      );
      // 初始化设置 labelData
      labelData.value = rawLabelData.value;
    });
  }

  function getStyle(item) {
    // 根据亮度来决定颜色
    return {
      color: colorByLuminance(item.color),
    };
  }

  function goAnnotateWithoutFile() {
    goAnnotate();
  }

  function goAnnotate(file) {
    const basePath = `/maData/${urlPrefix.value}/${datasetInfo.value.id}`;
    go(file ? `${basePath}/file/${file.id}` : `${basePath}/file/${0}`);
  }
</script>

<style lang="scss" scoped>
  .classify-tab {
    display: flex;
    align-items: center;
    padding: 4px 0;
    margin-bottom: 10px;
  }

  .sorting-menu-trigger {
    padding: 0;
  }

  .classify-tab .classify-button {
    flex: 1;
    margin: 13px 0 20px 20px;
  }
</style>
<style lang="scss">
  @import 'src/assets/scss/common.scss';

  .sorting-menu-trigger {
    .sorting-menu {
      padding: 8px 25px;
    }
  }

  .file-list-container {
    flex: 1;
  }

  .label-list-container {
    width: 20%;
    border-radius: 5px;
    background-color: #181d31;
    box-shadow: 0 4px 8px 0 rgb(0 0 0 / 30%), 0 6px 20px 0 rgb(0 0 0 / 25%);
  }

  .fixed-label-list {
    position: fixed;
    top: 80px;
    width: 20%;
    height: calc(100vh - 50px);
    padding: 28px 28px 0;
    margin-bottom: 33px;
    overflow-y: auto;
  }

  .label-style {
    font-size: 14px;
    color: #606266;
  }

  .labelTable {
    min-height: 100px;
    max-height: 300px;
    overflow-y: auto;

    tr {
      float: left;
      width: auto;
      margin: 3px;

      > td {
        padding: 8px 10px;
      }
    }
  }

  .imgs li {
    cursor: pointer;
  }

  .row-right {
    .el-checkbox {
      margin-left: 20px;
      margin-right: 10px;
    }
  }

  @media (max-width: 1440px) {
    .fixed-label-list {
      padding: 10px 15px 0;
    }
  }
</style>

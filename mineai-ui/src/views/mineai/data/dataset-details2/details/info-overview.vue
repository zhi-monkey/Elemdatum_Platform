<template>
  <div class="info-overview-container">
    <a-row :gutter="[8, 8]" style="height: 100%">
      <a-col :span="12" style="height: 60vh">
        <div style="height: 95%" class="flex flex-col">
          <a-card style="height: 100vh" :loading="false" title="数据集信息">
            <Description
              :column="1"
              :data="datasetInfo"
              :schema="isVideoDataset ? videoDatasetSchema : datasetSchema"
              layout="horizontal"
            />
          </a-card>
        </div>
      </a-col>
      <a-col :span="12" style="height: 60vh">
        <div style="height: 95%" class="flex flex-col">
          <a-card style="height: 100vh" :loading="false" title="文件信息">
            <Description
              :column="1"
              :data="picAndVideoInfo"
              :schema="isVideoDataset ? videoSchema : picSchema"
              layout="horizontal"
            />
          </a-card>
          <!-- <a-card style="height: 100vh" :loading="false" title="视频文件信息">
            <Description
              :column="1"
              :data="picAndVideoInfo"
              :schema="videoSchema"
              layout="horizontal"
            />
          </a-card> -->
        </div>
      </a-col>
    </a-row>
    <a-row :gutter="[8, 8]" style="height: 100%" v-if="!isVideoDataset">
      <a-col :span="24" style="height: auto">
        <div style="height: 95%" class="flex flex-col">
          <a-table :columns="labelColumn" :data-source="labelData">
            <template #bodyCell="{ column, text }">
              <template v-if="column.dataIndex === 'labelName'">
                <a>{{ text }}</a>
              </template>
            </template>
          </a-table>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
  import {
    datasetSchema,
    picSchema,
    videoDatasetSchema,
    videoSchema,
    labelColumn,
  } from '/@/views/mineai/data/dataset-details2/details/components/cards/image-list';
  import { Description } from '/@/components/Description';
  import { Card as ACard, Col as ACol, Row as ARow, Table as ATable } from 'ant-design-vue';
  import {
    datasetDetails,
    PicAndVideoDetails,
    getDatasetLabelInfo,
    getVideoDatasetDetail,
  } from '../api/index';
  import { onMounted, reactive, ref } from 'vue';

  const props = defineProps({
    id: Number,
    name: String,
    isVideoDataset: {
      type: Boolean,
      default: false,
    },
  });

  // 定义响应式对象
  const datasetInfo: any = reactive({});
  const picAndVideoInfo: any = reactive({});
  const labelData = ref([]);

  onMounted(async () => {
    if (props.isVideoDataset) {
      // 独立视频数据集：走 video 命名空间详情接口，避免数字 id 与图片数据集冲突（video_4 与 image_4 的 id 都是 4）
      const response = await getVideoDatasetDetail(props.id);
      datasetInfo.id = response.id;
      datasetInfo.name = response.name;
      datasetInfo.status = response.status;
      datasetInfo.uploadStatus = response.uploadStatus;
      datasetInfo.createTime = response.createTime;
      datasetInfo.updateTime = response.updateTime;
      picAndVideoInfo.totalVideos = response.fileCount;
      picAndVideoInfo.unextractedVideos = response.unextractedVideos;
      picAndVideoInfo.extractedVideos = response.extractedVideos;
      return;
    }
    // 发送请求获取数据
    const response = await datasetDetails(props.id);
    datasetInfo.id = response.id;
    datasetInfo.name = response.name;
    datasetInfo.type = response.type;
    datasetInfo.dataType = response.dataType;
    datasetInfo.annotationType = response.annotateType;
    datasetInfo.currentVersionName = response.currentVersionName;
    datasetInfo.status = response.status;
    datasetInfo.createTime = response.createTime;
    datasetInfo.updateTime = response.updateTime;

    const picAndVideoResponse = await PicAndVideoDetails(props.id);
    picAndVideoInfo.totalImages = picAndVideoResponse.totalImages;
    picAndVideoInfo.unlabeledImages = picAndVideoResponse.unlabeledImages;
    picAndVideoInfo.labeledImages = picAndVideoResponse.labeledImages;
    picAndVideoInfo.totalVideos = picAndVideoResponse.totalVideos;
    picAndVideoInfo.unextractedVideos = picAndVideoResponse.unextractedVideos;
    picAndVideoInfo.extractedVideos = picAndVideoResponse.extractedVideos;
    picAndVideoInfo.totalLabels = picAndVideoResponse.totalLabels;
    picAndVideoInfo.totalSamples = picAndVideoResponse.totalSamples;
    //标签列表
    const params = {
      datasetId: props.id,
    };
    const labelInfoResponse = await getDatasetLabelInfo(params);
    // 将数据转换成table所需要的格式
    labelData.value = labelInfoResponse.map((item, index) => ({
      key: index.toString(),
      labelId: item.labelId,
      labelName: item.labelName,
      annotationCount: item.annotationCount,
    }));
  });
</script>

<style scoped lang="less">
  .info-overview-container {
    box-sizing: border-box;
    background-color: #1a202c !important;
  }
</style>

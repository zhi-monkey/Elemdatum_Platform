<template>
  <PageWrapper @back="goBack" style="margin: 0 16px 0 16px">
    <template #title>
      数据集
      <span style="font-weight: 700; color: #f5b956; font-size: 18px"> {{ name }} </span>
      详情
    </template>
    <div>
      <component
        :is="content"
        :id="id"
        :name="name"
        :flag="flag"
        :annotateType="annotateType"
        :isVideoDataset="isVideoDataset"
        :image_haveAnnotation="image_haveAnnotation"
        :image_noAnnotation="image_noAnnotation"
        :extractedVideos="extractedVideos"
        :unextractedVideos="unextractedVideos"
        ref="tabContent"
      />
    </div>
    <template #footer>
      <Tabs :active-key="activeKey" @change="handleTabChange">
        <tab-pane key="1" tab="信息总览" />
        <!-- 独立视频数据集只显示独立视频文件；普通图片数据集恢复历史视频列表入口。 -->
        <tab-pane v-if="isVideoDataset" key="3" tab="视频列表" />
        <template v-else>
          <tab-pane key="2" tab="图片列表" />
          <tab-pane key="3" tab="视频列表" />
        </template>
      </Tabs>
    </template>
  </PageWrapper>
</template>
<script lang="ts" setup name="FileDetail">
  import { onActivated, ref, shallowRef } from 'vue';
  import { PageWrapper } from '/@/components/Page';
  import { useRoute, useRouter } from 'vue-router';
  import { Tabs, TabPane } from 'ant-design-vue';
  import ImageTable from '/@/views/mineai/data/dataset-details2/details/image-table.vue';
  import VideoTable from '/@/views/mineai/data/dataset-details2/details/video-table.vue';
  import VideoDatasetTable from '/@/views/mineai/data/dataset-details2/details/video-dataset-table.vue';
  import InfoOverView from '/@/views/mineai/data/dataset-details2/details/info-overview.vue';
  import { count } from '../api/index';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

  const route = useRoute();
  const router = useRouter();

  // 数据集id 数据集名称
  const id = parseInt(route.params?.id as string);
  const name = route.params?.name;
  const flag = parseInt(route.params?.flag as string);
  const annotateType = parseInt(route.params?.annotateType as string);
  // flag 是数据集 module，并非 dataType。独立视频数据集类型由详情入口通过查询参数明确传递。
  const dataType = parseInt(route.query?.dataType as string);
  const isVideoDataset = dataType === 6;
  // 信息总览（图片/视频数据集均可用，内部按 isVideoDataset 走对应命名空间接口）
  const content = shallowRef(InfoOverView);
  const tabContent = ref(null);
  const params = {
    status: '303',
  };
  // 独立视频数据集无图片命名空间统计（数字 id 跨类型冲突，datasets/{id} 会查到图片数据集），跳过 count/files-infos
  // 获取图片标注信息
  let image_haveAnnotation = ref(0);
  let image_noAnnotation = ref(0);
  if (!isVideoDataset) {
    count(id, params).then((data) => {
      image_haveAnnotation.value = data.haveAnnotation;
      image_noAnnotation.value = data.noAnnotation;
    });
  }

  //获取视频抽帧信息
  let extractedVideos = ref(0);
  let unextractedVideos = ref(0);
  if (!isVideoDataset) {
    maHttp
      .get(
        {
          url: `datasets/${id}/files/infos`,
          headers: {},
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      )
      .then((data) => {
        extractedVideos.value = data.extractedVideos;
        unextractedVideos.value = data.unextractedVideos;
      });
  }

  // 默认进入信息总览
  const activeKey = ref('1');

  const handleTabChange = (key) => {
    activeKey.value = key;
    switch (activeKey.value) {
      case '1':
        content.value = InfoOverView;
        break;
      case '2':
        content.value = ImageTable;
        break;
      case '3':
        // 图片数据集沿用历史视频表（data_file）；独立视频数据集使用 video_dataset_file。
        content.value = isVideoDataset ? VideoDatasetTable : VideoTable;
        break;
      default:
        // 可以在这里处理其他情况，或者什么都不做
        break;
    }
  };

  function goBack() {
    router.go(-1);
  }

  onActivated(() => {
    if (activeKey.value === '2' || activeKey.value === '3') tabContent.value?.reload();
  });
</script>
<style scoped>
  #exit-button :deep(.ant-tag-checkable) {
    background-color: rgb(128, 128, 128);
    border-color: transparent;
  }

  #exit-button :deep(.ant-tag-checkable-checked) {
    background-color: #0960bd;
    border-color: transparent;
  }

  .table-tag {
    margin-left: 15px;
    margin-right: 15px;
    font-size: 18px;
    padding: 7px;
  }
</style>

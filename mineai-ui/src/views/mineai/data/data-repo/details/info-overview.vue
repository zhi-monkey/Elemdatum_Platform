<template>
  <div>
    <a-row :gutter="[8, 8]" style="height: 100%">
      <a-col :span="12" style="height: 60vh">
        <div style="height: 95%" class="flex flex-col">
          <a-card style="height: 100vh" :loading="false" title="数据仓库信息">
            <Description
              :column="1"
              :data="datasetInfo"
              :schema="datasetSchema"
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
              :schema="picSchema"
              layout="horizontal"
            />
          </a-card>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
  import { datasetSchema, picSchema } from '/@/views/mineai/data/data-repo/details/image-list';
  import { Description } from '/@/components/Description';
  import { Card as ACard, Col as ACol, Row as ARow } from 'ant-design-vue';
  import { PicAndVideoOthersDetails, getDataRepoInfo } from '../../dataset-details2/api/index';
  import { onMounted, reactive } from 'vue';

  const props = defineProps({
    id: Number,
    name: String,
  });

  // 定义响应式对象
  const datasetInfo: any = reactive({});
  const picAndVideoInfo: any = reactive({});

  onMounted(async () => {
    // 发送请求获取数据
    const response = await getDataRepoInfo(props.id);
    datasetInfo.id = response.id;
    datasetInfo.name = response.name;
    datasetInfo.createTime = response.createTime;
    datasetInfo.remark = response.remark;

    const picVideoOthersResponse = await PicAndVideoOthersDetails(props.id);
    picAndVideoInfo.imageCount =
      picVideoOthersResponse == null ? 0 : picVideoOthersResponse.imageCount;
    picAndVideoInfo.videoCount =
      picVideoOthersResponse == null ? 0 : picVideoOthersResponse.videoCount;
    picAndVideoInfo.otherCount =
      picVideoOthersResponse == null ? 0 : picVideoOthersResponse.otherCount;
  });
</script>

<style scoped lang="less"></style>

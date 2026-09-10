<template>
  <div class="px-2 py-1">
    <div class="flex flex-row" style="margin-bottom: 8px"
      ><img src="../../../../../../assets/icons/titles.svg" /><span style="opacity: 0.8"
        >图片标注占比</span
      >
    </div>
    <Card :loading="loading" style="width: 100%; height: 300px">
      <a-row v-for="(value, key) in data" :key="key" style="margin-top: 10px">
        <a-col style="width: 15%">
          <a-tag>{{ key === 'annotatedFiles' ? '已标注' : '未标注' }}:</a-tag>
        </a-col>
        <a-col style="width: 85%">
          <a-progress
            :percent="value"
            :stroke-color="{
              from: '#108ee9',
              to: '#87d068',
            }"
            :strokeWidth="10"
            :format="(percent) => percent + '%'"
            style="padding-bottom: 20px"
          />
        </a-col>
      </a-row>
    </Card>
  </div>
</template>
<script lang="ts" setup>
  import { onUnmounted, ref } from 'vue';
  import {
    Card,
    Progress as AProgress,
    Tag as ATag,
    Row as ARow,
    Col as ACol,
  } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { ceil } from 'lodash-es';

  const loading = ref(true);
  let data = ref();
  let timer;

  function getData() {
    maHttp
      .get(
        {
          url: 'datasets/versions/countByFileAnnotate',
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      )
      .then((v) => {
        data.value = {
          annotatedFiles: ceil(100 * (v.annotatedFiles / (v.annotatedFiles + v.unannotatedFiles))),
          unannotatedFiles: ceil(
            100 * (v.unannotatedFiles / (v.annotatedFiles + v.unannotatedFiles)),
          ),
        };
      });
  }

  maHttp
    .get(
      {
        url: 'datasets/versions/countByFileAnnotate',
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    )
    .then((v) => {
      setTimeout(() => {
        data.value = {
          annotatedFiles: ceil(100 * (v.annotatedFiles / (v.annotatedFiles + v.unannotatedFiles))),
          unannotatedFiles: ceil(
            100 * (v.unannotatedFiles / (v.annotatedFiles + v.unannotatedFiles)),
          ),
        };
        loading.value = false;
      }, 100);
      timer = setInterval(getData, 10000);
    });

  onUnmounted(() => clearInterval(timer));
</script>

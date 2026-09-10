<template>
  <div class="flex flex-row">
    <div class="flex py-1">
      <img src="../../../../../../assets/icons/titles.svg" alt="标识符" />
      <span style="font-size: medium">报警图片</span>
    </div>
  </div>
  <Carousel :loading="loading" arrows autoplay dots-class="slick-dots slick-thumb" v-if="flush">
    <template #customPaging="props">
      <div
        style="
          width: 80px;
          height: 55px;
          display: flex;
          justify-content: center;
          align-items: center;
        "
        class="dark:bg-gray-1000"
      >
        <img :src="images[props.i]" alt="找不到" style="height: 45px; max-width: 100%" />
      </div>
    </template>
    <div v-for="(image, index) in images" :key="index">
      <div
        style="
          width: 100%;
          height: 200px;
          display: flex;
          justify-content: center;
          align-items: center;
        "
        class="dark:bg-gray-1000"
      >
        <img :src="image" alt="找不到" style="height: 200px; max-width: 100%"
      /></div>
      <span
        style="
          text-align: center;
          display: block;
          font-size: small;
          overflow: hidden;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
        "
        >{{ imageName[index] }}</span
      >
    </div>
  </Carousel>
</template>

<script lang="ts" setup>
  import { Carousel } from 'ant-design-vue';
  import { nextTick, onUnmounted, ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  let dataTimer;
  let data = [];
  const loading = ref(true);
  //不使用ref无法更新视图
  let flush = ref(true);
  let images = ref([]);
  let imageName = ref([]);
  function setImage() {
    maHttp
      .get(
        {
          url: 'modelAlert/getMonitorAlertPicUrl',
          params: { dataNum: 6 },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then((v) => {
        data = v;
      });
    images.value = [];
    imageName.value = [];
    for (let i in data) {
      images.value.push(data[i].picUrl as never);
      imageName.value.push(data[i].picName as never);
    }
    flush.value = false;
    nextTick(() => {
      flush.value = true;
    });
    console.log(images);
  }

  maHttp
    .get(
      {
        url: 'modelAlert/getMonitorAlertPicUrl',
        params: { dataNum: 6 },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((v) => {
      data = v;
      images.value = [];
      imageName.value = [];
      for (let i in data) {
        images.value.push(data[i].picUrl as never);
        imageName.value.push(data[i].picName as never);
      }
      flush.value = false;
      nextTick(() => {
        flush.value = true;
      });
      setTimeout(() => {
        loading.value = false;
      }, 100);
      dataTimer = setInterval(setImage, 60000);
    });
  onUnmounted(() => {
    clearInterval(dataTimer);
  });
</script>

<style scoped>
  .ant-carousel :deep(.slick-dots) {
    position: relative;
    height: 80%;
  }
  .ant-carousel :deep(.slick-slide img) {
    display: block;
    margin: auto;
    max-width: 80%;
  }
  .ant-carousel :deep(.slick-arrow) {
    display: none !important;
  }
  .ant-carousel :deep(.slick-thumb) {
    bottom: 0px;
  }
  /*.ant-carousel :deep(.slick-thumb li) {*/
  /*  width: 50px;*/
  /*  height: 35px;*/
  /*}*/
  /*.ant-carousel :deep(.slick-thumb li img) {*/
  /*  width: 100%;*/
  /*  height: 100%;*/
  /*  display: block;*/
  /*}*/
  .ant-carousel :deep .slick-thumb li.slick-active img {
  }
</style>

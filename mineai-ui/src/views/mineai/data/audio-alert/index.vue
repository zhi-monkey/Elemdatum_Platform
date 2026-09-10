<template>
  <PageWrapper dense contentClass="flex flex-col px-2" class="bp">
    <audio-alert-info style="height: 100%; width: 100%" />
  </PageWrapper>
</template>
<script lang="ts">
  import { PageWrapper } from '/@/components/Page';
  import { onMounted, onUnmounted } from 'vue';
  import AudioAlertInfo from './components/AudioAlertInfo.vue';

  export default {
    components: {
      PageWrapper,
      AudioAlertInfo,
    },
    setup() {
      let dataTimer;

      onMounted(async () => {
        // await getData();
        //每小时刷新一下页面
        // dataTimer = setInterval(reload, 3600000);
      });

      onUnmounted(() => {
        clearInterval(dataTimer);
      });
      //获取数据
      // async function getData() {}

      return {
        // getData,
      };
    },
  };
</script>
<style lang="less" scoped>
  .bp {
    background-image: url(../../../../assets/images/bigScreenBackground.png);
    width: 100%;
    height: 100%;
    background-size: 100% 100%;
  }
</style>

<template>
  <PageWrapper title="训练任务管理" @back="goBack" fixedHeight contentFullHeight class="myPage">
    <TrainInfo v-if="ifShow" :job-id="jobId" :show-bar="true" :change-tab-title="true" />
  </PageWrapper>
</template>

<script lang="ts">
  import { defineComponent, onMounted, ref, unref } from 'vue';
  import { PageWrapper } from '/@/components/Page';
  import { useGo } from '/@/hooks/web/usePage';
  import { useTabs } from '/@/hooks/web/useTabs';
  import TrainInfo from './TrainInfo.vue';
  import { router } from '/@/router';
  import { useMultipleTabStore } from '/@/store/modules/multipleTab';

  export default defineComponent({
    components: {
      TrainInfo,
      PageWrapper,
    },
    setup() {
      const go = useGo();
      const jobId = ref<number>(0);
      const ifShow = ref<boolean>(false);

      onMounted(() => {
        const jobInfo = location.href.substring(
          location.href.lastIndexOf('/') + 1,
          location.href.length,
        );
        jobId.value = Number(jobInfo.substring(0, jobInfo.lastIndexOf('_')));
        ifShow.value = true;
      });
      const goBack = async () => {
        const tabStore = useMultipleTabStore();
        const currentRoute = unref(router.currentRoute);

        // 先跳转页面
        go('/maTrainingCenter/modelGeneration');

        // 直接调用 tabStore 的 closeTab
        setTimeout(() => {
          tabStore.closeTab(currentRoute, router).catch(() => {});
        }, 100);
      };
      return {
        jobId,
        ifShow,
        goBack,
      };
    },
  });
</script>

<style scoped>
  span {
    font-size: 18px;
  }

  .myPage {
    margin: 0 16px 0 16px;
  }

  .myPage >>> .ant-card-head {
    min-height: 35px;
    display: flex;
  }

  .myPage >>> .ant-card-head-title {
    padding: 0 0;
  }

  .myPage >>> .vben-page-wrapper-content {
    margin: 16px 0 16px 0;
  }
</style>

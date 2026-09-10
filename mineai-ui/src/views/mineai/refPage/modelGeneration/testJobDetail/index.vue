<template>
  <PageWrapper title="训练任务管理" @back="goBack" fixedHeight contentFullHeight class="myPage">
    <TestInfo
      v-if="ifShow"
      :job-id="jobId"
      :another-job-id="anotherJobId"
      :show-bar="true"
      :change-tab-title="true"
    />
  </PageWrapper>
</template>

<script lang="ts">
  import { defineComponent, onMounted, ref } from 'vue';
  import { PageWrapper } from '/@/components/Page';
  import { useGo } from '/@/hooks/web/usePage';
  import { useTabs } from '/@/hooks/web/useTabs';
  import TestInfo from './TestInfo.vue';

  export default defineComponent({
    components: {
      TestInfo,
      PageWrapper,
    },
    setup() {
      const go = useGo();
      const jobId = ref<number>(0);
      const anotherJobId = ref<number>(0);
      const ifShow = ref<boolean>(false);
      const { closeCurrent } = useTabs();

      onMounted(() => {
        const jobInfo = location.href.substring(
          location.href.lastIndexOf('/') + 1,
          location.href.length,
        );
        jobId.value = Number(jobInfo.substring(0, jobInfo.lastIndexOf('_')));
        anotherJobId.value = Number(jobInfo.substring(jobInfo.lastIndexOf('_') + 1));
        ifShow.value = true;
      });
      const goBack = () => {
        go('/maTrainingCenter/modelGeneration');
        closeCurrent();
      };
      return {
        jobId,
        ifShow,
        goBack,
        anotherJobId,
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

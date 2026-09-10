<template>
  <div>
    <div style="margin: 3% 5%">
      <span style="font-size: 20px">当前引导任务: </span>
      <i style="margin: 0 5%; font-size: 22px">{{ inputValue }}</i>
      <a-button type="primary" @click="Publish()" v-if="boolResult"> 发布</a-button>
      <span style="font-size: 20px; margin-top: 10px"><br />模型训练结果: </span>
      <i
        class="h-1/15 w-1/2"
        style="margin: 0 5%; font-size: 22px; color: #2692ff"
        v-if="!accBarShow && newJobAcc > originJobAcc"
        >最新训练模型准确率为{{ newJobAcc }}%,原始模型准确率为{{ originJobAcc }}%,发布新模型</i
      >
      <i
        class="h-1/15 w-1/2"
        style="margin: 0 5%; font-size: 22px; color: #2692ff"
        v-if="!accBarShow && newJobAcc <= originJobAcc"
        >最新训练模型准确率为{{ newJobAcc }}%,原始模型准确率为{{ originJobAcc }}%,不会发布新模型</i
      >
      <i class="h-1/15 w-1/2" style="margin: 0 5%; font-size: 22px" v-if="accBarShow"
        >当前最新模型准确率为{{ newJobAcc }}%</i
      >
    </div>
    <div>
      <div style="display: flex; flex-direction: row; margin: 2% 5%; gap: 20px"
        ><new-model-pie class="w-1/2" :new-job-acc="newJobAcc" />
        <origin-model-pie class="w-1/2" :origin-job-acc="originJobAcc" v-if="!accBarShow" />
        <acc-bar class="w-1/2" v-if="accBarShow"
      /></div>
    </div>
    <Divider />
    <h3 style="margin-left: 2%">说明</h3>
    <p style="margin-left: 2%">根据当前训练任务质检结果来确定是否需要发布。</p>
  </div>
</template>
<script setup lang="ts">
  import { Button as AButton, Divider } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { computed, onMounted, onUnmounted, ref } from 'vue';
  import NewModelPie from './pie/newModelPie.vue';
  import OriginModelPie from '/@/views/mineai/model/generationGuide/pie/originModelPie.vue';
  import AccBar from './AccBar.vue';

  const props = defineProps<{ generationId: number; generationName: string }>();
  const { createMessage } = useMessage();
  const inputValue = computed(() => props.generationName);
  const boolResult = ref(false);

  let defaultPercent = ref();
  let accBarShow = ref(false);
  let progress = ref(0);
  let timer: NodeJS.Timer;
  let timer1: NodeJS.Timer;
  let newJobAcc = ref(0);
  let originJobAcc = ref();
  const modelGeneration = ref();
  const jobId = ref(0);
  const anotherJobId = ref(0);

  onMounted(async () => {
    newJobAcc.value = 0;
    await getModelGeneration();
    timer = setInterval(getProgress, 5000);
    timer1 = setInterval(compareGeneration, 10000);
  });

  onUnmounted(() => {
    clearInterval(timer);
    clearInterval(timer1);
  });

  function getProgress() {
    maHttp
      .get(
        {
          url: 'modelJob/getTestJobProgressAndAcc',
          params: { testJobId: jobId.value, anotherTestJobId: anotherJobId.value },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then((v) => {
        defaultPercent.value = v;
        progress.value = Math.round(defaultPercent.value.progress);
        newJobAcc.value = defaultPercent.value.newNowAcc.toFixed(3);
        originJobAcc.value = defaultPercent.value.newOriginAcc.toFixed(3);
      });
  }
  function compareGeneration() {
    if (
      Number(newJobAcc.value) > Number(originJobAcc.value) &&
      newJobAcc.value != 0 &&
      originJobAcc.value != 0
    ) {
      maHttp
        .get(
          {
            url: 'modelGeneration/publishGuideGeneration',
            params: { generationId: props.generationId },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        )
        .then(() => {
          createMessage.success('训练模型发布成功！引导任务完成！');
          clearInterval(timer);
          clearInterval(timer1);
        });
    } else if (newJobAcc.value != 0 && originJobAcc.value != 0) {
      maHttp
        .get(
          {
            url: 'modelGeneration/completeGuideGeneration',
            params: { generationId: props.generationId },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        )
        .then(() => {
          createMessage.success('引导任务完成！');
          clearInterval(timer);
          clearInterval(timer1);
        });
    }
  }

  /**
   * 根据ModelGenerationId获取ModelGeneration
   */
  function getModelGeneration() {
    maHttp
      .get(
        {
          url: 'modelGeneration/findModelGenerationById',
          params: {
            id: props.generationId,
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
          timeout: 102400000,
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(async (thisModelGeneration) => {
        modelGeneration.value = thisModelGeneration;
        jobId.value = Number(modelGeneration.value.testJob.id);
        anotherJobId.value = Number(modelGeneration.value.anotherTestJob.id);
        // if (modelGeneration.anotherTestJob === null) {
        //   jobInfo.value = modelGeneration.testJob.id + '_' + -1;
        //   jobId.value = Number(jobInfo.value.substring(0, jobInfo.value.lastIndexOf('_')));
        //   anotherJobId.value = Number(jobInfo.value.substring(jobInfo.value.lastIndexOf('_') + 1));
        //   accBarShow.value = true;
        //   getLog(jobId.value);
        // } else {
        //   jobInfo.value = modelGeneration.testJob.id + '_' + modelGeneration.anotherTestJob.id;
        //   jobId.value = Number(jobInfo.value.substring(0, jobInfo.value.lastIndexOf('_')));
        //   anotherJobId.value = Number(jobInfo.value.substring(jobInfo.value.lastIndexOf('_') + 1));
        // }
      });
  }

  // async function getLog(jobId) {
  //   return await maHttp
  //     .get(
  //       {
  //         url: 'modelJob/getJobByJobId',
  //         params: { id: jobId },
  //         headers: {
  //           // @ts-ignore
  //           ignoreCancelToken: true,
  //         },
  //       },
  //       { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  //     )
  //     .then((job) => {
  //       jobName.value = job.name;
  //       console.log(jobName.value);
  //       return maHttp.get(
  //         {
  //           url: 'log/getJobLog',
  //           params: {
  //             jobName: job.name,
  //             direction: 'BACKWARD',
  //             limit: 50,
  //             start: 1,
  //             namespace: 'ai-platform-s2',
  //           },
  //           headers: {
  //             // @ts-ignore
  //             ignoreCancelToken: true,
  //           },
  //         },
  //         { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  //       );
  //     });
  // }
  async function Publish() {
    await maHttp
      .get(
        {
          url: 'modelGeneration/publishModelGeneration',
          params: { modelGenerationId: props.generationId },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('发布成功！');
      });
  }
</script>

<style scoped lang="less">
  .step {
    &-form {
      width: 450px;
      margin: 0 auto;
    }

    h3 {
      margin: 0 0 12px 10px;
      font-size: 16px;
      line-height: 32px;
    }

    h4 {
      margin: 0 0 4px 10px;
      font-size: 14px;
      line-height: 22px;
    }

    p {
      margin-left: 10px;
    }
  }
</style>

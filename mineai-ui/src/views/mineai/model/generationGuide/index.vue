<template>
  <PageWrapper :contentStyle="{ 'background-color': '#181d31', 'border-radius': '4px' }">
    <div class="step-form-form">
      <a-steps :current="currentStep">
        <a-step title="新建引导任务" />
        <a-step title="模型训练" />
        <a-step title="模型转换" />
        <a-step title="结果" />
      </a-steps>
    </div>
    <div class="mt-5 justify-center">
      <Model @next="handleModelNext" v-if="current === 0" />
      <div v-if="current === 1">
        <div class="flex flex-col justify-center w-full">
          <a-card title="训练进度" style="height: 15vh">
            <i class="flex justify-center">{{ trainProgress }}%</i>
            <div class="flex justify-center">
              <a-progress :percent="trainProgress" :show-info="false" style="width: 60%" />
            </div>
          </a-card>
          <TrainInfo
            :job-id="trainJobId"
            :show-bar="false"
            :change-tab-title="false"
            v-if="ifTrainShow"
          />
        </div>
      </div>
      <div v-if="current === 2">
        <div class="flex flex-col justify-center w-full">
          <a-card title="质检进度" style="height: 15vh">
            <i class="flex justify-center">{{ testProgress }}%</i>
            <div class="flex justify-center">
              <a-progress :percent="testProgress" :show-info="false" style="width: 60%" />
            </div>
          </a-card>
        </div>
      </div>
      <Convert2
        :generation-id="generationId"
        :generation-name="generationName"
        v-if="current === 3"
      />
      <Release
        :generation-id="generationId"
        :generation-name="generationName"
        v-if="current === 4"
      />
    </div>
  </PageWrapper>
</template>
<script lang="ts">
  import { defineComponent, onMounted, onUnmounted, ref, watch } from 'vue';
  import Model from './Model.vue';
  import { PageWrapper } from '/@/components/Page';
  import { Steps, Progress as AProgress, Card as ACard } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import Release from '/@/views/mineai/model/generationGuide/Release.vue';
  import TrainInfo from '/@/views/mineai/model/modelGeneration/trainJobDetail/TrainInfo.vue';
  import Convert2 from '/@/views/mineai/model/generationGuide/convertPage/Convert2.vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useRoute } from 'vue-router';

  export default defineComponent({
    name: 'FormStepPage',
    components: {
      Release,
      AProgress,
      ACard,
      Model,
      Convert2,
      PageWrapper,
      [Steps.name]: Steps,
      [Steps.Step.name]: Steps.Step,
      TrainInfo,
    },
    setup() {
      const route = useRoute();
      const currentStep = ref(0);
      const current = ref(0);

      const trainJobId = ref<number>(0);
      const ifTrainShow = ref<boolean>(false);
      const testJobId = ref<number>(0);
      const anotherTestJobId = ref<number>(0);
      const ifTestShow = ref<boolean>(false);
      const hasConvertMV = ref<boolean>(false);

      const { createMessage } = useMessage();
      const loading = ref(false);
      let testTimer;
      let trainTimer;
      let timer;
      let defaultPercent = ref();
      let trainProgress = ref(0);
      let testProgress = ref(50);
      const generationStatus = ref(0);
      // const deploymentStatus = ref(null);

      const modelGeneration = ref();

      //Model组件传出的数据
      const generationName = ref('');
      const generationId = ref(0);
      const imageType = ref('');
      const datasetId = ref(0);

      //Upload组件传出的数据
      const modelVersion = ref(null);
      const jobType = ref(0);
      const monitor = ref(null);
      //存储质检作业完成之后的准确率
      const newJobAcc = ref(0);
      const originJobAcc = ref(0);

      function getTrainProgress() {
        maHttp
          .get(
            {
              url: 'modelJob/getTrainJobProgress',
              params: { jobId: modelGeneration.value.latestJobId },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((v) => {
            trainProgress.value = Math.round(v / 2);
          });
      }

      onMounted(() => {
        newJobAcc.value = 0;
        originJobAcc.value = 0;
        //首先判断传入的GenerationId是否为空
        if (route.query.modelGenerationId != null) {
          generationId.value = Number(route.query.modelGenerationId);
          generationName.value = String(route.query.modelGenerationName);

          //接下来判断，如果传入的作业类型是训练，并且传入的步骤是1，那么创建训练作业
          if (Number(route.query.jobType) === 1 && Number(route.query.currentStep) === 1) {
            current.value = 1;
            currentStep.value = 1;
            maHttp
              .get(
                {
                  url: 'modelJob/createJob',
                  params: {
                    modelGenerationId: generationId.value,
                    modelWorkingMode: 1,
                  },
                  headers: {
                    // @ts-ignore
                    ignoreCancelToken: true,
                  },
                },
                { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
              )
              .then(
                async () => {
                  //进入训练页面
                  setTimeout(setTrainInfo, 10000);
                  await maHttp
                    .get(
                      {
                        url: 'modelGeneration/findModelGenerationById',
                        params: { id: route.query.modelGenerationId },
                        headers: {
                          // @ts-ignore
                          ignoreCancelToken: true,
                        },
                      },
                      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
                    )
                    .then((value) => {
                      modelGeneration.value = value;
                    });
                  //将mg状态设置为1——正在训练

                  trainJobId.value = modelGeneration.value.latestJobId;
                  generationStatus.value = 1;
                  timer = setInterval(async () => {
                    console.log('onmounted train timer');
                    generationStatus.value = await maHttp
                      .get(
                        {
                          url: 'modelGeneration/findModelGenerationById',
                          params: { id: generationId.value },
                          headers: {
                            // @ts-ignore
                            ignoreCancelToken: true,
                          },
                        },
                        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
                      )
                      .then((value) => {
                        console.log(value.modelStore.convertModelVersion);
                        if (value.modelStore.convertModelVersion != null) {
                          hasConvertMV.value = true;
                          console.log(hasConvertMV.value);
                        }
                        return value.status;
                      });
                  }, 5000);
                  trainTimer = setInterval(async () => {
                    getTrainProgress();
                  }, 5000);
                },
                () => {
                  createMessage.error('训练作业创建失败！');
                },
              );
          }
          //若传入的作业类型为2，步骤也是2，进入质检页面
          if (Number(route.query.jobType) === 2 && Number(route.query.currentStep) === 1) {
            current.value = 2;
            currentStep.value = 1;
            maHttp
              .get(
                {
                  url: 'modelJob/createTestJob',
                  params: { modelGenerationId: generationId.value, modelWorkingMode: 5 },
                  headers: {
                    // @ts-ignore
                    ignoreCancelToken: true,
                  },
                },
                { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
              )
              .then(async () => {
                await maHttp
                  .get(
                    {
                      url: 'modelGeneration/findModelGenerationById',
                      params: { id: generationId.value },
                      headers: {
                        // @ts-ignore
                        ignoreCancelToken: true,
                      },
                    },
                    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
                  )
                  .then(async (value) => {
                    modelGeneration.value = value;
                    //设置质检作业的id
                    testJobId.value = modelGeneration.value.testJob.id;
                    anotherTestJobId.value = modelGeneration.value.anotherTestJob.id;
                    //将mg状态设置为2——正在质检
                    generationStatus.value = 2;
                    loading.value = true;

                    timer = setInterval(async () => {
                      console.log('onmounted test timer running');
                      generationStatus.value = await maHttp
                        .get(
                          {
                            url: 'modelGeneration/findModelGenerationById',
                            params: { id: generationId.value },
                            headers: {
                              // @ts-ignore
                              ignoreCancelToken: true,
                            },
                          },
                          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
                        )
                        .then((value) => {
                          console.log(value.modelStore.convertModelVersion);
                          if (value.modelStore.convertModelVersion != null) {
                            hasConvertMV.value = true;
                            console.log(hasConvertMV.value);
                          }
                          return value.status;
                        });
                    }, 5000);
                  });
                ifTrainShow.value = false;
                ifTestShow.value = true;
                testTimer = setInterval(async () => {
                  await getTestProgress();
                }, 5000);
              });
          }
          //若传入step为3，那么进入结果展示和发布页面
          else if (Number(route.query.jobType) === 1 && Number(route.query.currentStep) === 3) {
            current.value = 4;
            currentStep.value = 3;
          } else if (Number(route.query.jobType) === 3 && Number(route.query.currentStep) === 2) {
            current.value = 3;
            currentStep.value = 2;
          }
        }
      });
      function getTestProgress() {
        maHttp
          .get(
            {
              url: 'modelJob/getTestJobProgressAndAcc',
              params: { testJobId: testJobId.value, anotherTestJobId: anotherTestJobId.value },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((v) => {
            defaultPercent.value = v;
            testProgress.value = Math.round(defaultPercent.value.progress);
            newJobAcc.value = defaultPercent.value.newNowAcc.toFixed(3);
            originJobAcc.value = defaultPercent.value.newOriginAcc.toFixed(3);
            judgeConvert();
          });
      }
      function setTrainInfo() {
        ifTrainShow.value = true;
      }

      async function handleModelNext(modelInfo: { generationName: string; generationId: number }) {
        generationId.value = modelInfo.generationId;
        generationName.value = modelInfo.generationName;
        await maHttp
          .get(
            {
              url: 'modelJob/createJob',
              params: {
                modelGenerationId: generationId.value,
                modelWorkingMode: 1,
              },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then(
            async () => {
              await maHttp
                .get(
                  {
                    url: 'modelGeneration/findModelGenerationById',
                    params: { id: generationId.value },
                    headers: {
                      // @ts-ignore
                      ignoreCancelToken: true,
                    },
                  },
                  { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
                )
                .then((value) => {
                  modelGeneration.value = value;
                  //将mg状态设置为1——正在训练
                  generationStatus.value = 1;
                  loading.value = true;
                  currentStep.value = 1;
                  current.value = 1;
                  trainJobId.value = modelGeneration.value.latestJobId;
                  ifTrainShow.value = true;
                  timer = setInterval(async () => {
                    console.log('from start timer');
                    generationStatus.value = await maHttp
                      .get(
                        {
                          url: 'modelGeneration/findModelGenerationById',
                          params: { id: generationId.value },
                          headers: {
                            // @ts-ignore
                            ignoreCancelToken: true,
                          },
                        },
                        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
                      )
                      .then((value) => {
                        console.log(value.modelStore.convertModelVersion);
                        if (value.modelStore.convertModelVersion != null) {
                          hasConvertMV.value = true;
                          console.log(hasConvertMV.value);
                        }
                        return value.status;
                      });
                  }, 5000);
                  trainTimer = setInterval(async () => {
                    getTrainProgress();
                  }, 5000);
                });
            },
            () => {
              createMessage.error('训练作业创建失败！');
            },
          );
      }

      //监听job状态变化
      watch(generationStatus, async (newValue) => {
        if (newValue === 6) {
          clearInterval(trainTimer);
          maHttp
            .get(
              {
                url: 'modelJob/createTestJob',
                params: { modelGenerationId: generationId.value, modelWorkingMode: 5 },
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then(async () => {
              console.log('test Job Created');
              await maHttp
                .get(
                  {
                    url: 'modelGeneration/findModelGenerationById',
                    params: { id: generationId.value },
                    headers: {
                      // @ts-ignore
                      ignoreCancelToken: true,
                    },
                  },
                  { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
                )
                .then(async (value) => {
                  modelGeneration.value = value;
                  //设置质检作业的id
                  testJobId.value = modelGeneration.value.testJob.id;
                  anotherTestJobId.value = modelGeneration.value.anotherTestJob.id;
                  //将mg状态设置为2——正在质检
                  generationStatus.value = 2;
                  loading.value = true;
                  currentStep.value = 1;
                  current.value = 2;
                });
              ifTrainShow.value = false;
              ifTestShow.value = true;
              testTimer = setInterval(async () => {
                await getTestProgress();
              }, 5000);
            });
        }
        // if (modelGeneration.value.anotherTestJob.status === 1) {
        //   loading.value = false;
        //   console.log('new Value is 7 and testProgress');
        //   maHttp
        //     .get(
        //       {
        //         url: 'modelJob/getTestJobProgressAndAcc',
        //         params: { testJobId: testJobId.value, anotherTestJobId: anotherTestJobId.value },
        //         headers: {
        //           // @ts-ignore
        //           ignoreCancelToken: true,
        //         },
        //       },
        //       { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        //     )
        //     .then((v) => {
        //       testProgress.value = 50 + Math.round(v.progress / 2);
        //       newJobAcc.value = v.value.newNowAcc.toFixed(3);
        //       originJobAcc.value = v.value.newOriginAcc.toFixed(3);
        //       setTimeout(judgeConvert, 3000);
        //     });
        // }
        if (newValue === 15) {
          createMessage.success('算法转换完成');
          current.value = 4;
          currentStep.value = 3;
        }
      });
      function judgeConvert() {
        if (newJobAcc.value != 0 && originJobAcc.value != 0) {
          if (newJobAcc.value <= originJobAcc.value || !hasConvertMV.value) {
            clearInterval(testTimer);
            current.value = 4;
            currentStep.value = 3;
          } else if (newJobAcc.value > originJobAcc.value && hasConvertMV.value) {
            clearInterval(testTimer);
            current.value = 3;
            currentStep.value = 2;
          }
        }
      }

      onUnmounted(() => {
        clearInterval(timer);
        clearInterval(trainTimer);
        clearInterval(testTimer);
      });

      return {
        currentStep,
        current,
        loading,
        datasetId,
        generationName,
        generationId,
        imageType,
        modelVersion,
        trainJobId,
        testJobId,
        anotherTestJobId,
        ifTestShow,
        ifTrainShow,
        jobType,
        monitor,
        handleModelNext,
        trainProgress,
        testProgress,
        modelGeneration,
      };
    },
  });
</script>
<style lang="less" scoped>
  .step-form-content {
    padding: 24px;
  }

  .step-form-form {
    width: 80%;
    margin: 40px auto 0;
  }
</style>

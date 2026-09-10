<template>
  <PageWrapper :contentStyle="{ 'background-color': '#181d31', 'border-radius': '4px' }">
    <div class="step-form-form">
      <a-steps :current="currentStep">
        <a-step title="开始训练" />
        <a-step title="训练中" />
        <a-step title="开始质检" />
        <a-step title="质检中" />
        <a-step title="开始部署" />
        <a-step title="部署中" />
        <a-step title="部署完成" />
      </a-steps>
    </div>
    <div class="mt-5">
      <Model @next="handleModelNext" v-if="current === 0" />
      <Upload
        @next="handleUploadNext"
        :model-name="modelName"
        :model-english-name="modelEnglishName"
        :image-type="imageType"
        v-if="current === 1"
      />
      <Job
        @next="handleJobNext"
        :model-version="modelVersion"
        :job-type="jobType"
        v-if="current === 2"
      />
      <Loading :loading="loading" tip="训练中" v-if="current === 3" />
      <Job
        @next="handleJobNext"
        :model-version="modelVersion"
        :job-type="jobType"
        v-if="current === 4"
      />
      <Loading :loading="loading" tip="质检中" v-if="current === 5" />
      <Upload
        @next="handleDeployUploadNext"
        :model-name="modelName"
        :model-english-name="modelEnglishName"
        :image-type="imageType"
        v-if="current === 6"
      />
      <Bind
        @next="handleBindNext"
        :model-version-id="modelVersion.id"
        :model-version-show-name="modelVersion.showName"
        v-if="current === 7"
      />
      <Deploy
        @next="handleDeployNext"
        :monitor-id="monitor.id"
        :monitor-name="monitor.monitorName"
        :model-version-url="modelVersion.url"
        :model-version-show-name="modelVersion.showName"
        :model-version-id="modelVersion.id"
        v-if="current === 8"
      />
      <div style="width: 600px; margin: 0 auto" v-if="current === 9">
        <Result status="success" title="部署成功">
          <template #extra>
            <span>请前往算法管理页面查看部署结果，待部署完成后，于实时监控页面查看推理流</span>
          </template>
        </Result>
      </div>
    </div>
  </PageWrapper>
</template>
<script lang="ts">
  import { defineComponent, onUnmounted, ref, watch } from 'vue';
  import Model from './Model.vue';
  import Upload from './Upload.vue';
  import Job from './Job.vue';
  import Bind from './Bind.vue';
  import Deploy from './Deploy.vue';
  import { PageWrapper } from '/@/components/Page';
  import { Loading } from '/@/components/Loading';
  import { Steps, Result } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  export default defineComponent({
    name: 'FormStepPage',
    components: {
      Upload,
      Job,
      Bind,
      Deploy,
      Model,
      PageWrapper,
      Loading,
      [Steps.name]: Steps,
      [Steps.Step.name]: Steps.Step,
      Result,
    },
    setup() {
      const currentStep = ref(0);
      const current = ref(0);

      const loading = ref(false);

      let timer;
      const jobStatus = ref(2);
      // const deploymentStatus = ref(null);

      //Model组件传出的数据
      const modelName = ref('');
      const modelEnglishName = ref('');
      const imageType = ref('');

      //Upload组件传出的数据
      const modelVersion = ref(null);
      const jobType = ref(0);

      const monitor = ref(null);

      function handleModelNext(modelInfo: { modelName: string; modelEnglishName: string }) {
        modelName.value = modelInfo.modelName;
        modelEnglishName.value = modelInfo.modelEnglishName;
        imageType.value = '训练';
        currentStep.value++;
        current.value++;
      }

      function handleUploadNext(trainInfo: { modelVersion: any; jobType: number }) {
        modelVersion.value = trainInfo.modelVersion;
        jobType.value = trainInfo.jobType;
        current.value++;
      }

      async function handleJobNext(jobId: number) {
        jobStatus.value = 2;
        loading.value = true;
        //如果是创建完质检任务，step组件前进一步进入“进行中”
        if (current.value === 4) currentStep.value++;
        current.value++;
        //更新job状态
        timer = setInterval(async () => {
          jobStatus.value = await maHttp.get(
            {
              url: 'update/getModelJobStatus',
              params: { modelJobId: jobId },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          );
        }, 5000);
      }

      //监听job状态变化
      watch(jobStatus, (newValue) => {
        //如果状态不是ModelJob.EXECUTING——执行中，停止更新状态
        if (newValue !== 2) {
          loading.value = false;
          clearInterval(timer);
          //任务完成
          if (newValue === 1) {
            switch (current.value) {
              case 3:
                jobType.value = 2;
                break;
              case 5:
                imageType.value = '推理';
                break;
              default:
                break;
            }
            currentStep.value++;
            current.value++;
          }
          //TODO:任务失败
          // else if (newValue === -1) {
          // }
          //TODO:任务取消
          // else if (newValue === -2) {
          // }
          //TODO:其他情况
          // else {
          // }
        }
      });

      function handleDeployUploadNext(modelVersionInfo: { modelVersion: any; jobType: number }) {
        modelVersion.value = modelVersionInfo.modelVersion;
        currentStep.value++;
        current.value++;
      }

      function handleBindNext(bindMonitor: any) {
        monitor.value = bindMonitor;
        current.value++;
      }

      function handleDeployNext(deployMentInfo: { namespace: string; deployName: string }) {
        // loading.value = true;
        currentStep.value++;
        current.value++;
        // timer = setInterval(async () => {
        //   deploymentStatus.value = await maHttp.get(
        //     {
        //       url: 'deploy/getDeployStatus',
        //       params: deployMentInfo,
        //       headers: {
        //         // @ts-ignore
        //         ignoreCancelToken: true,
        //       },
        //     },
        //     { urlPrefix: MaBackendUrlEnum.CONTAINER_MANAGER },
        //   );
        // }, 5000);
      }

      //监听deployment状态变化
      // watch(deploymentStatus, (newValue) => {
      //   //如果等于1，则部署成功，停止更新状态
      //   if (newValue === 1) {
      //     loading.value = false;
      //     clearInterval(timer);
      //     currentStep.value++;
      //     current.value++;
      //   }
      // });

      onUnmounted(() => {
        clearInterval(timer);
      });

      return {
        currentStep,
        current,
        loading,
        modelName,
        modelEnglishName,
        imageType,
        modelVersion,
        jobType,
        monitor,
        handleModelNext,
        handleUploadNext,
        handleJobNext,
        handleDeployUploadNext,
        handleBindNext,
        handleDeployNext,
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

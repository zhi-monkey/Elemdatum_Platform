<template>
  <div style="position: relative; height: 96%">
    <LivePlayer
      v-show="videoUrl !== ''"
      aspect="fullscreen"
      :video-url="videoUrl"
      :video-title="props.title"
      :live="true"
      :hide-stretch-button="true"
      :show-custom-button="false"
      :fluent="false"
      :hasaudio="props.hasAudio"
    />
    <div v-if="videoUrl !== '' && showBtn" style="position: absolute; top: 5%; right: 5%">
      <Button
        type="primary"
        ghost
        size="small"
        @click="switchStream"
        :disabled="modelStream === ''"
      >
        {{ videoType }}
      </Button>
    </div>
    <div v-show="videoUrl === ''" class="ant-empty" style="height: 96%; margin: 0">
      <div class="ant-empty-image" style="height: 100%; background-color: rgb(20, 24, 41)">
        <svg
          t="1680504585131"
          class="icon"
          viewBox="0 0 1024 1024"
          version="1.1"
          xmlns="http://www.w3.org/2000/svg"
          p-id="1101"
          width="128"
          height="128"
        >
          <path
            d="M926.826667 86.506667c15.914667 15.893333 15.936 41.941333 0.576 59.605333l-2.304 2.474667-90.730667 90.688A382.208 382.208 0 0 1 896 448c0 141.077333-76.8 267.669333-195.306667 334.549333l-6.741333 3.712-6.165333 3.221334 54.378666 108.778666c13.226667 26.453333-3.84 57.301333-31.978666 61.312l-3.050667 0.32L704 960H320c-30.656 0-50.965333-31.189333-39.466667-58.88l1.301334-2.88 54.378666-108.757333-6.144-3.2c-9.173333-4.949333-18.133333-10.24-26.816-15.893334l-144 144.042667c-17.642667 17.621333-45.44 18.389333-62.08 1.728-15.914667-15.893333-15.936-41.941333-0.576-59.605333l2.304-2.474667 765.866667-765.845333c17.621333-17.621333 45.397333-18.389333 62.08-1.728zM772.266667 301.397333l-96.938667 96.96a170.666667 170.666667 0 0 1-212.970667 212.970667l-96.981333 96.96c13.866667 7.829333 28.48 14.592 43.733333 20.202667a42.666667 42.666667 0 0 1 24.832 56.149333l-1.365333 2.986667L389.034667 874.666667h245.930666l-43.52-87.04a42.666667 42.666667 0 0 1 17.536-56.448l2.88-1.450667 3.050667-1.258667A298.773333 298.773333 0 0 0 810.666667 448c0-53.290667-13.952-103.296-38.4-146.602667zM512 64c76.416 0 147.605333 22.314667 207.424 60.8l-62.208 62.165333A297.322667 297.322667 0 0 0 512 149.333333c-164.949333 0-298.666667 133.717333-298.666667 298.666667 0 52.224 13.525333 101.909333 37.653334 145.216L188.8 655.424a383.125333 383.125333 0 0 1-60.373333-189.568l-0.341334-9.877333L128 448c0-212.074667 171.925333-384 384-384z m0 213.333333c16.682667 0 32.789333 2.389333 48.021333 6.848l-211.84 211.84A170.666667 170.666667 0 0 1 512 277.333333z"
            fill="#333333"
            p-id="1102"
          />
        </svg>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import LivePlayer from '@liveqing/liveplayer-v3';
  import { defineProps, onMounted, Ref, ref, watch } from 'vue';
  import { Button } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';

  const { createMessage } = useMessage();

  const videoUrl: Ref<string> = ref<string>('');
  const showModelStream: Ref<boolean> = ref<boolean>(false);
  const videoType: Ref<string> = ref<string>('原始流');
  const props = defineProps<{
    showBtn: boolean;
    isModelStreamShow: boolean;
    monitorStream: string;
    modelStream: string;
    title: string;
    hasAudio: boolean;
    monitorInfo: { scene: string; model: string; monitor: string };
  }>();

  //进入页面时初始化
  onMounted(async () => {
    if (props.showBtn) {
      if (showModelStream.value == props.isModelStreamShow) {
        videoUrl.value = props.isModelStreamShow ? props.modelStream : props.monitorStream;
      } else {
        showModelStream.value = props.isModelStreamShow;
      }
    } else {
      videoUrl.value = props.monitorStream;
    }
  });

  //切换视频流
  async function switchStream() {
    await maHttp
      .post(
        {
          url: 'monitorModelConfig/savePushStream',
          params: {
            modelName: props.monitorInfo.model,
            monitorName: props.monitorInfo.monitor,
            pushStream: !showModelStream.value,
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
      )
      .then(async () => {
        await maHttp.get(
          {
            url: 'controller/createAllControllerJsonConfig',
            params: {
              flag: 1,
            },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
        );
      })
      .then(() => {
        showModelStream.value = !showModelStream.value;
      })
      .catch(() => {
        maHttp.post(
          {
            url: 'monitorModelConfig/savePushStream',
            params: {
              modelName: props.monitorInfo.model,
              monitorName: props.monitorInfo.monitor,
              pushStream: showModelStream.value,
            },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
        );
      });
  }

  //切换显示原始流或推理流
  watch(showModelStream, (newVal) => {
    if (newVal) {
      videoType.value = '推理流';
      videoUrl.value = props.modelStream;
    } else {
      videoType.value = '原始流';
      videoUrl.value = props.monitorStream;
    }
  });
</script>

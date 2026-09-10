<template>
  <div v-show="props.videoUrl !== ''" ref="container" class="jessibuca-container"></div>
  <div v-show="props.videoUrl === ''" class="ant-empty" style="height: 96%; margin: 0">
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
</template>

<script lang="ts" setup>
  import { onBeforeUnmount, onMounted, Ref, ref, watch } from 'vue';
  import Jessibuca from '/#/jessibuca-pro';

  let _jessibuca: any = null;
  const container = ref(null);
  let fontSize: Ref<string> = ref<string>('0');
  const videoInfo: any = ref(null);
  const props = defineProps<{
    modelStreamShow: boolean;
    videoUrl: string;
    title: string;
    bufferTime: number;
    bufferDelayTime: number;
    hasAudio: boolean;
    debug: boolean;
    useMSE: boolean;
    offScreen: boolean;
    contentList?: Array<any>;
  }>();

  const createJessibuca = () => {
    _jessibuca = new (window as any).JessibucaPro({
      container: container.value,
      decoder: '/decoder-pro.js',
      videoBuffer: props.bufferTime, // 缓存时长
      videoBufferDelay: props.bufferDelayTime, // 缓存延迟
      text: '',
      loadingText: '加载中',
      hasAudio: props.hasAudio,
      debug: props.debug,
      useVideoRender: true,
      hiddenAutoPause: true,
      timeout: 5,
      heartTimeout: 5,
      heartTimeoutReplay: true,
      heartTimeoutReplayTimes: -1, //无限重试
      loadingTimeout: 5,
      loadingTimeoutReplay: true,
      loadingTimeoutReplayTimes: -1,
      keepScreenOn: true, //保持手机常亮
      isFlv: true,
      controlAutoHide: true,
      showBandwidth: true, // 显示网速
      useMSE: props.useMSE,
      useCanvasRender: false,
      isWebrtcForZLM: true,
      operateBtns: {
        fullscreen: true,
        screenshot: false,
        play: false,
        audio: true,
        record: false,
        zoom: false,
        performance: true,
      },
      forceNoOffscreen: !props.offScreen,
      isNotMute: false,
      // watermarkConfig:
      //   !props.modelStreamShow && props.contentList !== undefined
      //     ? [
      //         {
      //           //opacity: 0.5, // 透明度
      //           backgroundColor: 'rgba(0,0,0,0.3)',
      //           // 文字配置
      //           text: {
      //             content: props.modelStreamShow ? '' : props.title,
      //             color: 'white',
      //             fontSize: fontSize.value,
      //           },
      //           right: 0,
      //           bottom: 0,
      //         },
      //         {
      //           image: {
      //             src: waterMark.value,
      //             width: width.value,
      //             height: height.value,
      //           },
      //           left: 0,
      //           top: 0,
      //         },
      //       ]
      //     : [
      //         {
      //           //opacity: 0.5, // 透明度
      //           backgroundColor: 'rgba(0,0,0,0.3)',
      //           // 文字配置
      //           text: {
      //             content: props.modelStreamShow ? '' : props.title,
      //             color: 'white',
      //             fontSize: fontSize.value,
      //           },
      //           right: 0,
      //           bottom: 0,
      //         },
      //       ],
    }) as Jessibuca;

    _jessibuca.on('error', async function (error: any) {
      console.error(`播放器${props.title}报错，重新创建播放器并1秒后重试播放`, error);
      if (_jessibuca !== null) {
        await _jessibuca.destroy();
        _jessibuca = null;
        await playVideo(props.videoUrl);
      }
    });
  };

  async function playVideo(url: string) {
    if (url !== '') {
      createJessibuca();
      _jessibuca.on('videoInfo', function (data) {
        if (data !== null && data !== undefined) videoInfo.value = data;
      });
      _jessibuca.on('play', () => {
        if (videoInfo.value !== null) {
          fontSize.value = ((35 * videoInfo.value.height) / 720).toString();
          _jessibuca.updateWatermark(
            !props.modelStreamShow && props.contentList !== undefined
              ? [
                  {
                    //opacity: 0.5, // 透明度
                    backgroundColor: 'rgba(0,0,0,0.3)',
                    // 文字配置
                    text: {
                      content: props.modelStreamShow ? '' : props.title,
                      color: 'white',
                      fontSize: fontSize.value,
                    },
                    right: 0,
                    bottom: 0,
                  },
                  {
                    image: {
                      src: draw(props.contentList, videoInfo.value.width, videoInfo.value.height),
                      width: videoInfo.value.width,
                      height: videoInfo.value.height,
                    },
                    left: 0,
                    top: 0,
                  },
                ]
              : [
                  {
                    //opacity: 0.5, // 透明度
                    backgroundColor: 'rgba(0,0,0,0.3)',
                    // 文字配置
                    text: {
                      content: props.modelStreamShow ? '' : props.title,
                      color: 'white',
                      fontSize: fontSize.value,
                    },
                    right: 0,
                    bottom: 0,
                  },
                ],
          );
        }
      });
      await _jessibuca.play(url);
      // if (!props.modelStreamShow && props.contentList !== undefined) {
      //   _jessibuca.addContentToCanvas(props.contentList);
      // }
    }
  }

  const draw = function (areas, width, height) {
    let canvas = document.createElement('canvas');
    canvas.height = height;
    canvas.width = width;
    let ctx = canvas.getContext('2d');
    ctx.fillStyle = 'rgba(255, 255, 255, 0)';
    ctx.beginPath();
    for (let i = 0; i < areas.length; i++) {
      const area = areas[i];
      ctx.moveTo(area[0]['x'], area[0]['y']);
      for (let j = 1; j < area.length; j++) ctx.lineTo(area[j]['x'], area[j]['y']);
      ctx.lineTo(area[0]['x'], area[0]['y']);
    }
    ctx.strokeStyle = 'green';
    ctx.lineWidth = 5;
    ctx.stroke();
    ctx.closePath();
    return canvas.toDataURL('image/png');
  };

  //进入页面时初始化
  onMounted(async () => {
    await playVideo(props.videoUrl);
  });

  //设置里切换显示视频流或推理流
  watch(
    () => props.videoUrl,
    async () => {
      if (_jessibuca !== null) {
        await _jessibuca.destroy();
        _jessibuca = null;
      }
      await playVideo(props.videoUrl);
    },
  );

  //bufferTime变更，只需要重新set
  watch(
    () => props.bufferTime,
    (bufferTime: number) => {
      _jessibuca && _jessibuca.setBufferTime(bufferTime);
    },
  );
  //debug变更，只需要重新set
  watch(
    () => props.debug,
    (debug: boolean) => {
      _jessibuca && _jessibuca.setDebug(debug);
    },
  );
  //hasAudio、useMSE、offScreen任意之一变更，需要销毁重新创建
  watch(
    () => [props.hasAudio, props.useMSE, props.offScreen, props.bufferDelayTime],
    async () => {
      if (_jessibuca !== null) {
        await _jessibuca.destroy();
        _jessibuca = null;
      }
      await playVideo(props.videoUrl);
    },
  );

  onBeforeUnmount(async () => {
    if (_jessibuca !== null) {
      await _jessibuca.destroy();
      _jessibuca = null;
    }
  });
</script>
<style scoped>
  .jessibuca-container {
    background: rgba(13, 14, 27, 0.7);
    width: 100%;
    height: 96%;
  }
</style>

<template>
  <BasicModal
    title="视频信息"
    :destroyOnClose="true"
    :draggable="true"
    :showOkBtn="false"
    :showCancelBtn="false"
    :canFullscreen="false"
    @register="register"
  >
    <div
      v-if="props?.mediaType === 'image'"
      style="
        width: 100%;
        height: 350px;
        display: flex;
        justify-content: center;
        align-items: center;
        position: relative;
      "
      @mouseover="showMask = true"
      @mouseout="showMask = false"
      class="dark:bg-gray-1000"
    >
      <div
        v-if="showMask && hasLocation"
        :style="{
          width: `${imgWidth}`,
          height: `${imgHeight}`,
          position: 'absolute',
          'z-index': 1,
          'pointer-events': 'none',
          display: 'flex',
          'justify-content': 'flex-end',
          'align-items': 'flex-end',
        }"
      >
        <div class="flex flex-row-reverse mr-2 mb-2">
          <Icon icon="gg:maximize" size="32" />
        </div>
      </div>
      <a-image
        :src="getfiledownloadUrl(props?.mediaPath)"
        :fallback="fallbackImage"
        style="max-height: 350px; max-width: 100%; object-fit: contain"
        :preview="true"
      />
    </div>
    <div
      v-else-if="props?.mediaType === 'audio'"
      style="
        width: 100%;
        height: 350px;
        display: flex;
        justify-content: center;
        align-items: center;
      "
      class="dark:bg-gray-1000"
    >
      <audio
        autoplay="autoplay"
        :src="getfiledownloadUrl(props?.mediaPath)"
        controls="controls"
        style="max-height: 100%; max-width: 100%"
      >
        您的浏览器不支持 audio 标签。
      </audio>
    </div>
    <div
      v-else-if="props?.mediaType === 'video'"
      style="
        width: 100%;
        height: 350px;
        display: flex;
        justify-content: center;
        align-items: center;
      "
      class="dark:bg-gray-1000"
      @contextmenu.prevent
    >
      <custom-player :key="videoUrl" :url="videoUrl" style="max-height: 350px; max-width: 100%">
        您的浏览器不支持 video 标签。
      </custom-player>
    </div>
  </BasicModal>
</template>
<script lang="ts" setup>
  import { onBeforeMount, onBeforeUnmount, ref, watch } from 'vue';
  import Icon from '/@/components/Icon';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { Image as AImage } from 'ant-design-vue';
  import CustomPlayer from './components/CustomPlayer.vue';
  import { bucketHost, bucketName } from '/@/utils/dubhe';

  interface AlertMsg {
    alertId: number;
    status: number;
    mediaPath: string;
    alertMsg: string;
    description: string;
    fullDescription: string;
    createTime: string;
    monitorName: string;
    fullMonitorName: string;
    modelName: string;
    fullModelName: string;
    mediaType: string;
    fileName: string;
  }
  const videoUrl = ref('');
  const props = ref<AlertMsg>();
  let showMask = ref(false);
  let imgWidth = ref();
  let imgHeight = ref();
  let hasLocation = ref(false);
  const fallbackImage =
    'data:image/png;base64,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';

  const [register] = useModalInner(async (data) => {
    props.value = data;
    const mediaPath = data.mediaPath;
    if (mediaPath) {
      videoUrl.value = getfiledownloadUrl(mediaPath);
    }
  });

  watch(showMask, (sm) => {
    if (sm && !hasLocation.value) {
      const imgList = document.getElementsByTagName('img');
      imgWidth.value = imgList[imgList.length - 1].offsetWidth + 'px';
      imgHeight.value = imgList[imgList.length - 1].offsetHeight + 'px';
      hasLocation.value = true;
    }
  });

  /**
   * 获取文件下载/预览的URL
   * 兼容两种存储形式：
   * 1) 老 dubhe 数据：url 已含 bucket 前缀，如 `cz-dev/dataset/xxx/yyy.mp4`
   * 2) 新视频数据集：url 是 objectKey（不含 bucket），如 `dataset/video/1/origin/xxx.mp4`
   * MinIO 直链必须带上 bucket 名（例如 /cz-dev/），否则返回 403。
   * */
  function getfiledownloadUrl(filePath) {
    if (!filePath) {
      return '';
    }
    if (/^https?:\/\//i.test(filePath)) {
      return filePath;
    }
    if (filePath.startsWith(`${bucketName}/`)) {
      return `${bucketHost}/${filePath}`;
    }
    return `${bucketHost}/${bucketName}/${filePath}`;
  }
</script>

<template>
  <video-player
    :key="url"
    :src="url"
    poster=""
    :muted="true"
    :volume="0.6"
    :autoplay="false"
    :playbackRates="[0.5, 1, 1.5, 2, 3, 4]"
    :disablePictureInPicture="true"
    :control-bar="{ pictureInPictureToggle: false }"
    playsinline
    controls
  />
</template>
<script lang="ts" setup>
  import { VideoPlayer } from '@videojs-player/vue';
  import 'video.js/dist/video-js.css';

  defineProps<{
    url: string;
  }>();
</script>
<style scoped>
  .video-js >>> .vjs-big-play-button {
    top: 50% !important;
    left: 50% !important;
  }
</style>

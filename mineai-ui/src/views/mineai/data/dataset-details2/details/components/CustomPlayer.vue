<template>
  <video-player
    :src="url"
    poster=""
    :muted="true"
    :volume="0.6"
    :autoplay="false"
    :playbackRates="[0.5, 1, 1.5, 2, 3, 4]"
    :disablePictureInPicture="true"
    :control-bar="{ pictureInPictureToggle: false, currentTimeDisplay: true }"
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
<style>
.vjs-live .vjs-time-control, .vjs-live .vjs-time-divider, .video-js .vjs-current-time, .video-js .vjs-duration {
  display: block;
}
</style>

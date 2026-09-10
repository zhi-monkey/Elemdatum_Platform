<template>
  <div class="p-4 bp" style="margin-left: 3px; margin-right: 3px">
    <!--    <image-card />-->
    <div id="exit-button">
      <Button type="primary" shape="circle" size="large" @click="switchVersion">
        <template #icon><SwitcherOutlined /></template>
      </Button>
    </div>
    <component :is="content" />
  </div>
</template>

<script lang="ts" setup name="AlertMes">
  import ImageCard from '/@/views/mineai/monitor/alertMsgNew/imageCard.vue';
  import AlertMsgOld from '/@/views/mineai/monitor/alert-msg/index.vue';
  import { SwitcherOutlined } from '@ant-design/icons-vue';
  import { Button } from 'ant-design-vue';
  import { shallowRef } from 'vue';

  const content = shallowRef(ImageCard);

  const switchVersion = () => {
    content.value = content.value === ImageCard ? AlertMsgOld : ImageCard;
  };
</script>

<style lang="less" scoped>
  .bp {
    background-image: url(../../../../assets/images/bigScreenBackground.png);
    width: 100%;
    height: 100%;
    background-size: 100% 100%;
  }

  #exit-button {
    z-index: 500;
    position: absolute;
    right: 1%;
    top: 0.5%;
  }
</style>

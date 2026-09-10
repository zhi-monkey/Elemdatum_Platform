<template>
  <div id="data-view">
    <full-screen-container>
      <div id="full-screen-button">
        <Button type="primary" shape="circle" size="large" @click="switchFullScreen">
          <template #icon>
            <FullscreenOutlined v-if="!isFullScreen" />
            <FullscreenExitOutlined v-else />
          </template>
        </Button>
      </div>
      <div id="exit-button">
        <Button type="primary" shape="circle" size="large" @click="goHome">
          <template #icon><ImportOutlined /></template>
        </Button>
      </div>

      <top-header />
      <component :is="infoId" />
      <tab-bar @on-click="getIndex" />
    </full-screen-container>
  </div>
</template>

<script lang="ts" setup>
  import TopHeader from './TopHeader.vue';
  import SystemInfo from './SystemInfo.vue';
  import ControllerInfo from './ControllerInfo.vue';
  import MonitorInfo from './MonitorInfo.vue';
  import DatasetInfo from './DatasetInfo.vue';
  import TabBar from './TabBar.vue';
  import { Button } from 'ant-design-vue';
  import {
    ImportOutlined,
    FullscreenOutlined,
    FullscreenExitOutlined,
  } from '@ant-design/icons-vue';
  import { FullScreenContainer } from '@kjgl77/datav-vue3';
  import { shallowRef, ref } from 'vue';
  import { useGo } from '/@/hooks/web/usePage';
  import { PageEnum } from '/@/enums/pageEnum';
  const go = useGo();
  const isFullScreen = ref(false);

  // 默认为系统总览
  const infoId = shallowRef(SystemInfo);

  // 得到index 0-3
  // 0系统总览 1算力控制器 2监控设备 3数据集
  const getIndex = (index: number) => {
    switch (index) {
      case 0:
        infoId.value = SystemInfo;
        break;
      case 1:
        infoId.value = ControllerInfo;
        break;
      case 2:
        infoId.value = MonitorInfo;
        break;
      case 3:
        infoId.value = DatasetInfo;
        break;
      default:
        infoId.value = SystemInfo;
        break;
    }
  };
  const goHome = function () {
    go(PageEnum.BASE_HOME);
  };

  const switchFullScreen = function () {
    const ele = document.body;
    if (!isFullScreen.value) {
      ele.requestFullscreen();
      isFullScreen.value = true;
    } else {
      document.exitFullscreen();
      isFullScreen.value = false;
    }
  };
</script>

<style lang="less">
  #data-view {
    width: 100%;
    height: 100%;
    background-color: #030409;
    color: #fff;

    #dv-full-screen-container {
      background-image: url('../../../../../src/assets/images/bg.png');
      background-size: 100% 100%;
      box-shadow: 0 0 3px blue;
      display: flex;
      flex-direction: column;
    }

    #exit-button {
      z-index: 500;
      position: absolute;
      right: 250px;
      top: 90%;
    }

    #full-screen-button {
      z-index: 500;
      position: absolute;
      right: 300px;
      top: 90%;
    }
  }
</style>

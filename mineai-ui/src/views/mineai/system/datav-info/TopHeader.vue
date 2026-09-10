<template>
  <div id="header-wrapper">
    <a-row class="header-decoration" :gutter="18">
      <a-col :span="8">
        <decoration8 />
      </a-col>
      <a-col :span="8">
        <decoration5 />
        <div class="center-title">AI平台系统总览</div>
      </a-col>
      <a-col :span="8">
        <decoration8 :reverse="true" />
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
  import { Decoration5, Decoration8 } from '@kjgl77/datav-vue3';
  import { Col as ACol, Row as ARow } from 'ant-design-vue';
</script>

<style lang="less">
  .header-decoration {
    height: 100px;
  }

  .center-title {
    position: absolute;
    font-size: 30px;
    font-weight: bold;
    left: 50%;
    top: 15px;
    transform: translateX(-50%);
  }

  #header-wrapper {
    padding-bottom: 10px;
  }
</style>

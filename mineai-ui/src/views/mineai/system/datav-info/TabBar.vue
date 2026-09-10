<template>
  <div id="nav-tab">
    <div
      class="nav-tab-item"
      :class="{ active: activeIndex === index }"
      v-for="(item, index) in items"
      :key="index"
      @click="switchTabBar(index)"
    >
      <i class="nav-tab-item_icon iconfont" :class="item.icon"></i>
      <p class="nav-tab-item_label">{{ item.label }}</p>
    </div>
    <div class="nav-tab-overlay" :style="{ left: activeIndex * 250 + 400 + 'px' }"></div>
  </div>
</template>

<script setup lang="ts">
  import { Ref, ref } from 'vue';

  let activeIndex: Ref<number> = ref(0);

  // 通过defineEmits派发一个事件
  // const emit = defineEmits(['on-click']);
  const emit = defineEmits<{ (e: 'on-click', index: number): void }>();
  const items: any[] = [
    { icon: 'icon-shouye', label: '首页' },
    { icon: 'icon-danxingsuanlitiaodu', label: '集群资源分析' },
    { icon: 'icon-jiankong', label: '监控设备' },
    { icon: 'icon-shujuji', label: '数据集' },
  ];

  const switchTabBar = (index: number) => {
    // 动态切换图标
    activeIndex.value = index;
    // 子组件给父组件传参
    emit('on-click', index);
  };
</script>

<style lang="less">
  #nav-tab {
    position: relative;
    width: 100%;
    height: 150px;
    background-color: #041135;
    display: flex;
    justify-content: center;
    align-items: center;
    border-radius: 20px 20px 90px 90px;
    overflow: hidden;
    border: 20px solid #041135;
  }

  .nav-tab-item {
    width: 250px;
    height: 100%;

    z-index: 2;
    transition: 0.3s;
    cursor: pointer;

    /* 居中 */
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
  }

  .nav-tab-item.active {
    width: 330px;
  }

  .nav-tab-item_icon {
    font-size: 32px;
    color: #4298e7;
    transition: 0.3s;
    transform: translate(0, 0px);
    padding-top: 10px;
  }

  .active .nav-tab-item_icon {
    transform: translate(0, -10px);
  }

  .nav-tab-item_label {
    font-size: 15px;
    color: #4298e7;
    opacity: 0;
    transition: 0.3s;
    user-select: none;
  }

  .active .nav-tab-item_label {
    opacity: 1;
  }

  .nav-tab-overlay {
    position: absolute;
    left: 400px;
    top: 0;

    height: 100%;
    width: 330px;

    background-color: #e4f2ff;
    border-radius: 20px;

    transition: 0.3s;
  }

  @font-face {
    font-family: 'iconfont'; /* Project id 4074674 */
    src: url('//at.alicdn.com/t/c/font_4074674_a390r250kec.woff2?t=1684393929124') format('woff2'),
      url('//at.alicdn.com/t/c/font_4074674_a390r250kec.woff?t=1684393929124') format('woff'),
      url('//at.alicdn.com/t/c/font_4074674_a390r250kec.ttf?t=1684393929124') format('truetype');
  }

  .iconfont {
    font-family: 'iconfont' !important;
    font-size: 20px;
    font-style: normal;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
  }

  .icon-shouye:before {
    content: '\e8b9';
  }

  .icon-shujuji:before {
    content: '\e6ad';
  }

  .icon-jiankong:before {
    content: '\eb37';
  }

  .icon-danxingsuanlitiaodu:before {
    content: '\e795';
  }
</style>

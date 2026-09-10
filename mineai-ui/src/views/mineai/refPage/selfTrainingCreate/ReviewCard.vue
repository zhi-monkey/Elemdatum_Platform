<template>
  <a-card title="审核文件查看" style="max-width: 1000px">
    <a-button type="primary">跳转标注</a-button>
    <swiper
      :style="{
        '--swiper-navigation-color': '#fff',
        '--swiper-pagination-color': '#fff',
      }"
      :loop="true"
      :prevent-clicks="true"
      :spaceBetween="10"
      :navigation="true"
      :thumbs="{ swiper: thumbsSwiper }"
      :modules="[FreeMode, Navigation, Thumbs]"
      class="mySwiper2"
    >
      <swiper-slide v-for="item in imgList" :key="item">
        <img :src="item" alt="item" />
      </swiper-slide>
    </swiper>
    <swiper
      @swiper="setThumbsSwiper"
      :loop="true"
      :spaceBetween="10"
      :slidesPerView="5"
      :freeMode="true"
      :watchSlidesProgress="true"
      :modules="modules"
      class="mySwiper w-3/4"
    >
      <swiper-slide v-for="item in imgList" :key="item">
        <img :src="item" alt="item" />
      </swiper-slide>
    </swiper>
  </a-card>
</template>

<script setup lang="ts">
import { FreeMode, Navigation, Thumbs } from 'swiper/modules';
import { Swiper, SwiperSlide } from 'swiper/vue';
import 'swiper/css';
import 'swiper/css/free-mode';
import 'swiper/css/navigation';
import 'swiper/css/thumbs';
import { ref } from 'vue';
import { Card as ACard, InputNumber as AInputNumber } from 'ant-design-vue';

const thumbsSwiper = ref(null);
const setThumbsSwiper = (swiper) => {
  thumbsSwiper.value = swiper;
};

const modules = [FreeMode, Navigation, Thumbs];

const imgList = ref([
  'https://raw.githubusercontent.com/vueComponent/ant-design-vue/main/components/carousel/demo/abstract01.jpg',
  'https://raw.githubusercontent.com/vueComponent/ant-design-vue/main/components/carousel/demo/abstract02.jpg',
  'https://raw.githubusercontent.com/vueComponent/ant-design-vue/main/components/carousel/demo/abstract03.jpg',
  'https://raw.githubusercontent.com/vueComponent/ant-design-vue/main/components/carousel/demo/abstract04.jpg',
  'https://raw.githubusercontent.com/vueComponent/ant-design-vue/main/components/carousel/demo/abstract04.jpg',
  'https://raw.githubusercontent.com/vueComponent/ant-design-vue/main/components/carousel/demo/abstract04.jpg',
  'https://raw.githubusercontent.com/vueComponent/ant-design-vue/main/components/carousel/demo/abstract04.jpg',
]);
</script>

<style scoped>
.swiper {
  height: 85%;
}

.swiper-thumbs {
  height: 20% !important;

  .swiper-slide-thumb-active {
    opacity: 1 !important;
  }

  .swiper-slide {
    opacity: 0.4;
  }
}

.swiper-slide {
  display: flex;
  justify-content: center;
}

img {
  height: 97%;
}

.swiper-thumbs :deep(.swiper-slide img) {
  width: 100%;
}

.mySwiper2 :deep(.swiper-button-prev:after) {
  font-size: 32px;
}

.mySwiper2 :deep(.swiper-button-next:after) {
  font-size: 32px;
}
</style>


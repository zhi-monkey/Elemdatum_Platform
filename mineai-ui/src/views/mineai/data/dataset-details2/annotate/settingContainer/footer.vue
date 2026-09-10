<template>
  <div class="setting-container-footer">
    <el-form-item label="标注类型标签">
      <el-switch v-model="getShowTag" @change="toggleShowTag" />
    </el-form-item>
    <!--    不使用置信度-->
    <!--    <el-form-item v-if="!isSegmentation" label="智能评分">-->
    <!--      <el-switch :value="showScore" @change="toggleShowScore" />-->
    <!--    </el-form-item>-->
    <el-form-item v-if="isTrack" label="标注 Id">
      <el-switch v-model="getShowId" @change="toggleShowId" />
    </el-form-item>
  </div>
</template>

<script setup>
  // export default {
  //   name: 'Footer',
  //   props: {
  //     updateState: Function,
  //     showScore: Boolean,
  //     showTag: Boolean,
  //     showId: Boolean,
  //     toggleShowTag: Function,
  //     toggleShowScore: Function,
  //     toggleShowId: Function,
  //     isTrack: Boolean,
  //     isSegmentation: Boolean,
  //   },
  // };
  import { computed } from 'vue';

  const props = defineProps({
    updateState: Function,
    showScore: Boolean,
    showTag: Boolean,
    showId: Boolean,
    toggleShowTag: Function,
    toggleShowScore: Function,
    toggleShowId: Function,
    isTrack: Boolean,
    isSegmentation: Boolean,
  });

  const getShowTag = computed(() => props.showTag);
  const getShowId = computed(() => props.showId);
</script>

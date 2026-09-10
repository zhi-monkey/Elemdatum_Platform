<template>
  <LabelGroup title="导入标签组" @handle-ok="handleImportLabelGroup" />
</template>
<script>
  import LabelGroup from '/@/views/mineai/data/dataset-details2/components/labelGroup/index.vue';
  export default {
    name: 'ImportLabelGroup',
    components: {
      LabelGroup,
    },
    props: {
      handleImportLabelGroup: Function, // 处理标签组导入的回调函数
    },
  };
</script>

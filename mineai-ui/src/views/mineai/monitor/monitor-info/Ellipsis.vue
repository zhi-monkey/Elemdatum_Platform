<template>
  <div style="white-space: nowrap; text-overflow: ellipsis; overflow: hidden">
    <Tooltip>
      <template #title>{{ props.fullStr }}</template>
      {{ props.fullStr }}
    </Tooltip>
  </div>
</template>
<script lang="ts" setup>
  import { Tooltip } from 'ant-design-vue';

  const props = defineProps<{
    fullStr: string;
  }>();
</script>

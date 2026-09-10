<template>
  <Card style="margin-top: 30px"><textContent /></Card>
</template>

<script>
  import { defineComponent } from 'vue';
  import textContent from './TextContent.vue';
  import { Card } from 'ant-design-vue';
  export default defineComponent({
    components: {
      Card,
      textContent,
      // Card,
    },
  });
</script>

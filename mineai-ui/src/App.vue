<template>
  <ConfigProvider :locale="getAntdLocale">
    <AppProvider>
      <RouterView v-if="isRouterActive" />
    </AppProvider>
  </ConfigProvider>
</template>

<script lang="ts" setup>
  import { ConfigProvider } from 'ant-design-vue';
  import { AppProvider } from '/@/components/Application';
  import { useTitle } from '/@/hooks/web/useTitle';
  import { useLocale } from '/@/locales/useLocale';
  import { ref, provide, nextTick } from 'vue';

  // support Multi-language
  const { getAntdLocale } = useLocale();

  // Listening to page changes and dynamically changing site titles
  useTitle();

  //刷新页面函数，每个页面通过inject('reload') as Function引入使用
  const isRouterActive = ref(true);
  provide('reload', () => {
    isRouterActive.value = false;
    nextTick(() => {
      isRouterActive.value = true;
    });
  });
</script>
<style>
  .ant-select-dropdown {
    padding-left: 3px !important;
    padding-right: 3px !important;
    border-radius: 3px !important;
    background-color: #383d56 !important;
    .ant-select-item {
      border-radius: 6px;
    }
  }

  // 鼠标hover时候的颜色
  .ant-checkbox-wrapper:hover .ant-checkbox-inner,
  .ant-checkbox:hover .ant-checkbox-inner,
  .ant-checkbox-input:focus + .ant-checkbox-inner {
    border: 1px solid #0960bd !important;
  }
  // 设置默认的颜色
  .ant-checkbox {
    .ant-checkbox-inner {
      border: 1px solid #c9d1d9 !important;
      background-color: transparent;
    }
  }
  // 设置选中的颜色
  .ant-checkbox-checked .ant-checkbox-inner,
  .ant-checkbox-indeterminate .ant-checkbox-inner {
    background-color: #0960bd;
    border: 2px solid #0960bd;
  }

  .ant-spin-container > .ant-table > .ant-table-content > .ant-table-scroll > .ant-table-header {
    overflow: hidden !important;
    width: calc(100% - 8px);
    margin-bottom: 0 !important;
  }
</style>

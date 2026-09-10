<template>
  <a-result title="正在加载中。。。">
    <template #icon>
      <LoadingOutlined />
    </template>
  </a-result>
</template>

<script lang="ts" setup>
  import { useUserStore } from '/@/store/modules/user';
  import { Result as AResult } from 'ant-design-vue';
  import { LoadingOutlined } from '@ant-design/icons-vue';

  const userStore = useUserStore();

  userStore.login({
    password: '123456',
    username: 'jumpuser',
    mode: 'none', //不要默认的错误提示
  });
</script>

<template>
  <PageWrapper style="margin: 0 16px 0 16px">
    <div v-if="activeKey !== '3'">
      <component :is="content" />
    </div>
    <template #footer v-if="activeKey !== '3'">
      <Tabs :active-key="activeKey" @change="handleTabChange">
        <tab-pane key="1" tab="我发起的任务" v-if="publishable" />
        <tab-pane key="2" tab="我接收的任务" v-if="acceptable" />
      </Tabs>
    </template>
    <div style="margin-top: 350px" v-if="activeKey === '3'">
      <empty description="您当前所属角色无该页面权限" />
    </div>
  </PageWrapper>
</template>
<script setup lang="ts">
  import { ref, shallowRef, watch } from 'vue';
  import { PageWrapper } from '/@/components/Page';
  import { Empty, TabPane, Tabs } from 'ant-design-vue';
  import AcceptTask from '/@/views/mineai/data/group-labeling/AcceptTask.vue';
  import InitiateTask from '/@/views/mineai/data/group-labeling/InitiateTask.vue';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { PermissionEnum } from '/@/enums/PermissionEnum';

  const { hasPermission } = usePermission();
  const publishable = ref(hasPermission([PermissionEnum.DATA_PUBLISH_TASK]));
  const acceptable = ref(hasPermission([PermissionEnum.DATA_ACCEPT_TASK]));

  // 通过 localStorage 或默认值恢复 activeKey
  const savedTabKey = localStorage.getItem('activeTabKey');
  const activeKey = ref(savedTabKey || (publishable.value ? '1' : acceptable.value ? '2' : '3'));

  // 保存 activeKey 到 localStorage
  watch(
    () => activeKey.value,
    (key) => {
      localStorage.setItem('activeTabKey', key);
    },
  );

  // 默认显示内容
  const content = shallowRef(
    activeKey.value === '1' ? InitiateTask : activeKey.value === '2' ? AcceptTask : null,
  );

  // 切换选项卡
  const handleTabChange = (key) => {
    activeKey.value = key;
    switch (activeKey.value) {
      case '1':
        content.value = InitiateTask;
        break;
      case '2':
        content.value = AcceptTask;
        break;
      default:
        break;
    }
  };
</script>

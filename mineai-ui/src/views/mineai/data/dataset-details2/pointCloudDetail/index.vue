<template>
  <PageWrapper @back="goBack" style="margin: 0 16px 0 16px">
    <template #title>
      点云数据集
      <span style="font-weight: 700; color: #f5b956; font-size: 18px"> {{ name }} </span>
      详情
    </template>
    <div>
      <component :is="content" :id="id" :name="name" ref="tabContent" />
    </div>
    <template #footer>
      <Tabs :active-key="activeKey" @change="handleTabChange">
        <tab-pane key="1" tab="信息总览" />
        <tab-pane key="2" tab="PCD 文件列表" />
      </Tabs>
    </template>
  </PageWrapper>
</template>
<script lang="ts" setup name="PointCloudDetail">
  import { ref, shallowRef } from 'vue';
  import { PageWrapper } from '/@/components/Page';
  import { useRoute, useRouter } from 'vue-router';
  import { Tabs, TabPane } from 'ant-design-vue';
  import InfoOverView from './info-overview.vue';
  import FileTable from './file-table.vue';

  const route = useRoute();
  const router = useRouter();
  const content = shallowRef(InfoOverView);
  const tabContent = ref(null);

  // 点云数据集ID 与名称（从路由参数获取）
  const id = parseInt(route.params?.id as string);
  const name = route.params?.name;

  const activeKey = ref('1');

  const handleTabChange = (key) => {
    activeKey.value = key;
    switch (activeKey.value) {
      case '1':
        content.value = InfoOverView;
        break;
      case '2':
        content.value = FileTable;
        break;
      default:
        break;
    }
  };

  function goBack() {
    router.go(-1);
  }
</script>

<template>
  <PageWrapper @back="goBack" style="margin: 0 16px 0 16px">
    <template #title>
      数据仓库<span style="font-weight: 700; color: #f5b956; font-size: 18px"> {{ name }} </span>详情
    </template>
    <div>
      <component :is="content" :id="id" :name="name" :flag="flag" />
    </div>
    <template #footer>
      <Tabs :active-key="activeKey" @change="handleTabChange">
        <tab-pane key="1" tab="信息总览" />
        <tab-pane key="2" tab="图片列表" />
        <tab-pane key="3" tab="视频列表" />
        <tab-pane key="4" tab="其他" />
      </Tabs>
    </template>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { ref, shallowRef } from 'vue';
  import { Tabs, TabPane } from 'ant-design-vue';
  import { PageWrapper } from '/@/components/Page';
  import { useRoute, useRouter } from 'vue-router';
  import OtherTable from '/@/views/mineai/data/data-repo/details/OtherTable.vue';
  import ImageTable from '/@/views/mineai/data/data-repo/details/ImageTable.vue';
  import VideoTable from '/@/views/mineai/data/data-repo/details/Videotable.vue';
  import InfoOverView from '/@/views/mineai/data/data-repo/details/info-overview.vue';

  const route = useRoute();
  const router = useRouter();
  const content = shallowRef(InfoOverView);

  // 数据集id 数据集名称
  const id = parseInt(route.params?.id as string);
  const name = route.params?.name;
  const flag = parseInt(route.params?.flag as string);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars,no-unused-vars
  const params = {
    status: '303',
  };
  // 先显示信息总览
  const activeKey = ref('1');

  const handleTabChange = (key) => {
    activeKey.value = key;
    switch (activeKey.value) {
      case '1':
        content.value = InfoOverView;
        break;
      case '2':
        content.value = ImageTable;
        break;
      case '3':
        content.value = VideoTable;
        break;
      case '4':
        content.value = OtherTable;
        break;
      default:
        // 可以在这里处理其他情况，或者什么都不做
        break;
    }
  };

  function goBack() {
    router.go(-1);
  }
</script>

<style scoped>
  #exit-button :deep(.ant-tag-checkable) {
    background-color: rgb(128, 128, 128);
    border-color: transparent;
  }

  #exit-button :deep(.ant-tag-checkable-checked) {
    background-color: #0960bd;
    border-color: transparent;
  }

  .table-tag {
    margin-left: 15px;
    margin-right: 15px;
    font-size: 18px;
    padding: 7px;
  }
</style>

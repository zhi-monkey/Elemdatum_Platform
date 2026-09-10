<template>
  <PageWrapper style="margin: 0 16px 0 16px">
    <template #title> 镜像中心</template>
    <template #footer>
      <Tabs :active-key="activeKey" @change="handleTabChange">
        <TabPane key="1" tab="公开镜像" />
        <TabPane key="2" tab="私有镜像" />
      </Tabs>
    </template>

    <template v-if="activeKey === '1'">
      <PublicImageTable />
    </template>
    <template v-if="activeKey === '2'">
      <PrivateImageTable />
    </template>

    <imageModel @register="registerCreateModal" @success="handleSuccess" />
  </PageWrapper>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import PublicImageTable from './PublicImageTable.vue';
  import PrivateImageTable from './PrivateImageTable.vue';
  import { useModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ImageModel from '/@/views/mineai/model/imageCenter/imageModel.vue';
  import { PageWrapper } from '/@/components/Page';
  import { TabPane, Tabs } from 'ant-design-vue';

  const activeKey = ref('1');
  const { createMessage } = useMessage();
  const [registerCreateModal, { openModal: create }] = useModal();

  function handleTabChange(key) {
    activeKey.value = key;
  }

  function handleSuccess() {
    createMessage.success('成功！');
  }
</script>

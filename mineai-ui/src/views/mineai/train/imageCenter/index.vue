<template>
  <PageWrapper style="margin: 0 16px 0 16px">
    <template #title> 镜像中心</template>
    <PrivateImageTable />
    <imageModel @register="registerCreateModal" @success="handleSuccess" />
  </PageWrapper>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import PrivateImageTable from './PrivateImageTable.vue';
  import { useModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ImageModel from '/@/views/mineai/model/imageCenter/imageModel.vue';
  import { PageWrapper } from '/@/components/Page';

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

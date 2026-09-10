<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    title="模型转换记录"
    :showCancelBtn="false"
    :showOkBtn="false"
  >
    <a-list :loading="loading" item-layout="horizontal" :data-source="dataList" style="margin: 4px">
      <template #renderItem="{ item }">
        <a-list-item>
          <a-list-item-meta :description="item.createTime + ' - ' + item.lastJobTime">
            <template #title>
              <span>{{ item.name }}</span>
            </template>
          </a-list-item-meta>
        </a-list-item>
      </template>
    </a-list>
  </BasicModal>
</template>
<script lang="ts" setup>
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import {
    List as AList,
    ListItem as AListItem,
    ListItemMeta as AListItemMeta,
  } from 'ant-design-vue';
  import { ref } from 'vue';

  defineEmits(['register']);

  const loading = ref(true);
  const dataList = ref([]);

  const [registerModal, { setModalProps }] = useModalInner(async (data) => {
    dataList.value = data.history;
    loading.value = false;
    setModalProps({ confirmLoading: false });
  });
</script>
<style scoped lang="less"></style>

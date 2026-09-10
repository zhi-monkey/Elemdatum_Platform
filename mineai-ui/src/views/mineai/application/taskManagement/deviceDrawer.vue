<template>
  <a-drawer
    title="选择关联设备-固件"
    :visible="visible"
    @close="handleClose"
    width="400px"
    :maskClosable="true"
  >
    <a-tree
      :treeData="deviceAndFirmwareNameList"
      :defaultExpandAll="true"
      :selectedKeys="selectedKeys"
      @select="handleSelect"
    >
      <template #title="{ title, dataRef }">
        <div
          v-if="!dataRef.children"
          @click.stop="() => handleItemClick(dataRef.key)"
          style="display: flex; align-items: center; cursor: pointer"
        >
          <a-radio :checked="selectedKeys.includes(dataRef.key)" />
          <span style="margin-left: 8px">{{ title }}</span>
        </div>
        <span v-else>{{ title }}</span>
      </template>
    </a-tree>
    <div style="position: absolute; right: 40px; bottom: 20px; width: 100%; text-align: right">
      <a-button @click="handleClose" style="margin-right: 8px">取消</a-button>
      <a-button type="primary" @click="handleConfirm">确认</a-button>
    </div>
  </a-drawer>
</template>

<script lang="ts" setup>
  import { computed, defineEmits, defineProps, ref, watch } from 'vue';
  import {
    Button as AButton,
    Drawer as ADrawer,
    message,
    Radio as ARadio,
    Tree as ATree,
  } from 'ant-design-vue';

  const props = defineProps<{
    visible: boolean;
    treeData: any[];
    currentRowCheckedKey: number;
  }>();

  const deviceAndFirmwareNameList = computed(() => {
    return props.treeData.map((item) => {
      return {
        title: item.deviceName + '-' + item.firmwareVersion,
        key: item.id,
      };
    });
  });

  const emit = defineEmits(['close', 'confirm']);

  const selectedKeys = ref<string[]>([]);

  function handleSelect(selectedKeysValue: string[], { node }) {
    if (!node.children) {
      selectedKeys.value = [node.key];
    }
  }

  function handleItemClick(key: string) {
    selectedKeys.value = [key];
  }

  function handleClose() {
    emit('close');
  }

  function handleConfirm() {
    if (selectedKeys.value.length > 0) {
      // console.log(selectedKeys.value[0]);
      message.success(
        '选择的设备已确认: ' +
          deviceAndFirmwareNameList.value.find((item) => item.key === selectedKeys.value[0]).title,
      );
      emit('confirm', selectedKeys.value[0]);
      handleClose();
    } else {
      message.warning('请选择一个设备');
    }
  }

  watch(
    () => props.currentRowCheckedKey,
    (newValue) => {
      // console.log('currentRowCheckedKeys has changed:', newValue);
      selectedKeys.value[0] = newValue;
    },
    { immediate: true }, // 可选：立即执行一次回调以获取初始值
  );
</script>

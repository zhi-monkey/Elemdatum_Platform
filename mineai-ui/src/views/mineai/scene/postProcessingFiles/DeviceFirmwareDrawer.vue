<template>
  <a-drawer
    title="选择关联设备-固件"
    :visible="visible"
    @close="handleClose"
    width="400px"
    :maskClosable="true"
  >
    <a-tree
      checkable
      :treeData="treeData"
      :defaultExpandAll="true"
      v-model:checkedKeys="checkedKeys"
      :selectable="false"
    >
      <template #title="{ title, dataRef }">
        <span>{{ title }}</span>
        <span v-if="dataRef.firmwareVersion" style="color: #999; margin-left: 8px">
          ({{ dataRef.firmwareVersion }})
        </span>
      </template>
    </a-tree>
    <div style="position: absolute; right: 40px; bottom: 20px; width: 100%; text-align: right">
      <a-button @click="handleClose" style="margin-right: 8px">取消</a-button>
      <a-button type="primary" @click="handleConfirm">确认</a-button>
    </div>
  </a-drawer>
</template>

<script lang="ts" setup>
  import { ref, defineProps, defineEmits } from 'vue';
  import { Drawer as ADrawer, Tree as ATree, Button as AButton, message } from 'ant-design-vue';

  const props = defineProps<{
    visible: boolean;
  }>();

  const emit = defineEmits(['close', 'confirm']);

  const checkedKeys = ref<string[]>([]);

  const treeData = [
    {
      title: 'RK3588',
      key: 'rk3588',
      children: [
        {
          title: 'KBA12C',
          key: 'KBA12C-v1.0',
          firmwareVersion: 'v1.0',
        },
        {
          title: 'KBA13D',
          key: 'KBA13D-v2.1',
          firmwareVersion: 'v2.1',
        },
      ],
    },
    {
      title: 'RK3399',
      key: 'rk3399',
      children: [
        {
          title: 'XTR45B',
          key: 'XTR45B-v3.0',
          firmwareVersion: 'v3.0',
        },
      ],
    },
    {
      title: 'RK3568',
      key: 'rk3568',
      children: [
        {
          title: 'LMN78D',
          key: 'LMN78D-v1.5',
          firmwareVersion: 'v1.5',
        },
        {
          title: 'PQR90E',
          key: 'PQR90E-v2.0',
          firmwareVersion: 'v2.0',
        },
      ],
    },
  ];

  function handleClose() {
    emit('close');
  }

  function handleConfirm() {
    if (checkedKeys.value.length > 0) {
      const selectedDeviceFirmware = checkedKeys.value.filter((key) => key.includes('-'));
      message.success('选择的设备-固件已确认: ' + selectedDeviceFirmware.join(', '));
      emit('confirm', selectedDeviceFirmware);
      handleClose();
    } else {
      message.warning('请至少选择一个设备-固件组合');
    }
  }
</script>

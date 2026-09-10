<template>
  <a-drawer
    title="选择关联算法"
    :visible="visible"
    @close="handleClose"
    width="400px"
    :maskClosable="true"
  >
    <a-tree
      :treeData="treeData"
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
  import { ref, defineProps, defineEmits } from 'vue';
  import {
    Drawer as ADrawer,
    Tree as ATree,
    Button as AButton,
    Radio as ARadio,
    message,
  } from 'ant-design-vue';

  const props = defineProps<{
    visible: boolean;
  }>();

  const emit = defineEmits(['close', 'confirm']);

  const selectedKeys = ref<string[]>([]);

  const treeData = [
    {
      title: '安全帽识别',
      key: 'safety-helmet',
      children: [
        {
          title: 'pytorch-gpu',
          key: 'pytorch-gpu',
          children: [
            { title: 'yolo-123456', key: 'yolo-123456' },
            { title: 'yolo-654321', key: 'yolo-654321' },
          ],
        },
        {
          title: 'rknn-rk3588',
          key: 'rknn-rk3588',
          children: [{ title: 'yolo-789012', key: 'yolo-789012' }],
        },
      ],
    },
    // 更多应用数据...
  ];

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
      message.success('选择的算法已确认: ' + selectedKeys.value[0]);
      emit('confirm', selectedKeys.value[0]);
      handleClose();
    } else {
      message.warning('请选择一个算法');
    }
  }
</script>

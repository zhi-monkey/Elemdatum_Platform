<template>
  <a-drawer
    title="选择关联场景"
    :visible="visible"
    @close="handleClose"
    width="400px"
    :maskClosable="true"
  >
    <a-tree
      checkable
      :treeData="sceneNameList"
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
  import { computed, defineEmits, defineProps, nextTick, onMounted, ref, watch } from 'vue';
  import { Button as AButton, Drawer as ADrawer, message, Tree as ATree } from 'ant-design-vue';

  const props = defineProps<{
    visible: boolean;
    treeData: any[];
    currentRowCheckedKeys: any[];
  }>();

  // sceneNameList中有title和key字段，title为显示名称，key为id
  const sceneNameList = computed(() => {
    return props.treeData.map((item) => ({
      title: item.name, // 显示名称
      key: item.id, // ID
    }));
  });
  const emit = defineEmits(['close', 'confirm']);

  const checkedKeys = ref<string[]>([]);

  function handleClose() {
    emit('close');
  }

  function handleConfirm() {
    if (checkedKeys.value.length > 0) {
      const selectedScene = checkedKeys.value;
      const selectedSceneNames = selectedScene.map(
        (id) => sceneNameList.value.find((item) => item.key === id)?.title,
      );
      message.success('选择的场景已确认: ' + selectedSceneNames.join(', '));
      emit('confirm', selectedScene);
      handleClose();
    } else {
      message.warning('请至少选择一个场景');
    }
  }
  // async function initCheckedKeys() {
  //   console.log('tree' + treeData);
  // }
  // 使用 watch 监听 treeData 的变化
  watch(
    () => props.currentRowCheckedKeys,
    (newValue) => {
      // console.log('currentRowCheckedKeys has changed:', newValue);
      checkedKeys.value = newValue;
    },
    { immediate: true }, // 可选：立即执行一次回调以获取初始值
  );
</script>

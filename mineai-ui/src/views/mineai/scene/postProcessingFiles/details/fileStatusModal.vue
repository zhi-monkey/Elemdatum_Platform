<!-- fileStatusModal.vue -->
<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="文件状态" @ok="handleOk">
    <ul style="margin: 0; list-style: none; padding: 0">
      <li
        v-for="file in requiredFiles"
        :key="file.name"
        style="margin-bottom: 8px; display: flex; align-items: center"
      >
        <Badge :status="getBadgeStatus(file)" />
        <span style="margin-left: 8px">{{ file.name }} - {{ getFileDescription(file) }}</span>
      </li>
    </ul>
  </BasicModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { Badge } from 'ant-design-vue';

  const requiredFiles = [
    { name: '打包', required: true },
    { name: '启动脚本', required: true },
    { name: '配置文件', required: true },
    { name: '日志文件', required: true },
    { name: '监控文件', required: false }, // 缺少的文件
  ];

  const existingFiles = ['打包', '启动脚本', '配置文件', '日志文件', '打包'];

  const [registerModal, { closeModal }] = useModalInner();

  function getBadgeStatus(file) {
    if (!existingFiles.includes(file.name)) {
      return 'error'; // 缺少
    }
    const count = existingFiles.filter((f) => f === file.name).length;
    if (count > 1) {
      return 'warning'; // 重复
    }
    return 'success'; // 符合要求
  }

  function getFileDescription(file) {
    const status = getBadgeStatus(file);
    if (status === 'error') {
      return '缺少此文件';
    } else if (status === 'warning') {
      return '此文件重复';
    }
    return '此文件符合要求';
  }

  function handleOk() {
    closeModal();
  }
</script>

<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="管理资源配置" @ok="handleDelete">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { ManagementSchemas } from './data';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { ref } from 'vue';

  const { createMessage } = useMessage();
  const isDeleting = ref(false);
  const selectedConfigs = ref([]);

  // 移除了预设的配置ID，不可删除的配置由后端判断

  const [registerForm, { validate, setFieldsValue }] = useForm({
    schemas: ManagementSchemas,
    showActionButtonGroup: false,
  });

  const [registerModal, { closeModal }] = useModalInner(async (data) => {
    setFieldsValue({
      configurations: [],
    });
  });

  async function handleDelete() {
    try {
      const values = await validate();
      selectedConfigs.value = values.configurations;

      if (selectedConfigs.value.length === 0) {
        createMessage.warning('请先选择要删除的配置');
        return;
      }

      // 移除了预设配置的检查逻辑

      isDeleting.value = true;
      try {
        const response = await maHttp.delete(
          {
            url: '/modelGeneration/deleteHardwareParamsBatch',
            data: selectedConfigs.value,
          },
          {
            urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
          },
        );

        // 判断响应内容是否包含"无法删除"的提示
        if (typeof response === 'string' && response.includes('无法删除')) {
          // 如果有无法删除的配置，显示警告提示
          createMessage.warning(response);
        } else if (typeof response === 'string' && response.includes('删除成功')) {
          // 如果只是删除成功的提示，显示成功提示
          createMessage.success(response);
          emit('delete-success');
        } else {
          // 其他情况默认显示成功
          createMessage.success('删除成功');
          emit('delete-success');
        }
      } catch (e) {
        // 更详细的错误处理
        const errorMsg = e.response?.data?.message || e.message || '删除配置失败';
        createMessage.error(errorMsg);
      } finally {
        isDeleting.value = false;
        setFieldsValue({
          configurations: [],
        });
        closeModal();
      }
    } catch (e) {
      console.error('表单验证错误:', e);
    }
  }

  const emit = defineEmits(['delete-success']);
</script>

<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts">
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { computed, defineComponent, ref, unref } from 'vue';
  import { formSchema } from './data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    addAndUpdateConverterDeviceManager,
    findChipsByID,
    isIpExists,
  } from '/src/views/mineai/train/externalModelConverter/externalModelConverterDetailApi.tsx';

  export default defineComponent({
    name: 'DeviceModal',
    components: { BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const isUpdate = ref(true);
      const { createMessage } = useMessage();
      const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
        labelWidth: 120,
        schemas: formSchema,
        showActionButtonGroup: false,
      });
      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        await resetFields();
        setModalProps({ confirmLoading: true });
        isUpdate.value = !!data?.isUpdate;
        // 提取所有 chips 中的 chipType，并进行数组回显
        const allChipTypes = data.record?.chips?.map((chip) => chip.id) || [];
        if (unref(isUpdate)) {
          await setFieldsValue({
            ...data.record,
            chipType: allChipTypes,
          });
        }
        setModalProps({ confirmLoading: false });
      });
      const getTitle = computed(() => (!unref(isUpdate) ? '新增设备' : '编辑设备'));

      async function handleSubmit() {
        try {
          const values = await validate();
          if (!unref(isUpdate)) {
            const ipExists = await isIpExists(values.ip);
            if (ipExists) {
              createMessage.error('该IP已存在，请使用其他IP'); // 如果名称已存在，显示错误信息
              return;
            }
          }
          console.log(values);
          if (Array.isArray(values.chipType)) {
            values.chips = values.chipType.map((chip) => ({
              id: chip,
              chipType: findChipsByID(chip).chipType,
            }));
          }
          await addAndUpdateConverterDeviceManager(values);
          // await setFieldsValue(values);
          createMessage.success(isUpdate.value ? '更新成功！' : '新增成功！');
          emit('success', values);
          closeModal();
        } catch (error) {
          console.error('操作失败:', error);
          createMessage.error('操作失败，请重试！');
        }
      }

      return {
        handleSubmit,
        registerModal,
        registerForm,
        getTitle,
      };
    },
  });
</script>

<style scoped lang="less"></style>

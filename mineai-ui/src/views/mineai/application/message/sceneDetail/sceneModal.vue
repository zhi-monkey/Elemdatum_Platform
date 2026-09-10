<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts">
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { computed, defineComponent, ref, unref } from 'vue';
  import { formSchema } from './modelTypeData';
  export default defineComponent({
    name: 'SceneModal',
    components: { BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const isUpdate = ref(true);
      const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
        labelWidth: 90,
        schemas: formSchema,
        showActionButtonGroup: false,
      });
      // 注册模态框，并在打开时设置表单初始状态
      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        await resetFields();
        setModalProps({ confirmLoading: true });
        isUpdate.value = !!data?.isUpdate;
        if (unref(isUpdate)) {
          await setFieldsValue({
            ...data.record,
          });
        }
        setModalProps({ confirmLoading: false });
      });
      const getTitle = computed(() => (!unref(isUpdate) ? '新增场景' : '编辑场景'));

      // 提交表单
      const handleSubmit = async () => {
        // 验证表单
        const values = await validate();
        if (values) {
          // 表单验证通过后，向父组件提交表单数据
          emit('success', values, unref(isUpdate));
          closeModal(); // 关闭模态框
        }
      };

      return {
        registerModal,
        registerForm,
        getTitle,
        handleSubmit,
      };
    },
  });
</script>

<style scoped lang="less"></style>

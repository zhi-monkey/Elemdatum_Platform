<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
  <ConfigModal @register="registerConfigModal" @success="handleSuccess" />
</template>
<script lang="ts">
  import { defineComponent, ref, computed, unref } from 'vue';
  import { BasicModal, useModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { formSchema } from './scene.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import ConfigModal from './ConfigModal.vue';
  export default defineComponent({
    name: 'UserModal',
    components: { BasicModal, BasicForm, ConfigModal },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const isUpdate = ref(true);

      const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
        labelWidth: 100,
        schemas: formSchema,
        showActionButtonGroup: false,
      });
      const [registerConfigModal, { openModal: openConfigModal }] = useModal();
      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        await resetFields();

        setModalProps({ confirmLoading: false });
        isUpdate.value = !!data?.isUpdate;

        if (unref(isUpdate)) {
          await setFieldsValue({
            ...data.record,
          });
        }
      });

      const getTitle = computed(() => (!unref(isUpdate) ? '新增场景' : '编辑场景'));

      async function handleSubmit() {
        try {
          const values = await validate();

          setModalProps({ confirmLoading: true });
          const getUrl = computed(() =>
            !unref(isUpdate) ? 'scene/addScene' : 'scene/updateScene',
          );
          await maHttp
            .post(
              {
                url: getUrl.value,
                params: values,
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
            )
            .then((v) => {
              closeModal();
              emit('success');
              if (getUrl.value === 'scene/updateScene') {
                if (v === true) {
                  openConfigModal(true, {});
                }
              }
            });
        } finally {
          setModalProps({ confirmLoading: false });
        }
      }
      return { registerModal, registerForm, getTitle, handleSubmit, registerConfigModal };
    },
  });
</script>

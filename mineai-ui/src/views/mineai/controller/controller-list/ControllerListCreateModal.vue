<template>
  <BasicModal
    v-bind="$attrs"
    destroy-on-close
    @register="registerCreateModal"
    :title="getTitle"
    @ok="handleSubmit"
  >
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts">
  import { computed, defineComponent } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formSchema } from './tableData';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  export default defineComponent({
    name: 'UserModal',
    components: { BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const [registerForm, { validate }] = useForm({
        labelWidth: 100,
        schemas: formSchema,
        showActionButtonGroup: false,
      });
      const [registerCreateModal, { setModalProps, closeModal }] = useModalInner();

      const getTitle = computed(() => '新增算力控制器');

      async function handleSubmit() {
        try {
          const values = await validate();
          values.scene = { id: values.sceneName };
          setModalProps({ confirmLoading: true });
          const getUrl = computed(() => 'controller/addController');
          await maHttp.post(
            {
              url: getUrl.value,
              params: values,
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
          );
          console.log(values);
          closeModal();
          emit('success');
        } finally {
          setModalProps({ confirmLoading: false });
        }
      }

      return { registerCreateModal, registerForm, getTitle, handleSubmit };
    },
  });
</script>

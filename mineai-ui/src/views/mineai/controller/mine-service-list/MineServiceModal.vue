<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts">
  import { defineComponent, ref, computed, unref } from 'vue';
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
      const isUpdate = ref(true);

      let originValues;

      const [registerForm, { resetFields, setFieldsValue, validate, getFieldsValue }] = useForm({
        labelWidth: 100,
        schemas: formSchema,
        showActionButtonGroup: false,
      });

      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        await resetFields();

        if (data.isUpdate === true) {
          data.record['name'] = data.record.name;
        }
        setModalProps({ confirmLoading: false });
        isUpdate.value = !!data?.isUpdate;

        if (unref(isUpdate)) {
          await setFieldsValue({
            ...data.record,
          });
        }
        originValues = getFieldsValue();
      });

      const getTitle = computed(() => (!unref(isUpdate) ? '新增服务' : '编辑服务'));

      function isObjectValueEqual(a, b) {
        const aProps = Object.getOwnPropertyNames(a);
        const bProps = Object.getOwnPropertyNames(b);
        if (aProps.length != bProps.length) {
          return false;
        }
        for (let i = 0; i < aProps.length; i++) {
          const propName = aProps[i];
          if (a[propName] !== b[propName]) {
            return false;
          }
        }
        return true;
      }

      async function handleSubmit() {
        try {
          const nowValues = getFieldsValue();
          const flag = isObjectValueEqual(originValues, nowValues);
          if (flag) {
            closeModal();
            return;
          }
          const values = await validate();

          setModalProps({ confirmLoading: true });
          const getUrl = computed(() =>
            !unref(isUpdate) ? 'mineService/addMineService' : 'mineService/updateMineService',
          );
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
          closeModal();
          emit('success');
        } finally {
          setModalProps({ confirmLoading: false });
        }
      }

      return { registerModal, registerForm, getTitle, handleSubmit };
    },
  });
</script>

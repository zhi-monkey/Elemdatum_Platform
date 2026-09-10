<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm" class="myForm" />
  </BasicModal>
  <ConfigModal @register="registerConfigModal" />
</template>
<script lang="ts">
  import { defineComponent, ref, computed, unref } from 'vue';
  import { BasicModal, useModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formSchema } from '../model.data';
  import ConfigModal from '/@/views/mineai/controller/scene-list/ConfigModal.vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  export default defineComponent({
    name: 'UserModal',
    components: { BasicModal, BasicForm, ConfigModal },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const isUpdate = ref(true);
      const [registerForm, { resetFields, setFieldsValue, validate, updateSchema }] = useForm({
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
          let monitorType = data.record.monitorType;
          maHttp
            .get(
              {
                url: 'model/isBindWithMonitor',
                params: { modelId: data.record.id },
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then((v) => {
              if (v) {
                //如果有绑定监控设备则无法切换监控设备类型
                updateSchema({
                  field: 'monitorType',
                  label: '算法类型',
                  required: true,
                  component: 'RadioButtonGroup',
                  componentProps: {
                    options: [
                      { label: '可见光', value: 1, disabled: monitorType != 1 },
                      { label: '红外', value: 2, disabled: monitorType != 2 },
                      { label: '三维', value: 3, disabled: monitorType != 3 },
                    ],
                  },
                });
              } else {
                //没有绑定则可以切换
                updateSchema({
                  field: 'monitorType',
                  label: '算法类型',
                  required: true,
                  component: 'RadioButtonGroup',
                  componentProps: {
                    options: [
                      { label: '可见光', value: 1 },
                      { label: '红外', value: 2 },
                      { label: '三维', value: 3, disabled: monitorType != 3 },
                    ],
                  },
                });
              }
              return v;
            });
          // 不要直接赋值，使用深拷贝
          let modifiedData = JSON.parse(JSON.stringify(data.record));
          let modelClassification: string[] = [];
          modifiedData.modelClassification?.forEach((item) => {
            modelClassification.push(item.id);
          });
          modifiedData.modelClassification = modelClassification;
          await setFieldsValue({
            ...modifiedData,
          });
        } else {
          await updateSchema({
            field: 'monitorType',
            label: '算法类型',
            required: true,
            component: 'RadioButtonGroup',
            defaultValue: 1,
            componentProps: {
              options: [
                { label: '可见光', value: 1 },
                { label: '红外', value: 2 },
                { label: '三维', value: 3, disabled: true },
              ],
            },
          });
        }
      });

      const getTitle = computed(() => (!unref(isUpdate) ? '新增算法' : '编辑算法'));

      async function handleSubmit() {
        try {
          let values = await validate();
          let modelClassifications: { id: string }[] = [];
          values.modelClassification.forEach((item) => {
            modelClassifications.push({ id: item });
          });
          values.modelClassification = modelClassifications;
          setModalProps({ confirmLoading: true });
          const getUrl = computed(() =>
            !unref(isUpdate) ? 'model/addModel' : 'model/updateModel',
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
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then((v) => {
              closeModal();
              emit('success');
              if (getUrl.value === 'model/updateModel') {
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

<style scoped>
  .myForm >>> .ant-radio-button-wrapper-disabled {
    background-color: #292421;
    color: grey;
  }
</style>

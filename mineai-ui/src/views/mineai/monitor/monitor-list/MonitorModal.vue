<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm" class="myForm" />
  </BasicModal>
  <ConfigModal @register="registerConfigModal" @success="handleSuccess" />
</template>
<script lang="ts">
  import { defineComponent, ref, computed, unref } from 'vue';
  import { BasicModal, useModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formSchema } from './monitor.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import ConfigModal from '/@/views/mineai/controller/scene-list/ConfigModal.vue';
  export default defineComponent({
    name: 'UserModal',
    components: { BasicModal, BasicForm, ConfigModal },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const isUpdate = ref(true);
      let record;
      let monitorId = ref();
      let url = ref();
      let originValues;
      const [registerConfigModal, { openModal: openConfigModal }] = useModal();
      const [registerMonitorConfigModal] = useModal();
      const [
        registerForm,
        { resetFields, setFieldsValue, validate, getFieldsValue, updateSchema },
      ] = useForm({
        labelWidth: 120,
        schemas: formSchema,
        showActionButtonGroup: false,
      });
      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        await resetFields();
        record = data.record;
        if (data.isUpdate === true && data.record.scene !== null) {
          data.record['sceneName'] = data.record.scene.name;
        }
        setModalProps({ confirmLoading: false });
        isUpdate.value = !!data?.isUpdate;

        if (unref(isUpdate)) {
          let monitorType = data.record.monitorType;
          await updateSchema([
            {
              field: 'useCustomInfo',
              label: '平台自定义信息',
              component: 'RadioButtonGroup',
              required: true,
              componentProps: {
                options: [
                  { label: '停用', value: 0 },
                  { label: '启用', value: 1 },
                ],
              },
            },
          ]);
          maHttp
            .get(
              {
                url: 'monitorModelConfig/isBindWithModel',
                params: { monitorId: data.record.id },
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
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
          await setFieldsValue({
            ...data.record,
          });
        } else {
          await updateSchema([
            {
              field: 'useCustomInfo',
              label: '平台自定义信息',
              component: 'RadioButtonGroup',
              required: true,
              defaultValue: 0,
              componentProps: {
                options: [
                  { label: '停用', value: 0 },
                  { label: '启用', value: 1, disabled: true },
                ],
              },
            },
            {
              field: 'monitorType',
              label: '设备类型',
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
            },
          ]);
        }
        originValues = getFieldsValue();
      });

      const getTitle = computed(() => (!unref(isUpdate) ? '新增设备' : '编辑设备'));

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
          values.scene = { id: values.sceneName };
          if (isUpdate.value) {
            if (values.sceneName === undefined) {
              values.scene = null;
            } else if (record.scene != null && values.sceneName === record.scene.name) {
              values.scene = { id: record.scene.id };
            }
          }
          setModalProps({ confirmLoading: true });
          const getUrl = computed(() =>
            !unref(isUpdate) ? 'monitor/addMonitor' : 'monitor/updateMonitor',
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
              { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
            )
            .then((v) => {
              closeModal();
              emit('success');
              if (getUrl.value === 'monitor/updateMonitor') {
                if (v === true) {
                  url.value = record.protocolMetaData;
                  monitorId.value = record.id;
                  openConfigModal(true);
                }
              }
            });
        } finally {
          setModalProps({ confirmLoading: false });
        }
      }

      function handleSuccess() {
        emit('success');
      }
      return {
        registerModal,
        registerForm,
        getTitle,
        handleSubmit,
        registerMonitorConfigModal,
        monitorId,
        url,
        handleSuccess,
        registerConfigModal,
      };
    },
  });
</script>

<style scoped>
  .myForm >>> .ant-radio-button-wrapper-disabled {
    background-color: #292421;
    color: grey;
  }
</style>

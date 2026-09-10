<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="算法转换" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts">
  import { defineComponent, ref } from 'vue';
  import { formSchema } from './ModelConvert.data';
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { maHttp } from '/@/utils/http/axios';

  export default defineComponent({
    name: 'Modal',
    components: { BasicForm, BasicModal },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const [registerForm, { setFieldsValue, resetFields, validate, updateSchema }] = useForm({
        labelWidth: 140,
        schemas: formSchema,
        showActionButtonGroup: false,
      });
      let modelVersionId;

      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        modelVersionId = data.record.id;
        await resetFields();
        setModalProps({ confirmLoading: false });
        await setFieldsValue({
          image: data.record.url,
        });
        console.log(data.record);
        await updateSchema({
          field: 'job',
          label: '选择权重文件',
          component: 'ApiSelect',
          componentProps: {
            placeholder: '请选择权重文件',
            dropdownAlign: {
              overflow: {
                adjustY: false, // 关闭下拉框垂直位置自适应
              },
            },
            api: () =>
              maHttp
                .get(
                  {
                    url: 'modelJob/getJobByModelVersionId',
                    params: { id: data.record.id },
                    headers: {
                      // @ts-ignore
                      ignoreCancelToken: true,
                    },
                  },
                  { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
                )
                .then((v) => {
                  return v;
                }),
            labelField: 'description',
            valueField: 'name',
            immediate: false,
          },
          show: true,
        });
      });

      async function handleSubmit() {
        try {
          const values = await validate();
          let convertValues = {};
          let weightPath = ref();
          weightPath = values.job;
          await maHttp
            .get(
              {
                url: 'modelVersion/findModelVersionById',
                params: { id: modelVersionId },
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then((v) => {
              values.modelVersion = v;
              convertValues['modelVersion'] = v;
              // modelValues[]
            });

          convertValues['namespace'] = 'ai-platform-s2';
          convertValues['image'] = values.image;
          convertValues['job'] = values;
          convertValues['convertType'] = values.convertType;
          convertValues['weightPath'] = weightPath;
          convertValues['weightRootPath'] = 'weight';
          convertValues['gpuNum'] = '2';
          convertValues['cpuNum'] = values.cpus;
          convertValues['memoryNum'] = values.memory;
          convertValues['labels'] = values.labels;
          convertValues['controllerId'] = values.controllerId;
          convertValues['weightName'] = values.weightName;
          setModalProps({ confirmLoading: true });
          console.log(convertValues);
          // pt转engine arm64控制器 在板子上转换，其他在服务器上
          // todo: 未解决添加job后，轮询k8s查不到的问题。
          if (values.architecture === 'arm64' && values.convertType === 'engine') {
            convertValues['jobId'] = await maHttp.post(
              {
                url: 'modelConvert/addModelConvertJob',
                params: convertValues,
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            );
            await maHttp.post(
              {
                url: 'controllerEdge/modelConvert',
                params: convertValues,
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
            );
          } else {
            await maHttp.post(
              {
                url: 'modelConvert/addModelConvert',
                params: convertValues,
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            );
          }
          closeModal();
          emit('success');
        } finally {
          setModalProps({ confirmLoading: false });
        }
      }

      return {
        registerModal,
        registerForm,
        handleSubmit,
      };
    },
  });
</script>

<style scoped lang="less"></style>

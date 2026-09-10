<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="选择应用场景" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<script lang="ts" setup>
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { ref } from 'vue';

  const modelExploreId = ref(-1);
  const modelGenerationId = ref(-1);

  const [registerForm, { validate, resetFields }] = useForm({
    labelWidth: 100,
    schemas: [
      {
        field: 'modelClassification',
        label: '应用场景',
        component: 'ApiSelect',
        componentProps: {
          dropdownAlign: {
            overflow: {
              adjustY: false, // 关闭下拉框垂直位置自适应
            },
          },
          mode: 'multiple',
          api: async () => {
            return await maHttp.get(
              {
                url: 'modelClassifications',
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            );
          },
          labelField: 'classificationName',
          valueField: 'id',
          immediate: true,
        },
        required: true,
      },
    ],
    showActionButtonGroup: false,
  });

  const { createMessage } = useMessage();
  const [registerModal, { setModalProps, closeModal }] = useModalInner((data) => {
    setModalProps({ confirmLoading: true });
    resetFields();
    modelExploreId.value = data.modelExploreId;
    modelGenerationId.value = data.modelGenerationId;
    setModalProps({ confirmLoading: false });
  });

  async function handleSubmit() {
    const values = await validate();
    let modelClassifications: { id: string }[] = [];
    values.modelClassification.forEach((item) => {
      modelClassifications.push({ id: item });
    });
    setModalProps({ confirmLoading: true });
    await maHttp
      .post(
        {
          url: 'modelExplore/publishModel',
          params: {
            modelExploreId: modelExploreId.value,
            modelGenerationId: modelGenerationId.value,
            modelClassifications: modelClassifications,
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        closeModal();
        createMessage.success('发布成功！');
      })
      .finally(() => {
        setModalProps({ confirmLoading: false });
      });
  }
</script>
<style scoped lang="less"></style>

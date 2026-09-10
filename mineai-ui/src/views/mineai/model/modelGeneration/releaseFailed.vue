<template>
  <BasicModal v-bind="$attrs" title="模型对比" @register="registerModal" :width="500" :height="500">
    <div class="centered-container">
      <p style="font-size: 18px; margin-top: 50px"> {{ result }}</p>
    </div>
    <template #footer>
      <a-button type="default" class="custom-ok-button1" @click="handleCancel">取消</a-button>
      <a-button
        type="primary"
        class="custom-ok-button"
        @click="handleRelease"
        :disabled="result === '当前模型训练效果不如之前的模型，不允许发布'"
        >发布</a-button
      >
    </template>
  </BasicModal>
</template>

<script setup lang="ts">
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Button as AButton } from 'ant-design-vue';

  const result = ref('');
  const modelGenerationId = ref();
  const boolResult = ref(false);

  const { createMessage } = useMessage();
  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    modelGenerationId.value = data.record.id;
    setModalProps({ confirmLoading: false });
    result.value = data.result;
    boolResult.value = data.boolResult;
  });

  function handleCancel() {
    closeModal();
  }

  function handleRelease() {
    if (boolResult.value === true) {
      maHttp
        .get(
          {
            url: 'modelGeneration/askPublishModelGeneration',
            params: { modelGenerationId: modelGenerationId.value },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        )
        .then(() => {
          createMessage.success('申请发布成功！');
          closeModal();
        });
    } else {
      createMessage.error('申请发布失败！');
      closeModal();
    }
  }
</script>

<style scoped>
  .centered-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 50%;
  }
</style>

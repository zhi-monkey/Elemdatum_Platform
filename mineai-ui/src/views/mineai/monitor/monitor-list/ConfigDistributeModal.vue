<template>
  <BasicModal
    v-bind="$attrs"
    title="配置下发"
    width="400px"
    ok-text="是"
    :centered="true"
    :body-style="{ 'max-height': '100px' }"
    @ok="handleConfig"
    @register="registerConfigModal"
    :loading="loading"
  >
    <div style="text-align: center; margin-top: 10px; font-size: 25px"> 是否进行配置下发？ </div>
  </BasicModal>
</template>
<script lang="ts">
  import { defineComponent, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  export default defineComponent({
    components: { BasicModal },
    props: {
      data: { type: Object },
    },
    emits: ['success'],
    setup(_, { emit }) {
      let modelId = ref();

      const { createMessage } = useMessage();
      let loading = ref<Boolean>(false);
      const [registerConfigModal, { closeModal }] = useModalInner(async (data) => {
        modelId.value = data.modelId;
      });
      async function handleConfig() {
        try {
          loading.value = true;
          // 根据模型查找服务
          const mineserviceId = await maHttp.get(
            {
              url: 'mineService/findMineServiceIdByModelId',
              params: { modelId: modelId.value },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
          );
          // 根据服务查找边缘设备
          const controller = await maHttp.get(
            {
              url: 'controller/findControllerByMineServiceId',
              params: { mineServiceId: mineserviceId },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
          );
          // 边缘设备下发配置
          await maHttp.post(
            {
              url: 'controller/createJsonConfig',
              params: controller,
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
          );
          createMessage.success('配置下发成功');
          loading.value = false;
          closeModal();
        } catch (e) {
          createMessage.error('配置下发失败');
          loading.value = false;
          closeModal();
        }
      }

      return { registerConfigModal, handleConfig, modelId, loading };
    },
  });
</script>

<template>
  <BasicModal
    v-bind="$attrs"
    title="配置下发"
    width="400px"
    ok-text="确认"
    :centered="true"
    :body-style="{ 'max-height': '100px' }"
    @ok="handleConfig"
    @cancel="handleCancel"
    @register="registerModal"
    :loading="loading"
  >
    <div style="text-align: left; margin-top: 0; font-size: 17px">
      配置信息已修改，请确认是否下发。点击确认将进行自动配置下发，取消将还原修改。
    </div>
  </BasicModal>
</template>
<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { difference } from 'lodash-es';
  const props = defineProps<{
    changeModel: Function;
    diff: Function;
    onChange: Function;
    changeLoading: Function;
  }>();
  let modelIds = ref();
  let url = ref();
  let nextTargetKeys = ref();
  const { createMessage } = useMessage();
  let nowKeys = ref();
  let nowKeys2 = ref();
  let loading = ref<Boolean>(false);

  // const props = defineProps<{
  //   changeModel: Function;
  // }>();
  const [registerModal, { closeModal }] = useModalInner(async (data) => {
    console.log(data.modelIds);
    modelIds.value = data.modelIds;
    url.value = data.url;
    nextTargetKeys.value = data.nextTargetKeys;
  });

  function handleConfig() {
    loading.value = true;
    maHttp
      .post(
        {
          url: 'controller/createAllJsonConfig',
          params: { modelIds: modelIds.value },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
      )
      .then(() => {
        createMessage.success('配置下发成功！');
        loading.value = false;
        closeModal();
      })
      .catch(() => {
        createMessage.error('配置下发失败！');
        handleCancel();
        loading.value = false;
        closeModal();
      });
  }

  function handleCancel() {
    for (let item in nextTargetKeys.value) {
      nextTargetKeys.value[item] = Number(nextTargetKeys.value[item]);
    }
    nowKeys.value = difference(nextTargetKeys.value, modelIds.value);
    for (let item in nowKeys.value) {
      nowKeys.value[item] = String(nowKeys.value[item]);
    }
    nowKeys2.value = nowKeys.value.concat(modelIds.value);
    for (let item in nowKeys2.value) {
      nowKeys2.value[item] = String(nowKeys2.value[item]);
    }
    props.changeLoading();
    if (url.value === 'model/bindModel') {
      props.changeModel('model/unBindModel', modelIds.value, nowKeys.value, false);
    } else {
      props.changeModel('model/bindModel', modelIds.value, nowKeys2.value, false);
    }

    // maHttp
    //   .post(
    //     {
    //       url: 'model/unBindModel',
    //       params: {
    //         monitorId: monitorId,
    //         modelIds: modelIds,
    //       },
    //       headers: {
    //         // @ts-ignore
    //         ignoreCancelToken: true,
    //       },
    //     },
    //     { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    //   )
    //   .then(() => {
    //     closeModal();
    //   });
  }
</script>

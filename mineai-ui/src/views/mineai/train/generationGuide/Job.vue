<template>
  <div class="step">
    <div class="step-form">
      <a-input
        v-model:value="gName"
        style="width: 80%; background-color: #303030; font-size: 16px"
        readOnly
      />
      <a-button type="primary" @click="customSubmitFunc"> {{ jobTypeName }}</a-button></div
    >
    <Divider />
    <h3>...</h3>
    <p>......</p>
  </div>
</template>

<script setup lang="ts">
  import { Divider, Input as AInput, Button as AButton } from 'ant-design-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { onMounted, ref } from 'vue';

  const { createMessage } = useMessage();
  const emit = defineEmits(['next']);
  const url = ref('');
  const props = defineProps<{
    generationId: number;
    generationName: string;
    jobType: number;
  }>();
  const jobTypeName = ref('');
  const gName = ref('');
  const generationId = ref(0);

  async function customSubmitFunc() {
    try {
      maHttp
        .get(
          {
            url: url.value,
            params: { modelGenerationId: props.generationId },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        )
        .then(
          () => {
            createMessage.success('作业创建成功！');
            emit('next');
          },
          () => {
            createMessage.error('作业创建失败！');
          },
        );
    } catch (error) {
      console.log(error);
    }
  }

  onMounted(async () => {
    gName.value = props.generationName;
    generationId.value = props.generationId;
    if (props.jobType === 1) {
      jobTypeName.value = '训练';
      url.value = 'modelJob/trainGuideJob';
    } else {
      jobTypeName.value = '质检';
      url.value = 'modelJob/testJob';
    }
  });
</script>
<style scoped lang="less">
  .step {
    &-form {
      width: 580px;
      margin: 0 auto;
    }

    h3 {
      margin: 0 0 12px 10px;
      font-size: 16px;
      line-height: 32px;
    }

    h4 {
      margin: 0 0 4px 10px;
      font-size: 14px;
      line-height: 22px;
    }

    p {
      margin-left: 10px;
    }
  }
</style>
